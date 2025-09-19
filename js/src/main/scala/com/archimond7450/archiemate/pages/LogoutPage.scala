package com.archimond7450.archiemate.pages

import com.archimond7450.archiemate.App.{Home, Page}
import com.archimond7450.archiemate.{Loaded, Loading}
import com.archimond7450.archiemate.elements.Buttons.asyncButton
import com.archimond7450.archiemate.elements.StyledStandardElements.h1Element
import com.archimond7450.archiemate.models.AuthModel
import com.raquo.laminar.api.L.*
import com.raquo.waypoint.Router

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object LogoutPage {
  def render()(using authModel: AuthModel, router: Router[Page]): HtmlElement = {
    sectionTag(
      div(
        cls("mx-auto text-center mt-12"),
        h1Element(authModel.stateSignal.map {
          case Loaded(Success(AuthModel.LoggedIn(_))) =>
            "Logout"

          case Loaded(Success(AuthModel.LoggedOut)) | Loaded(Failure(_)) =>
            router.replaceState(Home)
            "Loading..."

          case Loading =>
            "Loading..."
        })
      ),
      p("Please confirm you want to log out by pressing the button."),
      div(
        cls("flex flex-col items-center"),
        a(
          href("/logout/confirm"),
          button(
            cls("bg-blue-500 text-white font-bold rounded-md px-4 py-2 my-2 hover:bg-blue-700 transition"),
            "Log out now",
          )
        )
      )
    )
  }
}
