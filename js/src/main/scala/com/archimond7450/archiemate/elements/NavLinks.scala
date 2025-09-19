package com.archimond7450.archiemate.elements

import com.archimond7450.archiemate.App.Page
import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router

object NavLinks {
  def navLink(page: Page, content: String, activeSignal: Signal[Boolean])(using router: Router[Page]): Element = {
    a(
      cls <-- activeSignal.map {
        case true => "underline text-white hover:bg-blue-600 px-3 py-2 rounded-md text-sm font-medium"
        case false => "text-white hover:bg-blue-600 px-3 py-2 rounded-md text-sm font-medium"
      },
      router.navigateTo(page),
      content
    )
  }

  def navLink(address: String, content: String, activeSignal: Signal[Boolean]): Element = {
    a(
      cls <-- activeSignal.map {
        case true => "underline text-white hover:bg-blue-600 px-3 py-2 rounded-md text-sm font-medium"
        case false => "text-white hover:bg-blue-600 px-3 py-2 rounded-md text-sm font-medium"
      },
      href(address),
      content
    )
  }

  def mobileNavLink(page: Page, content: String)(using router: Router[Page]): Element = {
    a(
      cls("block w-full text-center text-white hover:bg-blue-600 px-3 py-2 rounded-md text-base font-medium"),
      router.navigateTo(page),
      content
    )
  }

  def mobileNavLink(address: String, content: String): Element = {
    a(
      cls("block w-full text-center text-white hover:bg-blue-600 px-3 py-2 rounded-md text-base font-medium"),
      href(address),
      content
    )
  }
}
