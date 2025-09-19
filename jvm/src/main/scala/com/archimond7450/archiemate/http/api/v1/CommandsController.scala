package com.archimond7450.archiemate.http.api.v1

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.services.controllerhelpers.CommandsControllerHelperService
import com.archimond7450.archiemate.http.IController
import com.archimond7450.archiemate.http.ChannelSettings.CommandsSettings
import com.github.pjfanning.pekkohttpcirce.FailFastCirceSupport
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.event.LoggingAdapter
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout

import scala.util.{Failure, Success}

class CommandsController(using mediator: ActorRef[ArchieMateMediator.Command])(using Scheduler, Timeout) extends IController("commands") with FailFastCirceSupport {
  override def routes: Route = extractLog { log =>
    given LoggingAdapter = log

    getChannelCommands
  }

  private def getChannelCommands(using log: LoggingAdapter): Route = (path(Segment) & pathEndOrSingleSlash) { twitchChannelName =>
    onComplete(mediator.ask[CommandsSettings](ref => ArchieMateMediator.SendCommandsControllerHelperServiceCommand(CommandsControllerHelperService.GetCommandsForChannelName(ref, twitchChannelName)))) {
      case Success(settings) =>
        complete(StatusCodes.OK, settings)

      case Failure(ex) =>
        log.error(ex, "Failed to retrieve channel {} commands settings!", twitchChannelName)
        complete(StatusCodes.InternalServerError)
    }
  }
}
