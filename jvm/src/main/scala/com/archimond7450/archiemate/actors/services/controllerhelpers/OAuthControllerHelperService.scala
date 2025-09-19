package com.archimond7450.archiemate.actors.services.controllerhelpers

import com.archimond7450.archiemate.actors
import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.repositories.sessions.TwitchUserSessionsRepository
import com.archimond7450.archiemate.actors.services.{JWTService, TwitchLoginValidatorService}
import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.providers.RandomProvider
import com.archimond7450.archiemate.twitch.api.TwitchApiResponse
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.http.scaladsl.model.Uri
import org.apache.pekko.http.scaladsl.model.Uri.Query
import org.apache.pekko.util.Timeout

import java.util.UUID
import scala.util.{Failure, Success}

object OAuthControllerHelperService {
  val actorName = "OAuthControllerHelperService"

  sealed trait Command

  final case class NewTwitchLoginRequest(replyTo: ActorRef[TwitchRedirectResponse]) extends Command
  final case class NewTwitchConnectionRequest(replyTo: ActorRef[TwitchRedirectResponse]) extends Command
  private final case class ConstructTwitchRedirectUri(replyTo: ActorRef[TwitchRedirectResponse], scopes: List[String]) extends Command
  private final case class PrepareErrorResponse(replyTo: ActorRef[TwitchRedirectResponse], ex: Throwable) extends Command
  private final case class ConstructTwitchRedirectUriWithUUID(replyTo: ActorRef[TwitchRedirectResponse], scopes: List[String], uuid: UUID) extends Command

  sealed trait TwitchRedirectResponse
  final case class TwitchRedirect(redirectUri: Uri) extends TwitchRedirectResponse
  final case class CannotRedirect(message: String) extends TwitchRedirectResponse

  final case class TwitchCodeReceived(replyTo: ActorRef[TwitchCodeReceivedResponse], code: String, scopes: String, state: String) extends Command
  private final case class NoSuchLogin(originalRequest: TwitchCodeReceived) extends Command
  private final case class UnknownError(replyTo: ActorRef[TwitchCodeReceivedResponse], ex: Throwable) extends Command
  private final case class GetTwitchToken(originalRequest: TwitchCodeReceived) extends Command
  private final case class LoginFailed(originalRequest: TwitchCodeReceived, ex: Throwable) extends Command
  private final case class TwitchApiClientFailure(originalRequest: TwitchCodeReceived, ex: Throwable) extends Command
  private final case class GetTokenUser(originalRequest: TwitchCodeReceived, token: TwitchApiResponse.GetToken) extends Command
  private final case class RememberToken(originalRequest: TwitchCodeReceived, token: TwitchApiResponse.GetToken, tokenUser: TwitchApiResponse.GetTokenUser) extends Command
  private final case class NewJWT(originalRequest: TwitchCodeReceived, jwt: String) extends Command

  sealed trait TwitchCodeReceivedResponse
  final case class Unauthorized(msg: String) extends TwitchCodeReceivedResponse
  final case class InternalError(msg: String) extends TwitchCodeReceivedResponse
  final case class Authorized(token: TwitchApiResponse.GetToken, tokenUser: TwitchApiResponse.GetTokenUser) extends TwitchCodeReceivedResponse
  final case class GeneratedJWT(jwt: String) extends TwitchCodeReceivedResponse

  def apply()(using mediator: ActorRef[ArchieMateMediator.Command], randomProvider: RandomProvider, settings: Settings, timeout: Timeout): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx

    Behaviors.receiveAndLogMessage {
      case NewTwitchLoginRequest(replyTo) =>
        ctx.self ! ConstructTwitchRedirectUri(replyTo, Nil)
        Behaviors.same

      case NewTwitchConnectionRequest(replyTo) =>
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
        ctx.self ! ConstructTwitchRedirectUri(replyTo, scopes)
        Behaviors.same

      case ConstructTwitchRedirectUri(replyTo, scopes) =>
        ctx.ask[ArchieMateMediator.Command, TwitchLoginValidatorService.NewRequestCreated](mediator, ref => ArchieMateMediator.SendTwitchLoginValidatorServiceCommand(TwitchLoginValidatorService.NewRequest(ref))) {
          case Success(TwitchLoginValidatorService.NewRequestCreated(uuid, when)) =>
            ConstructTwitchRedirectUriWithUUID(replyTo, scopes, uuid)

          case Failure(ex) =>
            PrepareErrorResponse(replyTo, ex)
        }
        Behaviors.same

      case PrepareErrorResponse(replyTo, ex) =>
        ctx.log.error("Cannot secure login to Twitch!", ex)
        replyTo ! CannotRedirect(s"Cannot secure login to Twitch! This error was logged. If the issue persists, please contact Archimond7450.")
        Behaviors.same

      case ConstructTwitchRedirectUriWithUUID(replyTo, scopes, uuid) =>
        //val encodedScopes = URLEncoder.encode(scopes.mkString(" "), "UTF-8")
        val twitchRedirectUri = Uri("https://id.twitch.tv/oauth2/authorize").withQuery(
          Query(
            "response_type" -> "code",
            "client_id" -> settings.twitchAppClientId,
            "redirect_uri" -> settings.twitchAppRedirectUri,
            "scope" -> scopes.mkString(" "),
            "state" -> uuid.toString,
            "force_verify" -> (if (scopes.nonEmpty) "yes" else "no")
          )
        )
        //response_type=code&client_id=${settings.twitchAppClientId}&redirect_uri=${settings.twitchAppRedirectUri}&scope=$encodedScopes&state=${uuid.toString}")
        replyTo ! TwitchRedirect(twitchRedirectUri)
        Behaviors.same

      case cmd @ TwitchCodeReceived(replyTo, code, scopes, state) =>
        ctx.ask[ArchieMateMediator.Command, TwitchLoginValidatorService.RequestSucceededResponse](mediator, ref => ArchieMateMediator.SendTwitchLoginValidatorServiceCommand(TwitchLoginValidatorService.RequestSucceeded(ref, UUID.fromString(state)))) {
          case Success(TwitchLoginValidatorService.Acknowledged(_)) =>
            GetTwitchToken(cmd)

          case Success(TwitchLoginValidatorService.InvalidRequest(uuid)) =>
            NoSuchLogin(cmd)

          case Failure(ex) =>
            UnknownError(replyTo, ex)
        }
        Behaviors.same

      case NoSuchLogin(originalRequest) =>
        ctx.log.warn(s"No such login: $originalRequest")
        originalRequest.replyTo ! Unauthorized(s"No such session id. You probably took too long to log in securely. The current timeout for secure login is set to ${settings.newLoginExpirationTime.toSeconds} seconds.")
        Behaviors.same

      case UnknownError(replyTo, ex) =>
        ctx.log.error("Unknown error", ex)
        replyTo ! InternalError("Unknown error. Details have been logged. If the problem persists, please contact Archimond7450.")
        Behaviors.same

      case GetTwitchToken(originalRequest) =>
        ctx.askWithStatus[ArchieMateMediator.Command, TwitchApiResponse.GetToken](mediator, ref => ArchieMateMediator.SendTwitchApiClientCommand(TwitchApiClient.GetToken(ref, originalRequest.code))) {
          case Success(token: TwitchApiResponse.GetToken) =>
            GetTokenUser(originalRequest, token)

          case Failure(ex: TwitchApiResponse.NOK) =>
            LoginFailed(originalRequest, ex)

          case Failure(ex) =>
            TwitchApiClientFailure(originalRequest, ex)
        }
        Behaviors.same

      case LoginFailed(originalRequest, ex) =>
        ctx.log.error("Login failed", ex)
        originalRequest.replyTo ! Unauthorized("Twitch Login failed. Please try again.")
        Behaviors.same

      case TwitchApiClientFailure(originalRequest, ex) =>
        ctx.log.error("Twitch API Client failed to respond", ex)
        originalRequest.replyTo ! InternalError("Twitch API Client failed. This error has been logged. If the issue persists, please contact Archimond7450.")
        Behaviors.same

      case GetTokenUser(originalRequest, token) =>
        ctx.askWithStatus[ArchieMateMediator.Command, TwitchApiResponse.GetTokenUser](mediator, ref => ArchieMateMediator.SendTwitchApiClientCommand(TwitchApiClient.GetTokenUserFromAccessToken(ref, token.access_token))) {
          case Success(tokenUser: TwitchApiResponse.GetTokenUser) =>
            RememberToken(originalRequest, token, tokenUser)

          case Failure(ex: TwitchApiResponse.NOK) =>
            LoginFailed(originalRequest, ex)

          case Failure(ex) =>
            TwitchApiClientFailure(originalRequest, ex)
        }
        Behaviors.same

      case RememberToken(originalRequest, token, tokenUser) =>
        mediator ! ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand(TwitchUserSessionsRepository.SetToken(originalRequest.state, tokenUser.id, token))
        if (token.scope.nonEmpty) {
          originalRequest.replyTo ! Authorized(token, tokenUser)
        } else {
          ctx.ask[actors.ArchieMateMediator.Command, JWTService.GeneratedJWT](mediator, ref => ArchieMateMediator.SendJWTServiceCommand(JWTService.GenerateJWT(ref, tokenUser.id, originalRequest.state))) {
            case Success(JWTService.GeneratedJWT(jwt)) =>
              NewJWT(originalRequest, jwt)

            case Failure(ex) =>
              UnknownError(originalRequest.replyTo, ex)
          }
        }
        Behaviors.same

      case NewJWT(originalRequest, jwt) =>
        originalRequest.replyTo ! GeneratedJWT(jwt)
        Behaviors.same
    }
  }
}
