package com.archimond7450.archiemate.components

import com.archimond7450.archiemate.CustomAttrs.scope
import com.archimond7450.archiemate.elements.StyledStandardElements.h2Element
import com.archimond7450.archiemate.helpers.FetchHelpers.{
  fetchGetStream,
  fetchPostStream
}
import com.archimond7450.archiemate.http.ChannelSettings.{
  ChannelVariable,
  VariablesSettings
}
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDivElement

object DashboardVariablesConfiguration {
  final val variablesSettingsEndpoint = "/api/v1/settings/variables"

  def render(): ReactiveHtmlElement[HTMLDivElement] = {
    val variablesSettingsStream =
      fetchGetStream[VariablesSettings](variablesSettingsEndpoint)

    div(
      cls("mt-12 border rounded-lg"),
      h2Element("Variables configuration"),
      form(
        onSubmit.preventDefault --> { _ => },
        child <-- variablesSettingsStream.splitStatus(
          (resolved, _) =>
            resolved.output match {
              case Right(settings) =>
                val state: Var[VariablesSettings] = Var(settings)
                val variables: Var[List[ChannelVariable]] =
                  state.zoomLazy(_.variables)((old, newVariables) =>
                    old.copy(newVariables)
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
                                "Variable"
                              ),
                              th(
                                scope("col"),
                                cls(
                                  "text-sm font-medium text-gray-900 px-6 py-4 text-left"
                                ),
                                "Value"
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
                            children <-- variables.signal.map {
                              currentVariables =>
                                currentVariables.zipWithIndex
                                  .map { (variable, index) =>
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
                                          placeholder("Variable name"),
                                          controlled(
                                            value <-- variables.signal.map(
                                              _(index).name
                                            ),
                                            onInput.mapToValue --> { newName =>
                                              variables.update(vars =>
                                                vars.updated(
                                                  index,
                                                  vars(index)
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
                                          placeholder("Variable value"),
                                          controlled(
                                            value <-- variables.signal.map(
                                              _(index).value
                                            ),
                                            onInput.mapToValue --> { newValue =>
                                              variables.update(cmds =>
                                                cmds.updated(
                                                  index,
                                                  cmds(index)
                                                    .copy(value = newValue)
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
                                            variables.update { cmds =>
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
                                  "Add new variable",
                                  onClick --> { _ =>
                                    variables.update(
                                      _ :+ ChannelVariable(None, "", "")
                                    )
                                  }
                                ),
                                SaveButton.render[VariablesSettings](
                                  state,
                                  currentSettings =>
                                    fetchPostStream(
                                      variablesSettingsEndpoint,
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
