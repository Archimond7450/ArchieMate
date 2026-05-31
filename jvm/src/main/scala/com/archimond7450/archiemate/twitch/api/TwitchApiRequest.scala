package com.archimond7450.archiemate.twitch.api

import com.archimond7450.archiemate.CirceConfiguration.twitchConfiguration
import com.archimond7450.archiemate.twitch.eventsub.{Condition, Transport}
import com.archimond7450.archiemate.helpers.JsonHelper.dropNulls
import io.circe.*
import io.circe.syntax.EncoderOps
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}

object TwitchApiRequest {
  final case class ModifyChannelInformationRequestData(
      gameId: Option[String] = None,
      title: Option[String] = None,
      tags: Option[Set[String]] = None
  )
  object ModifyChannelInformationRequestData {
    given Decoder[ModifyChannelInformationRequestData] =
      ConfiguredDecoder.derived
    given Encoder[ModifyChannelInformationRequestData] = dropNulls(
      ConfiguredEncoder.derived
    )
  }

  final case class CreateEventSubSubscriptionPayload(
      `type`: String,
      version: String,
      condition: Condition,
      transport: Transport
  )
  object CreateEventSubSubscriptionPayload {
    given Decoder[CreateEventSubSubscriptionPayload] = ConfiguredDecoder.derived
    given Encoder[CreateEventSubSubscriptionPayload] =
      (p: CreateEventSubSubscriptionPayload) => {
        Json.fromJsonObject(
          JsonObject(
            "type" -> Json.fromString(p.`type`),
            "version" -> Json.fromString(p.version),
            "condition" -> p.condition.asJson,
            "transport" -> p.transport.asJson
          )
        )
      }
  }

  final case class CreatePollRequestData(
      broadcasterId: String,
      title: String,
      choices: List[PollChoice],
      channelPointsVotingEnabled: Option[Boolean] = None,
      channelPointsPerVote: Option[Int] = None,
      duration: Int
  )
  object CreatePollRequestData {
    given Decoder[CreatePollRequestData] = ConfiguredDecoder.derived
    given Encoder[CreatePollRequestData] = dropNulls(ConfiguredEncoder.derived)
  }

  final case class PollChoice(title: String)
  object PollChoice {
    given Decoder[PollChoice] = ConfiguredDecoder.derived
    given Encoder[PollChoice] = ConfiguredEncoder.derived
  }

  final case class EndPollRequestData(
      broadcasterId: String,
      id: String,
      status: String
  )
  object EndPollRequestData {
    given Decoder[EndPollRequestData] = ConfiguredDecoder.derived
    given Encoder[EndPollRequestData] = ConfiguredEncoder.derived
  }

  final case class CreatePredictionRequestData(
      broadcasterId: String,
      title: String,
      outcomes: List[PredictionOutcome],
      predictionWindow: Int
  )
  object CreatePredictionRequestData {
    given Decoder[CreatePredictionRequestData] = ConfiguredDecoder.derived
    given Encoder[CreatePredictionRequestData] = ConfiguredEncoder.derived
  }

  final case class PredictionOutcome(title: String)
  object PredictionOutcome {
    given Decoder[PredictionOutcome] = ConfiguredDecoder.derived
    given Encoder[PredictionOutcome] = ConfiguredEncoder.derived
  }

  final case class EndPredictionRequestData(
      broadcasterId: String,
      id: String,
      status: String,
      winningOutcomeId: Option[String]
  )
  object EndPredictionRequestData {
    given Decoder[EndPredictionRequestData] = ConfiguredDecoder.derived
    given Encoder[EndPredictionRequestData] = dropNulls(
      ConfiguredEncoder.derived
    )
  }
}
