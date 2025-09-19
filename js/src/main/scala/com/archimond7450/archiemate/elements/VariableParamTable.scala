package com.archimond7450.archiemate.elements

import com.raquo.laminar.api.L.{*, given}

object VariableParamTable {
  final case class Row(name: String, optional: Boolean, defaultValue: String, description: Element)

  def element(elements: Seq[Row]): Element = {
    div(
      cls("container mx-auto"),
      div(
        cls("overflow-x-auto rounded-lg shadow"),
        table(
          cls("w-full text-sm text-left text-black"),
          thead(
            cls("text-xs text-black uppercase bg-blue-200 text-center"),
            tr(
              th(
                cls("px-6 py-3"),
                "Name"
              ),
              th(
                cls("px-6 py-3"),
                "Optional"
              ),
              th(
                cls("px-6 py-3"),
                "Default value"
              ),
              th(
                cls("px-6 py-3"),
                "Description"
              )
            )
          ),
          tbody(
            elements.map { el =>
              tr(
                cls("even:bg-blue-100 odd:bg-white text-black"),
                th(cls("bg-blue-200 px-4"), el.name),
                td(cls("text-center"), if (el.optional) "yes" else "no"),
                td(el.defaultValue),
                td(el.description)
              )
            }
          )
        )
      )
    )
  }
}
