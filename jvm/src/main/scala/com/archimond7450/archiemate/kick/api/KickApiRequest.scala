package com.archimond7450.archiemate.kick.api

import com.archimond7450.archiemate.CirceConfiguration.kickConfiguration
import com.archimond7450.archiemate.helpers.JsonHelper.dropNulls
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}

object KickApiRequest {
  final case class KickEvent(name: String, version: Int)
  object KickEvent {
    given Decoder[KickEvent] = ConfiguredDecoder.derived
    given Encoder[KickEvent] = ConfiguredEncoder.derived
  }

  final case class SubscribeToEvents(events: List[KickEvent], method: String = "webhook")
  object SubscribeToEvents {
    given Decoder[SubscribeToEvents] = ConfiguredDecoder.derived
    given Encoder[SubscribeToEvents] = ConfiguredEncoder.derived
  }

  final case class PostChatMessage(
      content: String,
      `type`: String,
      replyToMessageId: Option[String]
  )
  object PostChatMessage {
    given Decoder[PostChatMessage] = ConfiguredDecoder.derived
    given Encoder[PostChatMessage] = dropNulls(ConfiguredEncoder.derived)
  }

  final case class UpdateChannel(
      categoryId: Option[Int] = None,
      customTags: Option[List[String]] = None,
      streamTitle: Option[String] = None
  )
  object UpdateChannel {
    given Decoder[UpdateChannel] = ConfiguredDecoder.derived
    given Encoder[UpdateChannel] = dropNulls(ConfiguredEncoder.derived)
  }
}
