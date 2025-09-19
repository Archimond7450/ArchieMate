package com.archimond7450.archiemate.models

import com.raquo.laminar.api.L.{*, given}

final class MobileMenuModel {
  private val dataVar: Var[Boolean] = Var(false)
  val shownSignal: Signal[Boolean] = dataVar.signal
  val hiddenSignal: Signal[Boolean] = shownSignal.map(!_)
  val classSignal: Signal[String] = shownSignal.map(expanded => s"sm:hidden${if (expanded) "" else " hidden"}")

  def toggle(): Unit = dataVar.update(!_)
}
