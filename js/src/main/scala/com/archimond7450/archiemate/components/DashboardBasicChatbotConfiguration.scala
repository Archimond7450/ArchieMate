package com.archimond7450.archiemate.components

import com.archimond7450.archiemate.{Loaded, Loading}
import com.archimond7450.archiemate.elements.StyledStandardElements.h2Element
import com.archimond7450.archiemate.elements.Switches.switch
import com.archimond7450.archiemate.helpers.FetchHelpers.{fetchBasicChatbotSettings, fetchImmediateGetStream, fetchPostStream}
import com.archimond7450.archiemate.http.ChannelSettings.BasicChatbotSettings
import com.archimond7450.archiemate.http.Connections.Connections
import com.archimond7450.archiemate.models.DashboardModel
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDivElement

import scala.util.{Failure, Success}

object DashboardBasicChatbotConfiguration {
  final val basicChatbotSettingsEndpoint = "/api/v1/settings/basic"

  def render(): ReactiveHtmlElement[HTMLDivElement] = {
    val basicChatbotSettingsStream = fetchImmediateGetStream[BasicChatbotSettings](basicChatbotSettingsEndpoint)
    div(
      cls("mt-12 border rounded-lg"),
      h2Element("Basic chatbot configuration"),
      child <-- basicChatbotSettingsStream.splitStatus(
        (resolved, _) => resolved.output match {
          case Right(settings) =>
            val joinTwitch: Var[Boolean] = Var(settings.join)
            val saveClicked: Var[Boolean] = Var(false)
            val saveFailed: Var[Boolean] = Var(false)
            form(
              onSubmit.preventDefault --> {_ =>},
              div(
                cls("flex items-start flex-col md:flex-row"),
                switch("join-twitch", "Leave", "Join", joinTwitch),
                button(
                  cls("text-black font-bold rounded-md px-4 py-2 md:mx-2 md:my-4 transition"),
                  cls <-- saveFailed.signal.map {
                    case true => "bg-red-500 hover:bg-red-600"
                    case false => "bg-green-500 hover:bg-green-600"
                  },
                  text <-- saveClicked.signal.combineWith(saveFailed.signal).map {
                    case (true, _) => "Saving..."
                    case (false, true) => "Save failed! Try again?"
                    case (false, false) => "Save"
                  },
                  disabled <-- saveClicked.signal,
                  /*onClick.preventDefault(_.withCurrentValueOf(joinTwitch)) --> { params =>
                    val responseStream = fetchPostStream(basicChatbotSettingsEndpoint, BasicChatbotSettings(params._2))

                    val saveClickedSignal = responseStream.map(_.isPending)
                    val saveFailedSignal = responseStream.splitStatus(
                      (resolved, _) => resolved.output != 200,
                      (_, _) => false
                    )

                    saveClickedSignal --> saveClicked
                    saveFailedSignal --> saveFailed
                  },*/
                  inContext { thisNode =>
                    val responses = thisNode.events(onClick).sample(joinTwitch).flatMapSwitch { join =>
                      fetchPostStream(basicChatbotSettingsEndpoint, BasicChatbotSettings(join))
                    }
                    val saveClickedSignal = responses.map(_.isPending)
                    val saveFailedSignal = responses.splitStatus(
                      (resolved, _) => resolved.output != 204,
                      (_, _) => false
                    )

                    saveClickedSignal --> saveClicked
                    saveFailedSignal --> saveFailed
                  }
                )
              )
            )

          case Left(ex) =>
            div(s"Failed to load! ${ex.getMessage}")
        },
        (_, _) => div("Loading...")
      )
    )
  }
}
