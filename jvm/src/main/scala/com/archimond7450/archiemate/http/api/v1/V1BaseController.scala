package com.archimond7450.archiemate.http.api.v1

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.repositories.sessions.TwitchUserSessionsRepository
import com.archimond7450.archiemate.actors.services.controllerhelpers.{
  CommandsControllerHelperService,
  SettingsControllerHelperService,
  UserControllerHelperService
}
import com.archimond7450.archiemate.actors.services.JWTService
import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.helpers.HttpControllerHelpers.failWithoutSessionCookie
import com.archimond7450.archiemate.http.IController
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.event.LoggingAdapter
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.PathMatcher.given
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout

import scala.util.{Failure, Success}

final class V1BaseController(using
    ActorRef[ArchieMateMediator.Command],
    Settings,
    Scheduler,
    Timeout
) extends IController("api" / "v1") {
  private val healthController = new HealthController
  private val userController = new UserController
  private val settingsController = new SettingsController
  private val commandsController = new CommandsController

  override def routes: Route = extractLog { log =>
    given LoggingAdapter = log
    healthController.getAllRoutes ~ userController.getAllRoutes ~ settingsController.getAllRoutes ~ commandsController.getAllRoutes
  }
}
