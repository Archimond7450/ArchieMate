package com.archimond7450.archiemate.components

import com.archimond7450.archiemate.CustomAttrs.scope
import com.archimond7450.archiemate.elements.StyledStandardElements.h2Element
import com.archimond7450.archiemate.helpers.FetchHelpers.{
  fetchGetStream,
  fetchPostStream
}
import com.archimond7450.archiemate.http.ChannelSettings.{
  ChannelCommand,
  CommandsSettings
}
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDivElement

object DashboardCommandsConfiguration {
  final val commandsSettingsEndpoint = "/api/v1/settings/commands"

  def render(): ReactiveHtmlElement[HTMLDivElement] = {
    val commandsSettingsStream =
      fetchGetStream[CommandsSettings](commandsSettingsEndpoint)

    div(
      cls("mt-12 border rounded-lg"),
      h2Element("Commands configuration"),
      form(
        onSubmit.preventDefault --> { _ => },
        child <-- commandsSettingsStream.splitStatus(
          (resolved, _) =>
            resolved.output match {
              case Right(settings) =>
                val state: Var[CommandsSettings] = Var(settings)
                val commands: Var[List[ChannelCommand]] =
                  state.zoomLazy(_.commands)((old, newCommands) =>
                    old.copy(newCommands)
                  )
                div(
                  cls("flex flex-col"),
                  div(
                    cls("overflow-x-auto sm:mx-0.5 lg:mx-0.5"),
                    div(
                      cls("py-2 inline-block min-w-full sm:px-6 lg:px-8"),
                      div(
                        cls("overflow-hidden"),
                        table(
                          cls("min-w-full"),
                          thead(
                            cls("bg-white border-b"),
                            tr(
                              th(
                                scope("col"),
                                cls(
                                  "text-sm font-medium text-gray-900 px-6 py-4 text-left"
                                ),
                                "Command"
                              ),
                              th(
                                scope("col"),
                                cls(
                                  "text-sm font-medium text-gray-900 px-6 py-4 text-left"
                                ),
                                "Response"
                              ),
                              th(
                                scope("col"),
                                cls(
                                  "text-sm font-medium text-gray-900 px-6 py-4 text-left"
                                ),
                                "Delete?"
                              )
                            )
                          ),
                          tbody(
                            children <-- commands.signal.map {
                              currentCommands =>
                                currentCommands.zipWithIndex
                                  .map { (command, index) =>
                                    tr(
                                      cls(s"${
                                          if (index % 2 == 0) "bg-white"
                                          else "bg-gray-100"
                                        } border-b"),
                                      td(
                                        cls(
                                          "px-6 py-4"
                                        ),
                                        input(
                                          cls(
                                            "w-full mt-1 p-3 border rounded-md focus:ring-2 focus:ring-blue-500 outline-none"
                                          ),
                                          autoComplete("off"),
                                          placeholder("(!)commandName"),
                                          controlled(
                                            value <-- commands.signal.map(
                                              _(index).name
                                            ),
                                            onInput.mapToValue --> { newName =>
                                              commands.update(cmds =>
                                                cmds.updated(
                                                  index,
                                                  cmds(index)
                                                    .copy(name = newName)
                                                )
                                              )
                                            }
                                          )
                                        )
                                      ),
                                      td(
                                        cls(
                                          "px-6 py-4"
                                        ),
                                        input(
                                          cls(
                                            "w-full mt-1 p-3 border rounded-md focus:ring-2 focus:ring-blue-500 outline-none"
                                          ),
                                          autoComplete("off"),
                                          placeholder("Command response"),
                                          controlled(
                                            value <-- commands.signal.map(
                                              _(index).response
                                            ),
                                            onInput.mapToValue --> {
                                              newResponse =>
                                                commands.update(cmds =>
                                                  cmds.updated(
                                                    index,
                                                    cmds(index).copy(response =
                                                      newResponse
                                                    )
                                                  )
                                                )
                                            }
                                          )
                                        )
                                      ),
                                      td(
                                        cls("px-6 py-4"),
                                        button(
                                          cls(
                                            "bg-red-700 text-white font-bold rounded-md px-4 py-2 md:mx-2 hover:bg-red-800 transition"
                                          ),
                                          "Delete",
                                          onClick --> { _ =>
                                            commands.update { cmds =>
                                              cmds.patch(index, Nil, 1)
                                            }
                                          }
                                        )
                                      )
                                    )
                                  }
                            }
                          ),
                          tfoot(
                            tr(
                              cls("bg-white border-b"),
                              td(
                                colSpan(3),
                                button(
                                  cls(
                                    "bg-blue-500 text-white font-bold rounded-md px-4 py-2 my-2 hover:bg-blue-700 transition"
                                  ),
                                  "Add new command",
                                  onClick --> { _ =>
                                    commands.update(
                                      _ :+ ChannelCommand(None, "", "")
                                    )
                                  }
                                ),
                                SaveButton.render[CommandsSettings](
                                  state,
                                  currentSettings =>
                                    fetchPostStream(
                                      commandsSettingsEndpoint,
                                      currentSettings
                                    )
                                )
                              )
                            )
                          )
                        )
                      )
                    )
                  )
                )

              case Left(ex) =>
                div(s"Failed to load! ${ex.getMessage}")
            },
          (_, _) => div("Loading...")
        )
      )
    )
  }
}
