package com.archimond7450.archiemate.pages

import com.archimond7450.archiemate.App.{Dashboard, router}
import com.archimond7450.archiemate.Loaded
import com.archimond7450.archiemate.elements.Buttons.asyncButton
import com.archimond7450.archiemate.elements.StyledStandardElements.h1Element
import com.archimond7450.archiemate.models.AuthModel
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

object LoginPage {
  def render()(using authModel: AuthModel): HtmlElement = {
    sectionTag(
      children <-- authModel.stateSignal.map {
        case Loaded(Success(AuthModel.LoggedIn(_))) =>
          router.replaceState(Dashboard)
          Seq()

        case _ =>
          Seq()
      },
      div(
        cls("mx-auto text-center mt-12"),
        h1Element("Login")
      ),
      p("By logging in to ArchieMate you agree that the website can use a session cookie to ensure you stay logged in."),
      div(
        cls("flex flex-col items-center"),
        a(
          href("/oauth/twitch"),
          button(
            cls("bg-[#9146FF] text-white font-bold rounded-md px-4 py-2 my-2 hover:bg-purple-700 transition"),
            "Login with Twitch",
          )
        )/*,
        button(
          cls("bg-red-500 text-white font-bold rounded-md px-4 py-2 my-2 hover:bg-red-600 transition"),
          "Login with YouTube"
        )*/
      )
    )
  }
}
