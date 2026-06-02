package com.archimond7450.archiemate.http.api.v1

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.services.JWTService
import com.archimond7450.archiemate.actors.services.controllerhelpers.UserControllerHelperService
import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.helpers.HttpControllerHelpers.failWithoutSessionCookie
import com.archimond7450.archiemate.http.IController
import com.archimond7450.archiemate.http.User.{UserInfo, UserResponse}
import com.github.pjfanning.pekkohttpcirce.FailFastCirceSupport
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.event.LoggingAdapter
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout

import scala.util.{Failure, Success}

final class UserController(using
    mediator: ActorRef[ArchieMateMediator.Command]
)(using Settings, Scheduler, Timeout)
    extends IController("user")
    with FailFastCirceSupport {
  override def routes: Route = extractLog { log =>
    given LoggingAdapter = log
    info
  }

  private def info(using log: LoggingAdapter): Route =
    (get & pathEndOrSingleSlash) {
      failWithoutSessionCookie { jwt =>
        onComplete(
          mediator.ask[UserControllerHelperService.GetUserResponse](ref =>
            ArchieMateMediator.SendUserControllerHelperServiceCommand(
              UserControllerHelperService.GetUser(ref, jwt.value)
            )
          )
        ) {
          case Success(
                UserControllerHelperService.GetUserOKResponse(
                  Success(twitchUser),
                  Success(kickUser)
                )
              ) =>
            complete(
              UserResponse(
                UserInfo(
                  userId = twitchUser.id,
                  userName = twitchUser.login,
                  userDisplayName = twitchUser.displayName,
                  profilePictureUrl = twitchUser.profileImageUrl
                ),
                kickUser.map(user =>
                  UserInfo(
                    userId = user.userId.toString,
                    userName = user.name.toLowerCase(),
                    userDisplayName = user.name,
                    profilePictureUrl = user.profilePicture
                  )
                )
              )
            )

          case Success(UserControllerHelperService.InvalidJWT) =>
            complete(StatusCodes.Unauthorized)

          case Success(
                UserControllerHelperService.GetUserOKResponse(_, _)
              ) =>
            complete(StatusCodes.InternalServerError)

          case Failure(ex) =>
            complete(StatusCodes.InternalServerError)
        }
      }
    }
}
