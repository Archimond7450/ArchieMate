package com.archimond7450.archiemate.actors.services.controllerhelpers

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.kick.api.KickApiClient
import com.archimond7450.archiemate.actors.repositories.sessions.KickUserSessionsRepository
import com.archimond7450.archiemate.actors.services.JWTService
import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.kick.api.KickApiResponse
import com.archimond7450.archiemate.twitch.api.TwitchApiResponse
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import org.apache.pekko.util.Timeout

import scala.util.{Failure, Success, Try}

object UserControllerHelperService {
  val actorName = "UserControllerHelperService"

  sealed trait Command

  final case class GetUser(replyTo: ActorRef[GetUserResponse], jwt: String)
      extends Command
  private final case class GetUserWithTwitchIds(
      originalCommand: GetUser,
      twitchUserId: String,
      tokenId: String
  ) extends Command
  private final case class GetUserKickTokenId(
      originalCommand: GetUser,
      twitchUserId: String,
      twitchUser: Try[TwitchApiResponse.GetTokenUser]
  ) extends Command
  private final case class GetUserWithKickIds(
      originalCommand: GetUser,
      twitchUser: Try[TwitchApiResponse.GetTokenUser],
      kickTokenId: String
  ) extends Command
  private final case class GetUserWithResponse(
      originalCommand: GetUser,
      twitchUser: Try[TwitchApiResponse.GetTokenUser],
      kickUserOption: Try[Option[KickApiResponse.User]]
  ) extends Command

  sealed trait GetUserResponse
  final case class GetUserOKResponse(
      twitchUser: Try[TwitchApiResponse.GetTokenUser],
      kickUserOption: Try[Option[KickApiResponse.User]]
  ) extends GetUserResponse

  case object InvalidJWT extends GetUserResponse

  def apply()(using
      mediator: ActorRef[ArchieMateMediator.Command],
      timeout: Timeout
  ): Behavior[Command] = Behaviors
    .supervise[Command] {
      Behaviors.setup { ctx =>
        val invalidJWTMessage = "Invalid JWT"

        given ActorContext[Command] = ctx

        Behaviors.receiveAndLogMessage {
          case cmd @ GetUser(replyTo, jwt) =>
            ctx.ask[ArchieMateMediator.Command, JWTService.DecodeJWTResponse](
              mediator,
              ref =>
                ArchieMateMediator.SendJWTServiceCommand(
                  JWTService.DecodeJWT(ref, jwt)
                )
            ) {
              case Success(JWTService.DecodedJWT(userId, sessionId)) =>
                GetUserWithTwitchIds(
                  originalCommand = cmd,
                  twitchUserId = userId,
                  tokenId = sessionId
                )

              case Success(JWTService.InvalidJWT) =>
                val cause = Failure(RuntimeException(invalidJWTMessage))
                GetUserWithResponse(cmd, cause, cause)

              case Failure(ex) =>
                val cause = Failure(ex)
                GetUserWithResponse(cmd, cause, cause)
            }

            Behaviors.same

          case GetUserWithTwitchIds(originalCommand, twitchUserId, tokenId) =>
            ctx.askWithStatus[
              ArchieMateMediator.Command,
              TwitchApiResponse.GetTokenUser
            ](
              mediator,
              ref =>
                ArchieMateMediator.SendTwitchApiClientCommand(
                  TwitchApiClient.GetTokenUserFromTokenId(ref, tokenId)
                )
            ) { resp =>
              GetUserKickTokenId(originalCommand, twitchUserId, resp)
            }
            Behaviors.same

          case GetUserKickTokenId(originalCommand, twitchUserId, twitchUser) =>
            ctx.ask[
              ArchieMateMediator.Command,
              KickUserSessionsRepository.ReturnedTokenIdForTwitchUserId
            ](
              mediator,
              ref =>
                ArchieMateMediator.SendKickUserSessionsRepositoryCommand(
                  KickUserSessionsRepository
                    .GetTokenIdForTwitchUserId(ref, twitchUserId)
                )
            ) {
              case Success(
                    KickUserSessionsRepository.ReturnedTokenIdForTwitchUserId(
                      Some(tokenId)
                    )
                  ) =>
                GetUserWithKickIds(originalCommand, twitchUser, tokenId)

              case Success(
                    KickUserSessionsRepository.ReturnedTokenIdForTwitchUserId(
                      None
                    )
                  ) =>
                GetUserWithResponse(originalCommand, twitchUser, Success(None))

              case Failure(ex) =>
                GetUserWithResponse(originalCommand, twitchUser, Failure(ex))
            }
            Behaviors.same

          case GetUserWithKickIds(originalCommand, twitchUser, kickTokenId) =>
            ctx.askWithStatus[ArchieMateMediator.Command, KickApiResponse.User](
              mediator,
              ref =>
                ArchieMateMediator.SendKickApiClientCommand(
                  KickApiClient.GetTokenUserFromTokenId(ref, kickTokenId)
                )
            ) { resp =>
              GetUserWithResponse(
                originalCommand,
                twitchUser,
                resp.map(Some(_))
              )
            }
            Behaviors.same

          case GetUserWithResponse(
                originalCommand,
                Failure(ex1: RuntimeException),
                Failure(ex2: RuntimeException)
              )
              if ex1.getMessage == invalidJWTMessage && ex2.getMessage == invalidJWTMessage =>
            originalCommand.replyTo ! InvalidJWT
            Behaviors.same

          case GetUserWithResponse(originalCommand, twitchUser, kickUser) =>
            originalCommand.replyTo ! GetUserOKResponse(twitchUser, kickUser)
            Behaviors.same
        }
      }
    }
    .onFailure[Throwable](SupervisorStrategy.resume)
}
