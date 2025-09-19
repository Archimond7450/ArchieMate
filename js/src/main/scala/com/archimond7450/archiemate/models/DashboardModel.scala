package com.archimond7450.archiemate.models

import com.archimond7450.archiemate.helpers.FetchHelpers.{fetchUpdateAutomaticMessagesSettings, fetchUpdateBasicChatbotSettings, fetchUpdateBuiltInCommandsSettings, fetchUpdateCommandsSettings, fetchUpdateOverlaysSettings, fetchUpdateTimersSettings, fetchUpdateVariablesSettings}
import com.archimond7450.archiemate.{Loaded, Loading, LoadingState}
import com.archimond7450.archiemate.http.Connections.Connections
import com.archimond7450.archiemate.http.ChannelSettings.{AutomaticMessagesSettings, BasicChatbotSettings, BuiltInCommandsSettings, CommandsSettings, OverlaysSettings, TimersSettings, VariablesSettings}
import com.raquo.laminar.api.L.{*, given}

import scala.util.{Failure, Success, Try}

final class DashboardModel {
  private val basicChatbotSettings: Var[LoadingState[BasicChatbotSettings]] = Var(Loading)
  private val builtInCommandsSettings: Var[LoadingState[BuiltInCommandsSettings]] = Var(Loading)
  private val commandsSettings: Var[LoadingState[CommandsSettings]] = Var(Loading)
  private val variablesSettings: Var[LoadingState[VariablesSettings]] = Var(Loading)
  private val timersSettings: Var[LoadingState[TimersSettings]] = Var(Loading)
  private val overlaysSettings: Var[LoadingState[OverlaysSettings]] = Var(Loading)
  private val automaticMessagesSettings: Var[LoadingState[AutomaticMessagesSettings]] = Var(Loading)
  private val connections: Var[LoadingState[Connections]] = Var(Loading)
  val basicChatbotSettingsSignal: Signal[LoadingState[BasicChatbotSettings]] = basicChatbotSettings.signal
  val builtInCommandsSettingsSignal: Signal[LoadingState[BuiltInCommandsSettings]] = builtInCommandsSettings.signal
  val commandsSettingsSignal: Signal[LoadingState[CommandsSettings]] = commandsSettings.signal
  val variablesSettingsSignal: Signal[LoadingState[VariablesSettings]] = variablesSettings.signal
  val timersSettingsSignal: Signal[LoadingState[TimersSettings]] = timersSettings.signal
  val overlaysSettingsSignal: Signal[LoadingState[OverlaysSettings]] = overlaysSettings.signal
  val automaticMessagesSettingsSignal: Signal[LoadingState[AutomaticMessagesSettings]] = automaticMessagesSettings.signal
  val connectionsSignal: Signal[LoadingState[Connections]] = connections.signal

  private def updateSettings[Settings](newSettings: Settings, oldSettings: LoadingState[Settings], fetchUpdateFunction: (Settings, () => Unit, Throwable => Unit) => Unit, onOk: () => Unit, onError: Throwable => Unit): Unit = oldSettings match {
    case Loaded(Success(_)) => fetchUpdateFunction(newSettings, onOk, onError)
    case _ =>
  }

  def setBasicChatbotSettings(newSettings: BasicChatbotSettings): Unit = basicChatbotSettings.set(Loaded(Success(newSettings)))
  def failBasicChatbotSettings(ex: Throwable): Unit = basicChatbotSettings.set(Loaded(Failure(ex)))
  def updateBasicChatbotSettings(newBasicChatbotSettings: BasicChatbotSettings, onOk: () => Unit, onError: Throwable => Unit): Unit = {
    updateSettings(
      newBasicChatbotSettings,
      basicChatbotSettings.now(),
      fetchUpdateBasicChatbotSettings,
      onOk,
      onError
    )
  }
  def setBuiltInChatbotSettings(newSettings: BuiltInCommandsSettings): Unit = builtInCommandsSettings.set(Loaded(Success(newSettings)))
  def failBuiltInChatbotSettings(ex: Throwable): Unit = builtInCommandsSettings.set(Loaded(Failure(ex)))
  def updateBuiltInChatbotSettings(newBuiltInCommandsSettings: BuiltInCommandsSettings, onOk: () => Unit, onError: Throwable => Unit): Unit = {
    updateSettings(
      newBuiltInCommandsSettings,
      builtInCommandsSettings.now(),
      fetchUpdateBuiltInCommandsSettings,
      onOk,
      onError
    )
  }
  def setCommandsSettings(newSettings: CommandsSettings): Unit = commandsSettings.set(Loaded(Success(newSettings)))
  def failCommandsSettings(ex: Throwable): Unit = commandsSettings.set(Loaded(Failure(ex)))
  def updateCommandsSettings(newCommandsSettings: CommandsSettings, onOk: () => Unit, onError: Throwable => Unit): Unit = {
    updateSettings(
      newCommandsSettings,
      commandsSettings.now(),
      fetchUpdateCommandsSettings,
      onOk,
      onError
    )
  }
  def setVariablesSettings(newSettings: VariablesSettings): Unit = variablesSettings.set(Loaded(Success(newSettings)))
  def failVariablesSettings(ex: Throwable): Unit = variablesSettings.set(Loaded(Failure(ex)))
  def updateVariablesSettings(newVariablesSettings: VariablesSettings, onOk: () => Unit, onError: Throwable => Unit): Unit = {
    updateSettings(
      newVariablesSettings,
      variablesSettings.now(),
      fetchUpdateVariablesSettings,
      onOk,
      onError
    )
  }
  def setTimersSettings(newSettings: TimersSettings): Unit = timersSettings.set(Loaded(Success(newSettings)))
  def failTimersSettings(ex: Throwable): Unit = variablesSettings.set(Loaded(Failure(ex)))
  def updateTimersSettings(newTimersSettings: TimersSettings, onOk: () => Unit, onError: Throwable => Unit): Unit = {
    updateSettings(
      newTimersSettings,
      timersSettings.now(),
      fetchUpdateTimersSettings,
      onOk,
      onError
    )
  }
  def setOverlaysSettings(newSettings: OverlaysSettings): Unit = overlaysSettings.set(Loaded(Success(newSettings)))
  def failOverlaysSettings(ex: Throwable): Unit = overlaysSettings.set(Loaded(Failure(ex)))
  def updateOverlaysSettings(newOverlaysSettings: OverlaysSettings, onOk: () => Unit, onError: Throwable => Unit): Unit = {
    updateSettings(
      newOverlaysSettings,
      overlaysSettings.now(),
      fetchUpdateOverlaysSettings,
      onOk,
      onError
    )
  }
  def setAutomaticMessagesSettings(newSettings: AutomaticMessagesSettings): Unit = automaticMessagesSettings.set(Loaded(Success(newSettings)))
  def failAutomaticMessagesSettings(ex: Throwable): Unit = automaticMessagesSettings.set(Loaded(Failure(ex)))
  def updateAutomaticMessagesSettings(newAutomaticMessagesSettings: AutomaticMessagesSettings, onOk: () => Unit, onError: Throwable => Unit): Unit = {
    updateSettings(
      newAutomaticMessagesSettings,
      automaticMessagesSettings.now(),
      fetchUpdateAutomaticMessagesSettings,
      onOk,
      onError
    )
  }
  def setConnections(newConnections: Connections): Unit = connections.set(Loaded(Success(newConnections)))
  def failConnections(ex: Throwable): Unit = connections.set(Loaded(Failure(ex)))
}
