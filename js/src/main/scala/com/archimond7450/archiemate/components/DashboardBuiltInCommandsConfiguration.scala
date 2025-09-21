package com.archimond7450.archiemate.components

import com.archimond7450.archiemate.elements.StyledStandardElements.{
  h2Element,
  pElement
}
import com.archimond7450.archiemate.elements.Switches.switch
import com.archimond7450.archiemate.helpers.FetchHelpers.{
  fetchGetStream,
  fetchPostStream
}
import com.archimond7450.archiemate.http.ChannelSettings.BuiltInCommandsSettings
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDivElement

object DashboardBuiltInCommandsConfiguration {
  final val builtInCommandsSettingsEndpoint =
    "/api/v1/settings/commands/builtin"

  def render(): ReactiveHtmlElement[HTMLDivElement] = {
    val builtInCommandsSettingsStream =
      fetchGetStream[BuiltInCommandsSettings](
        builtInCommandsSettingsEndpoint
      )
    div(
      cls("mt-12 border rounded-lg"),
      h2Element("Built-in commands configuration"),
      child <-- builtInCommandsSettingsStream.splitStatus(
        (resolved, _) =>
          resolved.output match {
            case Right(settings) =>
              val state: Var[BuiltInCommandsSettings] = Var(settings)
              val game: Var[Boolean] =
                state.zoomLazy(_.game)((old, game) => old.copy(game = game))
              val title: Var[Boolean] =
                state.zoomLazy(_.title)((old, title) => old.copy(title = title))
              val subs: Var[Boolean] =
                state.zoomLazy(_.subs)((old, subs) => old.copy(subs = subs))
              val uptime: Var[Boolean] =
                state.zoomLazy(_.uptime)((old, uptime) =>
                  old.copy(uptime = uptime)
                )
              val followage: Var[Boolean] =
                state.zoomLazy(_.followage)((old, newFollowage) =>
                  old.copy(followage = newFollowage)
                )
              val afk: Var[Boolean] =
                state.zoomLazy(_.afk)((old, newAfk) => old.copy(afk = newAfk))

              form(
                onSubmit.preventDefault --> { _ => },
                div(
                  cls("flex items-start flex-col md:flex-row"),
                  pElement("!game"),
                  switch("game", "Off", "On", game)
                ),
                div(
                  cls("flex items-start flex-col md:flex-row"),
                  pElement("!title"),
                  switch("title", "Off", "On", title)
                ),
                div(
                  cls("flex items-start flex-col md:flex-row"),
                  pElement("!subs"),
                  switch("subs", "Off", "On", subs)
                ),
                div(
                  cls("flex items-start flex-col md:flex-row"),
                  pElement("!uptime"),
                  switch("uptime", "Off", "On", uptime)
                ),
                div(
                  cls("flex items-start flex-col md:flex-row"),
                  pElement("!followage"),
                  switch("followage", "Off", "On", followage)
                ),
                div(
                  cls("flex items-start flex-col md:flex-row"),
                  pElement("!afk"),
                  switch("afk", "Off", "On", afk)
                ),
                SaveButton.render[BuiltInCommandsSettings](
                  state,
                  currentSettings =>
                    fetchPostStream(
                      builtInCommandsSettingsEndpoint,
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
