package com.archimond7450.archiemate.actors.chatbot

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.repositories.sessions.{
  KickUserSessionsRepository,
  TwitchUserSessionsRepository
}
import com.archimond7450.archiemate.actors.repositories.settings.{
  AutomaticMessagesSettingsRepository,
  BasicChatbotSettingsRepository,
  BuiltInCommandsSettingsRepository,
  CommandsSettingsRepository,
  OverlaysSettingsRepository,
  TimersSettingsRepository,
  VariablesSettingsRepository
}
import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient.GetTokenUserFromTokenId
import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.http.ChannelSettings
import com.archimond7450.archiemate.http.ChannelSettings.{
  AutomaticMessagesSettings,
  BasicChatbotSettings,
  BuiltInCommandsSettings,
  CommandsSettings,
  OverlaysSettings,
  TimersSettings,
  VariablesSettings
}
import com.archimond7450.archiemate.http.Polls.ChannelPolls
import com.archimond7450.archiemate.http.Predictions.ChannelPredictions
import com.archimond7450.archiemate.kick.webhooks.KickWebhooks
import com.archimond7450.archiemate.providers.{RandomProvider, TimeProvider}
import com.archimond7450.archiemate.twitch.api.TwitchApiResponse
import com.archimond7450.archiemate.twitch.api.TwitchApiResponse.GetTokenUser
import com.archimond7450.archiemate.twitch.irc.{
  IncomingMessageDecoder,
  OutgoingMessageEncoder
}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import org.apache.pekko.util.Timeout

import scala.util.{Failure, Success}

object ChatbotsSupervisor {
  val actorName = "ChatbotsSupervisor"

  sealed trait Command
  final case class Join(twitchRoomId: String) extends Command
  final case class Leave(twitchRoomId: String) extends Command
  final case class AuthorizationNeeded(twitchRoomId: String) extends Command
  final case class JoinOK(twitchRoomId: String) extends Command
  final case class NewChannelSettingsEvent(
      twitchRoomId: String,
      event: SettingsEvent
  ) extends Command

  sealed trait SettingsEvent
  final case class BasicChatbotSettingsChanged(settings: BasicChatbotSettings)
      extends SettingsEvent
  final case class BuiltInCommandsSettingsChanged(
      settings: BuiltInCommandsSettings
  ) extends SettingsEvent
  final case class CommandsSettingsChanged(settings: CommandsSettings)
      extends SettingsEvent
  final case class VariablesSettingsChanged(settings: VariablesSettings)
      extends SettingsEvent
  final case class TimersSettingsChanged(settings: TimersSettings)
      extends SettingsEvent
  final case class OverlaysSettingsChanged(settings: OverlaysSettings)
      extends SettingsEvent
  final case class AutomaticMessagesSettingsChanged(
      settings: AutomaticMessagesSettings
  ) extends SettingsEvent
  final case class PollsChanged(polls: ChannelPolls) extends SettingsEvent
  final case class PredictionsChanged(predictions: ChannelPredictions)
      extends SettingsEvent

  final case class KickWebhookReceived(
      kickBroadcasterId: Int,
      webhook: KickWebhooks.KickWebhook
  ) extends Command
  private final case class KickWebhookWithTwitchUserId(
      twitchRoomIdOption: Option[String],
      webhook: KickWebhooks.KickWebhook
  ) extends Command

  def apply()(using
      mediator: ActorRef[ArchieMateMediator.Command],
      randomProvider: RandomProvider,
      timeProvider: TimeProvider,
      settings: Settings
  ): Behavior[Command] = Behaviors
    .supervise[Command] {
      Behaviors.setup { ctx =>
        given ActorContext[Command] = ctx

        (new ChatbotsSupervisor).operational()
      }
    }
    .onFailure[Throwable](SupervisorStrategy.resume)
}

class ChatbotsSupervisor(using
    private val ctx: ActorContext[ChatbotsSupervisor.Command],
    private val mediator: ActorRef[ArchieMateMediator.Command],
    private val randomProvider: RandomProvider,
    private val timeProvider: TimeProvider,
    private val settings: Settings
) {
  given Timeout = settings.askTimeout

  given IncomingMessageDecoder = new IncomingMessageDecoder
  given OutgoingMessageEncoder = new OutgoingMessageEncoder

  import ChatbotsSupervisor.*

  def operational(
      chatbots: Map[String, ActorRef[Chatbot.Command]] = Map.empty,
      operationalChatbots: Map[String, ActorRef[Chatbot.Command]] = Map.empty
  ): Behavior[ChatbotsSupervisor.Command] =
    Behaviors.receiveAndLogMessage {
      case Join(twitchRoomId) =>
        operational(
          chatbots + (twitchRoomId -> spawnChatbotActor(twitchRoomId)),
          operationalChatbots
        )

      case Leave(twitchRoomId) =>
        chatbots(twitchRoomId) ! Chatbot.Leave
        operational(chatbots - twitchRoomId, operationalChatbots - twitchRoomId)

      case AuthorizationNeeded(twitchRoomId) =>
        operational(chatbots - twitchRoomId, operationalChatbots - twitchRoomId)

      case JoinOK(twitchRoomId) =>
        val newOperationalChatbots =
          operationalChatbots + (twitchRoomId -> chatbots(twitchRoomId))
        operational(chatbots - twitchRoomId, newOperationalChatbots)

      case NewChannelSettingsEvent(twitchRoomId, event) =>
        val chatbotOption = chatbots
          .find(_._1 == twitchRoomId)
          .orElse(operationalChatbots.find(_._1 == twitchRoomId))

        chatbotOption match {
          case Some((_, chatbot)) =>
            event match {
              case BasicChatbotSettingsChanged(settings) if settings.join =>
                chatbot ! Chatbot.NewBasicChatbotSettings(settings)
              case BasicChatbotSettingsChanged(settings) =>
                ctx.self ! Leave(twitchRoomId)
              case BuiltInCommandsSettingsChanged(settings) =>
                chatbot ! Chatbot.NewBuiltInCommandsSettings(settings)
              case CommandsSettingsChanged(settings) =>
                chatbot ! Chatbot.NewCommandsSettings(settings)
              case VariablesSettingsChanged(settings) =>
                chatbot ! Chatbot.NewVariablesSettings(settings)
              case TimersSettingsChanged(settings) =>
                chatbot ! Chatbot.NewTimersSettings(settings)
              case OverlaysSettingsChanged(settings) =>
                chatbot ! Chatbot.NewOverlaysSettings(settings)
              case AutomaticMessagesSettingsChanged(settings) =>
                chatbot ! Chatbot.NewAutomaticMessagesSettings(settings)
              case PollsChanged(polls) =>
                chatbot ! Chatbot.NewPolls(polls)
              case PredictionsChanged(predictions) =>
                chatbot ! Chatbot.NewPredictions(predictions)
            }

          case None =>
            event match {
              case BasicChatbotSettingsChanged(settings) =>
                if (settings.join) {
                  ctx.self ! Join(twitchRoomId)
                }

              case _ =>
            }
        }
        Behaviors.same

      case KickWebhookReceived(kickBroadcasterId, webhook) =>
        ctx.ask[
          ArchieMateMediator.Command,
          KickUserSessionsRepository.ReturnedTwitchUserIdForKickUserId
        ](
          mediator,
          ref =>
            ArchieMateMediator.SendKickUserSessionsRepositoryCommand(
              KickUserSessionsRepository
                .GetTwitchUserIdForKickUserId(ref, kickBroadcasterId)
            )
        ) {
          case Success(KickUserSessionsRepository.ReturnedTwitchUserIdForKickUserId(maybeTwitchUserId)) =>
            KickWebhookWithTwitchUserId(maybeTwitchUserId, webhook)
          case Failure(ex) =>
            ctx.log.error(
              "There was an exception while waiting for kick user sessions repository to return twitch user id for kick user id.",
              ex
            )
            KickWebhookWithTwitchUserId(None, webhook)
        }
        Behaviors.same

      case KickWebhookWithTwitchUserId(Some(twitchUserId), webhook) =>
        chatbots.get(twitchUserId).orElse(operationalChatbots.get(twitchUserId)) match {
          case Some(chatbot) =>
            chatbot ! Chatbot.NewKickWebhook(webhook)
          case None =>
            ctx.log.warn("Received webhook {} for non joined channel.")
        }
        Behaviors.same
    }

  private def spawnChatbotActor(
      twitchRoomId: String
  ): ActorRef[Chatbot.Command] = {
    given ActorRef[Command] = ctx.self
    ctx.spawn(
      Chatbot(twitchRoomId),
      twitchRoomId
    )
  }
}
