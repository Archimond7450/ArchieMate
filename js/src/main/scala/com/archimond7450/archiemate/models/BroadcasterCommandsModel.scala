package com.archimond7450.archiemate.models

import com.archimond7450.archiemate.{Loaded, Loading, LoadingState}
import com.archimond7450.archiemate.http.ChannelSettings.CommandsSettings
import com.raquo.laminar.api.L.{*, given}

import scala.util.{Failure, Success}

class BroadcasterCommandsModel {
  private val commandsSettings: Var[LoadingState[CommandsSettings]] = Var(Loading)

  val commandsSettingsSignal: Signal[LoadingState[CommandsSettings]] = commandsSettings.signal

  def setCommandsSettings(newCommandsSettings: CommandsSettings): Unit = commandsSettings.set(Loaded(Success(newCommandsSettings)))
  def failCommandsSettings(ex: Throwable): Unit = commandsSettings.set(Loaded(Failure(ex)))
}
