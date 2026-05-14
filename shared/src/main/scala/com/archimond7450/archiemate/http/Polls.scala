package com.archimond7450.archiemate.http

import com.archimond7450.archiemate.CirceConfiguration.frontendConfiguration
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}

object Polls {
  final case class Poll(
      aliases: Set[String],
      question: String,
      choices: Set[String]
  )

  object Poll {
    given Decoder[Poll] = ConfiguredDecoder.derived
    given Encoder[Poll] = ConfiguredEncoder.derived
  }

  final case class ChannelPolls(polls: Map[String, Poll] = Map.empty) {
    def withPoll(
        pollId: String,
        poll: Poll
    ): ChannelPolls =
      copy(polls = polls + (pollId -> poll))

    def withEditedPoll(pollId: String, poll: Poll): ChannelPolls =
      withPoll(pollId, poll)

    def withDeletedPoll(pollId: String): ChannelPolls =
      copy(polls = polls - pollId)
  }

  object ChannelPolls {
    given Decoder[ChannelPolls] = ConfiguredDecoder.derived
    given Encoder[ChannelPolls] = ConfiguredEncoder.derived
  }
}
