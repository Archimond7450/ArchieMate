package com.archimond7450.archiemate.twitch.api

import com.archimond7450.archiemate.CirceConfiguration.twitchConfiguration
import com.archimond7450.archiemate.twitch.eventsub.{Condition, Subscription, Transport}
import com.archimond7450.archiemate.helpers.JsonHelper.OffsetDateTimeJson
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import org.apache.pekko.http.scaladsl.model.StatusCode

import java.time.OffsetDateTime

sealed trait TwitchApiResponse

object TwitchApiResponse {
  final case class NOK(status: StatusCode) extends RuntimeException(s"The HTTP request failed with HTTP status $status")

  final case class GetToken(access_token: String, expires_in: Int, refresh_token: String, scope: Option[List[String]], token_type: String) extends TwitchApiResponse

  object GetToken {
    given Decoder[GetToken] = ConfiguredDecoder.derived
    given Encoder[GetToken] = ConfiguredEncoder.derived
  }

  final case class ValidateToken(client_id: String, login: String, scopes: List[String], user_id: String, expires_in: Int) extends TwitchApiResponse

  object ValidateToken {
    given Decoder[ValidateToken] = ConfiguredDecoder.derived
    given Encoder[ValidateToken] = ConfiguredEncoder.derived
  }

  final case class GetTokenUser(
                                         id: String,
                                         login: String,
                                         display_name: String,
                                         `type`: String,
                                         broadcaster_type: String,
                                         description: String,
                                         profile_image_url: String,
                                         offline_image_url: String,
                                         view_count: Int,
                                         email: Option[String],
                                         created_at: OffsetDateTime) extends TwitchApiResponse
  object GetTokenUser {
    given Decoder[GetTokenUser] = ConfiguredDecoder.derived
    given Encoder[GetTokenUser] = ConfiguredEncoder.derived
  }

  final case class GetUsers(data: List[GetTokenUser]) extends TwitchApiResponse
  object GetUsers {
    given Decoder[GetUsers] = ConfiguredDecoder.derived
    given Encoder[GetUsers] = ConfiguredEncoder.derived
  }

  case object ModifyChannelInformation extends TwitchApiResponse

  final case class GameResponse(game: Option[TwitchApi.Game]) extends TwitchApiResponse
  object GameResponse {
    given Decoder[GameResponse] = ConfiguredDecoder.derived
    given Encoder[GameResponse] = ConfiguredEncoder.derived
  }

  case class GameNotFoundException(gameName: String) extends RuntimeException(s"Game with name $gameName was not found.")

  final case class GetGames(data: List[TwitchApi.Game]) extends TwitchApiResponse
  object GetGames {
    given Decoder[GetGames] = ConfiguredDecoder.derived
    given Encoder[GetGames] = ConfiguredEncoder.derived
  }

  final case class ChannelInformation(
                                               broadcaster_id: String,
                                               broadcaster_login: String,
                                               broadcaster_name: String,
                                               broadcaster_language: String,
                                               game_name: String,
                                               game_id: String,
                                               title: String,
                                               delay: Int,
                                               tags: List[String],
                                               content_classification_labels: List[String],
                                               is_branded_content: Boolean) extends TwitchApiResponse
  object ChannelInformation {
    given Decoder[ChannelInformation] = ConfiguredDecoder.derived
    given Encoder[ChannelInformation] = ConfiguredEncoder.derived
  }

  final case class GetChannelInformation(data: List[ChannelInformation]) extends TwitchApiResponse
  object GetChannelInformation {
    given Decoder[GetChannelInformation] = ConfiguredDecoder.derived
    given Encoder[GetChannelInformation] = ConfiguredEncoder.derived
  }

  final case class GetChatters(data: List[TwitchApi.User], pagination: TwitchApi.Pagination, total: Int) extends TwitchApiResponse
  object GetChatters {
    given Decoder[GetChatters] = ConfiguredDecoder.derived
    given Encoder[GetChatters] = ConfiguredEncoder.derived
  }

  final case class GetModerators(data: List[TwitchApi.User], pagination: TwitchApi.Pagination) extends TwitchApiResponse
  object GetModerators {
    given Decoder[GetModerators] = ConfiguredDecoder.derived
    given Encoder[GetModerators] = ConfiguredEncoder.derived
  }

  final case class GetVIPs(data: List[TwitchApi.User], pagination: TwitchApi.Pagination) extends TwitchApiResponse
  object GetVIPs {
    given Decoder[GetVIPs] = ConfiguredDecoder.derived
    given Encoder[GetVIPs] = ConfiguredEncoder.derived
  }

  final case class GetSubs(data: List[TwitchApi.SubbedUser], pagination: TwitchApi.Pagination, total: Int, points: Int) extends TwitchApiResponse
  object GetSubs {
    given Decoder[GetSubs] = ConfiguredDecoder.derived
    given Encoder[GetSubs] = ConfiguredEncoder.derived
  }

  final case class GetChannelFollowers(total: Int, data: List[TwitchApi.UserFollowage], pagination: TwitchApi.Pagination) extends TwitchApiResponse
  object GetChannelFollowers {
    given Decoder[GetChannelFollowers] = ConfiguredDecoder.derived
    given Encoder[GetChannelFollowers] = ConfiguredEncoder.derived
  }

  final case class CheckUserFollowage(followage: Option[TwitchApi.UserFollowage]) extends TwitchApiResponse
  object CheckUserFollowage {
    given Decoder[CheckUserFollowage] = ConfiguredDecoder.derived
    given Encoder[CheckUserFollowage] = ConfiguredEncoder.derived
  }

  case object SendShoutout extends TwitchApiResponse

  final case class CreateEventSubWebsocketSubscriptionResponse(data: List[Subscription], total: Int, totalCost: Int, maxTotalCost: Int) extends TwitchApiResponse
  object CreateEventSubWebsocketSubscriptionResponse {
    given Decoder[CreateEventSubWebsocketSubscriptionResponse] = ConfiguredDecoder.derived
    given Encoder[CreateEventSubWebsocketSubscriptionResponse] = ConfiguredEncoder.derived
  }

  final case class GetStream(data: List[TwitchApi.Stream], pagination: TwitchApi.Pagination) extends TwitchApiResponse
  object GetStream {
    given Decoder[GetStream] = ConfiguredDecoder.derived
    given Encoder[GetStream] = ConfiguredEncoder.derived
  }

  final case class GetGlobalEmotes(data: List[TwitchApi.Emote], template: String) extends TwitchApiResponse
  object GetGlobalEmotes {
    given Decoder[GetGlobalEmotes] = ConfiguredDecoder.derived
    given Encoder[GetGlobalEmotes] = ConfiguredEncoder.derived
  }

  final case class GetEmoteSets(data: List[TwitchApi.EmoteWithSet], template: String) extends TwitchApiResponse
  object GetEmoteSets {
    given Decoder[GetEmoteSets] = ConfiguredDecoder.derived
    given Encoder[GetEmoteSets] = ConfiguredEncoder.derived
  }
}