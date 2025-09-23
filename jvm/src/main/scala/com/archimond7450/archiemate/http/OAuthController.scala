package com.archimond7450.archiemate.http

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.services.TwitchLoginValidatorService
import com.archimond7450.archiemate.actors.services.controllerhelpers.OAuthControllerHelperService
import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.helpers.HttpControllerHelpers.createSessionCookie
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.http.scaladsl.model.headers.HttpCookie
import org.apache.pekko.http.scaladsl.model.{
  ContentTypes,
  HttpEntity,
  StatusCodes
}
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.util.Timeout

import scala.util.{Failure, Success}

class OAuthController(using
    mediator: ActorRef[ArchieMateMediator.Command],
    settings: Settings
)(using Scheduler, Timeout)
    extends IController("oauth") {
  private val unknownError =
    "Unknown error. The information has been logged. If the issue persists, please contact Archimond7450."

  override def routes: Route = twitch

  private def twitch: Route = {
    (get & extractLog & pathPrefix("twitch")) { log =>
      (pathEndOrSingleSlash & parameter(Symbol("code")) & parameter(
        Symbol("scope")
      ) & parameter(Symbol("state"))) { (code, scope, state) =>
        onComplete(
          mediator.ask[OAuthControllerHelperService.TwitchCodeReceivedResponse](
            ref =>
              ArchieMateMediator.SendOAuthControllerHelperServiceCommand(
                OAuthControllerHelperService
                  .TwitchCodeReceived(ref, code, scope, state)
              )
          )
        ) {
          case Success(OAuthControllerHelperService.Unauthorized(msg)) =>
            complete(StatusCodes.Unauthorized, msg)

          case Success(OAuthControllerHelperService.InternalError(msg)) =>
            complete(StatusCodes.InternalServerError, msg)

          case Success(OAuthControllerHelperService.Authorized(_, _)) =>
            redirect("/dashboard", StatusCodes.TemporaryRedirect)

          case Success(OAuthControllerHelperService.GeneratedJWT(jwt)) =>
            createSessionCookie(jwt) {
              redirect("/dashboard", StatusCodes.TemporaryRedirect)
            }

          case Failure(ex) =>
            log.error(
              "OAuthControllerHelperService failed to respond correctly in time and therefore login cannot finish",
              ex
            )
            complete(
              StatusCodes.InternalServerError,
              HttpEntity(ContentTypes.`text/plain(UTF-8)`, unknownError)
            )
        }
      } ~ path("connection") {
        onComplete(
          mediator.ask[OAuthControllerHelperService.TwitchRedirectResponse](
            ref =>
              ArchieMateMediator.SendOAuthControllerHelperServiceCommand(
                OAuthControllerHelperService.NewTwitchConnectionRequest(ref)
              )
          )
        ) {
          case Success(
                OAuthControllerHelperService.TwitchRedirect(redirectUri)
              ) =>
            redirect(redirectUri, StatusCodes.TemporaryRedirect)

          case Success(OAuthControllerHelperService.CannotRedirect(message)) =>
            complete(StatusCodes.InternalServerError, message)

          case Failure(ex) =>
            log.error(
              "OAuthControllerService failed to respond correctly in time and therefore cannot redirect to Twitch to initialize connection",
              ex
            )
            complete(StatusCodes.InternalServerError, unknownError)
        }
      } ~ pathEndOrSingleSlash {
        onComplete(
          mediator.ask[OAuthControllerHelperService.TwitchRedirectResponse](
            ref =>
              ArchieMateMediator.SendOAuthControllerHelperServiceCommand(
                OAuthControllerHelperService.NewTwitchLoginRequest(ref)
              )
          )
        ) {
          case Success(
                OAuthControllerHelperService.TwitchRedirect(redirectUri)
              ) =>
            redirect(redirectUri, StatusCodes.TemporaryRedirect)

          case Success(OAuthControllerHelperService.CannotRedirect(message)) =>
            complete(StatusCodes.InternalServerError, message)

          case Failure(ex) =>
            log.error(
              "OAuthControllerService failed to respond correctly in time and therefore cannot redirect to Twitch to initialize login",
              ex
            )
            complete(StatusCodes.InternalServerError, unknownError)
        }
      }
    }
  }
}
