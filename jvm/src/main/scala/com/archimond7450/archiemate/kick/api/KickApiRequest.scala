package com.archimond7450.archiemate.kick.api

import com.archimond7450.archiemate.CirceConfiguration.kickConfiguration
import com.archimond7450.archiemate.helpers.JsonHelper.dropNulls
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}

object KickApiRequest {
  final case class PostChatMessage(content: String, `type`: String, replyToMessageId: Option[String])
  object PostChatMessage {
    given Decoder[PostChatMessage] = ConfiguredDecoder.derived
    given Encoder[PostChatMessage] = dropNulls(ConfiguredEncoder.derived)
  }
}
