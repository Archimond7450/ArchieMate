package com.archimond7450.archiemate.http

import com.archimond7450.archiemate.CirceConfiguration.frontendConfiguration
import com.archimond7450.archiemate.helpers.JsonHelper
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}

object User {
  case class UserResponse(userId: String, userName: String, userDisplayName: String, profilePictureUrl: String)
  object UserResponse {
    given Decoder[UserResponse] = ConfiguredDecoder.derived
    given Encoder[UserResponse] = JsonHelper.dropNulls(ConfiguredEncoder.derived)
  }
}
