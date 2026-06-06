package com.archimond7450.archiemate.kick.webhooks

import com.archimond7450.archiemate.CirceConfiguration.kickConfiguration
import io.circe.{Decoder, Encoder}
import io.circe.jawn.decode
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}

import java.time.OffsetDateTime

object KickWebhooks {
  sealed trait KickWebhook
  object KickWebhook {
    def decodeJson(
        eventType: String,
        version: String,
        json: String
    ): Either[Throwable, KickWebhook] = (eventType, version) match {
      case ("chat.message.sent", "1") =>
        decode[KickWebhooks.ChatMessageSentV1](json)
      case ("channel.followed", "1") =>
        decode[KickWebhooks.ChannelFollowedV1](json)
      case _ =>
        Left(RuntimeException("Unsupported event/version combination"))
    }
  }

  final case class ChatMessageSentV1(
      messageId: String,
      repliesTo: Option[ChatMessageSentRepliesTo],
      broadcaster: User,
      sender: User,
      content: String,
      emotes: List[ChatMessageEmote],
      createdAt: OffsetDateTime
  ) extends KickWebhook
  object ChatMessageSentV1 {
    given Decoder[ChatMessageSentV1] = ConfiguredDecoder.derived
    given Encoder[ChatMessageSentV1] = ConfiguredEncoder.derived
  }

  final case class ChatMessageSentRepliesTo(
      messageId: String,
      content: String,
      sender: User
  )
  object ChatMessageSentRepliesTo {
    given Decoder[ChatMessageSentRepliesTo] = ConfiguredDecoder.derived
    given Encoder[ChatMessageSentRepliesTo] = ConfiguredEncoder.derived
  }

  final case class User(
      isAnonymous: Boolean,
      userId: Int,
      username: String,
      isVerified: Boolean,
      profilePicture: String,
      channelSlug: String,
      identity: Option[ChatMessageIdentity]
  )
  object User {
    given Decoder[User] = ConfiguredDecoder.derived
    given Encoder[User] = ConfiguredEncoder.derived
  }

  final case class ChatMessageIdentity(
      usernameColor: String,
      badges: List[UserBadge]
  )
  object ChatMessageIdentity {
    given Decoder[ChatMessageIdentity] = ConfiguredDecoder.derived
    given Encoder[ChatMessageIdentity] = ConfiguredEncoder.derived
  }

  final case class UserBadge(
      text: String,
      `type`: String,
      count: Option[Int] = None
  )
  object UserBadge {
    given Decoder[UserBadge] = ConfiguredDecoder.derived
    given Encoder[UserBadge] = ConfiguredEncoder.derived
  }

  final case class ChatMessageEmote(
      emoteId: String,
      positions: List[ChatMessageEmotePosition]
  )
  object ChatMessageEmote {
    given Decoder[ChatMessageEmote] = ConfiguredDecoder.derived
    given Encoder[ChatMessageEmote] = ConfiguredEncoder.derived
  }

  final case class ChatMessageEmotePosition(s: Int, e: Int)
  object ChatMessageEmotePosition {
    given Decoder[ChatMessageEmotePosition] = ConfiguredDecoder.derived
    given Encoder[ChatMessageEmotePosition] = ConfiguredEncoder.derived
  }

  final case class ChannelFollowedV1(
      broadcaster: User,
      follower: User
  ) extends KickWebhook
  object ChannelFollowedV1 {
    given Decoder[ChannelFollowedV1] = ConfiguredDecoder.derived
    given Encoder[ChannelFollowedV1] = ConfiguredEncoder.derived
  }
}
