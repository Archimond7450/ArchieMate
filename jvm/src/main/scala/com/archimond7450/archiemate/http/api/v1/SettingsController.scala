package com.archimond7450.archiemate.http.api.v1

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.services.controllerhelpers.SettingsControllerHelperService.{GetConnectionsOKResponse, InvalidJWT}
import com.archimond7450.archiemate.actors.services.JWTService
import com.archimond7450.archiemate.actors.services.controllerhelpers.SettingsControllerHelperService
import com.archimond7450.archiemate.helpers.HttpControllerHelpers.failWithoutSessionCookie
import com.archimond7450.archiemate.http.Connections.{Connections, YouTubeConnection}
import com.archimond7450.archiemate.http.ChannelSettings.*
import com.archimond7450.archiemate.http.{ChannelSettings, IController}
import com.github.pjfanning.pekkohttpcirce.FailFastCirceSupport
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.event.LoggingAdapter
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.model.headers.HttpCookiePair
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout

import scala.util.{Failure, Success}

final class SettingsController(using mediator: ActorRef[ArchieMateMediator.Command])(using Scheduler, Timeout) extends IController("settings") with FailFastCirceSupport {
  override def routes: Route = extractLog { log =>
    given LoggingAdapter = log
    connections
      ~ getBasicChatbotSettings
      ~ getBuiltInCommandsSettings
      ~ getCommandsSettings
      ~ getVariablesSettings
      ~ getTimersSettings
      ~ getOverlaysSettings
      ~ getAutomaticMessagesSettings
      ~ changeBasicChatbotSettings
      ~ changeBuiltInCommandsSettings
      ~ changeCommandsSettings
      ~ changeVariablesSettings
      ~ changeTimersSettings
      ~ changeOverlaysSettings
      ~ changeAutomaticMessagesSettings
  }

  private def connections(using log: LoggingAdapter): Route = (get & path("connections")) {
    failWithoutSessionCookie { jwt =>
      onComplete(mediator.ask[SettingsControllerHelperService.GetConnectionsResponse](ref => ArchieMateMediator.SendSettingsControllerHelperServiceCommand(SettingsControllerHelperService.GetConnections(ref, jwt.value)))) {
        case Success(response: GetConnectionsOKResponse) =>
          if (response.tryTwitchConnected.isFailure || response.tryYouTubeConnections.isFailure || response.tryYouTubeConnections.get.exists(_.isFailure)) {
            log.error("Connections response from helper service contains fails: {}", response)
          }
          complete(
            StatusCodes.OK,
            Connections(
              response.tryTwitchConnected match {
                case Success(twitchConnected) => twitchConnected
                case Failure(ex) => false
              },
              response.tryYouTubeConnections match {
                case Success(youtubeConnections) => youtubeConnections.filter(_.isSuccess).map(_.get).map(connection => YouTubeConnection(connection.channelId, connection.channelName, connection.channelProfileImageUrl))
                case Failure(ex) => Nil
              }
            )
          )

        case Success(InvalidJWT) =>
          complete(StatusCodes.Forbidden)

        case Failure(ex) =>
          log.error(ex, "Failed to get connections response from helper service")
          complete(StatusCodes.InternalServerError)
      }
    }
  }

  private def getBasicChatbotSettings(using log: LoggingAdapter): Route = (get & path("basic")) {
    failWithoutSessionCookie { jwt =>
      onComplete(mediator.ask[SettingsControllerHelperService.GetBasicChatbotSettingsResponse](ref => ArchieMateMediator.SendSettingsControllerHelperServiceCommand(SettingsControllerHelperService.GetBasicChatbotSettings(ref, jwt.value)))) {
        case Success(SettingsControllerHelperService.GetBasicChatbotSettingsOKResponse(Success(settings: BasicChatbotSettings))) =>
          complete(StatusCodes.OK, settings)

        case Success(SettingsControllerHelperService.GetBasicChatbotSettingsOKResponse(Failure(ex))) =>
          log.error(ex, "Failed to retrieve basic chatbot settings")
          complete(StatusCodes.InternalServerError)

        case Success(SettingsControllerHelperService.InvalidJWT) =>
          complete(StatusCodes.Forbidden)

        case Failure(ex) =>
          completeFail(ex)
      }
    }
  }

  private def getBuiltInCommandsSettings(using log: LoggingAdapter): Route = (get & path("commands" / "builtin")) {
    failWithoutSessionCookie { jwt =>
      onComplete(mediator.ask[SettingsControllerHelperService.GetBuiltInCommandsSettingsResponse](ref => ArchieMateMediator.SendSettingsControllerHelperServiceCommand(SettingsControllerHelperService.GetBuiltInCommandsSettings(ref, jwt.value)))) {
        case Success(SettingsControllerHelperService.GetBuiltInCommandsSettingsOKResponse(Success(settings: BuiltInCommandsSettings))) =>
          complete(StatusCodes.OK, settings)

        case Success(SettingsControllerHelperService.GetBuiltInCommandsSettingsOKResponse(Failure(ex))) =>
          log.error(ex, "Failed to retrieve built in commands settings")
          complete(StatusCodes.InternalServerError)

        case Success(SettingsControllerHelperService.InvalidJWT) =>
          complete(StatusCodes.Forbidden)

        case Failure(ex) =>
          completeFail(ex)
      }
    }
  }

  private def getCommandsSettings(using log: LoggingAdapter): Route = (get & path("commands")) {
    failWithoutSessionCookie { jwt =>
      onComplete(mediator.ask[SettingsControllerHelperService.GetCommandsSettingsResponse](ref => ArchieMateMediator.SendSettingsControllerHelperServiceCommand(SettingsControllerHelperService.GetCommandsSettings(ref, jwt.value)))) {
        case Success(SettingsControllerHelperService.GetCommandsSettingsOKResponse(Success(settings))) =>
          complete(StatusCodes.OK, settings)

        case Success(SettingsControllerHelperService.GetCommandsSettingsOKResponse(Failure(ex))) =>
          log.error(ex, "Failed to retrieve commands settings")
          complete(StatusCodes.InternalServerError)

        case Success(SettingsControllerHelperService.InvalidJWT) =>
          complete(StatusCodes.Forbidden)

        case Failure(ex) =>
          completeFail(ex)
      }
    }
  }

  private def getVariablesSettings(using log: LoggingAdapter): Route = (get & path("variables")) {
    failWithoutSessionCookie { jwt =>
      onComplete(mediator.ask[SettingsControllerHelperService.GetVariablesSettingsResponse](ref => ArchieMateMediator.SendSettingsControllerHelperServiceCommand(SettingsControllerHelperService.GetVariablesSettings(ref, jwt.value)))) {
        case Success(SettingsControllerHelperService.GetVariablesSettingsOKResponse(Success(settings))) =>
          complete(StatusCodes.OK, settings)

        case Success(SettingsControllerHelperService.GetVariablesSettingsOKResponse(Failure(ex))) =>
          log.error(ex, "Failed to retrieve variables settings")
          complete(StatusCodes.InternalServerError)

        case Success(SettingsControllerHelperService.InvalidJWT) =>
          complete(StatusCodes.Forbidden)

        case Failure(ex) =>
          completeFail(ex)
      }
    }
  }

  private def getTimersSettings(using log: LoggingAdapter): Route = (get & path("timers")) {
    failWithoutSessionCookie { jwt =>
      onComplete(mediator.ask[SettingsControllerHelperService.GetTimersSettingsResponse](ref => ArchieMateMediator.SendSettingsControllerHelperServiceCommand(SettingsControllerHelperService.GetTimersSettings(ref, jwt.value)))) {
        case Success(SettingsControllerHelperService.GetTimersSettingsOKResponse(Success(settings))) =>
          complete(StatusCodes.OK, settings)

        case Success(SettingsControllerHelperService.GetTimersSettingsOKResponse(Failure(ex))) =>
          log.error(ex, "Failed to retrieve timers settings")
          complete(StatusCodes.InternalServerError)

        case Success(SettingsControllerHelperService.InvalidJWT) =>
          complete(StatusCodes.Forbidden)

        case Failure(ex) =>
          completeFail(ex)
      }
    }
  }

  private def getOverlaysSettings(using log: LoggingAdapter): Route = (get & path("overlays")) {
    failWithoutSessionCookie { jwt =>
      onComplete(mediator.ask[SettingsControllerHelperService.GetOverlaysSettingsResponse](ref => ArchieMateMediator.SendSettingsControllerHelperServiceCommand(SettingsControllerHelperService.GetOverlaysSettings(ref, jwt.value)))) {
        case Success(SettingsControllerHelperService.GetOverlaysSettingsOKResponse(Success(settings))) =>
          complete(StatusCodes.OK, settings)

        case Success(SettingsControllerHelperService.GetOverlaysSettingsOKResponse(Failure(ex))) =>
          log.error(ex, "Failed to retrieve overlays settings")
          complete(StatusCodes.InternalServerError)

        case Success(SettingsControllerHelperService.InvalidJWT) =>
          complete(StatusCodes.Forbidden)

        case Failure(ex) =>
          completeFail(ex)
      }
    }
  }

  private def getAutomaticMessagesSettings(using log: LoggingAdapter): Route = (get & path("automatic_messages")) {
    failWithoutSessionCookie { jwt =>
      onComplete(mediator.ask[SettingsControllerHelperService.GetAutomaticMessagesSettingsResponse](ref => ArchieMateMediator.SendSettingsControllerHelperServiceCommand(SettingsControllerHelperService.GetAutomaticMessagesSettings(ref, jwt.value)))) {
        case Success(SettingsControllerHelperService.GetAutomaticMessagesSettingsOKResponse(Success(settings))) =>
          complete(StatusCodes.OK, settings)

        case Success(SettingsControllerHelperService.GetAutomaticMessagesSettingsOKResponse(Failure(ex))) =>
          log.error(ex, "Failed to retrieve automatic messages settings")
          complete(StatusCodes.InternalServerError)

        case Success(SettingsControllerHelperService.InvalidJWT) =>
          complete(StatusCodes.Forbidden)

        case Failure(ex) =>
          completeFail(ex)
      }
    }
  }

  private def changeBasicChatbotSettings(using log: LoggingAdapter): Route = (post & path("basic") & entity(as[BasicChatbotSettings])) { settings =>
    failWithoutSessionCookie { jwt =>
      onComplete(mediator.ask[SettingsControllerHelperService.ChangeSettingsResponse](ref => ArchieMateMediator.SendSettingsControllerHelperServiceCommand(SettingsControllerHelperService.ChangeBasicChatbotSettings(ref, jwt.value, settings)))) {
        case Success(SettingsControllerHelperService.SettingsChanged) =>
          complete(StatusCodes.NoContent)

        case Success(SettingsControllerHelperService.SettingsFailedToChange(ex)) =>
          log.error(ex, "Failed to change chatbot settings")
          complete(StatusCodes.InternalServerError)

        case Success(SettingsControllerHelperService.InvalidJWT) =>
          complete(StatusCodes.Forbidden)

        case Failure(ex) =>
          completeFail(ex)
      }
    }
  }

  private def changeBuiltInCommandsSettings(using log: LoggingAdapter): Route = (post & path("commands" / "builtin") & entity(as[BuiltInCommandsSettings])) { settings =>
    failWithoutSessionCookie { jwt =>
      onComplete(mediator.ask[SettingsControllerHelperService.ChangeSettingsResponse](ref => ArchieMateMediator.SendSettingsControllerHelperServiceCommand(SettingsControllerHelperService.ChangeBuiltInChatbotSettings(ref, jwt.value, settings)))) {
        case Success(SettingsControllerHelperService.SettingsChanged) =>
          complete(StatusCodes.NoContent)

        case Success(SettingsControllerHelperService.SettingsFailedToChange(ex)) =>
          log.error(ex, "Failed to change built in commands settings")
          complete(StatusCodes.InternalServerError)

        case Success(SettingsControllerHelperService.InvalidJWT) =>
          complete(StatusCodes.Forbidden)

        case Failure(ex) =>
          completeFail(ex)
      }
    }
  }

  private def changeCommandsSettings(using log: LoggingAdapter): Route = (post & path("commands") & entity(as[CommandsSettings])) { settings =>
    failWithoutSessionCookie { jwt =>
      onComplete(mediator.ask[SettingsControllerHelperService.ChangeSettingsResponse](ref => ArchieMateMediator.SendSettingsControllerHelperServiceCommand(SettingsControllerHelperService.ChangeCommandsSettings(ref, jwt.value, settings)))) {
        case Success(SettingsControllerHelperService.SettingsChanged) =>
          complete(StatusCodes.NoContent)

        case Success(SettingsControllerHelperService.SettingsFailedToChange(ex)) =>
          log.error(ex, "Failed to change commands settings")
          complete(StatusCodes.InternalServerError)

        case Success(SettingsControllerHelperService.InvalidJWT) =>
          complete(StatusCodes.Forbidden)

        case Failure(ex) =>
          completeFail(ex)
      }
    }
  }

  private def changeVariablesSettings(using log: LoggingAdapter): Route = (post & path("variables") & entity(as[VariablesSettings])) { settings =>
    failWithoutSessionCookie { jwt =>
      onComplete(mediator.ask[SettingsControllerHelperService.ChangeSettingsResponse](ref => ArchieMateMediator.SendSettingsControllerHelperServiceCommand(SettingsControllerHelperService.ChangeVariablesSettings(ref, jwt.value, settings)))) {
        case Success(SettingsControllerHelperService.SettingsChanged) =>
          complete(StatusCodes.NoContent)

        case Success(SettingsControllerHelperService.SettingsFailedToChange(ex)) =>
          log.error(ex, "Failed to change variables settings")
          complete(StatusCodes.InternalServerError)

        case Success(SettingsControllerHelperService.InvalidJWT) =>
          complete(StatusCodes.Forbidden)

        case Failure(ex) =>
          completeFail(ex)
      }
    }
  }

  private def changeTimersSettings(using log: LoggingAdapter): Route = (post & path("timers") & entity(as[TimersSettings])) { settings =>
    failWithoutSessionCookie { jwt =>
      onComplete(mediator.ask[SettingsControllerHelperService.ChangeSettingsResponse](ref => ArchieMateMediator.SendSettingsControllerHelperServiceCommand(SettingsControllerHelperService.ChangeTimersSettings(ref, jwt.value, settings)))) {
        case Success(SettingsControllerHelperService.SettingsChanged) =>
          complete(StatusCodes.NoContent)

        case Success(SettingsControllerHelperService.SettingsFailedToChange(ex)) =>
          log.error(ex, "Failed to change timers settings")
          complete(StatusCodes.InternalServerError)

        case Success(SettingsControllerHelperService.InvalidJWT) =>
          complete(StatusCodes.Forbidden)

        case Failure(ex) =>
          completeFail(ex)
      }
    }
  }

  private def changeOverlaysSettings(using log: LoggingAdapter): Route = (post & path("overlays") & entity(as[OverlaysSettings])) { settings =>
    failWithoutSessionCookie { jwt =>
      onComplete(mediator.ask[SettingsControllerHelperService.ChangeSettingsResponse](ref => ArchieMateMediator.SendSettingsControllerHelperServiceCommand(SettingsControllerHelperService.ChangeOverlaysSettings(ref, jwt.value, settings)))) {
        case Success(SettingsControllerHelperService.SettingsChanged) =>
          complete(StatusCodes.NoContent)

        case Success(SettingsControllerHelperService.SettingsFailedToChange(ex)) =>
          log.error(ex, "Failed to change overlays settings")
          complete(StatusCodes.InternalServerError)

        case Success(SettingsControllerHelperService.InvalidJWT) =>
          complete(StatusCodes.Forbidden)

        case Failure(ex) =>
          completeFail(ex)
      }
    }
  }

  private def changeAutomaticMessagesSettings(using log: LoggingAdapter): Route = (post & path("automatic_messages") & entity(as[AutomaticMessagesSettings])) { settings =>
    failWithoutSessionCookie { jwt =>
      onComplete(mediator.ask[SettingsControllerHelperService.ChangeSettingsResponse](ref => ArchieMateMediator.SendSettingsControllerHelperServiceCommand(SettingsControllerHelperService.ChangeAutomaticMessagesSettings(ref, jwt.value, settings)))) {
        case Success(SettingsControllerHelperService.SettingsChanged) =>
          complete(StatusCodes.NoContent)

        case Success(SettingsControllerHelperService.SettingsFailedToChange(ex)) =>
          log.error(ex, "Failed to change automatic messages settings")
          complete(StatusCodes.InternalServerError)

        case Success(SettingsControllerHelperService.InvalidJWT) =>
          complete(StatusCodes.Forbidden)

        case Failure(ex) =>
          completeFail(ex)
      }
    }
  }

  private def completeFail(ex: Throwable)(using log: LoggingAdapter): Route = {
    log.error(ex, "Failed to retrieve a response from helper service")
    complete(StatusCodes.InternalServerError)
  }
}
