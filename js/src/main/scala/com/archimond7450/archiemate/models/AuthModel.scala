package com.archimond7450.archiemate.models

import com.archimond7450.archiemate.{Loaded, Loading, LoadingState}
import com.archimond7450.archiemate.helpers.FetchHelpers.checkLoginStatus
import com.archimond7450.archiemate.http.User.UserResponse
import com.raquo.laminar.api.L.{*, given}

import scala.concurrent.duration.DurationInt
import scala.scalajs.js.timers.{SetIntervalHandle, setInterval}
import scala.util.{Failure, Success}

object AuthModel {
  sealed trait State
  case object LoggedOut extends State
  case class LoggedIn(user: UserResponse) extends State
}

final class AuthModel {
  private val stateVar: Var[LoadingState[AuthModel.State]] = Var(Loading)
  val stateSignal: Signal[LoadingState[AuthModel.State]] = stateVar.signal

  def setState(state: AuthModel.State): Unit = stateVar.set(Loaded(Success(state)))
  def failState(ex: Throwable): Unit = stateVar.set(Loaded(Failure(ex)))
}
