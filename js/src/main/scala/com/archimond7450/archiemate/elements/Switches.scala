package com.archimond7450.archiemate.elements

import com.raquo.laminar.api.L.{*, given}

object Switches {
  def switch(id: String, offString: String, onString: String, on: Var[Boolean]): HtmlElement = {
    div(
      cls("flex items-center"),
      input(
        `type`("checkbox"),
        cls("hidden"),
        idAttr(id),
        checked <-- on,
        onChange --> { _ => on.update(!_) }
      ),
      label(
        forId(id),
        cls <-- on.signal.map {
          case true => "select-none relative inline-flex h-8 w-20 items-center rounded-full transition-colors duration-200 cursor-pointer bg-[#0E947A]"
          case false => "select-none relative inline-flex h-8 w-20 items-center rounded-full transition-colors duration-200 cursor-pointer bg-[#E6E6E6]"
        },
        span(
          cls <-- on.signal.map {
            case true => "absolute w-full text-s transition-transform duration-200 text-left pl-2 text-white"
            case false => "absolute w-full text-s transition-transform duration-200 text-right pr-1.5 text-black"
          },
          text <-- on.signal.map {
            case true => onString
            case false => offString
          }
        ),
        span(
          cls <-- on.signal.map {
            case true => "inline-block w-[23px] h-[23px] transform rounded-full bg-white transition-transform duration-200 translate-x-1"
            case false => "inline-block w-[23px] h-[23px] transform rounded-full bg-white transition-transform duration-200 translate-x-1"
          },
          styleAttr <-- on.signal.map {
            case true => "transform: translateX(53px)"
            case false => "transform: translateX(4px)"
          }
        )
      )
    )
  }
}
