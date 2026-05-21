package com.archimond7450.archiemate.http

import com.archimond7450.archiemate.CirceConfiguration.frontendConfiguration
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import io.circe.{Decoder, Encoder}

object Predictions {
  final case class Prediction(
      aliases: Set[String],
      title: String,
      outcomes: Set[String]
  )

  object Prediction {
    given Decoder[Prediction] = ConfiguredDecoder.derived
    given Encoder[Prediction] = ConfiguredEncoder.derived
  }

  final case class ChannelPredictions(
      predictions: Map[String, Prediction] = Map.empty
  ) {
    def withPrediction(
        predictionId: String,
        prediction: Prediction
    ): ChannelPredictions =
      copy(predictions = predictions + (predictionId -> prediction))

    def withEditedPrediction(
        predictionId: String,
        prediction: Prediction
    ): ChannelPredictions =
      withPrediction(predictionId, prediction)

    def withDeletedPrediction(predictionId: String): ChannelPredictions =
      copy(predictions = predictions - predictionId)
  }

  object ChannelPredictions {
    given Decoder[ChannelPredictions] = ConfiguredDecoder.derived
    given Encoder[ChannelPredictions] = ConfiguredEncoder.derived
  }
}
