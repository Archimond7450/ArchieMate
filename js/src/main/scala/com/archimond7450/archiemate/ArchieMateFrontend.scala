package com.archimond7450.archiemate

import com.archimond7450.archiemate.helpers.FetchHelpers.checkLoginStatus
import com.archimond7450.archiemate.models.{AuthModel, MobileMenuModel}
import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}

import scala.concurrent.duration.DurationInt
import scala.scalajs.js.timers.*

object ArchieMateFrontend {
  def main(args: Array[String]): Unit = {
    lazy val container = Option(dom.document.getElementById("app")).getOrElse {
      val element = dom.document.createElement("div")
      element.id = "app"
      dom.document.body.appendChild(element)
      element
    }

    given MobileMenuModel = new MobileMenuModel
    given AuthModel = new AuthModel

    renderOnDomContentLoaded(container, App.render())
  }
}

