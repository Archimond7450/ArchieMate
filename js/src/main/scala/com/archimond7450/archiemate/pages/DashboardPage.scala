package com.archimond7450.archiemate.pages

import com.archimond7450.archiemate.App.{Login, Page}
import com.archimond7450.archiemate.components.{DashboardBasicChatbotConfiguration, DashboardConnections, DashboardTwitchInformation}
import com.archimond7450.archiemate.{Loaded, Loading, LoadingState}
import com.archimond7450.archiemate.elements.Buttons.asyncButton
import com.archimond7450.archiemate.elements.StyledStandardElements.{h1Element, h1ElementMapped, h2Element, pElement}
import com.archimond7450.archiemate.elements.Switches.switch
import com.archimond7450.archiemate.helpers.FetchHelpers.{fetchAutomaticMessagesSettings, fetchBasicChatbotSettings, fetchBuiltInCommandsSettings, fetchCommandsSettings, fetchImmediateGetStream, fetchOverlaysSettings, fetchTimersSettings, fetchVariablesSettings}
import com.archimond7450.archiemate.http.ChannelSettings.{BasicChatbotSettings, Settings}
import com.archimond7450.archiemate.http.Connections.Connections
import com.archimond7450.archiemate.http.User.UserResponse
import com.archimond7450.archiemate.models.AuthModel.LoggedOut
import com.archimond7450.archiemate.models.{AuthModel, DashboardModel}
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.waypoint.*
import org.scalajs.dom.{HTMLElement, Request, RequestCredentials}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object DashboardPage {
  def render()(using authModel: AuthModel, router: Router[Page]): ReactiveHtmlElement[HTMLElement] = {
    given dashboardModel: DashboardModel = new DashboardModel

    sectionTag(
      div(
        cls("mx-auto text-center mt-12"),
        h1Element("Dashboard")
      ),
      child <-- authModel.stateSignal.map {
        case Loaded(Success(AuthModel.LoggedIn(user))) =>
          val connectionsStream: EventStream[Status[String, Either[Throwable, Connections]]] = fetchImmediateGetStream[Connections]("/api/v1/settings/connections")
          val twitchConnectionExistsStream: EventStream[Boolean] = connectionsStream.splitStatus(
            (resolved, _) => resolved.output match {
              case Right(connections) => connections.twitchConnectionExists
              case Left(_) => false
            },
            (_, _) => false
          )
          div(
            DashboardTwitchInformation.render(user),
            div(
              cls("mt-12 border rounded-lg"),
              h2Element("Connections"),
              DashboardConnections.render(connectionsStream)
            ),
            child(
              DashboardBasicChatbotConfiguration.render()
            ) <-- twitchConnectionExistsStream
          )

        case Loaded(Success(LoggedOut)) | Loaded(Failure(_)) =>
          router.replaceState(Login)
          div()

        case Loading =>
          div("Loading...")
      }
    )
  }
}
