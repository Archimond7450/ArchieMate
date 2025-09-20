package com.archimond7450.archiemate.helpers

import com.archimond7450.archiemate.{Loaded, LoadingState}
import com.archimond7450.archiemate.extensions.EitherExtensions.whenLeft
import com.archimond7450.archiemate.extensions.FutureExtensions.onError
import com.archimond7450.archiemate.http.Connections.Connections
import com.archimond7450.archiemate.http.ChannelSettings.{AutomaticMessagesSettings, BasicChatbotSettings, BuiltInCommandsSettings, CommandsSettings, OverlaysSettings, Settings, TimersSettings, VariablesSettings}
import com.archimond7450.archiemate.http.User.UserResponse
import com.archimond7450.archiemate.models.{AuthModel, BroadcasterCommandsModel, DashboardModel}
import com.raquo.laminar.api.L.{*, given}
import io.circe.{Decoder, Encoder}
import io.circe.jawn.decode
import io.circe.syntax.EncoderOps
import org.scalajs.dom
import org.scalajs.dom.{Request, RequestInit}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.scalajs.js
import scala.util.{Failure, Success}

object FetchHelpers {
  given ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  private val defaultInit = new dom.RequestInit {
    method = dom.HttpMethod.GET
  }

  def fetchWithCookies(url: String, init: dom.RequestInit = defaultInit, method: dom.HttpMethod = dom.HttpMethod.GET, headers: js.Dictionary[String] = js.Dictionary(), body: String = ""): Future[dom.Response] = {
    init.credentials = dom.RequestCredentials.include
    if (method != dom.HttpMethod.GET) {
      init.method = method
    }
    if (headers.nonEmpty) {
      init.headers = dom.Headers(headers)
    }
    if (body != "") {
      init.body = body
    }
    dom.fetch(url, init).toFuture
  }

  def fetchGetStream[Data](url: String)(using Decoder[Data]): EventStream[Status[String, Either[Throwable, Data]]] = {
    EventStream.fromValue(url, emitOnce = true).flatMapWithStatus { url =>
      FetchStream
        .get(url, _.credentials(_ => dom.RequestCredentials.include))
        .map(decode[Data])
        .recover {
          case ex => Some(Left(ex))
        }
    }
  }

  def fetchPostStream[Payload](url: String, payload: Payload)(using Encoder[Payload]): EventStream[Status[(String, Payload), Int]] = {
    EventStream.fromValue((url, payload), emitOnce = true)
      .flatMapWithStatus { params =>
        FetchStream
          .withDecoder { response =>
            EventStream.fromValue(response.status, emitOnce = true)
          }
          .post(
            params._1,
            _.credentials(_ => dom.RequestCredentials.include),
            _.headers("Content-Type" -> "application/json"),
            _.body(params._2.asJson.noSpaces)
          )
          .recover {
            case _ => Some(500)
          }
      }
  }

  def checkLoginStatus()(using model: AuthModel): Unit = {
    for {
      userResponse <- fetchWithCookies("/api/v1/user").onError(model.failState)
      json <- userResponse.text().toFuture.onError(model.failState)
      user <- decode[UserResponse](json).whenLeft(model.failState)
    } if (userResponse.ok) {
      model.setState(AuthModel.LoggedIn(user))
    } else {
      model.setState(AuthModel.LoggedOut)
    }
  }

  private def fetchAndDecode[JsonCaseClass](urlSuffix: String, onOk: JsonCaseClass => Unit, onError: Throwable => Unit)(using Decoder[JsonCaseClass]): Unit = {
    for {
      settingsResponse <- fetchWithCookies(s"/api/v1/$urlSuffix").onError(onError)
      json <- settingsResponse.text().toFuture.onError(onError)
      settings <- decode[JsonCaseClass](json).whenLeft(onError)
    } onOk(settings)
  }

  def fetchBasicChatbotSettings(model: DashboardModel): Unit = {
    fetchAndDecode(
      "settings/basic",
      onOk = model.setBasicChatbotSettings,
      onError = model.failBasicChatbotSettings
    )
  }

  def fetchBuiltInCommandsSettings(model: DashboardModel): Unit = {
    fetchAndDecode(
      "settings/commands/builtin",
      onOk = model.setBuiltInChatbotSettings,
      onError = model.failBuiltInChatbotSettings
    )
  }

  def fetchCommandsSettings(model: DashboardModel): Unit = {
    fetchAndDecode(
      "settings/commands",
      onOk = model.setCommandsSettings,
      onError = model.failCommandsSettings
    )
  }

  def fetchVariablesSettings(model: DashboardModel): Unit = {
    fetchAndDecode(
      "settings/variables",
      onOk = model.setVariablesSettings,
      onError = model.failVariablesSettings
    )
  }

  def fetchTimersSettings(model: DashboardModel): Unit = {
    fetchAndDecode(
      "settings/timers",
      onOk = model.setTimersSettings,
      onError = model.failTimersSettings
    )
  }

  def fetchOverlaysSettings(model: DashboardModel): Unit = {
    fetchAndDecode(
      "settings/overlays",
      onOk = model.setOverlaysSettings,
      onError = model.failOverlaysSettings
    )
  }

  def fetchAutomaticMessagesSettings(model: DashboardModel): Unit = {
    fetchAndDecode(
      "settings/automatic_messages",
      onOk = model.setAutomaticMessagesSettings,
      onError = model.failAutomaticMessagesSettings
    )
  }

  def fetchConnections(model: DashboardModel): Unit = {
    for {
      connectionsResponse <- fetchWithCookies("/api/v1/settings/connections").onError(model.failConnections)
      json <- connectionsResponse.text().toFuture.onError(model.failConnections)
      connections <- decode[Connections](json).whenLeft(model.failConnections)
    } model.setConnections(connections)
  }

  def fetchUpdate[Payload](urlSuffix: String, settings: Payload, onOk: () => Unit, onError: Throwable => Unit)(using Encoder[Payload]): Unit = {
    for {
      response <- fetchWithCookies(
        s"/api/v1/$urlSuffix",
        method = dom.HttpMethod.POST,
        headers = js.Dictionary("Content-Type" -> "application/json"),
        body = settings.asJson.noSpaces
      ).onError(onError)
    } if (response.ok) {
      onOk()
    } else {
      onError(RuntimeException(s"Request failed with code ${response.status}"))
    }
  }

  def fetchUpdateBasicChatbotSettings(settings: BasicChatbotSettings, onOk: () => Unit, onError: Throwable => Unit): Unit = {
    fetchUpdate("settings/basic", settings, onOk, onError)
  }

  def fetchUpdateBuiltInCommandsSettings(settings: BuiltInCommandsSettings, onOk: () => Unit, onError: Throwable => Unit): Unit = {
    fetchUpdate("commands/builtin", settings, onOk, onError)
  }

  def fetchUpdateCommandsSettings(settings: CommandsSettings, onOk: () => Unit, onError: Throwable => Unit): Unit = {
    fetchUpdate("settings/commands", settings, onOk, onError)
  }

  def fetchUpdateVariablesSettings(settings: VariablesSettings, onOk: () => Unit, onError: Throwable => Unit): Unit = {
    fetchUpdate("settings/variables", settings, onOk, onError)
  }

  def fetchUpdateTimersSettings(settings: TimersSettings, onOk: () => Unit, onError: Throwable => Unit): Unit = {
    fetchUpdate("settings/timers", settings, onOk, onError)
  }

  def fetchUpdateOverlaysSettings(settings: OverlaysSettings, onOk: () => Unit, onError: Throwable => Unit): Unit = {
    fetchUpdate("settings/overlays", settings, onOk, onError)
  }

  def fetchUpdateAutomaticMessagesSettings(settings: AutomaticMessagesSettings, onOk: () => Unit, onError: Throwable => Unit): Unit = {
    fetchUpdate("settings/automatic_messages", settings, onOk, onError)
  }

  def fetchBroadcasterCommands(model: BroadcasterCommandsModel, channelName: String): Unit = {
    fetchAndDecode(
      s"commands/$channelName",
      onOk = model.setCommandsSettings,
      onError = model.failCommandsSettings
    )
  }
}