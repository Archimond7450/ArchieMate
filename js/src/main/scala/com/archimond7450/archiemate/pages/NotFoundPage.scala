package com.archimond7450.archiemate.pages

import com.archimond7450.archiemate.App.{Home, NotFound, Page}
import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router

object NotFoundPage {
  def render(signal: Signal[NotFound])(using router: Router[Page]): HtmlElement = {
    sectionTag(
      cls("flex items-center h-screen p-16"),
      div(
        cls("container flex flex-col items-center"),
        div(
          cls("flex flex-col gap-6 max-w-md text-center"),
          h1(
            cls("font-extrabold text-9xl text-black"),
            span(
              cls("sr-only"),
              "Error"
            ),
            "404"
          ),
          p(
            cls("text-2xl md:text-3xl"),
            text <-- signal.map(notFoundPage => s"Sorry, the page ${notFoundPage.url} doesn't exist.")
          ),
          a(
            cls("px-8 py-4 text-xl font-semibold rounded bg-blue-500 text-white hover:bg-blue-600"),
            router.navigateTo(Home),
            "Back to home"
          )
        )
      )
    )
  }
}
