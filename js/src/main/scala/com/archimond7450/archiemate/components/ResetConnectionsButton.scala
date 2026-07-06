package com.archimond7450.archiemate.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLButtonElement

object ResetConnectionsButton {
  def render(
      onClickStream: () => EventStream[Status[String, Int]]
  ): ReactiveHtmlElement[HTMLButtonElement] = {
    val resetClicked: Var[Boolean] = Var(false)
    val resetFailed: Var[Boolean] = Var(false)
    button(
      cls(
        "text-black font-bold rounded-md px-4 py-2 md:mx-2 md:my-4 transition"
      ),
      cls("bg-red-500 hover:bg-red-600"),
      text <-- resetClicked.signal
        .combineWith(resetFailed.signal)
        .map {
          case (true, _)      => "Resetting Connections..."
          case (false, true)  => "Resetting Connections failed! Try again?"
          case (false, false) => "Reset Connections"
        },
      disabled <-- resetClicked.signal,
      inContext { thisNode =>
        val responses = thisNode
          .events(onClick)
          .flatMapSwitch(_ => onClickStream())

        val resetClickedSignal = responses.map(_.isPending)
        val resetFailedSignal = responses.splitStatus(
          (resolved, _) => resolved.output != 204,
          (_, _) => false
        )

        resetClickedSignal --> resetClicked
        resetFailedSignal --> resetFailed
      }
    )
  }
}
