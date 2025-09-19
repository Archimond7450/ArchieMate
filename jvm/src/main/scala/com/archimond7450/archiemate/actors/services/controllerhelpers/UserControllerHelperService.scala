package com.archimond7450.archiemate.actors.services.controllerhelpers

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.services.JWTService
import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.twitch.api.TwitchApiResponse
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.util.Timeout

import scala.util.{Failure, Success, Try}

object UserControllerHelperService {
  val actorName = "UserControllerHelperService"

  sealed trait Command

  final case class GetUser(replyTo: ActorRef[GetUserResponse], jwt: String) extends Command
  private final case class GetUserWithIds(originalCommand: GetUser, userId: String, tokenId: String) extends Command
  private final case class GetUserWithResponse(originalCommand: GetUser, user: Try[TwitchApiResponse.GetTokenUser]) extends Command

  sealed trait GetUserResponse
  final case class GetUserOKResponse(user: Try[TwitchApiResponse.GetTokenUser]) extends GetUserResponse

  case object InvalidJWT extends GetUserResponse

  def apply()(using mediator: ActorRef[ArchieMateMediator.Command], timeout: Timeout): Behavior[Command] = Behaviors.setup { ctx =>
    val invalidJWTMessage = "Invalid JWT"

    given ActorContext[Command] = ctx

    Behaviors.receiveAndLogMessage {
      case cmd @ GetUser(replyTo, jwt) =>
        ctx.ask[ArchieMateMediator.Command, JWTService.DecodeJWTResponse](mediator, ref => ArchieMateMediator.SendJWTServiceCommand(JWTService.DecodeJWT(ref, jwt))) {
          case Success(JWTService.DecodedJWT(userId, sessionId)) =>
            GetUserWithIds(originalCommand = cmd, userId = userId, tokenId = sessionId)

          case Success(JWTService.InvalidJWT) =>
            GetUserWithResponse(cmd, Failure(RuntimeException(invalidJWTMessage)))

          case Failure(ex) =>
            GetUserWithResponse(cmd, Failure(ex))
        }

        Behaviors.same

      case GetUserWithIds(originalCommand, userId, tokenId) =>
        ctx.askWithStatus[ArchieMateMediator.Command, TwitchApiResponse.GetTokenUser](mediator, ref => ArchieMateMediator.SendTwitchApiClientCommand(TwitchApiClient.GetTokenUserFromTokenId(ref, tokenId))) {
          resp => GetUserWithResponse(originalCommand, resp)
        }
        Behaviors.same

      case GetUserWithResponse(originalCommand, Failure(ex: RuntimeException)) if ex.getMessage == invalidJWTMessage =>
        originalCommand.replyTo ! InvalidJWT
        Behaviors.same

      case GetUserWithResponse(originalCommand, user) =>
        originalCommand.replyTo ! GetUserOKResponse(user)
        Behaviors.same
    }
  }
}
