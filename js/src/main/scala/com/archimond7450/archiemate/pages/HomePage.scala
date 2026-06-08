package com.archimond7450.archiemate.pages

import com.archimond7450.archiemate.elements.StyledStandardElements.{
  h1Element,
  pElement
}
import com.raquo.laminar.api.L.{*, given}

object HomePage {
  def render(): HtmlElement = {
    sectionTag(
      div(
        cls("mx-auto text-center mt-12"),
        h1Element("Home")
      ),
      pElement(
        "Welcome to the website for the chatbot ArchieMate made by Archimond7450. The chatbot supports Twitch and Kick at the moment with YouTube support to be added as well. Primary login to the chatbot is initiated through Twitch. Separate login from both Twitch and Kick are needed before the chatbot can join these and these logins are done through the Dashboard."
      ),
      pElement(
        "For documentation of built-in variables and commands, please go to the Docs page."
      )
    )
  }
}
