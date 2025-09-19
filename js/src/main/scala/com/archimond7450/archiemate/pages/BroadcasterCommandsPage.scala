package com.archimond7450.archiemate.pages

import com.archimond7450.archiemate.App.BroadcasterCommands
import com.archimond7450.archiemate.{Loaded, Loading}
import com.archimond7450.archiemate.CustomAttrs.scope
import com.archimond7450.archiemate.elements.StyledStandardElements.{h1Element, pElement}
import com.archimond7450.archiemate.helpers.FetchHelpers.fetchBroadcasterCommands
import com.archimond7450.archiemate.http.ChannelSettings.CommandsSettings
import com.archimond7450.archiemate.models.BroadcasterCommandsModel
import com.raquo.laminar.api.L.{*, given}

import scala.util.{Failure, Success}

object BroadcasterCommandsPage {
  def render(signal: Signal[BroadcasterCommands]): HtmlElement = {
    val model = new BroadcasterCommandsModel
    sectionTag(
      children <-- signal.map { page =>
        Seq(
          div(
            cls("max-w-3xl mx-auto text-center mt-12"),
            h1Element(signal.map(page => s"Commands in the ${page.userName} channel"))
          ),
          div(
            child <-- model.commandsSettingsSignal.map {
              case Loading =>
                fetchBroadcasterCommands(model, page.userName)
                "Loading"

              case Loaded(Success(CommandsSettings(commands))) =>
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
                                cls("text-sm font-medium text-gray-900 px-6 py-4 text-left"),
                                "#"
                              ),
                              th(
                                scope("col"),
                                cls("text-sm font-medium text-gray-900 px-6 py-4 text-left"),
                                "Command"
                              ),
                              th(
                                scope("col"),
                                cls("text-sm font-medium text-gray-900 px-6 py-4 text-left"),
                                "Response"
                              )
                            )
                          ),
                          tbody(
                            commands.zipWithIndex.map { (command, index) =>
                              tr(
                                cls(s"${if (index % 2 == 0) "bg-white" else "bg-gray-100"} border-b"),
                                td(
                                  cls("px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900"),
                                  index.toString
                                ),
                                td(
                                  cls("text-sm text-gray-900 font-light px-6 py-4 whitespace-nowrap"),
                                  s"!${command.name}"
                                ),
                                td(
                                  cls("text-sm text-gray-900 font-light px-6 py-4 whitespace-nowrap"),
                                  command.response
                                )
                              )
                            }
                          )
                        )
                      )
                    )
                  )
                )
              case Loaded(Failure(ex)) =>
                div(
                  pElement(s"No commands found for the ${page.userName} channel. Either the broadcaster has not added any commands yet or they don't use ArchieMate."),
                  pElement(s"Error description: ${ex.getMessage}")
                )
            }
          )
        )
      }
    )
  }
}
