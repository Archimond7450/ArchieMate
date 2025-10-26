package com.archimond7450.archiemate.pages

import com.archimond7450.archiemate.App.Overlay
import com.archimond7450.archiemate.elements.StyledStandardElements.{h1Element, pElement}
import com.archimond7450.archiemate.http.OverlayMessages
import com.raquo.laminar.api.L.{*, given}
import io.laminext.websocket.WebSocket
import io.laminext.websocket.circe.webSocketReceiveBuilderSyntax

object OverlayPage {
  def render(signal: Signal[Overlay]): HtmlElement = {
    div(
      child <-- signal.map { overlay =>
        val ws = WebSocket.url(s"/overlay/ws/${overlay.twitchRoomId}/${overlay.secret}").json[OverlayMessages.OverlayMessage, OverlayMessages.OverlayMessage].build(managed = true)
        div(
          ws.connect,
          children.command <-- ws.received.map { message =>
            CollectionCommand.Append(
              code(message.toString)
            )
          }
        )
      }
    )
  }
}
