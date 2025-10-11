package com.archimond7450.archiemate.actors.services.controllerhelpers

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.repositories.sessions.{TwitchUserSessionsRepository, YouTubeChannelSessionsRepository}
import com.archimond7450.archiemate.actors.repositories.settings.*
import com.archimond7450.archiemate.actors.services.JWTService
import com.archimond7450.archiemate.actors.youtube.api.YouTubeApiClient
import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.http.ChannelSettings.*
import com.archimond7450.archiemate.youtube.api.YouTubeApiResponse
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.util.Timeout

import scala.util.{Failure, Success, Try}

object SettingsControllerHelperService {
  val actorName = "SettingsControllerHelperService"

  sealed trait Command
  sealed trait PublicCommand extends Command

  final case class GetConnections(replyTo: ActorRef[GetConnectionsResponse], jwt: String) extends PublicCommand
  private final case class TwitchConnectionReceived(originalCommand: GetConnections, userId: String, tryTwitchConnected: Try[Boolean]) extends Command
  private final case class GetYouTubeConnections(twitchConnection: TwitchConnectionReceived, tokenIds: List[String], receivedChannels: Map[String, Try[YouTubeApiResponse.Response[YouTubeApiResponse.Channel]]] = Map.empty) extends Command
  final case class GetBasicChatbotSettings(replyTo: ActorRef[GetBasicChatbotSettingsResponse], jwt: String) extends PublicCommand
  final case class GetBuiltInCommandsSettings(replyTo: ActorRef[GetBuiltInCommandsSettingsResponse], jwt: String) extends PublicCommand
  final case class GetCommandsSettings(replyTo: ActorRef[GetCommandsSettingsResponse], jwt: String) extends PublicCommand
  final case class GetVariablesSettings(replyTo: ActorRef[GetVariablesSettingsResponse], jwt: String) extends PublicCommand
  final case class GetTimersSettings(replyTo: ActorRef[GetTimersSettingsResponse], jwt: String) extends PublicCommand
  final case class GetOverlaysSettings(replyTo: ActorRef[GetOverlaysSettingsResponse], jwt: String) extends PublicCommand
  final case class GetAutomaticMessagesSettings(replyTo: ActorRef[GetAutomaticMessagesSettingsResponse], jwt: String) extends PublicCommand
  private final case class DecodeJWT(originalCommand: PublicCommand) extends Command
  private final case class PublicCommandWithUserId(originalCommand: PublicCommand, userId: String) extends Command
  private final case class SendReply[T](replyTo: ActorRef[T], response: T) extends Command
  final case class ChangeBasicChatbotSettings(replyTo: ActorRef[ChangeSettingsResponse], jwt: String, settings: BasicChatbotSettings) extends PublicCommand
  final case class ChangeBuiltInChatbotSettings(replyTo: ActorRef[ChangeSettingsResponse], jwt: String, settings: BuiltInCommandsSettings) extends PublicCommand
  final case class ChangeCommandsSettings(replyTo: ActorRef[ChangeSettingsResponse], jwt: String, settings: CommandsSettings) extends PublicCommand
  final case class ChangeVariablesSettings(replyTo: ActorRef[ChangeSettingsResponse], jwt: String, settings: VariablesSettings) extends PublicCommand
  final case class ChangeTimersSettings(replyTo: ActorRef[ChangeSettingsResponse], jwt: String, settings: TimersSettings) extends PublicCommand
  final case class ChangeOverlaysSettings(replyTo: ActorRef[ChangeSettingsResponse], jwt: String, settings: OverlaysSettings) extends PublicCommand
  final case class ChangeAutomaticMessagesSettings(replyTo: ActorRef[ChangeSettingsResponse], jwt: String, settings: AutomaticMessagesSettings) extends PublicCommand

  sealed trait GetConnectionsResponse
  final case class GetConnectionsOKResponse(tryTwitchConnected: Try[Boolean], tryYouTubeConnections: Try[List[Try[YouTubeConnection]]]) extends GetConnectionsResponse
  final case class YouTubeConnection(channelId: String, channelName: String, channelProfileImageUrl: String)
  sealed trait GetBasicChatbotSettingsResponse
  final case class GetBasicChatbotSettingsOKResponse(trySettings: Try[BasicChatbotSettings]) extends GetBasicChatbotSettingsResponse
  sealed trait GetBuiltInCommandsSettingsResponse
  final case class GetBuiltInCommandsSettingsOKResponse(trySettings: Try[BuiltInCommandsSettings]) extends GetBuiltInCommandsSettingsResponse
  sealed trait GetCommandsSettingsResponse
  final case class GetCommandsSettingsOKResponse(trySettings: Try[CommandsSettings]) extends GetCommandsSettingsResponse
  sealed trait GetVariablesSettingsResponse
  final case class GetVariablesSettingsOKResponse(trySettings: Try[VariablesSettings]) extends GetVariablesSettingsResponse
  sealed trait GetTimersSettingsResponse
  final case class GetTimersSettingsOKResponse(trySettings: Try[TimersSettings]) extends GetTimersSettingsResponse
  sealed trait GetOverlaysSettingsResponse
  final case class GetOverlaysSettingsOKResponse(trySettings: Try[OverlaysSettings]) extends GetOverlaysSettingsResponse
  sealed trait GetAutomaticMessagesSettingsResponse
  final case class GetAutomaticMessagesSettingsOKResponse(trySettings: Try[AutomaticMessagesSettings]) extends GetAutomaticMessagesSettingsResponse
  sealed trait ChangeSettingsResponse
  case object SettingsChanged extends ChangeSettingsResponse
  final case class SettingsFailedToChange(cause: Throwable) extends ChangeSettingsResponse
  case object InvalidJWT
    extends GetConnectionsResponse
      with GetBasicChatbotSettingsResponse
      with GetBuiltInCommandsSettingsResponse
      with GetCommandsSettingsResponse
      with GetVariablesSettingsResponse
      with GetTimersSettingsResponse
      with GetOverlaysSettingsResponse
      with GetAutomaticMessagesSettingsResponse
      with ChangeSettingsResponse

  def apply()(using mediator: ActorRef[ArchieMateMediator.Command], timeout: Timeout): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx
    Behaviors.receiveAndLogMessage {
      case cmd: GetConnections =>
        ctx.log.debug("GetConnections received: {}", cmd)
        ctx.self ! DecodeJWT(cmd)
        Behaviors.same

      case cmd: TwitchConnectionReceived =>
        ctx.log.debug("TwitchConnectionReceived received: {}", cmd)
        ctx.ask[ArchieMateMediator.Command, YouTubeChannelSessionsRepository.ReturnedTokenIdsForUserId](mediator, ref => ArchieMateMediator.SendYouTubeChannelSessionsRepositoryCommand(YouTubeChannelSessionsRepository.GetTokenIdsForUserId(ref, cmd.userId))) {
          case Success(YouTubeChannelSessionsRepository.ReturnedTokenIdsForUserId(userId, tokenIds)) =>
            GetYouTubeConnections(cmd, tokenIds)
          case Failure(ex) =>
            SendReply(cmd.originalCommand.replyTo, GetConnectionsOKResponse(cmd.tryTwitchConnected, Failure(ex)))
        }
        Behaviors.same

      case cmd @ GetYouTubeConnections(twitchConnection, Nil, receivedChannels) =>
        ctx.log.debug("Last GetYouTubeConnections received: {}", cmd)
        ctx.self ! SendReply(
          twitchConnection.originalCommand.replyTo,
          GetConnectionsOKResponse(
            twitchConnection.tryTwitchConnected,
            Try(
              receivedChannels.toList.map(
                channelMap => channelMap._2.map(channel => {
                  val channelItem = channel.items.last
                  YouTubeConnection(channelItem.id, channelItem.snippet.title, channelItem.snippet.thumbnails("default").url)
                })
              )
            )
          )
        )
        Behaviors.same

      case cmd @ GetYouTubeConnections(twitchConnection, tokenIds, receivedChannels) =>
        ctx.log.debug("GetYouTubeConnections received: {}", cmd)
        ctx.askWithStatus[ArchieMateMediator.Command, YouTubeApiResponse.Response[YouTubeApiResponse.Channel]](mediator, ref => ArchieMateMediator.SendYouTubeApiClientCommand(YouTubeApiClient.GetChannelFromTokenId(ref, tokenIds.last))) {
          case Success(response) => cmd.copy(tokenIds = tokenIds.take(tokenIds.length - 1), receivedChannels = receivedChannels + (tokenIds.last -> Success(response)))
          case Failure(ex) => cmd.copy(tokenIds = tokenIds.take(tokenIds.length - 1), receivedChannels = receivedChannels + (tokenIds.last -> Failure(ex)))
        }
        Behaviors.same

      case cmd: GetBasicChatbotSettings =>
        ctx.log.debug("GetBasicChatbotSettings received: {}", cmd)
        ctx.self ! DecodeJWT(cmd)
        Behaviors.same

      case cmd: GetBuiltInCommandsSettings =>
        ctx.log.debug("GetBuiltInCommandsSettings received: {}", cmd)
        ctx.self ! DecodeJWT(cmd)
        Behaviors.same

      case cmd: GetCommandsSettings =>
        ctx.log.debug("GetCommandsSettings received: {}", cmd)
        ctx.self ! DecodeJWT(cmd)
        Behaviors.same

      case cmd: GetVariablesSettings =>
        ctx.log.debug("GetVariablesSettings received: {}", cmd)
        ctx.self ! DecodeJWT(cmd)
        Behaviors.same

      case cmd: GetTimersSettings =>
        ctx.log.debug("GetTimersSettings received: {}", cmd)
        ctx.self ! DecodeJWT(cmd)
        Behaviors.same

      case cmd: GetOverlaysSettings =>
        ctx.log.debug("GetOverlaysSettings received: {}", cmd)
        ctx.self ! DecodeJWT(cmd)
        Behaviors.same

      case cmd: GetAutomaticMessagesSettings =>
        ctx.log.debug("GetAutomaticMessagesSettings received: {}", cmd)
        ctx.self ! DecodeJWT(cmd)
        Behaviors.same

      case cmd @ DecodeJWT(originalCommand) =>
        ctx.log.debug("DecodeJWT received: {}", cmd)
        val (replyTo, jwt) = originalCommand match {
          case GetConnections(replyTo, jwt) => (replyTo, jwt)
          case GetBasicChatbotSettings(replyTo, jwt) => (replyTo, jwt)
          case GetBuiltInCommandsSettings(replyTo, jwt) => (replyTo, jwt)
          case GetCommandsSettings(replyTo, jwt) => (replyTo, jwt)
          case GetVariablesSettings(replyTo, jwt) => (replyTo, jwt)
          case GetTimersSettings(replyTo, jwt) => (replyTo, jwt)
          case GetOverlaysSettings(replyTo, jwt) => (replyTo, jwt)
          case GetAutomaticMessagesSettings(replyTo, jwt) => (replyTo, jwt)
          case ChangeBasicChatbotSettings(replyTo, jwt, _) => (replyTo, jwt)
          case ChangeBuiltInChatbotSettings(replyTo, jwt, _) => (replyTo, jwt)
          case ChangeCommandsSettings(replyTo, jwt, _) => (replyTo, jwt)
          case ChangeVariablesSettings(replyTo, jwt, _) => (replyTo, jwt)
          case ChangeTimersSettings(replyTo, jwt, _) => (replyTo, jwt)
          case ChangeOverlaysSettings(replyTo, jwt, _) => (replyTo, jwt)
          case ChangeAutomaticMessagesSettings(replyTo, jwt, _) => (replyTo, jwt)
        }
        ctx.ask[ArchieMateMediator.Command, JWTService.DecodeJWTResponse](mediator, ref => ArchieMateMediator.SendJWTServiceCommand(JWTService.DecodeJWT(ref, jwt))) {
          case Success(JWTService.DecodedJWT(userId, _)) =>
            PublicCommandWithUserId(originalCommand, userId)

          case Success(JWTService.InvalidJWT) =>
            SendReply(replyTo, InvalidJWT)

          case Failure(ex) =>
            sendErrorResponse(ctx, originalCommand, ex)
        }
        Behaviors.same

      case publicCmd @ PublicCommandWithUserId(originalCommand, userId) =>
        ctx.log.debug("PublicCommandWithUserId received: {}", publicCmd)
        originalCommand match {
          case cmd@GetConnections(replyTo, _) =>
            ctx.ask[ArchieMateMediator.Command, TwitchUserSessionsRepository.ReturnedTokenIdForUserId](mediator, ref => ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand(TwitchUserSessionsRepository.GetTokenIdForUserId(ref, userId))) {
              case Success(TwitchUserSessionsRepository.ReturnedTokenIdForUserId(maybeTokenId)) => TwitchConnectionReceived(cmd, userId, Success(maybeTokenId.nonEmpty))
              case Failure(ex) => TwitchConnectionReceived(cmd, userId, Failure(ex))
            }
            Behaviors.same

          case GetBasicChatbotSettings(replyTo, _) =>
            ctx.ask[ArchieMateMediator.Command, BasicChatbotSettings](mediator, ref => ArchieMateMediator.SendBasicChatbotSettingsRepositoryCommand(BasicChatbotSettingsRepository.GetSettings(ref, userId))) { trySettings =>
              SendReply(replyTo, GetBasicChatbotSettingsOKResponse(trySettings))
            }
            Behaviors.same

          case GetBuiltInCommandsSettings(replyTo, _) =>
            ctx.ask[ArchieMateMediator.Command, BuiltInCommandsSettings](mediator, ref => ArchieMateMediator.SendBuiltInCommandsSettingsRepositoryCommand(BuiltInCommandsSettingsRepository.GetSettings(ref, userId))) { trySettings =>
              SendReply(replyTo, GetBuiltInCommandsSettingsOKResponse(trySettings))
            }
            Behaviors.same

          case GetCommandsSettings(replyTo, _) =>
            ctx.ask[ArchieMateMediator.Command, CommandsSettings](mediator, ref => ArchieMateMediator.SendCommandsSettingsRepositoryCommand(CommandsSettingsRepository.GetCommandsSettings(ref, userId))) { trySettings =>
              SendReply(replyTo, GetCommandsSettingsOKResponse(trySettings))
            }
            Behaviors.same

          case GetVariablesSettings(replyTo, _) =>
            ctx.ask[ArchieMateMediator.Command, VariablesSettings](mediator, ref => ArchieMateMediator.SendVariablesSettingsRepositoryCommand(VariablesSettingsRepository.GetVariablesSettings(ref, userId))) { trySettings =>
              SendReply(replyTo, GetVariablesSettingsOKResponse(trySettings))
            }
            Behaviors.same

          case GetTimersSettings(replyTo, _) =>
            ctx.ask[ArchieMateMediator.Command, TimersSettings](mediator, ref => ArchieMateMediator.SendTimersSettingsRepositoryCommand(TimersSettingsRepository.GetSettings(ref, userId))) { trySettings =>
              SendReply(replyTo, GetTimersSettingsOKResponse(trySettings))
            }
            Behaviors.same

          case GetOverlaysSettings(replyTo, _) =>
            ctx.ask[ArchieMateMediator.Command, OverlaysSettings](mediator, ref => ArchieMateMediator.SendOverlaysSettingsRepositoryCommand(OverlaysSettingsRepository.GetSettings(ref, userId))) { trySettings =>
              SendReply(replyTo, GetOverlaysSettingsOKResponse(trySettings))
            }
            Behaviors.same

          case GetAutomaticMessagesSettings(replyTo, _) =>
            ctx.ask[ArchieMateMediator.Command, AutomaticMessagesSettings](mediator, ref => ArchieMateMediator.SendAutomaticMessagesSettingsRepositoryCommand(AutomaticMessagesSettingsRepository.GetSettings(ref, userId))) { trySettings =>
              SendReply(replyTo, GetAutomaticMessagesSettingsOKResponse(trySettings))
            }
            Behaviors.same

          case ChangeBasicChatbotSettings(replyTo, _, settings) =>
            ctx.ask[ArchieMateMediator.Command, BasicChatbotSettingsRepository.Acknowledged.type](mediator, ref => ArchieMateMediator.SendBasicChatbotSettingsRepositoryCommand(BasicChatbotSettingsRepository.ChangeSettings(ref, userId, settings))) {
              case Success(BasicChatbotSettingsRepository.Acknowledged) =>
                SendReply(replyTo, SettingsChanged)

              case Failure(ex) =>
                SendReply(replyTo, SettingsFailedToChange(ex))
            }
            Behaviors.same

          case ChangeBuiltInChatbotSettings(replyTo, _, settings) =>
            ctx.ask[ArchieMateMediator.Command, BuiltInCommandsSettingsRepository.Acknowledged.type](mediator, ref => ArchieMateMediator.SendBuiltInCommandsSettingsRepositoryCommand(BuiltInCommandsSettingsRepository.ChangeSettings(ref, userId, settings))) {
              case Success(BuiltInCommandsSettingsRepository.Acknowledged) =>
                SendReply(replyTo, SettingsChanged)

              case Failure(ex) =>
                SendReply(replyTo, SettingsFailedToChange(ex))
            }
            Behaviors.same

          case ChangeCommandsSettings(replyTo, _, settings) =>
            ctx.ask[ArchieMateMediator.Command, CommandsSettingsRepository.Acknowledged.type](mediator, ref => ArchieMateMediator.SendCommandsSettingsRepositoryCommand(CommandsSettingsRepository.SetCommandsSettings(ref, userId, settings))) {
              case Success(CommandsSettingsRepository.Acknowledged) =>
                SendReply(replyTo, SettingsChanged)

              case Failure(ex) =>
                SendReply(replyTo, SettingsFailedToChange(ex))
            }
            Behaviors.same

          case ChangeVariablesSettings(replyTo, _, settings) =>
            ctx.ask[ArchieMateMediator.Command, VariablesSettingsRepository.Acknowledged.type](mediator, ref => ArchieMateMediator.SendVariablesSettingsRepositoryCommand(VariablesSettingsRepository.SetVariablesSettings(ref, userId, settings))) {
              case Success(VariablesSettingsRepository.Acknowledged) =>
                SendReply(replyTo, SettingsChanged)

              case Failure(ex) =>
                SendReply(replyTo, SettingsFailedToChange(ex))
            }
            Behaviors.same

          case ChangeTimersSettings(replyTo, _, settings) =>
            ctx.ask[ArchieMateMediator.Command, TimersSettingsRepository.Acknowledged.type](mediator, ref => ArchieMateMediator.SendTimersSettingsRepositoryCommand(TimersSettingsRepository.ChangeSettings(ref, userId, settings))) {
              case Success(TimersSettingsRepository.Acknowledged) =>
                SendReply(replyTo, SettingsChanged)

              case Failure(ex) =>
                SendReply(replyTo, SettingsFailedToChange(ex))
            }
            Behaviors.same

          case ChangeOverlaysSettings(replyTo, _, settings) =>
            ctx.ask[ArchieMateMediator.Command, OverlaysSettingsRepository.Acknowledged.type](mediator, ref => ArchieMateMediator.SendOverlaysSettingsRepositoryCommand(OverlaysSettingsRepository.ChangeSettings(ref, userId, settings))) {
              case Success(OverlaysSettingsRepository.Acknowledged) =>
                SendReply(replyTo, SettingsChanged)

              case Failure(ex) =>
                SendReply(replyTo, SettingsFailedToChange(ex))
            }
            Behaviors.same

          case ChangeAutomaticMessagesSettings(replyTo, _, settings) =>
            ctx.ask[ArchieMateMediator.Command, AutomaticMessagesSettingsRepository.Acknowledged.type](mediator, ref => ArchieMateMediator.SendAutomaticMessagesSettingsRepositoryCommand(AutomaticMessagesSettingsRepository.ChangeSettings(ref, userId, settings))) {
              case Success(AutomaticMessagesSettingsRepository.Acknowledged) =>
                SendReply(replyTo, SettingsChanged)

              case Failure(ex) =>
                SendReply(replyTo, SettingsFailedToChange(ex))
            }
            Behaviors.same
        }

      case cmd @ SendReply(replyTo, response) =>
        ctx.log.debug("SendReply received: {}", cmd)
        replyTo ! response
        Behaviors.same

      case cmd: ChangeBasicChatbotSettings =>
        ctx.log.debug("ChangeChatbotSettings received: {}", cmd)
        ctx.self ! DecodeJWT(cmd)
        Behaviors.same

      case cmd: ChangeBuiltInChatbotSettings =>
        ctx.log.debug("ChangeBuiltInChatbotSettings received: {}", cmd)
        ctx.self ! DecodeJWT(cmd)
        Behaviors.same

      case cmd: ChangeCommandsSettings =>
        ctx.log.debug("ChangeCommandsSettings received: {}", cmd)
        ctx.self ! DecodeJWT(cmd)
        Behaviors.same

      case cmd: ChangeVariablesSettings =>
        ctx.log.debug("ChangeVariablesSettings received: {}", cmd)
        ctx.self ! DecodeJWT(cmd)
        Behaviors.same

      case cmd: ChangeTimersSettings =>
        ctx.log.debug("ChangeTimersSettings received: {}", cmd)
        ctx.self ! DecodeJWT(cmd)
        Behaviors.same

      case cmd: ChangeOverlaysSettings =>
        ctx.log.debug("ChangeOverlaysSettings received: {}", cmd)
        ctx.self ! DecodeJWT(cmd)
        Behaviors.same

      case cmd: ChangeAutomaticMessagesSettings =>
        ctx.log.debug("ChangeAutomaticMessagesSettings received: {}", cmd)
        ctx.self ! DecodeJWT(cmd)
        Behaviors.same
    }
  }

  private def sendErrorResponse(ctx: ActorContext[SettingsControllerHelperService.Command], cmd: PublicCommand, ex: Throwable): SendReply[_] = cmd match {
    case GetConnections(replyTo, _) => SendReply(replyTo, GetConnectionsOKResponse(Failure(ex), Failure(ex)))
    case GetBasicChatbotSettings(replyTo, _) => SendReply(replyTo, GetBasicChatbotSettingsOKResponse(Failure(ex)))
    case GetBuiltInCommandsSettings(replyTo, _) => SendReply(replyTo, GetBuiltInCommandsSettingsOKResponse(Failure(ex)))
    case GetCommandsSettings(replyTo, _) => SendReply(replyTo, GetCommandsSettingsOKResponse(Failure(ex)))
    case GetVariablesSettings(replyTo, _) => SendReply(replyTo, GetVariablesSettingsOKResponse(Failure(ex)))
    case GetTimersSettings(replyTo, _) => SendReply(replyTo, GetTimersSettingsOKResponse(Failure(ex)))
    case GetOverlaysSettings(replyTo, _) => SendReply(replyTo, GetOverlaysSettingsOKResponse(Failure(ex)))
    case GetAutomaticMessagesSettings(replyTo, _) => SendReply(replyTo, GetAutomaticMessagesSettingsOKResponse(Failure(ex)))
    case ChangeBasicChatbotSettings(replyTo, _, _) => SendReply(replyTo, SettingsFailedToChange(ex))
    case ChangeBuiltInChatbotSettings(replyTo, _, _) => SendReply(replyTo, SettingsFailedToChange(ex))
    case ChangeCommandsSettings(replyTo, _, _) => SendReply(replyTo, SettingsFailedToChange(ex))
    case ChangeVariablesSettings(replyTo, _, _) => SendReply(replyTo, SettingsFailedToChange(ex))
    case ChangeTimersSettings(replyTo, _, _) => SendReply(replyTo, SettingsFailedToChange(ex))
    case ChangeOverlaysSettings(replyTo, _, _) => SendReply(replyTo, SettingsFailedToChange(ex))
    case ChangeAutomaticMessagesSettings(replyTo, _, _) => SendReply(replyTo, SettingsFailedToChange(ex))
  }
}
