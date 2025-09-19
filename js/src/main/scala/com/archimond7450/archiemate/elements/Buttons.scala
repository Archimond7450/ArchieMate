package com.archimond7450.archiemate.elements

import com.raquo.laminar.api.L.{*, given}

import scala.concurrent.{ExecutionContext, Future}

object Buttons {
  def asyncButton(modifiers: Modifier[Button]*)(onClickCallback: () => Future[Unit])(using ec: ExecutionContext): HtmlElement = {
    val isDisabled = Var(false)

    button(
      disabled <-- isDisabled.signal,
      onClick --> { _ =>
        if (!isDisabled.now()) {
          isDisabled.set(true)
          onClickCallback().onComplete { _ =>
            isDisabled.set(false)
          }
        }
      },
      modifiers,
    )
  }
}
