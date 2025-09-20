package com.archimond7450.archiemate.components

import com.archimond7450.archiemate.{Loaded, Loading}
import com.archimond7450.archiemate.elements.StyledStandardElements.h2Element
import com.archimond7450.archiemate.elements.Switches.switch
import com.archimond7450.archiemate.helpers.FetchHelpers.{
  fetchBasicChatbotSettings,
  fetchGetStream,
  fetchPostStream
}
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
    val basicChatbotSettingsStream =
      fetchGetStream[BasicChatbotSettings](
        basicChatbotSettingsEndpoint
      )
    div(
      cls("mt-12 border rounded-lg"),
      h2Element("Basic chatbot configuration"),
      child <-- basicChatbotSettingsStream.splitStatus(
        (resolved, _) =>
          resolved.output match {
            case Right(settings) =>
              val state: Var[BasicChatbotSettings] = Var(settings)
              val joinTwitch: Var[Boolean] =
                state.zoomLazy(_.join)((old, join) => old.copy(join = join))
              form(
                onSubmit.preventDefault --> { _ => },
                SaveButton.render[BasicChatbotSettings](
                  state,
                  currentSettings =>
                    fetchPostStream(
                      basicChatbotSettingsEndpoint,
                      currentSettings
                    )
                ),
                div(
                  cls("flex items-start flex-col md:flex-row"),
                  switch("join-twitch", "Leave", "Join", joinTwitch)
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
