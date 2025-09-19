package com.archimond7450.archiemate.twitch.api

import com.archimond7450.archiemate.CirceConfiguration.twitchConfiguration
import com.archimond7450.archiemate.twitch.eventsub.{Condition, Transport}
import com.archimond7450.archiemate.helpers.JsonHelper.dropNulls
import io.circe.*
import io.circe.syntax.EncoderOps
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}

object TwitchApiRequest {
  final case class ModifyChannelInformationRequestData(game_id: Option[String] = None, title: Option[String] = None, tags: Option[Set[String]] = None)
  object ModifyChannelInformationRequestData {
    given Decoder[ModifyChannelInformationRequestData] = ConfiguredDecoder.derived
    given Encoder[ModifyChannelInformationRequestData] = dropNulls(ConfiguredEncoder.derived)
  }

  final case class CreateEventSubSubscriptionPayload(`type`: String, version: String, condition: Condition, transport: Transport)
  object CreateEventSubSubscriptionPayload {
    given Decoder[CreateEventSubSubscriptionPayload] = ConfiguredDecoder.derived
    given Encoder[CreateEventSubSubscriptionPayload] = (p: CreateEventSubSubscriptionPayload) => {
      Json.fromJsonObject(JsonObject(
        "type" -> Json.fromString(p.`type`),
        "version" -> Json.fromString(p.version),
        "condition" -> p.condition.asJson,
        "transport" -> p.transport.asJson
      ))
    }
  }
}
