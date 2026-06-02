package com.archimond7450.archiemate.http

import com.archimond7450.archiemate.CirceConfiguration.frontendConfiguration
import com.archimond7450.archiemate.helpers.JsonHelper
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}

object User {
  case class UserInfo(userId: String, userName: String, userDisplayName: String, profilePictureUrl: String)
  object UserInfo {
    given Decoder[UserInfo] = ConfiguredDecoder.derived
    given Encoder[UserInfo] = ConfiguredEncoder.derived
  }

  case class UserResponse(twitchUserInfo: UserInfo, kickUserInfo: Option[UserInfo])
  object UserResponse {
    given Decoder[UserResponse] = ConfiguredDecoder.derived
    given Encoder[UserResponse] = JsonHelper.dropNulls(ConfiguredEncoder.derived)
  }
}
