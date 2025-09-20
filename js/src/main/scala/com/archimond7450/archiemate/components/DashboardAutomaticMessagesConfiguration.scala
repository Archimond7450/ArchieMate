package com.archimond7450.archiemate.components

import com.archimond7450.archiemate.elements.StyledStandardElements.{
  h2Element,
  pElement
}
import com.archimond7450.archiemate.helpers.FetchHelpers.{
  fetchGetStream,
  fetchPostStream
}
import com.archimond7450.archiemate.http.ChannelSettings.AutomaticMessagesSettings
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDivElement

object DashboardAutomaticMessagesConfiguration {
  final val automaticMessagesSettingsEndpoint =
    "/api/v1/settings/automatic_messages"

  def render(): ReactiveHtmlElement[HTMLDivElement] = {
    val automaticMessagesStream = fetchGetStream[AutomaticMessagesSettings](
      automaticMessagesSettingsEndpoint
    )

    div(
      cls("mt-12 border rounded-lg"),
      h2Element("Automatic messages configuration"),
      child <-- automaticMessagesStream.splitStatus(
        (resolved, _) =>
          resolved.output match {
            case Right(settings) =>
              val state: Var[AutomaticMessagesSettings] = Var(settings)
              val streamStart: Var[Option[String]] = {
                state.zoomLazy(_.streamStart)((old, newStreamStart) =>
                  old.copy(streamStart = newStreamStart)
                )
              }
              val streamEnd: Var[Option[String]] = {
                state.zoomLazy(_.streamEnd)((old, newStreamEnd) =>
                  old.copy(streamEnd = newStreamEnd)
                )
              }
              form(
                onSubmit.preventDefault --> { _ => },
                OptionalInput.render(
                  "Stream start",
                  s"Message sent when the stream starts | The following variables can be used: $${game}, $${title}",
                  streamStart
                ),
                OptionalInput.render(
                  "Stream end",
                  "Message sent when the stream ends",
                  streamEnd
                ),
                SaveButton.render[AutomaticMessagesSettings](
                  state,
                  currentSettings =>
                    fetchPostStream(
                      automaticMessagesSettingsEndpoint,
                      currentSettings
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
