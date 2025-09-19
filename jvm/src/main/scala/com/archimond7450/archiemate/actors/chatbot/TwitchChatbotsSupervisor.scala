package com.archimond7450.archiemate.actors.chatbot

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.repositories.sessions.TwitchUserSessionsRepository
import com.archimond7450.archiemate.actors.repositories.settings.{AutomaticMessagesSettingsRepository, BasicChatbotSettingsRepository, BuiltInCommandsSettingsRepository, CommandsSettingsRepository, OverlaysSettingsRepository, TimersSettingsRepository, VariablesSettingsRepository}
import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient.GetTokenUserFromTokenId
import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.http.ChannelSettings
import com.archimond7450.archiemate.http.ChannelSettings.{AutomaticMessagesSettings, BasicChatbotSettings, BuiltInCommandsSettings, CommandsSettings, OverlaysSettings, TimersSettings, VariablesSettings}
import com.archimond7450.archiemate.providers.{RandomProvider, TimeProvider}
import com.archimond7450.archiemate.twitch.api.TwitchApiResponse
import com.archimond7450.archiemate.twitch.api.TwitchApiResponse.GetTokenUser
import com.archimond7450.archiemate.twitch.irc.{IncomingMessageDecoder, OutgoingMessageEncoder}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.util.Timeout

import scala.util.{Failure, Success}

object TwitchChatbotsSupervisor {
  val actorName = "TwitchChatbotsSupervisor"

  sealed trait Command
  final case class Join(twitchRoomId: String) extends Command
  final case class Leave(twitchRoomId: String) extends Command
  final case class AuthorizationNeeded(twitchRoomId: String) extends Command
  final case class JoinOK(twitchRoomId: String) extends Command
  final case class NewChannelSettingsEvent(twitchRoomId: String, event: SettingsEvent) extends Command

  sealed trait SettingsEvent
  final case class BasicChatbotSettingsChanged(settings: BasicChatbotSettings) extends SettingsEvent
  final case class BuiltInCommandsSettingsChanged(settings: BuiltInCommandsSettings) extends SettingsEvent
  final case class CommandsSettingsChanged(settings: CommandsSettings) extends SettingsEvent
  final case class VariablesSettingsChanged(settings: VariablesSettings) extends SettingsEvent
  final case class TimersSettingsChanged(settings: TimersSettings) extends SettingsEvent
  final case class OverlaysSettingsChanged(settings: OverlaysSettings) extends SettingsEvent
  final case class AutomaticMessagesSettingsChanged(settings: AutomaticMessagesSettings) extends SettingsEvent

  def apply()(using
              mediator: ActorRef[ArchieMateMediator.Command],
              randomProvider: RandomProvider,
              timeProvider: TimeProvider,
              settings: Settings
  ): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx
    (new TwitchChatbotsSupervisor).operational()
  }
}

class TwitchChatbotsSupervisor(using
    private val ctx: ActorContext[TwitchChatbotsSupervisor.Command],
    private val mediator: ActorRef[ArchieMateMediator.Command],
    private val randomProvider: RandomProvider,
    private val timeProvider: TimeProvider,
    private val settings: Settings
) {
  given Timeout = settings.askTimeout

  given IncomingMessageDecoder = new IncomingMessageDecoder
  given OutgoingMessageEncoder = new OutgoingMessageEncoder

  import TwitchChatbotsSupervisor.*

  def operational(
      chatbots: Map[String, ActorRef[TwitchChatbot.Command]] = Map.empty,
      operationalChatbots: Map[String, ActorRef[TwitchChatbot.Command]] = Map.empty
  ): Behavior[TwitchChatbotsSupervisor.Command] = Behaviors.receiveAndLogMessage {
    case Join(twitchRoomId) =>
      operational(chatbots + (twitchRoomId -> spawnChatbotActor(twitchRoomId)), operationalChatbots)

    case Leave(twitchRoomId) =>
      chatbots(twitchRoomId) ! TwitchChatbot.Leave
      operational(chatbots - twitchRoomId, operationalChatbots - twitchRoomId)

    case AuthorizationNeeded(twitchRoomId) =>
      operational(chatbots - twitchRoomId, operationalChatbots - twitchRoomId)

    case JoinOK(twitchRoomId) =>
      val newOperationalChatbots = operationalChatbots + (twitchRoomId -> chatbots(twitchRoomId))
      operational(chatbots - twitchRoomId, newOperationalChatbots)

    case NewChannelSettingsEvent(twitchRoomId, event) =>
      val chatbotOption = chatbots.find(_._1 == twitchRoomId).orElse(operationalChatbots.find(_._1 == twitchRoomId))

      chatbotOption match {
        case Some((_, chatbot)) =>
          event match {
            case BasicChatbotSettingsChanged(settings) if settings.join => chatbot ! TwitchChatbot.NewBasicChatbotSettings(settings)
            case BasicChatbotSettingsChanged(settings) => ctx.self ! Leave(twitchRoomId)
            case BuiltInCommandsSettingsChanged(settings) => chatbot ! TwitchChatbot.NewBuiltInCommandsSettings(settings)
            case CommandsSettingsChanged(settings) => chatbot ! TwitchChatbot.NewCommandsSettings(settings)
            case VariablesSettingsChanged(settings) => chatbot ! TwitchChatbot.NewVariablesSettings(settings)
            case TimersSettingsChanged(settings) => chatbot ! TwitchChatbot.NewTimersSettings(settings)
            case OverlaysSettingsChanged(settings) => chatbot ! TwitchChatbot.NewOverlaysSettings(settings)
            case AutomaticMessagesSettingsChanged(settings) => chatbot ! TwitchChatbot.NewAutomaticMessagesSettings(settings)
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
  }

  private def spawnChatbotActor(
                                 twitchRoomId: String
                               ): ActorRef[TwitchChatbot.Command] = {
    given ActorRef[Command] = ctx.self
    ctx.spawn(
      TwitchChatbot(twitchRoomId),
      twitchRoomId
    )
  }
}
