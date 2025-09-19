package com.archimond7450.archiemate.pages

import com.archimond7450.archiemate.App.Overlay
import com.archimond7450.archiemate.elements.StyledStandardElements.{h1Element, pElement}
import com.raquo.laminar.api.L.{*, given}

object OverlayPage {
  def render(signal: Signal[Overlay]): HtmlElement = {
    sectionTag(
      div(
        cls("mx-auto text-center mt-12"),
        h1Element(signal.map(page => s"Widget ${page.overlayId}"))
      ),
      pElement(
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit."
      )
    )
  }
}
