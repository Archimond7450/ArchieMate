package com.archimond7450.archiemate.twitch.api

import com.archimond7450.archiemate.CirceConfiguration.twitchConfiguration
import com.archimond7450.archiemate.helpers.JsonHelper.OffsetDateTimeJson
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}

import java.time.OffsetDateTime

object TwitchApi {
  final case class Game(id: String, name: String, box_art_url: String, igdb_id: String)
  object Game {
    given Decoder[Game] = ConfiguredDecoder.derived
    given Encoder[Game] = ConfiguredEncoder.derived
  }

  final case class User(user_id: String, user_login: String, user_name: String)
  object User {
    given Decoder[User] = ConfiguredDecoder.derived
    given Encoder[User] = ConfiguredEncoder.derived
  }

  final case class Pagination(cursor: Option[String])
  object Pagination {
    given Decoder[Pagination] = ConfiguredDecoder.derived
    given Encoder[Pagination] = ConfiguredEncoder.derived
  }

  final case class SubbedUser(broadcaster_id: String, broadcaster_login: String, broadcaster_name: String, gifter_id: String, gifter_login: String, gifter_name: String, is_gift: Boolean, tier: String, plan_name: String, user_id: String, user_login: String, user_name: String)
  object SubbedUser {
    given Decoder[SubbedUser] = ConfiguredDecoder.derived
    given Encoder[SubbedUser] = ConfiguredEncoder.derived
  }

  final case class UserFollowage(user_id: String, user_name: String, user_login: String, followed_at: OffsetDateTime)
  object UserFollowage {
    given Decoder[UserFollowage] = ConfiguredDecoder.derived
    given Encoder[UserFollowage] = ConfiguredEncoder.derived
  }

  final case class Stream(id: String, userId: String, userLogin: String, userName: String, gameId: String, gameName: String, `type`: String, title: String, tags: List[String], viewerCount: Int, startedAt: OffsetDateTime, language: String, thumbnailUrl: String, tagIds: List[String], isMature: Boolean)
  object Stream {
    given Decoder[Stream] = ConfiguredDecoder.derived
    given Encoder[Stream] = ConfiguredEncoder.derived
  }

  final case class Emote(id: String, name: String, images: EmoteImages, format: List[String], scale: List[String], themeMode: List[String])
  object Emote {
    given Decoder[Emote] = ConfiguredDecoder.derived
    given Encoder[Emote] = ConfiguredEncoder.derived
  }

  final case class EmoteWithSet(id: String, name: String, images: EmoteImages, emoteType: String, emoteSetId: String, ownerId: String, format: List[String], scale: List[String], themeMode: List[String])
  object EmoteWithSet {
    given Decoder[EmoteWithSet] = ConfiguredDecoder.derived
    given Encoder[EmoteWithSet] = ConfiguredEncoder.derived
  }

  final case class EmoteImages(url_1x: String, url_2x: String, url_4x: String)
  object EmoteImages {
    given Decoder[EmoteImages] = ConfiguredDecoder.derived
    given Encoder[EmoteImages] = ConfiguredEncoder.derived
  }
}
