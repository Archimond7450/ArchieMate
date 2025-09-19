package com.archimond7450.archiemate.youtube.api

import com.archimond7450.archiemate.CirceConfiguration.youtubeConfiguration
import com.archimond7450.archiemate.helpers.JsonHelper.dropNulls
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import io.circe.syntax.EncoderOps
import io.circe.*
import org.apache.pekko.http.scaladsl.model.StatusCode

import java.time.OffsetDateTime

sealed trait YouTubeApiResponse

object YouTubeApiResponse {
  final case class NOK(status: StatusCode) extends RuntimeException(s"The HTTP request failed with HTTP status $status")

  final case class GetToken(access_token: String, expires_in: Int, refresh_token: String, refresh_token_expires_in: Option[Int], scope: List[String], token_type: String) extends YouTubeApiResponse

  object GetToken {
    given Decoder[GetToken] = (c: HCursor) => {
      for {
        access_token <- c.get[String]("access_token")
        expires_in <- c.get[Int]("expires_in")
        refresh_token <- c.get[String]("refresh_token")
        refresh_token_expires_in <- c.get[Option[Int]]("refresh_token_expires_in")
        scope <- c.get[String]("scope")
        token_type <- c.get[String]("token_type")
      } yield GetToken(access_token, expires_in, refresh_token, refresh_token_expires_in, scope.split("\\s").toList, token_type)
    }
    given Encoder[GetToken] = dropNulls((t: GetToken) => {
      Json.fromJsonObject(JsonObject(
        "access_token" -> Json.fromString(t.access_token),
        "expires_in" -> Json.fromInt(t.expires_in),
        "refresh_token" -> Json.fromString(t.refresh_token),
        "refresh_token_expires_in" -> t.refresh_token_expires_in.map(c => c.asJson).getOrElse(Json.Null),
        "scope" -> Json.fromString(t.scope.mkString(" ")),
        "token_type" -> Json.fromString(t.token_type)
      ))
    })
  }

  final case class PageInfo(totalResults: Int, resultsPerPage: Int)

  object PageInfo {
    given Decoder[PageInfo] = ConfiguredDecoder.derived
    given Encoder[PageInfo] = ConfiguredEncoder.derived
  }

  final case class Response[T <: Item](kind: String, etag: String, nextPageToken: Option[String], prevPageToken: Option[String], pageInfo: PageInfo, items: List[T]) extends YouTubeApiResponse

  object Response {
    given Decoder[Response[Item]] = ConfiguredDecoder.derived
    given Encoder[Response[Item]] = ConfiguredEncoder.derived
    given channelResponseDecoder: Decoder[Response[Channel]] = ConfiguredDecoder.derived
    given channelResponseEncoder: Encoder[Response[Channel]] = ConfiguredEncoder.derived
    given liveBroadcastsResponseDecoder: Decoder[Response[LiveBroadcast]] = ConfiguredDecoder.derived
    given liveBroadcastsResponseEncoder: Encoder[Response[LiveBroadcast]] = ConfiguredEncoder.derived
  }

  sealed trait Item

  object Item {
    given Decoder[Item] = ConfiguredDecoder.derived
    given Encoder[Item] = ConfiguredEncoder.derived
  }

  final case class Channel(kind: String, etag: String, id: String, snippet: ChannelSnippet, contentDetails: ChannelContentDetails, statistics: ChannelStatistics, topicDetails: ChannelTopicDetails, status: ChannelStatus, brandingSettings: ChannelBrandingSettings, contentOwnerDetails: ChannelContentOwnerDetails, localizations: Option[Map[String, ChannelLocalization]]) extends Item

  object Channel {
    given Decoder[Channel] = ConfiguredDecoder.derived
    given Encoder[Channel] = ConfiguredEncoder.derived
  }

  final case class ChannelSnippet(title: String, description: String, customUrl: String, publishedAt: OffsetDateTime, thumbnails: Map[String, Thumbnail], defaultLanguage: Option[String], localized: ChannelLocalized, country: Option[String])

  object ChannelSnippet {
    given Decoder[ChannelSnippet] = ConfiguredDecoder.derived
    given Encoder[ChannelSnippet] = ConfiguredEncoder.derived
  }

  final case class Thumbnail(url: String, width: Int, height: Int)

  object Thumbnail {
    given Decoder[Thumbnail] = ConfiguredDecoder.derived
    given Encoder[Thumbnail] = ConfiguredEncoder.derived
  }

  final case class ChannelLocalized(title: String, description: String)

  object ChannelLocalized {
    given Decoder[ChannelLocalized] = ConfiguredDecoder.derived
    given Encoder[ChannelLocalized] = ConfiguredEncoder.derived
  }

  final case class ChannelContentDetails(relatedPlaylists: ChannelRelatedPlaylists)

  object ChannelContentDetails {
    given Decoder[ChannelContentDetails] = ConfiguredDecoder.derived
    given Encoder[ChannelContentDetails] = ConfiguredEncoder.derived
  }

  final case class ChannelRelatedPlaylists(likes: String, favorites: Option[String], uploads: String)

  object ChannelRelatedPlaylists {
    given Decoder[ChannelRelatedPlaylists] = ConfiguredDecoder.derived
    given Encoder[ChannelRelatedPlaylists] = ConfiguredEncoder.derived
  }

  final case class ChannelStatistics(viewCount: Int, subscriberCount: Int, hiddenSubscriberCount: Boolean, videoCount: Int)

  object ChannelStatistics {
    given Decoder[ChannelStatistics] = ConfiguredDecoder.derived
    given Encoder[ChannelStatistics] = ConfiguredEncoder.derived
  }

  final case class ChannelTopicDetails(topicIds: List[String], topicCategories: List[String])

  object ChannelTopicDetails {
    given Decoder[ChannelTopicDetails] = ConfiguredDecoder.derived
    given Encoder[ChannelTopicDetails] = ConfiguredEncoder.derived
  }

  final case class ChannelStatus(privacyStatus: String, isLinked: Boolean, longUploadsStatus: String, isChannelMonetizationEnabled: Boolean, madeForKids: Option[Boolean], selfDeclaredMadeForKids: Option[Boolean])

  object ChannelStatus {
    given Decoder[ChannelStatus] = ConfiguredDecoder.derived
    given Encoder[ChannelStatus] = ConfiguredEncoder.derived
  }

  final case class ChannelBrandingSettings(channel: ChannelBrandingSettingsChannel, watch: Option[ChannelWatch])

  object ChannelBrandingSettings {
    given Decoder[ChannelBrandingSettings] = ConfiguredDecoder.derived
    given Encoder[ChannelBrandingSettings] = ConfiguredEncoder.derived
  }

  final case class ChannelBrandingSettingsChannel(title: Option[String], description: Option[String], keywords: Option[String], trackingAnalyticsAccountId: Option[String], unsubscribedTrailer: Option[String], defaultLanguage: Option[String], country: Option[String])

  object ChannelBrandingSettingsChannel {
    given Decoder[ChannelBrandingSettingsChannel] = ConfiguredDecoder.derived
    given Encoder[ChannelBrandingSettingsChannel] = ConfiguredEncoder.derived
  }

  final case class ChannelWatch(textColor: String, backgroundColor: String, featuredPlaylistId: String)

  object ChannelWatch {
    given Decoder[ChannelWatch] = ConfiguredDecoder.derived
    given Encoder[ChannelWatch] = ConfiguredEncoder.derived
  }

  final case class ChannelContentOwnerDetails(contentOwner: Option[String], timeLinked: Option[String])

  object ChannelContentOwnerDetails {
    given Decoder[ChannelContentOwnerDetails] = ConfiguredDecoder.derived
    given Encoder[ChannelContentOwnerDetails] = ConfiguredEncoder.derived
  }

  final case class ChannelLocalization(title: String, description: String)

  object ChannelLocalization {
    given Decoder[ChannelLocalization] = ConfiguredDecoder.derived
    given Encoder[ChannelLocalization] = ConfiguredEncoder.derived
  }

  final case class LiveBroadcast(kind: String, etag: String, id: String, snippet: LiveBroadcastSnippet, status: LiveBroadcastStatus, contentDetails: LiveBroadcastContentDetails, statistics: Option[LiveBroadcastStatistics], monetizationDetails: LiveBroadcastMonetizationDetails) extends Item

  object LiveBroadcast {
    given Decoder[LiveBroadcast] = ConfiguredDecoder.derived
    given Encoder[LiveBroadcast] = ConfiguredEncoder.derived
  }

  final case class LiveBroadcastSnippet(publishedAt: OffsetDateTime, channelId: String, title: String, description: String, thumbnails: Map[String, Thumbnail], scheduledStartTime: OffsetDateTime, scheduledEndTime: Option[OffsetDateTime], actualStartTime: Option[OffsetDateTime], actualEndTime: Option[OffsetDateTime], isDefaultBroadcast: Boolean, liveChatId: String)

  object LiveBroadcastSnippet {
    given Decoder[LiveBroadcastSnippet] = ConfiguredDecoder.derived
    given Encoder[LiveBroadcastSnippet] = ConfiguredEncoder.derived
  }

  final case class LiveBroadcastStatus(lifeCycleStatus: String, privacyStatus: String, recordingStatus: String, madeForKids: Boolean, selfDeclaredMadeForKids: Boolean)

  object LiveBroadcastStatus {
    given Decoder[LiveBroadcastStatus] = ConfiguredDecoder.derived
    given Encoder[LiveBroadcastStatus] = ConfiguredEncoder.derived
  }

  final case class LiveBroadcastContentDetails(boundStreamId: String, boundStreamLastUpdateTimeMs: OffsetDateTime, monitorStream: LiveBroadcastContentDetailsMonitorStream, enableEmbed: Boolean, enableDvr: Boolean, enableContentEncryption: Boolean, recordFromStart: Boolean, enableClosedCaptions: Boolean, closedCaptionsType: String, projection: String, enableLowLatency: Boolean, latencyPreference: String, enableAutoStart: Boolean, enableAutoStop: Boolean)

  object LiveBroadcastContentDetails {
    given Decoder[LiveBroadcastContentDetails] = ConfiguredDecoder.derived
    given Encoder[LiveBroadcastContentDetails] = ConfiguredEncoder.derived
  }

  final case class LiveBroadcastContentDetailsMonitorStream(enableMonitorStream: Boolean, broadcastStreamDelayMs: Int, embedHtml: String)

  object LiveBroadcastContentDetailsMonitorStream {
    given Decoder[LiveBroadcastContentDetailsMonitorStream] = ConfiguredDecoder.derived
    given Encoder[LiveBroadcastContentDetailsMonitorStream] = ConfiguredEncoder.derived
  }

  final case class LiveBroadcastStatistics(totalChatCount: Int)

  object LiveBroadcastStatistics {
    given Decoder[LiveBroadcastStatistics] = ConfiguredDecoder.derived
    given Encoder[LiveBroadcastStatistics] = ConfiguredEncoder.derived
  }

  final case class LiveBroadcastMonetizationDetails(cuepointSchedule: LiveBroadcastMonetizationDetailsCuepointSchedule)

  object LiveBroadcastMonetizationDetails {
    given Decoder[LiveBroadcastMonetizationDetails] = ConfiguredDecoder.derived
    given Encoder[LiveBroadcastMonetizationDetails] = ConfiguredEncoder.derived
  }

  final case class LiveBroadcastMonetizationDetailsCuepointSchedule(enabled: Boolean, pauseAdsUntil: Option[OffsetDateTime], scheduleStrategy: Option[String], repeatIntervalSec: Option[Int])

  object LiveBroadcastMonetizationDetailsCuepointSchedule {
    given Decoder[LiveBroadcastMonetizationDetailsCuepointSchedule] = ConfiguredDecoder.derived
    given Encoder[LiveBroadcastMonetizationDetailsCuepointSchedule] = ConfiguredEncoder.derived
  }
}
