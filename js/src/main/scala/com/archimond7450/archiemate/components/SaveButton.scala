package com.archimond7450.archiemate.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLButtonElement

object SaveButton {
  def render[Payload](value: Var[Payload], onClickStream: Payload => EventStream[Status[(String, Payload), Int]]): ReactiveHtmlElement[HTMLButtonElement] = {
    val saveClicked: Var[Boolean] = Var(false)
    val saveFailed: Var[Boolean] = Var(false)
    button(
      cls(
        "text-black font-bold rounded-md px-4 py-2 md:mx-2 md:my-4 transition"
      ),
      cls <-- saveFailed.signal.map {
        case true  => "bg-red-500 hover:bg-red-600"
        case false => "bg-green-500 hover:bg-green-600"
      },
      text <-- saveClicked.signal
        .combineWith(saveFailed.signal)
        .map {
          case (true, _)      => "Saving..."
          case (false, true)  => "Save failed! Try again?"
          case (false, false) => "Save"
        },
      disabled <-- saveClicked.signal,
      inContext { thisNode =>
        val responses = thisNode
          .events(onClick)
          .sample(value)
          .flatMapSwitch(onClickStream)

        val saveClickedSignal = responses.map(_.isPending)
        val saveFailedSignal = responses.splitStatus(
          (resolved, _) => resolved.output != 204,
          (_, _) => false
        )

        saveClickedSignal --> saveClicked
        saveFailedSignal --> saveFailed
      }
    )
  }
}
