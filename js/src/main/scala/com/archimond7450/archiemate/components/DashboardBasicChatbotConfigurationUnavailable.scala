package com.archimond7450.archiemate.components

import com.archimond7450.archiemate.elements.StyledStandardElements.{
  h2Element,
  pElement
}
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDivElement

object DashboardBasicChatbotConfigurationUnavailable {
  def render(): ReactiveHtmlElement[HTMLDivElement] = {

    div(
      cls("mt-12 border rounded-lg"),
      h2Element("Basic chatbot configuration"),
      pElement(
        "This configuration will become available after Twitch connection is added (in the previous section)."
      )
    )
  }
}
