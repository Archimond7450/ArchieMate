package com.archimond7450.archiemate.components

import com.archimond7450.archiemate.elements.StyledStandardElements.pElement
import com.archimond7450.archiemate.http.Connections.Connections
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDivElement

object DashboardConnections {
  def render(stream: EventStream[Status[String, Either[Throwable, Connections]]]): ReactiveHtmlElement[HTMLDivElement] = div(
    child <-- stream.splitStatus(
      (resolved, resolvedSignal) => resolved.output match {
        case Right(connections) =>
          val youTubeConnectionsNumber = connections.youtubeConnections match {
            case Nil => "No connections"
            case List(_) => "1 connection"
            case _ => s"${connections.youtubeConnections.length} connections"
          }
          div(
            div(
              cls("flex items-start flex-col md:flex-row"),
              div(
                cls("md:mr-2 md:my-2"),
                pElement("Twitch:")
              ),
              div(
                cls("md:mr-2 md:my-2"),
                pElement(if (connections.twitchConnectionExists) "Connected" else "Not connected")
              ),
              if (connections.twitchConnectionExists) {
                a(
                  disabled(true),
                  cls("bg-red-700 text-white font-bold rounded-md px-4 py-2 md:mx-2 hover:bg-red-800 transition"),
                  href("/oauth/twitch/connection"),
                  "Disconnect"
                )
              } else {
                a(
                  cls("bg-[#9146FF] text-white font-bold rounded-md px-4 py-2 md:mx-2 md:my-4 hover:bg-purple-700 transition"),
                  href("/oauth/twitch/connection"),
                  "Connect"
                )
              }
            ),
            pElement(s"YouTube: $youTubeConnectionsNumber"),
            children <-- resolvedSignal.map { res =>
              res.output match {
                case Right(connections) => connections.youtubeConnections
                case Left(_) => Nil
              }
            }.split(_.channelId){ (channelId, initialConnection, connectionSignal) =>
              pElement(connectionSignal.map(conn => s"YouTube ${conn.channelName}"))
            }
          )

        case Left(exception) =>
          div("Failed to load!")
      },
      (pending, _) => div("Loading...")
    )
  )
}
