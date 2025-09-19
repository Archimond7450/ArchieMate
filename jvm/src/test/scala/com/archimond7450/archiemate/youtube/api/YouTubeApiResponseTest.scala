package com.archimond7450.archiemate.youtube.api

import com.archimond7450.archiemate.youtube.api.YouTubeApiResponse
import io.circe.jawn.decode
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}
import scala.io.Source

class YouTubeApiResponseTest extends AnyWordSpecLike with Matchers {
  val TEST_JSON_DIRECTORY = "youtube/api"

  def readFile(resourcePath: String): String = {
    val source = Source.fromResource(resourcePath)
    try source.getLines().mkString("\n") finally source.close()
  }

  "YouTube API response decoder" when {
    "Get My Channel response is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/GetMyChannelResponse.json")
        val expectedResponse: YouTubeApiResponse.Response[YouTubeApiResponse.Channel] = YouTubeApiResponse.Response(
          kind = "youtube#channelListResponse",
          etag = "CflM3Ykcf-c3fmmb6RSuq7TPHq0",
          nextPageToken = None,
          prevPageToken = None,
          pageInfo = YouTubeApiResponse.PageInfo(
            totalResults = 1,
            resultsPerPage = 5
          ),
          items = List(
            YouTubeApiResponse.Channel(
              kind = "youtube#channel",
              etag = "Gslb6y3-x9bKZH2pwkH84ek_twQ",
              id = "UCj_hcfqnLWGNXtBlwasdOOg",
              snippet = YouTubeApiResponse.ChannelSnippet(
                title = "Archimond Gaming",
                description = "",
                customUrl = "@archimondgaming5298",
                publishedAt = OffsetDateTime.of(LocalDateTime.of(2017, 9, 3, 12, 15, 58, 0), ZoneOffset.UTC),
                thumbnails = Map(
                  "default" -> YouTubeApiResponse.Thumbnail(
                    url = "https://yt3.ggpht.com/ytc/AIdro_mB-TJwbUWdsCFxRKkm3ND96RKdIP1Gudsp58oQ4Kl-Yw=s88-c-k-c0x00ffffff-no-rj",
                    width = 88,
                    height = 88
                  ),
                  "medium" -> YouTubeApiResponse.Thumbnail(
                    url = "https://yt3.ggpht.com/ytc/AIdro_mB-TJwbUWdsCFxRKkm3ND96RKdIP1Gudsp58oQ4Kl-Yw=s240-c-k-c0x00ffffff-no-rj",
                    width = 240,
                    height = 240
                  ),
                  "high" -> YouTubeApiResponse.Thumbnail(
                    url = "https://yt3.ggpht.com/ytc/AIdro_mB-TJwbUWdsCFxRKkm3ND96RKdIP1Gudsp58oQ4Kl-Yw=s800-c-k-c0x00ffffff-no-rj",
                    width = 800,
                    height = 800
                  )
                ),
                defaultLanguage = None,
                localized = YouTubeApiResponse.ChannelLocalized(
                  title = "Archimond Gaming",
                  description = ""
                ),
                country = None
              ),
              contentDetails = YouTubeApiResponse.ChannelContentDetails(
                relatedPlaylists = YouTubeApiResponse.ChannelRelatedPlaylists(
                  likes = "LL",
                  favorites = None,
                  uploads = "UUj_hcfqnLWGNXtBlwasdOOg"
                )
              ),
              statistics = YouTubeApiResponse.ChannelStatistics(
                viewCount = 1651,
                subscriberCount = 11,
                hiddenSubscriberCount = false,
                videoCount = 55
              ),
              topicDetails = YouTubeApiResponse.ChannelTopicDetails(
                topicIds = List(
                  "/m/025zzc",
                  "/m/03hf_rm",
                  "/m/0bzvm2",
                  "/m/0403l3g",
                  "/m/04q1x3q",
                  "/m/02ntfj"
                ),
                topicCategories = List(
                  "https://en.wikipedia.org/wiki/Action_game",
                  "https://en.wikipedia.org/wiki/Strategy_video_game",
                  "https://en.wikipedia.org/wiki/Video_game_culture",
                  "https://en.wikipedia.org/wiki/Role-playing_video_game",
                  "https://en.wikipedia.org/wiki/Puzzle_video_game",
                  "https://en.wikipedia.org/wiki/Action-adventure_game"
                )
              ),
              status = YouTubeApiResponse.ChannelStatus(
                privacyStatus = "public",
                isLinked = true,
                longUploadsStatus = "allowed",
                isChannelMonetizationEnabled = false,
                madeForKids = None,
                selfDeclaredMadeForKids = None
              ),
              brandingSettings = YouTubeApiResponse.ChannelBrandingSettings(
                channel = YouTubeApiResponse.ChannelBrandingSettingsChannel(
                  title = Some("Archimond Gaming"),
                  description = None,
                  keywords = Some("Warcraft III"),
                  trackingAnalyticsAccountId = None,
                  unsubscribedTrailer = None,
                  defaultLanguage = None,
                  country = None
                ),
                watch = None
              ),
              contentOwnerDetails = YouTubeApiResponse.ChannelContentOwnerDetails(
                contentOwner = None,
                timeLinked = None
              ),
              localizations = None
            )
          )
        )
        decode[YouTubeApiResponse.Response[YouTubeApiResponse.Channel]](json) shouldEqual Right(expectedResponse)
      }
    }

    "Get My Live Broadcasts response is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/GetMyLiveBroadcastsResponse.json")
        val expectedResponse: YouTubeApiResponse.Response[YouTubeApiResponse.LiveBroadcast] = YouTubeApiResponse.Response(
          kind = "youtube#liveBroadcastListResponse",
          etag = "0X6R_gGnK2b0IeOM_MeDijQeJXI",
          nextPageToken = Some("CAUQAA"),
          prevPageToken = None,
          pageInfo = YouTubeApiResponse.PageInfo(
            totalResults = 44,
            resultsPerPage = 5
          ),
          items = List(
            YouTubeApiResponse.LiveBroadcast(
              kind = "youtube#liveBroadcast",
              etag = "PKSSuu9iltobC4uf2rk6PlSd8OU",
              id = "Rw9damIA33A",
              snippet = YouTubeApiResponse.LiveBroadcastSnippet(
                publishedAt = OffsetDateTime.of(LocalDateTime.of(2021, 4, 26, 18, 5, 11, 0), ZoneOffset.UTC),
                channelId = "UCj_hcfqnLWGNXtBlwasdOOg",
                title = "Mon 26th Apr: More Hearthstone chilling",
                description = "",
                thumbnails = Map(
                  "default" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/Rw9damIA33A/default_live.jpg",
                    width = 120,
                    height = 90
                  ),
                  "medium" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/Rw9damIA33A/mqdefault_live.jpg",
                    width = 320,
                    height = 180
                  ),
                  "high" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/Rw9damIA33A/hqdefault_live.jpg",
                    width = 480,
                    height = 360
                  ),
                  "standard" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/Rw9damIA33A/sddefault_live.jpg",
                    width = 640,
                    height = 480
                  ),
                  "maxres" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/Rw9damIA33A/maxresdefault_live.jpg",
                    width = 1280,
                    height = 720
                  )
                ),
                scheduledStartTime = OffsetDateTime.of(LocalDateTime.of(2021, 4, 26, 18, 5, 29, 0), ZoneOffset.UTC),
                scheduledEndTime = None,
                actualStartTime = None,
                actualEndTime = None,
                isDefaultBroadcast = false,
                liveChatId = "KicKGFVDal9oY2ZxbkxXR05YdEJsd2FzZE9PZxILUnc5ZGFtSUEzM0E"
              ),
              status = YouTubeApiResponse.LiveBroadcastStatus(
                lifeCycleStatus = "ready",
                privacyStatus = "public",
                recordingStatus = "notRecording",
                madeForKids = false,
                selfDeclaredMadeForKids = false
              ),
              contentDetails = YouTubeApiResponse.LiveBroadcastContentDetails(
                boundStreamId = "j_hcfqnLWGNXtBlwasdOOg1619460312792676",
                boundStreamLastUpdateTimeMs = OffsetDateTime.of(LocalDateTime.of(2022, 9, 16, 20, 53, 2, 0), ZoneOffset.UTC),
                monitorStream = YouTubeApiResponse.LiveBroadcastContentDetailsMonitorStream(
                  enableMonitorStream = true,
                  broadcastStreamDelayMs = 0,
                  embedHtml = "\u003ciframe width=\"425\" height=\"344\" src=\"https://www.youtube.com/embed/Rw9damIA33A?autoplay=1&livemonitor=1\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" referrerpolicy=\"strict-origin-when-cross-origin\" allowfullscreen\u003e\u003c/iframe\u003e"
                ),
                enableEmbed = true,
                enableDvr = true,
                enableContentEncryption = false,
                recordFromStart = true,
                enableClosedCaptions = false,
                closedCaptionsType = "closedCaptionsDisabled",
                projection = "rectangular",
                enableLowLatency = false,
                latencyPreference = "normal",
                enableAutoStart = true,
                enableAutoStop = true
              ),
              statistics = None,
              monetizationDetails = YouTubeApiResponse.LiveBroadcastMonetizationDetails(
                cuepointSchedule = YouTubeApiResponse.LiveBroadcastMonetizationDetailsCuepointSchedule(
                  enabled = false,
                  pauseAdsUntil = None,
                  scheduleStrategy = None,
                  repeatIntervalSec = None
                )
              )
            ),
            YouTubeApiResponse.LiveBroadcast(
              kind = "youtube#liveBroadcast",
              etag = "Ig5muWboG1bL4Ikk1vSwfLSHtG4",
              id = "NL798roEsiQ",
              snippet = YouTubeApiResponse.LiveBroadcastSnippet(
                publishedAt = OffsetDateTime.of(LocalDateTime.of(2021, 4, 24, 18, 1, 55, 0), ZoneOffset.UTC),
                channelId = "UCj_hcfqnLWGNXtBlwasdOOg",
                title = "Sat 24th Apr: Hearthstone chilling",
                description = "",
                thumbnails = Map(
                  "default" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/NL798roEsiQ/default.jpg",
                    width = 120,
                    height = 90
                  ),
                  "medium" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/NL798roEsiQ/mqdefault.jpg",
                    width = 320,
                    height = 180
                  ),
                  "high" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/NL798roEsiQ/hqdefault.jpg",
                    width = 480,
                    height = 360
                  ),
                  "standard" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/NL798roEsiQ/sddefault.jpg",
                    width = 640,
                    height = 480
                  ),
                  "maxres" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/NL798roEsiQ/maxresdefault.jpg",
                    width = 1280,
                    height = 720
                  )
                ),
                scheduledStartTime = OffsetDateTime.of(LocalDateTime.of(2021, 4, 24, 18, 2, 11, 0), ZoneOffset.UTC),
                scheduledEndTime = None,
                actualStartTime = Some(OffsetDateTime.of(LocalDateTime.of(2021, 4, 24, 18, 2, 15, 0), ZoneOffset.UTC)),
                actualEndTime = Some(OffsetDateTime.of(LocalDateTime.of(2021, 4, 24, 21, 53, 57, 0), ZoneOffset.UTC)),
                isDefaultBroadcast = false,
                liveChatId = "KicKGFVDal9oY2ZxbkxXR05YdEJsd2FzZE9PZxILTkw3OThyb0VzaVE"
              ),
              status = YouTubeApiResponse.LiveBroadcastStatus(
                lifeCycleStatus = "complete",
                privacyStatus = "public",
                recordingStatus = "recorded",
                madeForKids = false,
                selfDeclaredMadeForKids = false
              ),
              contentDetails = YouTubeApiResponse.LiveBroadcastContentDetails(
                boundStreamId = "j_hcfqnLWGNXtBlwasdOOg1619287315791552",
                boundStreamLastUpdateTimeMs = OffsetDateTime.of(LocalDateTime.of(2022, 9, 16, 2, 58, 37, 0), ZoneOffset.UTC),
                monitorStream = YouTubeApiResponse.LiveBroadcastContentDetailsMonitorStream(
                  enableMonitorStream = true,
                  broadcastStreamDelayMs = 0,
                  embedHtml = "\u003ciframe width=\"425\" height=\"344\" src=\"https://www.youtube.com/embed/NL798roEsiQ?autoplay=1&livemonitor=1\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" referrerpolicy=\"strict-origin-when-cross-origin\" allowfullscreen\u003e\u003c/iframe\u003e"
                ),
                enableEmbed = true,
                enableDvr = true,
                enableContentEncryption = false,
                recordFromStart = true,
                enableClosedCaptions = false,
                closedCaptionsType = "closedCaptionsDisabled",
                projection = "rectangular",
                enableLowLatency = false,
                latencyPreference = "normal",
                enableAutoStart = true,
                enableAutoStop = true
              ),
              statistics = None,
              monetizationDetails = YouTubeApiResponse.LiveBroadcastMonetizationDetails(
                cuepointSchedule = YouTubeApiResponse.LiveBroadcastMonetizationDetailsCuepointSchedule(
                  enabled = false,
                  pauseAdsUntil = None,
                  scheduleStrategy = None,
                  repeatIntervalSec = None
                )
              )
            ),
            YouTubeApiResponse.LiveBroadcast(
              kind = "youtube#liveBroadcast",
              etag = "G7m-wjO-8yq1-AtCK2oo28P89u0",
              id = "v-2vABF1e8E",
              snippet = YouTubeApiResponse.LiveBroadcastSnippet(
                publishedAt = OffsetDateTime.of(LocalDateTime.of(2021, 4, 7, 15, 57, 29, 0), ZoneOffset.UTC),
                channelId = "UCj_hcfqnLWGNXtBlwasdOOg",
                title = "Wed 7th Apr: Hearthstone - Battlegrounds, then new standard decks for ranked",
                description = "",
                thumbnails = Map(
                  "default" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/v-2vABF1e8E/default.jpg",
                    width = 120,
                    height = 90
                  ),
                  "medium" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/v-2vABF1e8E/mqdefault.jpg",
                    width = 320,
                    height = 180
                  ),
                  "high" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/v-2vABF1e8E/hqdefault.jpg",
                    width = 480,
                    height = 360
                  ),
                  "standard" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/v-2vABF1e8E/sddefault.jpg",
                    width = 640,
                    height = 480
                  ),
                  "maxres" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/v-2vABF1e8E/maxresdefault.jpg",
                    width = 1280,
                    height = 720
                  )
                ),
                scheduledStartTime = OffsetDateTime.of(LocalDateTime.of(2021, 4, 7, 15, 57, 30, 0), ZoneOffset.UTC),
                scheduledEndTime = None,
                actualStartTime = Some(OffsetDateTime.of(LocalDateTime.of(2021, 4, 7, 15, 57, 48, 0), ZoneOffset.UTC)),
                actualEndTime = Some(OffsetDateTime.of(LocalDateTime.of(2021, 4, 7, 20, 51, 48, 0), ZoneOffset.UTC)),
                isDefaultBroadcast = false,
                liveChatId = "KicKGFVDal9oY2ZxbkxXR05YdEJsd2FzZE9PZxILdi0ydkFCRjFlOEU"
              ),
              status = YouTubeApiResponse.LiveBroadcastStatus(
                lifeCycleStatus = "complete",
                privacyStatus = "public",
                recordingStatus = "recorded",
                madeForKids = false,
                selfDeclaredMadeForKids = false
              ),
              contentDetails = YouTubeApiResponse.LiveBroadcastContentDetails(
                boundStreamId = "j_hcfqnLWGNXtBlwasdOOg1617811050752560",
                boundStreamLastUpdateTimeMs = OffsetDateTime.of(LocalDateTime.of(2022, 9, 19, 12, 13, 22, 0), ZoneOffset.UTC),
                monitorStream = YouTubeApiResponse.LiveBroadcastContentDetailsMonitorStream(
                  enableMonitorStream = true,
                  broadcastStreamDelayMs = 0,
                  embedHtml = "\u003ciframe width=\"425\" height=\"344\" src=\"https://www.youtube.com/embed/v-2vABF1e8E?autoplay=1&livemonitor=1\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" referrerpolicy=\"strict-origin-when-cross-origin\" allowfullscreen\u003e\u003c/iframe\u003e"
                ),
                enableEmbed = true,
                enableDvr = true,
                enableContentEncryption = false,
                recordFromStart = true,
                enableClosedCaptions = false,
                closedCaptionsType = "closedCaptionsDisabled",
                projection = "rectangular",
                enableLowLatency = false,
                latencyPreference = "normal",
                enableAutoStart = true,
                enableAutoStop = true
              ),
              statistics = None,
              monetizationDetails = YouTubeApiResponse.LiveBroadcastMonetizationDetails(
                cuepointSchedule = YouTubeApiResponse.LiveBroadcastMonetizationDetailsCuepointSchedule(
                  enabled = false,
                  pauseAdsUntil = None,
                  scheduleStrategy = None,
                  repeatIntervalSec = None
                )
              )
            ),
            YouTubeApiResponse.LiveBroadcast(
              kind = "youtube#liveBroadcast",
              etag = "hHdpTwhXK9PyaWqYK11B09DiZBI",
              id = "_h_wGKlNnRs",
              snippet = YouTubeApiResponse.LiveBroadcastSnippet(
                publishedAt = OffsetDateTime.of(LocalDateTime.of(2021, 4, 6, 17, 17, 41, 0), ZoneOffset.UTC),
                channelId = "UCj_hcfqnLWGNXtBlwasdOOg",
                title = "Tue 6th Apr: Hearthstone - Battlegrounds, then new standard decks for ranked",
                description = "",
                thumbnails = Map(
                  "default" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/_h_wGKlNnRs/default.jpg",
                    width = 120,
                    height = 90
                  ),
                  "medium" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/_h_wGKlNnRs/mqdefault.jpg",
                    width = 320,
                    height = 180
                  ),
                  "high" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/_h_wGKlNnRs/hqdefault.jpg",
                    width = 480,
                    height = 360
                  ),
                  "standard" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/_h_wGKlNnRs/sddefault.jpg",
                    width = 640,
                    height = 480
                  ),
                  "maxres" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/_h_wGKlNnRs/maxresdefault.jpg",
                    width = 1280,
                    height = 720
                  )
                ),
                scheduledStartTime = OffsetDateTime.of(LocalDateTime.of(2021, 4, 6, 17, 17, 41, 0), ZoneOffset.UTC),
                scheduledEndTime = None,
                actualStartTime = Some(OffsetDateTime.of(LocalDateTime.of(2021, 4, 6, 17, 18, 1, 0), ZoneOffset.UTC)),
                actualEndTime = Some(OffsetDateTime.of(LocalDateTime.of(2021, 4, 6, 21, 37, 17, 0), ZoneOffset.UTC)),
                isDefaultBroadcast = false,
                liveChatId = "KicKGFVDal9oY2ZxbkxXR05YdEJsd2FzZE9PZxILX2hfd0dLbE5uUnM"
              ),
              status = YouTubeApiResponse.LiveBroadcastStatus(
                lifeCycleStatus = "complete",
                privacyStatus = "public",
                recordingStatus = "recorded",
                madeForKids = false,
                selfDeclaredMadeForKids = false
              ),
              contentDetails = YouTubeApiResponse.LiveBroadcastContentDetails(
                boundStreamId = "j_hcfqnLWGNXtBlwasdOOg1617729463472074",
                boundStreamLastUpdateTimeMs = OffsetDateTime.of(LocalDateTime.of(2022, 9, 17, 3, 49, 58, 0), ZoneOffset.UTC),
                monitorStream = YouTubeApiResponse.LiveBroadcastContentDetailsMonitorStream(
                  enableMonitorStream = true,
                  broadcastStreamDelayMs = 0,
                  embedHtml = "\u003ciframe width=\"425\" height=\"344\" src=\"https://www.youtube.com/embed/_h_wGKlNnRs?autoplay=1&livemonitor=1\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" referrerpolicy=\"strict-origin-when-cross-origin\" allowfullscreen\u003e\u003c/iframe\u003e"
                ),
                enableEmbed = true,
                enableDvr = true,
                enableContentEncryption = false,
                recordFromStart = true,
                enableClosedCaptions = false,
                closedCaptionsType = "closedCaptionsDisabled",
                projection = "rectangular",
                enableLowLatency = false,
                latencyPreference = "normal",
                enableAutoStart = true,
                enableAutoStop = true
              ),
              statistics = None,
              monetizationDetails = YouTubeApiResponse.LiveBroadcastMonetizationDetails(
                cuepointSchedule = YouTubeApiResponse.LiveBroadcastMonetizationDetailsCuepointSchedule(
                  enabled = false,
                  pauseAdsUntil = None,
                  scheduleStrategy = None,
                  repeatIntervalSec = None
                )
              )
            ),
            YouTubeApiResponse.LiveBroadcast(
              kind = "youtube#liveBroadcast",
              etag = "hNNlybeUcQU8kTcxv9BYABiOEME",
              id = "3uBmr5PzqLs",
              snippet = YouTubeApiResponse.LiveBroadcastSnippet(
                publishedAt = OffsetDateTime.of(LocalDateTime.of(2021, 4, 5, 16, 0, 36, 0), ZoneOffset.UTC),
                channelId = "UCj_hcfqnLWGNXtBlwasdOOg",
                title = "Mon 5th Apr: Hearthstone - Battlegrounds, then new standard decks for ranked, later maybe Duels",
                description = "",
                thumbnails = Map(
                  "default" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/3uBmr5PzqLs/default.jpg",
                    width = 120,
                    height = 90
                  ),
                  "medium" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/3uBmr5PzqLs/mqdefault.jpg",
                    width = 320,
                    height = 180
                  ),
                  "high" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/3uBmr5PzqLs/hqdefault.jpg",
                    width = 480,
                    height = 360
                  ),
                  "standard" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/3uBmr5PzqLs/sddefault.jpg",
                    width = 640,
                    height = 480
                  ),
                  "maxres" -> YouTubeApiResponse.Thumbnail(
                    url = "https://i.ytimg.com/vi/3uBmr5PzqLs/maxresdefault.jpg",
                    width = 1280,
                    height = 720
                  )
                ),
                scheduledStartTime = OffsetDateTime.of(LocalDateTime.of(2021, 4, 5, 16, 0, 34, 0), ZoneOffset.UTC),
                scheduledEndTime = None,
                actualStartTime = Some(OffsetDateTime.of(LocalDateTime.of(2021, 4, 5, 16, 0, 57, 0), ZoneOffset.UTC)),
                actualEndTime = Some(OffsetDateTime.of(LocalDateTime.of(2021, 4, 5, 21, 40, 43, 0), ZoneOffset.UTC)),
                isDefaultBroadcast = false,
                liveChatId = "KicKGFVDal9oY2ZxbkxXR05YdEJsd2FzZE9PZxILM3VCbXI1UHpxTHM"
              ),
              status = YouTubeApiResponse.LiveBroadcastStatus(
                lifeCycleStatus = "complete",
                privacyStatus = "public",
                recordingStatus = "recorded",
                madeForKids = false,
                selfDeclaredMadeForKids = false
              ),
              contentDetails = YouTubeApiResponse.LiveBroadcastContentDetails(
                boundStreamId = "j_hcfqnLWGNXtBlwasdOOg1617638437551575",
                boundStreamLastUpdateTimeMs = OffsetDateTime.of(LocalDateTime.of(2022, 9, 19, 16, 59, 32, 0), ZoneOffset.UTC),
                monitorStream = YouTubeApiResponse.LiveBroadcastContentDetailsMonitorStream(
                  enableMonitorStream = true,
                  broadcastStreamDelayMs = 0,
                  embedHtml = "\u003ciframe width=\"425\" height=\"344\" src=\"https://www.youtube.com/embed/3uBmr5PzqLs?autoplay=1&livemonitor=1\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" referrerpolicy=\"strict-origin-when-cross-origin\" allowfullscreen\u003e\u003c/iframe\u003e"
                ),
                enableEmbed = true,
                enableDvr = true,
                enableContentEncryption = false,
                recordFromStart = true,
                enableClosedCaptions = false,
                closedCaptionsType = "closedCaptionsDisabled",
                projection = "rectangular",
                enableLowLatency = false,
                latencyPreference = "normal",
                enableAutoStart = true,
                enableAutoStop = true
              ),
              statistics = None,
              monetizationDetails = YouTubeApiResponse.LiveBroadcastMonetizationDetails(
                cuepointSchedule = YouTubeApiResponse.LiveBroadcastMonetizationDetailsCuepointSchedule(
                  enabled = false,
                  pauseAdsUntil = None,
                  scheduleStrategy = None,
                  repeatIntervalSec = None
                )
              )
            )
          )
        )
        decode[YouTubeApiResponse.Response[YouTubeApiResponse.LiveBroadcast]](json) shouldEqual Right(expectedResponse)
      }
    }
  }
}
