package com.archimond7450.archiemate.elements

import com.archimond7450.archiemate.models.MobileMenuModel
import com.raquo.laminar.api.L.{*, given}

object SVGs {
  def menuSvg(mobileMenuModel: MobileMenuModel): Element = {
    svg.svg(
      svg.className("block h-6 w-6"),
      svg.xmlns("http://www.w3.org/2000/svg"),
      svg.fill("none"),
      svg.viewBox("0 0 24 24"),
      svg.stroke("currentColor"),
      aria.hidden <-- mobileMenuModel.hiddenSignal,
      svg.path(
        svg.strokeLineCap("round"),
        svg.strokeLineJoin("round"),
        svg.strokeWidth("2"),
        svg.d("M4 6h16M4 12h16M4 18h16")
      )
    )
  }
}
