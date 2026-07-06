package com.archimond7450.archiemate.actors.services.controllerhelpers

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.kick.api.KickApiClient
import com.archimond7450.archiemate.actors.repositories.sessions.{
  KickUserSessionsRepository,
  TwitchUserSessionsRepository
}
import com.archimond7450.archiemate.actors.services.JWTService
import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.kick.api.KickApiResponse
import com.archimond7450.archiemate.twitch.api.TwitchApiResponse
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import org.apache.pekko.pattern.Backoff
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

  final case class ResetConnections(
      replyTo: ActorRef[Done],
      jwt: String
  ) extends Command
  private final case class ResetConnectionsWithTwitchId(
      originalCommand: ResetConnections,
      tryTwitchUserId: Try[String]
  ) extends Command
  private final case class ResetConnectionsWithTwitchResponse(
      twitchUserIdCommand: ResetConnectionsWithTwitchId,
      tryTwitchResponse: Try[TwitchUserSessionsRepository.Acknowledged.type]
  ) extends Command
  private final case class ResetConnectionsWithKickResponse(
      twitchUserIdCommand: ResetConnectionsWithTwitchId,
      tryTwitchRespnose: Try[TwitchUserSessionsRepository.Acknowledged.type],
      tryKickResponse: Try[KickUserSessionsRepository.Acknowledged.type]
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

        Behaviors.logMessages {
          Behaviors.receiveMessage {
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

            case GetUserKickTokenId(
                  originalCommand,
                  twitchUserId,
                  twitchUser
                ) =>
              ctx.ask[
                ArchieMateMediator.Command,
                KickUserSessionsRepository.ReturnedTokenIdForUserId
              ](
                mediator,
                ref =>
                  ArchieMateMediator.SendKickUserSessionsRepositoryCommand(
                    KickUserSessionsRepository
                      .GetTokenIdForTwitchUserId(ref, twitchUserId)
                  )
              ) {
                case Success(
                      KickUserSessionsRepository.ReturnedTokenIdForUserId(
                        Some(tokenId)
                      )
                    ) =>
                  GetUserWithKickIds(originalCommand, twitchUser, tokenId)

                case Success(
                      KickUserSessionsRepository.ReturnedTokenIdForUserId(
                        None
                      )
                    ) =>
                  GetUserWithResponse(
                    originalCommand,
                    twitchUser,
                    Success(None)
                  )

                case Failure(ex) =>
                  GetUserWithResponse(originalCommand, twitchUser, Failure(ex))
              }
              Behaviors.same

            case GetUserWithKickIds(originalCommand, twitchUser, kickTokenId) =>
              ctx.askWithStatus[
                ArchieMateMediator.Command,
                KickApiResponse.User
              ](
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

            case cmd @ ResetConnections(replyTo, jwt) =>
              ctx.ask[ArchieMateMediator.Command, JWTService.DecodeJWTResponse](
                mediator,
                ref =>
                  ArchieMateMediator.SendJWTServiceCommand(
                    JWTService.DecodeJWT(ref, jwt)
                  )
              ) {
                case Success(JWTService.DecodedJWT(userId, sessionId)) =>
                  ResetConnectionsWithTwitchId(cmd, Success(userId))

                case Success(JWTService.InvalidJWT) =>
                  val cause = Failure(RuntimeException(invalidJWTMessage))
                  val commandWithUserId =
                    ResetConnectionsWithTwitchId(cmd, cause)
                  ResetConnectionsWithKickResponse(
                    commandWithUserId,
                    cause,
                    cause
                  )

                case Failure(ex) =>
                  val cause = Failure(ex)
                  val commandWithUserId =
                    ResetConnectionsWithTwitchId(cmd, cause)
                  ResetConnectionsWithKickResponse(
                    commandWithUserId,
                    cause,
                    cause
                  )
              }
              Behaviors.same

            case cmd @ ResetConnectionsWithTwitchId(
                  originalCommand,
                  Failure(ex)
                ) =>
              val cause = Failure(ex)
              ctx.self ! ResetConnectionsWithKickResponse(cmd, cause, cause)
              Behaviors.same

            case cmd @ ResetConnectionsWithTwitchId(
                  originalCommand,
                  Success(twitchUserId)
                ) =>
              ctx.ask[
                ArchieMateMediator.Command,
                TwitchUserSessionsRepository.Acknowledged.type
              ](
                mediator,
                ref =>
                  ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand(
                    TwitchUserSessionsRepository
                      .ResetTokensForUserId(ref, twitchUserId)
                  )
              )(ResetConnectionsWithTwitchResponse(cmd, _))
              Behaviors.same

            case cmd @ ResetConnectionsWithTwitchResponse(
                  twitchUserIdCommand,
                  tryTwitchResponse
                ) =>
              ctx.ask[
                ArchieMateMediator.Command,
                KickUserSessionsRepository.Acknowledged.type
              ](
                mediator,
                ref =>
                  ArchieMateMediator.SendKickUserSessionsRepositoryCommand(
                    KickUserSessionsRepository.ResetTokensForTwitchUserId(
                      ref,
                      twitchUserIdCommand.tryTwitchUserId.get
                    )
                  )
              )(
                ResetConnectionsWithKickResponse(
                  cmd.twitchUserIdCommand,
                  tryTwitchResponse,
                  _
                )
              )
              Behaviors.same

            case cmd @ ResetConnectionsWithKickResponse(
                  twitchUserIdCommand,
                  tryTwitchRespnose,
                  tryKickResponse
                ) =>
              twitchUserIdCommand.originalCommand.replyTo ! Done
              Behaviors.same
          }
        }
      }
    }
    .onFailure[Throwable](SupervisorStrategy.resume)
}
