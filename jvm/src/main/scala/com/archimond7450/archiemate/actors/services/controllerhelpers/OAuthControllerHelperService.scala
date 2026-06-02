package com.archimond7450.archiemate.actors.services.controllerhelpers

import com.archimond7450.archiemate.actors
import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.kick.api.KickApiClient
import com.archimond7450.archiemate.actors.repositories.sessions.{
  KickUserSessionsRepository,
  TwitchUserSessionsRepository
}
import com.archimond7450.archiemate.actors.services.{
  JWTService,
  KickLoginValidatorService,
  TwitchLoginValidatorService
}
import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.kick.api.KickApiResponse
import com.archimond7450.archiemate.providers.RandomProvider
import com.archimond7450.archiemate.twitch.api.TwitchApiResponse
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import org.apache.pekko.http.scaladsl.model.Uri
import org.apache.pekko.http.scaladsl.model.Uri.Query
import org.apache.pekko.util.Timeout

import java.util.UUID
import scala.util.{Failure, Success}

object OAuthControllerHelperService {
  val actorName = "OAuthControllerHelperService"

  sealed trait Command

  final case class NewTwitchLoginRequest(
      replyTo: ActorRef[TwitchRedirectResponse]
  ) extends Command
  final case class NewTwitchConnectionRequest(
      replyTo: ActorRef[TwitchRedirectResponse],
      jwtOption: Option[String]
  ) extends Command
  final case class NewKickConnectionRequest(
      replyTo: ActorRef[KickRedirectResponse],
      jwt: String
  ) extends Command
  private final case class ConstructTwitchRedirectUri(
      replyTo: ActorRef[TwitchRedirectResponse],
      scopes: List[String],
      twitchUserIdOption: Option[String]
  ) extends Command
  private final case class ConstructKickRedirectUri(
      replyTo: ActorRef[KickRedirectResponse],
      scopes: List[String],
      twitchUserId: String
  ) extends Command
  private final case class PrepareTwitchErrorResponse(
      replyTo: ActorRef[TwitchRedirectResponse],
      ex: Throwable
  ) extends Command
  private final case class PrepareKickErrorResponse(
      replyTo: ActorRef[KickRedirectResponse],
      ex: Throwable
  ) extends Command
  private final case class ConstructTwitchRedirectUriWithUUID(
      replyTo: ActorRef[TwitchRedirectResponse],
      scopes: List[String],
      uuid: UUID
  ) extends Command
  private final case class ConstructKickRedirectUriWithUUIDAndCodeChallenge(
      replyTo: ActorRef[KickRedirectResponse],
      scopes: List[String],
      uuid: UUID,
      codeChallenge: String
  ) extends Command

  sealed trait TwitchRedirectResponse
  final case class TwitchRedirect(redirectUri: Uri)
      extends TwitchRedirectResponse
  final case class CannotRedirectToTwitch(message: String)
      extends TwitchRedirectResponse

  sealed trait KickRedirectResponse
  final case class KickRedirect(redirectUri: Uri) extends KickRedirectResponse
  final case class CannotRedirectToKick(message: String)
      extends KickRedirectResponse

  final case class TwitchCodeReceived(
      replyTo: ActorRef[TwitchCodeReceivedResponse],
      code: String,
      scopes: String,
      state: String
  ) extends Command
  final case class KickCodeReceived(
      replyTo: ActorRef[KickCodeReceivedResponse],
      code: String,
      state: String
  ) extends Command
  private final case class NoSuchTwitchLogin(
      originalRequest: TwitchCodeReceived
  ) extends Command
  private final case class NoSuchKickLogin(originalRequest: KickCodeReceived)
      extends Command
  private final case class UnknownTwitchLoginError(
      replyTo: ActorRef[TwitchCodeReceivedResponse],
      ex: Throwable
  ) extends Command
  private final case class UnknownKickLoginError(
      replyTo: ActorRef[KickCodeReceivedResponse],
      ex: Throwable
  ) extends Command
  private final case class GetTwitchToken(
      originalRequest: TwitchCodeReceived,
      twitchUserIdOption: Option[String]
  ) extends Command
  private final case class GetKickToken(
      originalRequest: KickCodeReceived,
      twitchUserId: String,
      codeVerifier: String
  ) extends Command
  private final case class TwitchLoginFailed(
      originalRequest: TwitchCodeReceived,
      ex: Throwable
  ) extends Command
  private final case class KickLoginFailed(
      originalRequest: KickCodeReceived,
      ex: Throwable
  ) extends Command
  private final case class TwitchApiClientFailure(
      originalRequest: TwitchCodeReceived,
      ex: Throwable
  ) extends Command
  private final case class KickApiClientFailure(
      originalRequest: KickCodeReceived,
      ex: Throwable
  ) extends Command
  private final case class GetTwitchTokenUser(
      originalRequest: TwitchCodeReceived,
      twitchUserIdOption: Option[String],
      token: TwitchApiResponse.GetToken
  ) extends Command
  private final case class GetKickTokenUser(
      originalRequest: KickCodeReceived,
      twitchUserId: String,
      token: KickApiResponse.GetToken
  ) extends Command
  private final case class RememberTwitchToken(
      originalRequest: TwitchCodeReceived,
      token: TwitchApiResponse.GetToken,
      tokenUser: TwitchApiResponse.GetTokenUser
  ) extends Command
  private final case class RememberKickToken(
      originalRequest: KickCodeReceived,
      twitchUserId: String,
      token: KickApiResponse.GetToken,
      tokenUser: KickApiResponse.User
  ) extends Command
  private final case class NewTwitchJWT(
      originalRequest: TwitchCodeReceived,
      jwt: String
  ) extends Command
  private final case class NewKickJWT(
      originalRequest: KickCodeReceived,
      jwt: String
  ) extends Command

  sealed trait TwitchCodeReceivedResponse
  final case class TwitchUnauthorized(msg: String)
      extends TwitchCodeReceivedResponse
  final case class TwitchInternalError(msg: String)
      extends TwitchCodeReceivedResponse
  final case class TwitchAuthorized(
      token: TwitchApiResponse.GetToken,
      tokenUser: TwitchApiResponse.GetTokenUser
  ) extends TwitchCodeReceivedResponse
  final case class GeneratedTwitchJWT(jwt: String)
      extends TwitchCodeReceivedResponse

  sealed trait KickCodeReceivedResponse
  final case class KickUnauthorized(msg: String)
      extends KickCodeReceivedResponse
  final case class KickInternalError(msg: String)
      extends KickCodeReceivedResponse
  final case class KickAuthorized(
      token: KickApiResponse.GetToken,
      tokenUser: KickApiResponse.User
  ) extends KickCodeReceivedResponse

  def apply()(using
      mediator: ActorRef[ArchieMateMediator.Command],
      randomProvider: RandomProvider,
      settings: Settings,
      timeout: Timeout
  ): Behavior[Command] = Behaviors
    .supervise[Command] {
      Behaviors.setup { ctx =>
        given ActorContext[Command] = ctx

        Behaviors.receiveAndLogMessage {
          case NewTwitchLoginRequest(replyTo) =>
            ctx.self ! ConstructTwitchRedirectUri(replyTo, Nil, None)
            Behaviors.same

          case NewTwitchConnectionRequest(replyTo, jwtOption) =>
            val scopes = List(
              "bits:read",
              "channel:manage:broadcast",
              "channel:read:hype_train",
              "channel:manage:moderators",
              "channel:manage:polls",
              "channel:manage:predictions",
              "channel:manage:raids",
              "channel:manage:redemptions",
              "channel:read:subscriptions",
              "channel:manage:vips",
              "clips:edit",
              "moderation:read",
              "moderator:manage:announcements",
              "moderator:manage:banned_users",
              "moderator:read:blocked_terms",
              "moderator:manage:blocked_terms",
              "moderator:manage:chat_messages",
              "moderator:read:chat_settings",
              "moderator:manage:chat_settings",
              "moderator:read:chatters",
              "moderator:read:followers",
              "moderator:read:guest_star",
              "moderator:manage:guest_star",
              "moderator:read:shield_mode",
              "moderator:manage:shield_mode",
              "moderator:read:shoutouts",
              "moderator:manage:shoutouts",
              "moderator:read:unban_requests",
              "moderator:manage:unban_requests",
              "moderator:manage:warnings",
              "user:read:chat"
            )
            ctx.ask[ArchieMateMediator.Command, JWTService.DecodeJWTResponse](
              mediator,
              ref =>
                ArchieMateMediator.SendJWTServiceCommand(
                  JWTService.DecodeJWT(ref, jwtOption.getOrElse(""))
                )
            ) {
              case Success(JWTService.DecodedJWT(twitchUserId, _)) =>
                ConstructTwitchRedirectUri(replyTo, scopes, Some(twitchUserId))
              case Success(JWTService.InvalidJWT) =>
                PrepareTwitchErrorResponse(
                  replyTo,
                  RuntimeException("Invalid JWT token")
                )
              case Failure(ex) =>
                PrepareTwitchErrorResponse(replyTo, ex)
            }
            Behaviors.same

          case NewKickConnectionRequest(replyTo, jwt) =>
            val scopes = List(
              "user:read",
              "channel:read",
              "channel:write",
              "channel:rewards:read",
              "channel:rewards:write",
              "chat:write",
              "events:subscribe",
              "kicks:read"
            )
            ctx.ask[ArchieMateMediator.Command, JWTService.DecodeJWTResponse](
              mediator,
              ref =>
                ArchieMateMediator.SendJWTServiceCommand(
                  JWTService.DecodeJWT(ref, jwt)
                )
            ) {
              case Success(JWTService.DecodedJWT(twitchUserId, _)) =>
                ConstructKickRedirectUri(replyTo, scopes, twitchUserId)
              case Success(JWTService.InvalidJWT) =>
                PrepareKickErrorResponse(
                  replyTo,
                  RuntimeException("Invalid JWT token")
                )
              case Failure(ex) =>
                PrepareKickErrorResponse(replyTo, ex)
            }
            Behaviors.same

          case ConstructTwitchRedirectUri(
                replyTo,
                scopes,
                twitchUserIdOption
              ) =>
            ctx.ask[
              ArchieMateMediator.Command,
              TwitchLoginValidatorService.NewRequestCreated
            ](
              mediator,
              ref =>
                ArchieMateMediator.SendTwitchLoginValidatorServiceCommand(
                  TwitchLoginValidatorService
                    .NewRequest(ref, twitchUserIdOption)
                )
            ) {
              case Success(
                    TwitchLoginValidatorService.NewRequestCreated(uuid, when)
                  ) =>
                ConstructTwitchRedirectUriWithUUID(replyTo, scopes, uuid)

              case Failure(ex) =>
                PrepareTwitchErrorResponse(replyTo, ex)
            }
            Behaviors.same

          case ConstructKickRedirectUri(replyTo, scopes, twitchUserId) =>
            ctx.ask[
              ArchieMateMediator.Command,
              KickLoginValidatorService.NewRequestCreated
            ](
              mediator,
              ref =>
                ArchieMateMediator.SendKickLoginValidatorServiceCommand(
                  KickLoginValidatorService.NewRequest(ref, twitchUserId)
                )
            ) {
              case Success(
                    KickLoginValidatorService.NewRequestCreated(
                      uuid,
                      codeChallenge,
                      when
                    )
                  ) =>
                ConstructKickRedirectUriWithUUIDAndCodeChallenge(
                  replyTo,
                  scopes,
                  uuid,
                  codeChallenge
                )
              case Failure(ex) => PrepareKickErrorResponse(replyTo, ex)
            }
            Behaviors.same

          case PrepareTwitchErrorResponse(replyTo, ex) =>
            ctx.log.error("Cannot secure login to Twitch!", ex)
            replyTo ! CannotRedirectToTwitch(
              s"Cannot secure login to Twitch! This error was logged. If the issue persists, please contact Archimond7450."
            )
            Behaviors.same

          case PrepareKickErrorResponse(replyTo, ex) =>
            ctx.log.error("Cannot secure login to KicK!", ex)
            replyTo ! CannotRedirectToKick(
              s"Cannot secure login to Kick! This error was logged. If the issue persists, please contact Archimond7450."
            )
            Behaviors.same

          case ConstructTwitchRedirectUriWithUUID(replyTo, scopes, uuid) =>
            // val encodedScopes = URLEncoder.encode(scopes.mkString(" "), "UTF-8")
            val twitchRedirectUri =
              Uri("https://id.twitch.tv/oauth2/authorize").withQuery(
                Query(
                  "response_type" -> "code",
                  "client_id" -> settings.twitchAppClientId,
                  "redirect_uri" -> settings.twitchAppRedirectUri,
                  "scope" -> scopes.mkString(" "),
                  "state" -> uuid.toString,
                  "force_verify" -> (if (scopes.nonEmpty) "yes" else "no")
                )
              )
            // response_type=code&client_id=${settings.twitchAppClientId}&redirect_uri=${settings.twitchAppRedirectUri}&scope=$encodedScopes&state=${uuid.toString}")
            replyTo ! TwitchRedirect(twitchRedirectUri)
            Behaviors.same

          case ConstructKickRedirectUriWithUUIDAndCodeChallenge(
                replyTo,
                scopes,
                uuid,
                codeChallenge
              ) =>
            val kickRedirectUri =
              Uri("https://id.kick.com/oauth/authorize").withQuery(
                Query(
                  "response_type" -> "code",
                  "client_id" -> settings.kickAppClientId,
                  "redirect_uri" -> settings.kickAppRedirectUri,
                  "scope" -> scopes.mkString(" "),
                  "state" -> uuid.toString,
                  "code_challenge" -> codeChallenge,
                  "code_challenge_method" -> "S256"
                )
              )
            replyTo ! KickRedirect(kickRedirectUri)
            Behaviors.same

          case cmd @ TwitchCodeReceived(replyTo, code, scopes, state) =>
            ctx.ask[
              ArchieMateMediator.Command,
              TwitchLoginValidatorService.RequestSucceededResponse
            ](
              mediator,
              ref =>
                ArchieMateMediator.SendTwitchLoginValidatorServiceCommand(
                  TwitchLoginValidatorService
                    .RequestSucceeded(ref, UUID.fromString(state))
                )
            ) {
              case Success(
                    TwitchLoginValidatorService.Acknowledged(
                      _,
                      twitchUserIdOption
                    )
                  ) =>
                GetTwitchToken(cmd, twitchUserIdOption)

              case Success(TwitchLoginValidatorService.InvalidRequest(uuid)) =>
                NoSuchTwitchLogin(cmd)

              case Failure(ex) =>
                UnknownTwitchLoginError(replyTo, ex)
            }
            Behaviors.same

          case cmd @ KickCodeReceived(replyTo, code, state) =>
            ctx.ask[
              ArchieMateMediator.Command,
              KickLoginValidatorService.RequestSucceededResponse
            ](
              mediator,
              ref =>
                ArchieMateMediator.SendKickLoginValidatorServiceCommand(
                  KickLoginValidatorService
                    .RequestSucceeded(ref, UUID.fromString(state))
                )
            ) {
              case Success(
                    KickLoginValidatorService.Acknowledged(
                      _,
                      twitchUserId,
                      codeVerifier
                    )
                  ) =>
                GetKickToken(cmd, twitchUserId, codeVerifier)

              case Success(KickLoginValidatorService.InvalidRequest(uuid)) =>
                NoSuchKickLogin(cmd)

              case Failure(ex) =>
                UnknownKickLoginError(replyTo, ex)
            }
            Behaviors.same

          case NoSuchTwitchLogin(originalRequest) =>
            ctx.log.warn("No such Twitch login: {}", originalRequest)
            originalRequest.replyTo ! TwitchUnauthorized(
              s"No such session id. You probably took too long to log in securely. The current timeout for secure login is set to ${settings.newLoginExpirationTime.toSeconds} seconds."
            )
            Behaviors.same

          case NoSuchKickLogin(originalRequest) =>
            ctx.log.warn("No such Kick login: {}", originalRequest)
            originalRequest.replyTo ! KickUnauthorized(
              s"No such session id. You probably took too long to log in securely. The current timeout for secure login is set to ${settings.newLoginExpirationTime.toSeconds} seconds."
            )
            Behaviors.same

          case UnknownTwitchLoginError(replyTo, ex) =>
            ctx.log.error("Unknown Twitch login error", ex)
            replyTo ! TwitchInternalError(
              "Unknown Twitch login error. Details have been logged. If the problem persists, please contact Archimond7450."
            )
            Behaviors.same

          case UnknownKickLoginError(replyTo, ex) =>
            ctx.log.error("Unknown Kick login error", ex)
            replyTo ! KickInternalError(
              "Unknown Kick login error. Details have been logged. If the problem persists, please contact Archimond7450."
            )
            Behaviors.same

          case GetTwitchToken(originalRequest, twitchUserIdOption) =>
            ctx.askWithStatus[
              ArchieMateMediator.Command,
              TwitchApiResponse.GetToken
            ](
              mediator,
              ref =>
                ArchieMateMediator.SendTwitchApiClientCommand(
                  TwitchApiClient.GetToken(ref, originalRequest.code)
                )
            ) {
              case Success(token: TwitchApiResponse.GetToken) =>
                GetTwitchTokenUser(originalRequest, twitchUserIdOption, token)

              case Failure(ex: TwitchApiResponse.NOK) =>
                TwitchLoginFailed(originalRequest, ex)

              case Failure(ex) =>
                TwitchApiClientFailure(originalRequest, ex)
            }
            Behaviors.same

          case GetKickToken(originalRequest, twitchUserId, codeVerifier) =>
            ctx.askWithStatus[
              ArchieMateMediator.Command,
              KickApiResponse.GetToken
            ](
              mediator,
              ref =>
                ArchieMateMediator.SendKickApiClientCommand(
                  KickApiClient
                    .GetToken(ref, originalRequest.code, codeVerifier)
                )
            ) {
              case Success(token: KickApiResponse.GetToken) =>
                GetKickTokenUser(originalRequest, twitchUserId, token)

              case Failure(ex: TwitchApiResponse.NOK) =>
                KickLoginFailed(originalRequest, ex)

              case Failure(ex) =>
                KickApiClientFailure(originalRequest, ex)
            }
            Behaviors.same

          case TwitchLoginFailed(originalRequest, ex) =>
            ctx.log.error("Twitch Login failed", ex)
            originalRequest.replyTo ! TwitchUnauthorized(
              "Twitch Login failed. Please try again."
            )
            Behaviors.same

          case KickLoginFailed(originalRequest, ex) =>
            ctx.log.error("Kick Login failed", ex)
            originalRequest.replyTo ! KickUnauthorized(
              "Kick Login failed. Please try again."
            )
            Behaviors.same

          case TwitchApiClientFailure(originalRequest, ex) =>
            ctx.log.error("Twitch API Client failed to respond", ex)
            originalRequest.replyTo ! TwitchInternalError(
              "Twitch API Client failed. This error has been logged. If the issue persists, please contact Archimond7450."
            )
            Behaviors.same

          case KickApiClientFailure(originalRequest, ex) =>
            ctx.log.error("Kick API Client failed to respond", ex)
            originalRequest.replyTo ! KickInternalError(
              "Kick API Client failed. This error has been logged. If the issue persists, please contact Archimond7450."
            )
            Behaviors.same

          case GetTwitchTokenUser(originalRequest, twitchUserIdOption, token) =>
            ctx.askWithStatus[
              ArchieMateMediator.Command,
              TwitchApiResponse.GetTokenUser
            ](
              mediator,
              ref =>
                ArchieMateMediator.SendTwitchApiClientCommand(
                  TwitchApiClient
                    .GetTokenUserFromAccessToken(ref, token.accessToken)
                )
            ) {
              case Success(tokenUser: TwitchApiResponse.GetTokenUser) =>
                val isLoginRequest = token.scope.isEmpty
                val isValidConnection =
                  token.scope.exists(_.nonEmpty) && twitchUserIdOption.nonEmpty
                if (isLoginRequest || isValidConnection) {
                  RememberTwitchToken(originalRequest, token, tokenUser)
                } else {
                  TwitchLoginFailed(
                    originalRequest,
                    RuntimeException(
                      "Twitch connection account must be the same as the one that is currently logged in"
                    )
                  )
                }

              case Failure(ex: TwitchApiResponse.NOK) =>
                TwitchLoginFailed(originalRequest, ex)

              case Failure(ex) =>
                TwitchApiClientFailure(originalRequest, ex)
            }
            Behaviors.same

          case GetKickTokenUser(originalRequest, twitchUserId, token) =>
            ctx.askWithStatus[ArchieMateMediator.Command, KickApiResponse.User](
              mediator,
              ref =>
                ArchieMateMediator.SendKickApiClientCommand(
                  KickApiClient
                    .GetTokenUserFromAccessToken(ref, token.accessToken)
                )
            ) {
              case Success(tokenUser: KickApiResponse.User) =>
                RememberKickToken(
                  originalRequest,
                  twitchUserId,
                  token,
                  tokenUser
                )

              case Failure(ex: KickApiResponse.NOK) =>
                KickLoginFailed(originalRequest, ex)

              case Failure(ex) =>
                KickApiClientFailure(originalRequest, ex)
            }
            Behaviors.same

          case RememberTwitchToken(originalRequest, token, tokenUser) =>
            mediator ! ArchieMateMediator
              .SendTwitchUserSessionsRepositoryCommand(
                TwitchUserSessionsRepository
                  .SetToken(originalRequest.state, tokenUser.id, token)
              )
            if (token.scope.nonEmpty) {
              originalRequest.replyTo ! TwitchAuthorized(token, tokenUser)
            } else {
              ctx.ask[
                actors.ArchieMateMediator.Command,
                JWTService.GeneratedJWT
              ](
                mediator,
                ref =>
                  ArchieMateMediator.SendJWTServiceCommand(
                    JWTService
                      .GenerateJWT(ref, tokenUser.id, originalRequest.state)
                  )
              ) {
                case Success(JWTService.GeneratedJWT(jwt)) =>
                  NewTwitchJWT(originalRequest, jwt)

                case Failure(ex) =>
                  UnknownTwitchLoginError(originalRequest.replyTo, ex)
              }
            }
            Behaviors.same

          case RememberKickToken(
                originalRequest,
                twitchUserId: String,
                token,
                tokenUser
              ) =>
            mediator ! ArchieMateMediator.SendKickUserSessionsRepositoryCommand(
              KickUserSessionsRepository
                .SetToken(originalRequest.state, twitchUserId, token)
            )
            originalRequest.replyTo ! KickAuthorized(token, tokenUser)
            Behaviors.same

          case NewTwitchJWT(originalRequest, jwt) =>
            originalRequest.replyTo ! GeneratedTwitchJWT(jwt)
            Behaviors.same
        }
      }
    }
    .onFailure[Throwable](SupervisorStrategy.resume)
}
