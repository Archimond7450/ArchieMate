package com.archimond7450.archiemate.actors.services.controllerhelpers

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.repositories.settings.CommandsSettingsRepository
import com.archimond7450.archiemate.actors.services.caches.TwitchTokenUserCacheService
import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.http.ChannelSettings.CommandsSettings
import com.archimond7450.archiemate.twitch.api.TwitchApiResponse
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.util.Timeout

import scala.util.{Failure, Success}

object CommandsControllerHelperService {
  val actorName = "CommandsControllerHelperService"

  sealed trait Command
  final case class GetCommandsForChannelName(replyTo: ActorRef[CommandsSettings], twitchChannelName: String) extends Command
  private final case class GetCommandsForTwitchRoomId(replyTo: ActorRef[CommandsSettings], twitchRoomId: String) extends Command
  private final case class Reply(replyTo: ActorRef[CommandsSettings], reply: CommandsSettings) extends Command

  def apply()(using mediator: ActorRef[ArchieMateMediator.Command], timeout: Timeout): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx
    Behaviors.receiveAndLogMessage {
      case GetCommandsForChannelName(replyTo, twitchChannelName) =>
        ctx.ask[ArchieMateMediator.Command, Option[TwitchApiResponse.GetTokenUser]](mediator, ref => ArchieMateMediator.SendTwitchTokenUserCacheServiceCommand(TwitchTokenUserCacheService.GetTokenUserFromUserName(ref, twitchChannelName))) {
          case Success(Some(tokenUser)) =>
            GetCommandsForTwitchRoomId(replyTo, tokenUser.id)

          case Success(None) =>
            Reply(replyTo, CommandsSettings())

          case Failure(ex) =>
            Reply(replyTo, CommandsSettings())
        }
        Behaviors.same

      case GetCommandsForTwitchRoomId(replyTo, twitchRoomId) =>
        ctx.ask[ArchieMateMediator.Command, CommandsSettings](mediator, ref => ArchieMateMediator.SendCommandsSettingsRepositoryCommand(CommandsSettingsRepository.GetCommandsSettings(ref, twitchRoomId))) {
          case Success(settings) =>
            Reply(replyTo, settings)

          case Failure(ex) =>
            Reply(replyTo, CommandsSettings())
        }
        Behaviors.same

      case Reply(replyTo, reply) =>
        replyTo ! reply
        Behaviors.same
    }
  }
}
