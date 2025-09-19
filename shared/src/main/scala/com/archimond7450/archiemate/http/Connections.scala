package com.archimond7450.archiemate.http

import com.archimond7450.archiemate.CirceConfiguration.frontendConfiguration
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}

object Connections {
  case class YouTubeConnection(channelId: String, channelName: String, channelProfileImageUrl: String)
  object YouTubeConnection {
    given Decoder[YouTubeConnection] = ConfiguredDecoder.derived
    given Encoder[YouTubeConnection] = ConfiguredEncoder.derived
  }

  case class Connections(twitchConnectionExists: Boolean, youtubeConnections: List[YouTubeConnection])
  object Connections {
    given Decoder[Connections] = ConfiguredDecoder.derived
    given Encoder[Connections] = ConfiguredEncoder.derived
  }
}
