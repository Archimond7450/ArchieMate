package com.archimond7450.archiemate.kick.api

import com.archimond7450.archiemate.CirceConfiguration.kickConfiguration
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import org.apache.pekko.http.scaladsl.model.StatusCode

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
      expiresIn: String,
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
}
