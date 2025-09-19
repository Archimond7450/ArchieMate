package com.archimond7450.archiemate.actors

import com.archimond7450.archiemate.actors.chatbot.TwitchChatbotsSupervisor
import com.archimond7450.archiemate.actors.repositories.sessions.{TwitchUserSessionsRepository, YouTubeChannelSessionsRepository}
import com.archimond7450.archiemate.actors.repositories.settings.{AutomaticMessagesSettingsRepository, BasicChatbotSettingsRepository, BuiltInCommandsSettingsRepository, CommandsSettingsRepository, OverlaysSettingsRepository, TimersSettingsRepository, VariablesSettingsRepository}
import com.archimond7450.archiemate.actors.services.caches.TwitchTokenUserCacheService
import com.archimond7450.archiemate.actors.services.controllerhelpers.{CommandsControllerHelperService, OAuthControllerHelperService, SettingsControllerHelperService, UserControllerHelperService}
import com.archimond7450.archiemate.actors.services.{JWTService, TwitchLoginValidatorService}
import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.actors.youtube.api.YouTubeApiClient
import org.apache.pekko.actor.typed.{ActorRef, Behavior, Scheduler}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import com.archimond7450.archiemate.extensions.*
import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.http.OAuthController
import com.archimond7450.archiemate.http.api.v1.{SettingsController, V1BaseController}
import com.archimond7450.archiemate.providers.{RandomProvider, TimeProvider}
import org.apache.pekko.actor.ClassicActorSystemProvider
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.model.headers.HttpCookie
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout

import scala.util.{Failure, Success, Try}
import scala.concurrent.duration.DurationInt

object UserGuardian {
  val actorName = "user"
  
  sealed trait Command
  private case object StartHttp extends Command
  private final case class HttpBound(binding: Http.ServerBinding) extends Command
  private final case class HttpFailed(ex: Throwable) extends Command
  private case object Stop extends Command

  def apply(): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx
    Try(Settings(ctx.system)) match {
      case Success(settings) =>
        ctx.log.info("Settings retrieved successfully!")
        ctx.self ! StartHttp
        given Settings = settings
        withSettings

      case Failure(ex) =>
        ctx.log.error("Settings could not be retrieved!", ex)
        Behaviors.stopped
    }
  }

  private def withSettings(using ctx: ActorContext[Command], settings: Settings): Behavior[Command] = Behaviors.receiveAndLogMessage {
    case StartHttp =>
      val (interface, port) = (settings.httpInterface, settings.httpPort)
      ctx.log.info("Starting HTTP server at {}:{}", interface, port)
      given ClassicActorSystemProvider = ctx.system.classicSystem
      given Scheduler = ctx.system.scheduler
      given Timeout = settings.askTimeout
      val server = Http().newServerAt(interface = interface, port = port)

      given RandomProvider = new RandomProvider
      given TimeProvider = new TimeProvider

      given Settings = settings

      given ActorRef[ArchieMateMediator.Command] = ctx.spawn(ArchieMateMediator(), ArchieMateMediator.actorName)

      val v1BaseController = new V1BaseController
      val OAuthController = new OAuthController

      def logout: Route = (get & path("logout" / "confirm")) {
        optionalCookie("session") {
          case Some(jwt) =>
            setCookie(
              HttpCookie("session", jwt.value)
                .withPath("/")
                .withSecure(settings.secureCookies)
                .withHttpOnly(true)
                .withMaxAge(0)
            ) {
              redirect("/", StatusCodes.TemporaryRedirect)
            }
          case None =>
            redirect("/", StatusCodes.TemporaryRedirect)
        }
      }

      val routes = logout ~ v1BaseController.getAllRoutes ~ OAuthController.getAllRoutes ~ getFromResourceDirectory("public") ~ getFromResource("public/index.html")

      ctx.pipeToSelf(server.bind(routes)) {
        case Success(binding) => HttpBound(binding)
        case Failure(ex) => HttpFailed(ex)
      }

      Behaviors.same

    case HttpFailed(ex) =>
      ctx.log.error("Failed to bind the HTTP server!", ex)
      Behaviors.stopped

    case HttpBound(binding) =>
      ctx.log.info("HTTP server successfully bound to {}", binding.localAddress)
      binding.addToCoordinatedShutdown(10.seconds)(ctx.system.classicSystem)

      ready

    case Stop =>
      Behaviors.stopped
  }

  private def ready(using ctx: ActorContext[Command], settings: Settings): Behavior[Command] = Behaviors.receiveAndLogMessage {
    case Stop =>
      ctx.log.info("Stopping ArchieMate!")
      Behaviors.stopped

    case msg =>
      ctx.log.debug("Ignoring message {}", msg)
      Behaviors.same
  }
}
