package com.archimond7450.archiemate.components

import com.archimond7450.archiemate.elements.StyledStandardElements.pElement
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDivElement

object OptionalInput {
  def render(
      label: String,
      placeHolder: String,
      state: Var[Option[String]]
  ): ReactiveHtmlElement[HTMLDivElement] = {
    val hasValueSignal = state.signal.map(_.nonEmpty)
    val inputVar: Var[String] =
      state.zoomLazy(_.getOrElse(""))((old, newState) => Some(newState))
    div(
      cls("flex items-start flex-col md:flex-row"),
      pElement(label),
      child(
        input(
          cls(
            "w-full mt-1 p-3 border rounded-md focus:ring-2 focus:ring-blue-500 outline-none"
          ),
          autoComplete("off"),
          placeholder(placeHolder),
          controlled(
            value <-- inputVar,
            onInput.mapToValue --> inputVar
          )
        )
      ) <-- hasValueSignal,
      button(
        cls(
          "text-black font-bold rounded-md px-4 py-2 md:mx-2 md:my-4 transition"
        ),
        cls <-- hasValueSignal.map {
          case true  => "bg-red-500 hover:bg-red-600"
          case false => "bg-green-500 hover:bg-green-600"
        },
        text <-- hasValueSignal.map {
          case true  => "Remove message"
          case false => "Add message"
        },
        inContext { thisNode =>
          val stream = thisNode.events(onClick).sample(state).flatMapSwitch {
            case Some(message) => EventStream.fromValue(None)
            case None          => EventStream.fromValue(Some(""))
          }

          stream --> state
        }
      )
    )
  }
}
