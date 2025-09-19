package com.archimond7450.archiemate

import io.circe.derivation.Configuration

object CirceConfiguration {
  given frontendConfiguration: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults
  given twitchConfiguration: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults
  given youtubeConfiguration: Configuration = Configuration.default.withDefaults
}
