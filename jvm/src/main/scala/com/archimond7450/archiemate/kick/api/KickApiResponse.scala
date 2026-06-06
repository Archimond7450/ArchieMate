package com.archimond7450.archiemate.kick.api

import com.archimond7450.archiemate.CirceConfiguration.kickConfiguration
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import org.apache.pekko.http.scaladsl.model.StatusCode

import java.time.OffsetDateTime

sealed trait KickApiResponse

object KickApiResponse {
  final case class NOK(status: StatusCode)
      extends RuntimeException(
        s"The Kick HTTP request failed with HTTP status $status"
      )

  final case class GetToken(
      accessToken: String,
      tokenType: String,
      refreshToken: String,
      expiresIn: Int,
      scope: String
  ) extends KickApiResponse

  object GetToken {
    given Decoder[GetToken] = ConfiguredDecoder.derived
    given Encoder[GetToken] = ConfiguredEncoder.derived
  }

  final case class ValidateToken(
      data: ValidateTokenData,
      message: String
  )

  object ValidateToken {
    given Decoder[ValidateToken] = ConfiguredDecoder.derived
    given Encoder[ValidateToken] = ConfiguredEncoder.derived
  }

  final case class ValidateTokenData(
      active: Boolean,
      clientId: String,
      tokenType: String,
      scope: String,
      exp: Int
  ) extends KickApiResponse

  object ValidateTokenData {
    given Decoder[ValidateTokenData] = ConfiguredDecoder.derived
    given Encoder[ValidateTokenData] = ConfiguredEncoder.derived
  }

  final case class GetUsers(data: List[User], message: Option[String])
      extends KickApiResponse
  object GetUsers {
    given Decoder[GetUsers] = ConfiguredDecoder.derived
    given Encoder[GetUsers] = ConfiguredEncoder.derived
  }

  final case class User(
      email: String,
      name: String,
      profilePicture: String,
      userId: Int
  ) extends KickApiResponse
  object User {
    given Decoder[User] = ConfiguredDecoder.derived
    given Encoder[User] = ConfiguredEncoder.derived
  }

  final case class GetPublicKey(data: GetPublicKeyData, message: String)
      extends KickApiResponse
  object GetPublicKey {
    given Decoder[GetPublicKey] = ConfiguredDecoder.derived
    given Encoder[GetPublicKey] = ConfiguredEncoder.derived
  }

  final case class GetPublicKeyData(publicKey: String)
  object GetPublicKeyData {
    given Decoder[GetPublicKeyData] = ConfiguredDecoder.derived
    given Encoder[GetPublicKeyData] = ConfiguredEncoder.derived
  }

  final case class GetEventsSubscriptions(
      data: GetEventsSubscriptionsData,
      message: String
  ) extends KickApiResponse
  object GetEventsSubscriptions {
    given Decoder[GetEventsSubscriptions] = ConfiguredDecoder.derived
    given Encoder[GetEventsSubscriptions] = ConfiguredEncoder.derived
  }

  final case class GetEventsSubscriptionsData(
      appId: String,
      broadcasterUserId: Int,
      createdAt: OffsetDateTime,
      event: String,
      id: String,
      method: String,
      updatedAt: OffsetDateTime,
      version: Int
  )
  object GetEventsSubscriptionsData {
    given Decoder[GetEventsSubscriptionsData] = ConfiguredDecoder.derived
    given Encoder[GetEventsSubscriptionsData] = ConfiguredEncoder.derived
  }

  final case class SubscribeToEvents(
      data: SubscribeToEventsData,
      message: String
  ) extends KickApiResponse
  object SubscribeToEvents {
    given Decoder[SubscribeToEvents] = ConfiguredDecoder.derived
    given Encoder[SubscribeToEvents] = ConfiguredEncoder.derived
  }

  final case class SubscribeToEventsData(
      error: Option[String],
      name: String,
      subscriptionId: String,
      version: Int
  )
  object SubscribeToEventsData {
    given Decoder[SubscribeToEventsData] = ConfiguredDecoder.derived
    given Encoder[SubscribeToEventsData] = ConfiguredEncoder.derived
  }

  final case class PostChatMessage(
      data: List[PostChatMessageData],
      message: Option[String]
  ) extends KickApiResponse
  object PostChatMessage {
    given Decoder[PostChatMessage] = ConfiguredDecoder.derived
    given Encoder[PostChatMessage] = ConfiguredEncoder.derived
  }

  final case class PostChatMessageData(isSent: Boolean, messageId: String)
      extends KickApiResponse
  object PostChatMessageData {
    given Decoder[PostChatMessageData] = ConfiguredDecoder.derived
    given Encoder[PostChatMessageData] = ConfiguredEncoder.derived
  }

  final case class GetCategories(
      data: List[GetCategoriesData],
      message: Option[String],
      pagination: Option[Pagination]
  ) extends KickApiResponse
  object GetCategories {
    given Decoder[GetCategories] = ConfiguredDecoder.derived
    given Encoder[GetCategories] = ConfiguredEncoder.derived
  }

  final case class GetCategoriesData(
      id: Int,
      name: String,
      tags: List[String],
      thumbnail: String
  ) extends KickApiResponse
  object GetCategoriesData {
    given Decoder[GetCategoriesData] = ConfiguredDecoder.derived
    given Encoder[GetCategoriesData] = ConfiguredEncoder.derived
  }

  final case class Pagination(nextCursor: String)
  object Pagination {
    given Decoder[Pagination] = ConfiguredDecoder.derived
    given Encoder[Pagination] = ConfiguredEncoder.derived
  }

  final case class GetChannels(data: List[Channel], message: Option[String])
      extends KickApiResponse
  object GetChannels {
    given Decoder[GetChannels] = ConfiguredDecoder.derived
    given Encoder[GetChannels] = ConfiguredEncoder.derived
  }

  final case class Channel(
      activeSubscribersCount: Option[Int],
      bannerPicture: String,
      broadcasterUserId: Int,
      canceledSubscribersCount: Option[Int],
      category: Category,
      channelDescription: String,
      slug: String,
      stream: StreamInfo,
      streamTitle: String
  ) extends KickApiResponse
  object Channel {
    given Decoder[Channel] = ConfiguredDecoder.derived
    given Encoder[Channel] = ConfiguredEncoder.derived
  }

  final case class Category(id: String, name: String, thumbnail: String)
  object Category {
    given Decoder[Category] = ConfiguredDecoder.derived
    given Encoder[Category] = ConfiguredEncoder.derived
  }

  final case class StreamInfo(
      customTags: List[String],
      isLive: Boolean,
      isMature: Boolean,
      key: Option[String],
      language: String,
      startTime: OffsetDateTime,
      thumbnail: String,
      url: String,
      viewerCount: Int
  )
  object StreamInfo {
    given Decoder[StreamInfo] = ConfiguredDecoder.derived
    given Encoder[StreamInfo] = ConfiguredEncoder.derived
  }

  case object ChannelUpdated
}
