package com.archimond7450.archiemate.kick.webhooks

import com.archimond7450.archiemate.kick.webhooks
import io.circe.jawn.decode
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}
import scala.io.Source

class KickWebhooksSpec extends AnyWordSpecLike with Matchers {
  val TEST_JSON_DIRECTORY = "kick/webhooks"

  def readFile(resourcePath: String): String = {
    val source = Source.fromResource(resourcePath)
    try source.getLines().mkString("\n")
    finally source.close()
  }

  "KickWebhooks" when {
    "ChatMessageSentV1 is received" should {
      "be correctly decoded" in {
        val json =
          readFile(s"$TEST_JSON_DIRECTORY/chat.message.sent.v1.json")
        val expected = KickWebhooks.ChatMessageSentV1(
          messageId = "unique_message_id_123",
          repliesTo = Some(
            KickWebhooks.ChatMessageSentRepliesTo(
              messageId = "unique_message_id_456",
              content = "This is the parent message!",
              sender = KickWebhooks.User(
                isAnonymous = false,
                userId = 12345,
                username = "parent_sender_name",
                isVerified = Some(false),
                profilePicture = "https://example.com/parent_sender_avatar.jpg",
                channelSlug = "parent_sender_channel",
                identity = None
              )
            )
          ),
          broadcaster = KickWebhooks.User(
            isAnonymous = false,
            userId = 123456789,
            username = "broadcaster_name",
            isVerified = Some(true),
            profilePicture = "https://example.com/broadcaster_avatar.jpg",
            channelSlug = "broadcaster_channel",
            identity = None
          ),
          sender = KickWebhooks.User(
            isAnonymous = false,
            userId = 987654321,
            username = "sender_name",
            isVerified = Some(false),
            profilePicture = "https://example.com/sender_avatar.jpg",
            channelSlug = "sender_channel",
            identity = Some(
              KickWebhooks.ChatMessageIdentity(
                usernameColor = "#FF5733",
                badges = List(
                  KickWebhooks
                    .UserBadge(text = "Moderator", `type` = "moderator"),
                  KickWebhooks.UserBadge(
                    text = "Sub Gifter",
                    `type` = "sub_gifter",
                    count = Some(5)
                  ),
                  KickWebhooks.UserBadge(
                    text = "Subscriber",
                    `type` = "subscriber",
                    count = Some(3)
                  )
                )
              )
            )
          ),
          content =
            "Hello [emote:4148074:HYPERCLAP] [emote:4148074:HYPERCLAP] [emote:37226:KEKW]",
          emotes = List(
            KickWebhooks.ChatMessageEmote(
              emoteId = "4148074",
              positions = List(
                KickWebhooks.ChatMessageEmotePosition(s = 6, e = 30),
                KickWebhooks.ChatMessageEmotePosition(s = 32, e = 56)
              )
            ),
            KickWebhooks.ChatMessageEmote(
              emoteId = "37226",
              positions = List(
                KickWebhooks.ChatMessageEmotePosition(s = 58, e = 75)
              )
            )
          ),
          createdAt = OffsetDateTime.of(
            LocalDateTime.of(2025, 1, 14, 16, 8, 6, 0),
            ZoneOffset.UTC
          )
        )
        KickWebhooks.KickWebhook.decodeJson(
          "chat.message.sent",
          "1",
          json
        ) shouldEqual Right(expected)
      }
    }

    "ChannelFollowedV1 is received" should {
      "be correctly decoded" in {
        val json =
          readFile(s"$TEST_JSON_DIRECTORY/channel.followed.v1.json")
        val expected =
          KickWebhooks.ChannelFollowedV1(
            broadcaster = KickWebhooks.User(
              isAnonymous = false,
              userId = 123456789,
              username = "broadcaster_name",
              isVerified = Some(true),
              profilePicture = "https://example.com/broadcaster_avatar.jpg",
              channelSlug = "broadcaster_channel",
              identity = None
            ),
            follower = KickWebhooks.User(
              isAnonymous = false,
              userId = 987654321,
              username = "follower_name",
              isVerified = Some(false),
              profilePicture = "https://example.com/sender_avatar.jpg",
              channelSlug = "follower_channel",
              identity = None
            )
          )
        KickWebhooks.KickWebhook.decodeJson(
          "channel.followed",
          "1",
          json
        ) shouldEqual Right(expected)
      }
    }

    "LivestreamStatusUpdatedV1 is received" should {
      "correctly decode stream started" in {
        val json = readFile(
          s"$TEST_JSON_DIRECTORY/livestream.status.updated.v1.stream.started.json"
        )
        val expected = KickWebhooks.LivestreamStatusUpdatedV1(
          broadcaster = KickWebhooks.User(
            isAnonymous = false,
            userId = 123456789,
            username = "broadcaster_name",
            isVerified = Some(true),
            profilePicture = "https://example.com/broadcaster_avatar.jpg",
            channelSlug = "broadcaster_channel",
            identity = None
          ),
          isLive = true,
          title = "Stream Title",
          startedAt = OffsetDateTime.of(
            LocalDateTime.of(2025, 1, 1, 11, 0, 0, 0),
            ZoneOffset.ofHours(11)
          ),
          endedAt = None
        )
        KickWebhooks.KickWebhook.decodeJson(
          "livestream.status.updated",
          "1",
          json
        ) shouldEqual Right(expected)
      }

      "correctly decode stream ended" in {
        val json = readFile(
          s"$TEST_JSON_DIRECTORY/livestream.status.updated.v1.stream.ended.json"
        )
        val expected = KickWebhooks.LivestreamStatusUpdatedV1(
          broadcaster = KickWebhooks.User(
            isAnonymous = false,
            userId = 123456789,
            username = "broadcaster_name",
            isVerified = Some(true),
            profilePicture = "https://example.com/broadcaster_avatar.jpg",
            channelSlug = "broadcaster_channel",
            identity = None
          ),
          isLive = false,
          title = "Stream Title",
          startedAt = OffsetDateTime.of(
            LocalDateTime.of(2025, 1, 1, 11, 0, 0, 0),
            ZoneOffset.ofHours(11)
          ),
          endedAt = Some(
            OffsetDateTime.of(
              LocalDateTime.of(2025, 1, 1, 15, 0, 0, 0),
              ZoneOffset.ofHours(11)
            )
          )
        )
        KickWebhooks.KickWebhook.decodeJson(
          "livestream.status.updated",
          "1",
          json
        ) shouldEqual Right(expected)
      }
    }
  }
}
