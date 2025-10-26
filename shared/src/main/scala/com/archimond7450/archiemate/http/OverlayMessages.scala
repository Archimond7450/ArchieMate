package com.archimond7450.archiemate.http

import com.archimond7450.archiemate.CirceConfiguration.frontendConfiguration
import com.archimond7450.archiemate.http.ChannelSettings.OverlaysSettings
import com.archimond7450.archiemate.twitch.eventsub
import com.archimond7450.archiemate.twitch.eventsub.eventsub.Event
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}

object OverlayMessages {
  enum OverlayMessageType {
    case Ping
    case Pong
    case OverlaysSettings
    case TwitchEvent
  }
  object OverlayMessageType {
    given Decoder[OverlayMessageType] = ConfiguredDecoder.derived
    given Encoder[OverlayMessageType] = ConfiguredEncoder.derived
  }

  case class OverlayMessage(id: String, `type`: OverlayMessageType, twitchEvent: Option[Event] = None, overlaysSettings: Option[OverlaysSettings] = None)
  object OverlayMessage {
    given Decoder[OverlayMessage] = ConfiguredDecoder.derived
    given Encoder[OverlayMessage] = ConfiguredEncoder.derived
  }
}
