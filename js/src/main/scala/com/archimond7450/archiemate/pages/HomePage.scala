package com.archimond7450.archiemate.pages

import com.archimond7450.archiemate.elements.StyledStandardElements.{h1Element, pElement}
import com.raquo.laminar.api.L.{*, given}

object HomePage {
  def render(): HtmlElement = {
    sectionTag(
      div(
        cls("mx-auto text-center mt-12"),
        h1Element("Home")
      ),
      pElement(
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit."
      )
    )
  }
}
