package com.archimond7450.archiemate.twitch.eventsub
import io.circe.parser._
import io.circe.syntax.EncoderOps
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}
import scala.io.Source
import scala.language.postfixOps


class IncomingMessageSpec extends AnyWordSpecLike with Matchers {
  //val TEST_JSON_DIRECTORY = "src/test/resources/twitch/eventsub"
  val TEST_JSON_DIRECTORY = "twitch/eventsub"

  def readFile(resourcePath: String): String = {
    val source = Source.fromResource(resourcePath)
    try source.getLines().mkString("\n") finally source.close()
  }

  "IncomingMessage decoder" when {
    "Welcome message is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/welcome.json")
        val expectedMessage = IncomingMessage(
          metadata = Metadata(
            messageId = "96a3f3b5-5dec-4eed-908e-e11ee657416c",
            messageType = "session_welcome",
            messageTimestamp = OffsetDateTime.of(LocalDateTime.of(2023, 7, 19, 14, 56, 51, 634234626), ZoneOffset.UTC)
          ),
          payload = Payload(
            session = Some(Session(
              id = "AQoQILE98gtqShGmLD7AM6yJThAB",
              status = "connected",
              keepaliveTimeoutSeconds = Some(10),
              reconnectUrl = None,
              connectedAt = OffsetDateTime.of(LocalDateTime.of(2023, 7, 19, 14, 56, 51, 616329898), ZoneOffset.UTC)
            ))
          )
        )
        decode[IncomingMessage](json) shouldEqual Right(expectedMessage)
      }
    }

    "Keepalive message is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/keepalive.json")
        val expectedMessage = IncomingMessage(
          metadata = Metadata(
            messageId = "84c1e79a-2a4b-4c13-ba0b-4312293e9308",
            messageType = "session_keepalive",
            messageTimestamp = OffsetDateTime.of(LocalDateTime.of(2023, 7, 19, 10, 11, 12, 634234626), ZoneOffset.UTC),
          ),
          payload = Payload()
        )
        decode[IncomingMessage](json) shouldEqual Right(expectedMessage)
      }
    }

    "Notification message is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification.json")
        val expectedMessage = IncomingMessage(
          metadata = Metadata(
            messageId = "befa7b53-d79d-478f-86b9-120f112b044e",
            messageType = "notification",
            messageTimestamp = OffsetDateTime.of(LocalDateTime.of(2022, 11, 16, 10, 11, 12, 464757833), ZoneOffset.UTC),
            subscriptionType = Some("channel.follow"),
            subscriptionVersion = Some("1")
          ),
          payload = Payload(
            subscription = Some(Subscription(
              id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
              status = "enabled",
              `type` = "channel.follow",
              version = "1",
              cost = 1,
              condition = Condition(broadcasterUserId = Some("12826")),
              transport = Transport(method = "websocket", session = Some("AQoQexAWVYKSTIu4ec_2VAxyuhAB")),
              createdAt = OffsetDateTime.of(LocalDateTime.of(2022, 11, 16, 10, 11, 12, 464757833), ZoneOffset.UTC)
            )),
            event = Some(ChannelFollowEvent(
              userId = "1337",
              userLogin = "awesome_user",
              userName = "Awesome_User",
              broadcasterUserId = "12826",
              broadcasterUserLogin = "twitch",
              broadcasterUserName = "Twitch",
              followedAt = OffsetDateTime.of(LocalDateTime.of(2023, 7, 15, 18, 16, 11, 171067130), ZoneOffset.UTC)
            ))
          )
        )
        decode[IncomingMessage](json) shouldEqual Right(expectedMessage)
      }
    }

    "Reconnect message is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/reconnect.json")
        val expectedMessage = IncomingMessage(
          metadata = Metadata(
            messageId = "84c1e79a-2a4b-4c13-ba0b-4312293e9308",
            messageType = "session_reconnect",
            messageTimestamp = OffsetDateTime.of(LocalDateTime.of(2022, 11, 18, 9, 10, 11, 634234626), ZoneOffset.UTC),
          ),
          payload = Payload(
            session = Some(Session(
              id = "AQoQexAWVYKSTIu4ec_2VAxyuhAB",
              status = "reconnecting",
              keepaliveTimeoutSeconds = None,
              reconnectUrl = Some("wss://eventsub.wss.twitch.tv?..."),
              connectedAt = OffsetDateTime.of(LocalDateTime.of(2022, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
            ))
          )
        )
        decode[IncomingMessage](json) shouldEqual Right(expectedMessage)
      }
    }

    "Revocation message is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/revocation.json")
        val expectedMessage = IncomingMessage(
          metadata = Metadata(
            messageId = "84c1e79a-2a4b-4c13-ba0b-4312293e9308",
            messageType = "revocation",
            messageTimestamp = OffsetDateTime.of(LocalDateTime.of(2022, 11, 16, 10, 11, 12, 464757833), ZoneOffset.UTC),
            subscriptionType = Some("channel.follow"),
            subscriptionVersion = Some("1")
          ),
          payload = Payload(
            subscription = Some(Subscription(
              id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
              status = "authorization_revoked",
              `type` = "channel.follow",
              version = "1",
              cost = 1,
              condition = Condition(broadcasterUserId = Some("12826")),
              transport = Transport(method = "websocket", session = Some("AQoQexAWVYKSTIu4ec_2VAxyuhAB")),
              createdAt = OffsetDateTime.of(LocalDateTime.of(2022, 11, 16, 10, 11, 12, 464757833), ZoneOffset.UTC)
            ))
          )
        )
        decode[IncomingMessage](json) shouldEqual Right(expectedMessage)
      }
    }
  }

  "Notification payloads decoder" when {
    "Automod Message Hold Notification is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/automod.message.hold.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "automod.message.hold",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337"), moderatorUserId = Some("9001")),
            transport = Transport(method = "websocket", session = Some("AQoQILE98gtqShGmLD7AM6yJThAB")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2023, 4, 11, 10, 11, 12, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(AutomodMessageHoldEvent(
            broadcasterUserId = "1337",
            broadcasterUserLogin = "blahblah",
            broadcasterUserName = "blah",
            userId = "456789012",
            userLogin = "baduserbla",
            userName = "baduser",
            messageId = "bad-message-id",
            message = "This is a bad message… ",
            category = "aggressive",
            level = 5,
            heldAt = OffsetDateTime.of(LocalDateTime.of(2022, 12, 2, 15, 0, 0, 0), ZoneOffset.UTC),
            fragments = MessageFragments(
              emotes = List(
                EmoteFragment(text = "badtextemote1", id = "emote-123", `set-id` = "set-emote-1"),
                EmoteFragment(text = "badtextemote2", id = "emote-234", `set-id` = "set-emote-2")
              ),
              cheermotes = List(
                CheermoteFragment(text = "badtextcheermote1", amount = 1000, prefix = "prefix", tier = 1)
              )
            )
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Automod Message Update Notification is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/automod.message.update.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "automod.message.update",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337"), moderatorUserId = Some("9001")),
            transport = Transport(method = "websocket", session = Some("AQoQILE98gtqShGmLD7AM6yJThAB")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2023, 4, 11, 10, 11, 12, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(AutomodMessageUpdateEvent(
            broadcasterUserId = "1337",
            broadcasterUserLogin = "blahblah",
            broadcasterUserName = "blah",
            userId = "456789012",
            userLogin = "baduserbla",
            userName = "baduser",
            moderatorUserId = "9001",
            moderatorUserLogin = "the_mod",
            moderatorUserName = "The_Mod",
            messageId = "bad-message-id",
            message = "This is a bad message… ",
            category = "aggressive",
            level = 5,
            status = "approved",
            heldAt = OffsetDateTime.of(LocalDateTime.of(2022, 12, 2, 15, 0, 0, 0), ZoneOffset.UTC),
            fragments = MessageFragments(
              emotes = List(
                EmoteFragment(text = "badtextemote1", id = "emote-123", `set-id` = "set-emote-1"),
                EmoteFragment(text = "badtextemote2", id = "emote-234", `set-id` = "set-emote-2")
              ),
              cheermotes = List(
                CheermoteFragment(text = "badtextcheermote1", amount = 1000, prefix = "prefix", tier = 1)
              )
            )
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Automod Settings Update Notification is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/automod.settings.update.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "automod.settings.update",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337"), moderatorUserId = Some("9001")),
            transport = Transport(method = "websocket", session = Some("AQoQILE98gtqShGmLD7AM6yJThAB")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2023, 4, 11, 10, 11, 12, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(AutomodSettingsUpdateEvent(
            data = List(
              AutomodSettingsData(
                broadcasterUserId = "1337",
                broadcasterUserLogin = "cooluser",
                broadcasterUserName = "CoolUser",
                moderatorUserId = "9001",
                moderatorUserLogin = "coolmod",
                moderatorUserName = "CoolMod",
                bullying = 3,
                overallLevel = None,
                disability = 3,
                raceEthnicityOrReligion = 3,
                misogyny = 3,
                sexualitySexOrGender = 3,
                aggression = 3,
                sexBasedTerms = 30,
                swearing = 0)
            )
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Automod Terms Update Notification is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/automod.terms.update.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "automod.terms.update",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337"), moderatorUserId = Some("9001")),
            transport = Transport(method = "websocket", session = Some("AQoQILE98gtqShGmLD7AM6yJThAB")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2023, 4, 11, 10, 11, 12, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(AutomodTermsUpdateEvent(
            broadcasterUserId = "1337",
            broadcasterUserLogin = "blahblah",
            broadcasterUserName = "blah",
            moderatorUserId = "9001",
            moderatorUserLogin = "the_mod",
            moderatorUserName = "The_Mod",
            action = "bad-message-id",
            fromAutomod = true,
            terms = List("automodterm1", "automodterm2", "automodterm3")
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Update Notification is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.update.v2.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.update",
            version = "2",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AQoQILE98gtqShGmLD7AM6yJThAB")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2023, 6, 29, 17, 20, 33, 860897266), ZoneOffset.UTC)
          )),
          event = Some(ChannelUpdateEvent(
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            title = "Best Stream Ever",
            language = "en",
            categoryId = "12453",
            categoryName = "Grand Theft Auto",
            contentClassificationLabels = List("MatureGame")
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Follow Notification is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.follow.v2.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.follow",
            version = "2",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337"), moderatorUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AQoQILE98gtqShGmLD7AM6yJThAB")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelFollowEvent(
            userId = "1234",
            userLogin = "cool_user",
            userName = "Cool_User",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cooler_user",
            broadcasterUserName = "Cooler_User",
            followedAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 18, 16, 11, 171067130), ZoneOffset.UTC)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Ad Break Begin Notification is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.ad_break.begin.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.ad_break.begin",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AQoQILE98gtqShGmLD7AM6yJThAB")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelAdBreakBeginEvent(
            durationSeconds = 60,
            startedAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC),
            isAutomatic = false,
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            requesterUserId = "1337",
            requesterUserLogin = "cool_user",
            requesterUserName = "Cool_User"
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Chat Clear Notification is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.chat.clear.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.chat.clear",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337"), userId = Some("9001")),
            transport = Transport(method = "websocket", session = Some("AQoQILE98gtqShGmLD7AM6yJThAB")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2023, 4, 11, 10, 11, 12, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(ChannelChatClearEvent(
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User"
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Chat Clear User Messages Notification is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.chat.clear_user_messages.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.chat.clear_user_messages",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337"), userId = Some("9001")),
            transport = Transport(method = "websocket", session = Some("AQoQILE98gtqShGmLD7AM6yJThAB")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2023, 4, 11, 10, 11, 12, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(ChannelChatClearUserMessagesEvent(
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            targetUserId = "7734",
            targetUserLogin = "uncool_viewer",
            targetUserName = "Uncool_viewer"
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Chat Message Notification is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.chat.message.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "0b7f3361-672b-4d39-b307-dd5b576c9b27",
            status = "enabled",
            `type` = "channel.chat.message",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1971641"), userId = Some("2914196")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2023, 11, 6, 18, 11, 47, 492253549), ZoneOffset.UTC)
          )),
          event = Some(ChannelChatMessageEvent(
            broadcasterUserId = "1971641",
            broadcasterUserLogin = "streamer",
            broadcasterUserName = "streamer",
            chatterUserId = "4145994",
            chatterUserLogin = "viewer32",
            chatterUserName = "viewer32",
            messageId = "cc106a89-1814-919d-454c-f4f2f970aae7",
            message = ChatMessage(
              text = "Hi chat",
              fragments = List(
                ChatMessageFragment(
                  `type` = "text",
                  text = "Hi chat",
                  cheermote = None,
                  emote = None,
                  mention = None)
              )
            ),
            message_type = "text",
            badges = List(
              Badge(setId = "moderator", id = "1", info = ""),
              Badge(setId = "subscriber", id = "12", info = "16"),
              Badge(setId = "sub-gifter", id = "1", info = "")
            ),
            cheer = None,
            color = "#00FF7F",
            reply = None,
            channel_points_custom_reward_id = None,
            channel_points_animation_id = None
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Chat Message Delete is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.chat.message_delete.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.chat.message_delete",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337"), userId = Some("9001")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2023, 4, 11, 10, 11, 12, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(ChannelChatMessageDeleteEvent(
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            targetUserId = "7734",
            targetUserLogin = "uncool_viewer",
            targetUserName = "Uncool_viewer",
            messageId = "ab24e0b0-2260-4bac-94e4-05eedd4ecd0e"
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Chat Notification is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.chat.notification.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.chat.notification",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337"), userId = Some("9001")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2023, 4, 11, 10, 11, 12, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(ChannelChatNotificationEvent(
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            chatterUserId = "444",
            chatterUserLogin = "cool_chatter",
            chatterUserName = "Cool_Chatter",
            chatterIsAnonymous = false,
            color = "red",
            badges = List(
              Badge(setId = "moderator", id = "1", info = ""),
              Badge(setId = "subscriber", id = "12", info = "16"),
              Badge(setId = "sub-gifter", id = "1", info = "")
            ),
            systemMessage = "chat message",
            messageId = "ab24e0b0-2260-4bac-94e4-05eedd4ecd0e",
            message = ChatMessage(
              text = "chat-msg",
              fragments = List(
                ChatMessageFragment(
                  `type` = "emote",
                  text = "chat-msg",
                  cheermote = None,
                  emote = Some(ChatMessageEmote(
                    id = "emote-id",
                    emoteSetId = "emote-set",
                    ownerId = Some("emote-owner"),
                    format = Some(List("static"))
                  )),
                  mention = None)
              )
            ),
            noticeType = "announcement",
            sub = None,
            resub = None,
            subGift = None,
            communitySubGift = None,
            giftPaidUpgrade = None,
            primePaidUpgrade = None,
            raid = None,
            unraid = None,
            payItForward = None,
            announcement = Some(NoticeAnnouncement(color = "blue")),
            charityDonation = None,
            bitsBadgeTier = None
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Chat Settings Update is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.chat_settings.update.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.chat_settings.update",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337"), userId = Some("9001")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2023, 4, 11, 10, 11, 12, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(ChannelChatSettingsUpdateEvent(
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            emoteMode = true,
            followerMode = false,
            followerModeDurationMinutes = None,
            slowMode = true,
            slowModeWaitTimeSeconds = Some(10),
            subscriberMode = false,
            uniqueChatMode = false
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Chat User Message Hold is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.chat.user_message_hold.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.chat.user_message_hold",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337"), userId = Some("9001")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2023, 4, 11, 10, 11, 12, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(ChannelChatUserMessageHoldEvent(
            broadcasterUserId = "123",
            broadcasterUserLogin = "bob",
            broadcasterUserName = "Bob",
            userId = "456",
            userLogin = "tom",
            userName = "Tommy",
            messageId = "789",
            message = ChatMessage(
              text = "hey world",
              fragments = List(
                ChatMessageFragment(
                  `type` = "emote",
                  text = "hey world",
                  cheermote = None,
                  emote = Some(ChatMessageEmote(id = "foo", emoteSetId = "7")),
                  mention = None
                ),
                ChatMessageFragment(
                  `type` = "cheermote",
                  text = "bye world",
                  cheermote = Some(ChatMessageCheermote(prefix = "prefix", bits = 100, tier = 1)),
                  emote = None,
                  mention = None
                ),
                ChatMessageFragment(
                  `type` = "text",
                  text = "surprise",
                  cheermote = None,
                  emote = None,
                  mention = None
                )
              )
            )
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Chat User Message Update is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.chat.user_message_update.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.chat.user_message_update",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337"), userId = Some("9001")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2023, 4, 11, 10, 11, 12, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(ChannelChatUserMessageUpdateEvent(
            broadcasterUserId = "123",
            broadcasterUserLogin = "bob",
            broadcasterUserName = "Bob",
            userId = "456",
            userLogin = "tom",
            userName = "Tommy",
            status = "approved",
            messageId = "789",
            message = ChatMessage(
              text = "hey world",
              fragments = List(
                ChatMessageFragment(
                  `type` = "emote",
                  text = "hey world",
                  cheermote = None,
                  emote = Some(ChatMessageEmote(id = "foo", emoteSetId = "7")),
                  mention = None
                ),
                ChatMessageFragment(
                  `type` = "cheermote",
                  text = "bye world",
                  cheermote = Some(ChatMessageCheermote(prefix = "prefix", bits = 100, tier = 1)),
                  emote = None,
                  mention = None
                ),
                ChatMessageFragment(
                  `type` = "text",
                  text = "surprise",
                  cheermote = None,
                  emote = None,
                  mention = None
                )
              )
            )
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Subscribe is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.subscribe.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.subscribe",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelSubscribeEvent(
            userId = "1234",
            userLogin = "cool_user",
            userName = "Cool_User",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cooler_user",
            broadcasterUserName = "Cooler_User",
            tier = "1000",
            isGift = false
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Subscription End is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.subscription.end.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.subscription.end",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelSubscriptionEndEvent(
            userId = "1234",
            userLogin = "cool_user",
            userName = "Cool_User",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cooler_user",
            broadcasterUserName = "Cooler_User",
            tier = "1000",
            isGift = false
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Subscription Gift is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.subscription.gift.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.subscription.gift",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelSubscriptionGiftEvent(
            userId = Some("1234"),
            userLogin = Some("cool_user"),
            userName = Some("Cool_User"),
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cooler_user",
            broadcasterUserName = "Cooler_User",
            total = 2,
            tier = "1000",
            cumulativeTotal = Some(284),
            isAnonymous = false
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Subscription Message is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.subscription.message.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.subscription.message",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelSubscriptionMessageEvent(
            userId = "1234",
            userLogin = "cool_user",
            userName = "Cool_User",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cooler_user",
            broadcasterUserName = "Cooler_User",
            tier = "1000",
            message = Message(
              text = "Love the stream! FevziGG",
              emotes = List(
                EmotePosition(begin = 23, end = 30, id = "302976485")
              )
            ),
            cumulativeMonths = 15,
            streakMonths = Some(1),
            durationMonths = 6
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Cheer is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.cheer.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.cheer",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelCheerEvent(
            isAnonymous = false,
            userId = Some("1234"),
            userLogin = Some("cool_user"),
            userName = Some("Cool_User"),
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cooler_user",
            broadcasterUserName = "Cooler_User",
            message = "pogchamp",
            bits = 1000
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Raid is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.raid.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.raid",
            version = "1",
            cost = 0,
            condition = Condition(toBroadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelRaidEvent(
            fromBroadcasterUserId = "1234",
            fromBroadcasterUserLogin = "cool_user",
            fromBroadcasterUserName = "Cool_User",
            toBroadcasterUserId = "1337",
            toBroadcasterUserLogin = "cooler_user",
            toBroadcasterUserName = "Cooler_User",
            viewers = 9001
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Ban is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.ban.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.ban",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelBanEvent(
            userId = "1234",
            userLogin = "cool_user",
            userName = "Cool_User",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cooler_user",
            broadcasterUserName = "Cooler_User",
            moderatorUserId = "1339",
            moderatorUserLogin = "mod_user",
            moderatorUserName = "Mod_User",
            reason = "Offensive language",
            bannedAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 18, 15, 11, 171067130), ZoneOffset.UTC),
            endsAt = Some(OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 18, 16, 11, 171067130), ZoneOffset.UTC)),
            isPermanent = false
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Unban is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.unban.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.unban",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelUnbanEvent(
            userId = "1234",
            userLogin = "cool_user",
            userName = "Cool_User",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cooler_user",
            broadcasterUserName = "Cooler_User",
            moderatorUserId = "1339",
            moderatorUserLogin = "mod_user",
            moderatorUserName = "Mod_User"
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Unban Request Create is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.unban_request.create.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.unban_request.create",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337"), moderatorUserId = Some("1338")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelUnbanRequestCreateEvent(
            id = "60",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            userId = "1339",
            userLogin = "not_cool_user",
            userName = "Not_Cool_User",
            text = "unban me",
            createdAt = OffsetDateTime.of(LocalDateTime.of(2023, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Unban Request Resolve is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.unban_request.resolve.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.unban_request.resolve",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337"), moderatorUserId = Some("1338")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelUnbanRequestResolveEvent(
            id = "60",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            moderatorUserId = Some("1337"),
            moderatorUserLogin = Some("cool_user"),
            moderatorUserName = Some("Cool_User"),
            userId = "1339",
            userLogin = "not_cool_user",
            userName = "Not_Cool_User",
            resolutionText = Some("no"),
            status = "denied"
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Moderate is received" should {
      "be correctly decoded when adding a moderator" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.moderate.v1/adding_moderator.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "7297f7eb-3bf5-461f-8ae6-7cd7781ebce3",
            status = "enabled",
            `type` = "channel.moderate",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2024, 2, 23, 21, 12, 33, 771005262), ZoneOffset.UTC)
          )),
          event = Some(ChannelModerateEvent(
            broadcasterUserId = "1337",
            broadcasterUserLogin = "glowillig",
            broadcasterUserName = "glowillig",
            moderatorUserId = "424596340",
            moderatorUserLogin = "quotrok",
            moderatorUserName = "quotrok",
            action = "mod",
            mod = Some(Mod(userId = "141981764", "twitchdev", "TwitchDev"))
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }

      "be correctly decoded when timing out a user" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.moderate.v1/timing_out.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "7297f7eb-3bf5-461f-8ae6-7cd7781ebce3",
            status = "enabled",
            `type` = "channel.moderate",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2024, 2, 23, 21, 12, 33, 771005262), ZoneOffset.UTC)
          )),
          event = Some(ChannelModerateEvent(
            broadcasterUserId = "1337",
            broadcasterUserLogin = "glowillig",
            broadcasterUserName = "glowillig",
            moderatorUserId = "424596340",
            moderatorUserLogin = "quotrok",
            moderatorUserName = "quotrok",
            action = "timeout",
            timeout = Some(Timeout(
              userId = "141981764",
              userLogin = "twitchdev",
              userName = "TwitchDev",
              reason = Some("Does not like pineapple on pizza."),
              expiresAt = OffsetDateTime.of(LocalDateTime.of(2022, 3, 15, 2, 0, 28), ZoneOffset.UTC)))
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }

      "be correctly decoded when emote only mode is used" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.moderate.v1/emote_only.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "7297f7eb-3bf5-461f-8ae6-7cd7781ebce3",
            status = "enabled",
            `type` = "channel.moderate",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2024, 2, 23, 21, 12, 33, 771005262), ZoneOffset.UTC)
          )),
          event = Some(ChannelModerateEvent(
            broadcasterUserId = "1337",
            broadcasterUserLogin = "glowillig",
            broadcasterUserName = "glowillig",
            moderatorUserId = "424596340",
            moderatorUserLogin = "quotrok",
            moderatorUserName = "quotrok",
            action = "emoteonly"
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Moderate v2 is received" should {
      "be correctly decoded when issuing a warning" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.moderate.v2/issuing_warning.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "7297f7eb-3bf5-461f-8ae6-7cd7781ebce3",
            status = "enabled",
            `type` = "channel.moderate",
            version = "2",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("423374343"), moderatorUserId = Some("424596340")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2024, 2, 23, 21, 12, 33, 771005262), ZoneOffset.UTC)
          )),
          event = Some(ChannelModerateV2Event(
            broadcasterUserId = "423374343",
            broadcasterUserLogin = "glowillig",
            broadcasterUserName = "glowillig",
            moderatorUserId = "424596340",
            moderatorUserLogin = "quotrok",
            moderatorUserName = "quotrok",
            action = "warn",
            warn = Some(Warn(
              userId = "141981764",
              userLogin = "twitchdev",
              userName = "TwitchDev",
              reason = Some("cut it out"),
              chatRulesCited = None
            ))
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }

      "be correctly decoded when adding a moderator" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.moderate.v2/adding_moderator.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "7297f7eb-3bf5-461f-8ae6-7cd7781ebce3",
            status = "enabled",
            `type` = "channel.moderate",
            version = "2",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("423374343")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2024, 2, 23, 21, 12, 33, 771005262), ZoneOffset.UTC)
          )),
          event = Some(ChannelModerateV2Event(
            broadcasterUserId = "423374343",
            broadcasterUserLogin = "glowillig",
            broadcasterUserName = "glowillig",
            moderatorUserId = "424596340",
            moderatorUserLogin = "quotrok",
            moderatorUserName = "quotrok",
            action = "mod",
            mod = Some(Mod(userId = "141981764", "twitchdev", "TwitchDev"))
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }

      "be correctly decoded when timing out a user" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.moderate.v2/timing_out.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "7297f7eb-3bf5-461f-8ae6-7cd7781ebce3",
            status = "enabled",
            `type` = "channel.moderate",
            version = "2",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("423374343")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2024, 2, 23, 21, 12, 33, 771005262), ZoneOffset.UTC)
          )),
          event = Some(ChannelModerateV2Event(
            broadcasterUserId = "423374343",
            broadcasterUserLogin = "glowillig",
            broadcasterUserName = "glowillig",
            moderatorUserId = "424596340",
            moderatorUserLogin = "quotrok",
            moderatorUserName = "quotrok",
            action = "timeout",
            timeout = Some(Timeout(
              userId = "141981764",
              userLogin = "twitchdev",
              userName = "TwitchDev",
              reason = Some("Does not like pineapple on pizza."),
              expiresAt = OffsetDateTime.of(LocalDateTime.of(2022, 3, 15, 2, 0, 28), ZoneOffset.UTC)))
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }

      "be correctly decoded when emote only mode is used" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.moderate.v2/emote_only.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "7297f7eb-3bf5-461f-8ae6-7cd7781ebce3",
            status = "enabled",
            `type` = "channel.moderate",
            version = "2",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("423374343")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2024, 2, 23, 21, 12, 33, 771005262), ZoneOffset.UTC)
          )),
          event = Some(ChannelModerateV2Event(
            broadcasterUserId = "423374343",
            broadcasterUserLogin = "glowillig",
            broadcasterUserName = "glowillig",
            moderatorUserId = "424596340",
            moderatorUserLogin = "quotrok",
            moderatorUserName = "quotrok",
            action = "emoteonly"
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Moderator Add is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.moderator.add.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.moderator.add",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelModeratorAddEvent(
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cooler_user",
            broadcasterUserName = "Cooler_User",
            userId = "1234",
            userLogin = "mod_user",
            userName = "Mod_User"
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Moderator Remove is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.moderator.remove.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.moderator.remove",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelModeratorRemoveEvent(
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cooler_user",
            broadcasterUserName = "Cooler_User",
            userId = "1234",
            userLogin = "not_mod_user",
            userName = "Not_Mod_User"
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Guest Star Session Begin is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.guest_star_session.begin.vbeta.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.guest_star_session.begin",
            version = "beta",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337"), moderatorUserId = Some("1338")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2023, 4, 11, 10, 11, 12, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(ChannelGuestStarSessionBeginEvent(
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            moderatorUserId = Some("1338"),
            moderatorUserLogin = Some("cool_mod"),
            moderatorUserName = Some("Cool_Mod"),
            sessionId = "2KFRQbFtpmfyD3IevNRnCzOPRJI",
            startedAt = OffsetDateTime.of(LocalDateTime.of(2023, 4, 11, 16, 20, 3, 171067130), ZoneOffset.UTC)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Guest Star Session End is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.guest_star_session.end.vbeta.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.guest_star_session.end",
            version = "beta",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337"), moderatorUserId = Some("1338")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2023, 4, 11, 10, 11, 22, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(ChannelGuestStarSessionEndEvent(
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            moderatorUserId = Some("1338"),
            moderatorUserLogin = Some("cool_mod"),
            moderatorUserName = Some("Cool_Mod"),
            sessionId = "2KFRQbFtpmfyD3IevNRnCzOPRJI",
            startedAt = OffsetDateTime.of(LocalDateTime.of(2023, 4, 11, 16, 20, 3, 171067130), ZoneOffset.UTC),
            endedAt = OffsetDateTime.of(LocalDateTime.of(2023, 4, 11, 17, 51, 29, 153485 * 1000), ZoneOffset.UTC)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Guest Star Guest Update is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.guest_star_guest.update.vbeta.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.guest_star_guest.update",
            version = "beta",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337"), moderatorUserId = Some("1312")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2023, 4, 11, 10, 11, 32, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(ChannelGuestStarGuestUpdateEvent(
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            sessionId = "2KFRQbFtpmfyD3IevNRnCzOPRJI",
            moderatorUserId = Some("1312"),
            moderatorUserLogin = Some("cool_mod"),
            moderatorUserName = Some("Cool_Mod"),
            guestUserId = Some("1234"),
            guestUserLogin = Some("cool_guest"),
            guestUserName = Some("Cool_Guest"),
            slotId = Some("1"),
            state = Some("live"),
            hostVideoEnabled = Some(true),
            hostAudioEnabled = Some(true),
            hostVolume = Some(100)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Guest Star Settings Update is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.guest_star_settings.update.vbeta.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.guest_star_settings.update",
            version = "beta",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337"), moderatorUserId = Some("1312")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2023, 4, 11, 10, 11, 52, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(ChannelGuestStarSettingsUpdateEvent(
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            isModeratorSendLiveEnabled = true,
            moderatorUserId = None,
            moderatorUserLogin = None,
            moderatorUserName = None,
            slotCount = 5,
            isBrowserSourceAudioEnabled = true,
            groupLayout = "tiled"
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Points Automatic Reward Add is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.channel_points_automatic_reward_redemption.add.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "7297f7eb-3bf5-461f-8ae6-7cd7781ebce3",
            status = "enabled",
            `type` = "channel.channel_points_automatic_reward_redemption.add",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("12826")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2024, 2, 23, 21, 12, 33, 771005262), ZoneOffset.UTC)
          )),
          event = Some(ChannelPointsAutomaticRewardRedemptionAddEvent(
            broadcasterUserId = "12826",
            broadcasterUserLogin = "twitch",
            broadcasterUserName = "Twitch",
            userId = "141981764",
            userLogin = "twitchdev",
            userName = "TwitchDev",
            id = "f024099a-e0fe-4339-9a0a-a706fb59f353",
            reward = RewardInformation(`type` = "send_highlighted_message", cost = 100, unlockedEmote = None),
            message = Message(
              text = "Hello world! VoHiYo",
              emotes = List(
                EmotePosition(id = "81274", begin = 13, end = 18)
              )
            ),
            userInput = Some("Hello world! VoHiYo "),
            redeemedAt = OffsetDateTime.of(LocalDateTime.of(2024, 2, 23, 21, 14, 34, 260398045), ZoneOffset.UTC)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Points Custom Reward Add is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.channel_points_custom_reward.add.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.channel_points_custom_reward.add",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelPointsCustomRewardAddEvent(
            id = "9001",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            isEnabled = true,
            isPaused = false,
            isInStock = true,
            title = "Cool Reward",
            cost = 100,
            prompt = "reward prompt",
            isUserInputRequired = true,
            shouldRedemptionsSkipRequestQueue = false,
            maxPerStream = MaxPerStream(isEnabled = true, value = 1000),
            maxPerUserPerStream = MaxPerStream(isEnabled = true, value = 1000),
            backgroundColor = "#FA1ED2",
            image = Some(Image(
              url_1x = "https://static-cdn.jtvnw.net/image-1.png",
              url_2x = "https://static-cdn.jtvnw.net/image-2.png",
              url_4x = "https://static-cdn.jtvnw.net/image-4.png"
            )),
            defaultImage = Image(
              url_1x = "https://static-cdn.jtvnw.net/default-1.png",
              url_2x = "https://static-cdn.jtvnw.net/default-2.png",
              url_4x = "https://static-cdn.jtvnw.net/default-4.png"
            ),
            globalCooldown = GlobalCooldown(isEnabled = true, seconds = 1000),
            cooldownExpiresAt = None,
            redemptionsRedeemedCurrentStream = None
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Points Custom Reward Update is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.channel_points_custom_reward.update.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.channel_points_custom_reward.update",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelPointsCustomRewardUpdateEvent(
            id = "9001",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            isEnabled = true,
            isPaused = false,
            isInStock = true,
            title = "Cool Reward",
            cost = 100,
            prompt = "reward prompt",
            isUserInputRequired = true,
            shouldRedemptionsSkipRequestQueue = false,
            maxPerStream = MaxPerStream(isEnabled = true, value = 1000),
            maxPerUserPerStream = MaxPerStream(isEnabled = true, value = 1000),
            backgroundColor = "#FA1ED2",
            image = Some(Image(
              url_1x = "https://static-cdn.jtvnw.net/image-1.png",
              url_2x = "https://static-cdn.jtvnw.net/image-2.png",
              url_4x = "https://static-cdn.jtvnw.net/image-4.png"
            )),
            defaultImage = Image(
              url_1x = "https://static-cdn.jtvnw.net/default-1.png",
              url_2x = "https://static-cdn.jtvnw.net/default-2.png",
              url_4x = "https://static-cdn.jtvnw.net/default-4.png"
            ),
            globalCooldown = GlobalCooldown(isEnabled = true, seconds = 1000),
            cooldownExpiresAt = Some(OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)),
            redemptionsRedeemedCurrentStream = Some(123)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Points Custom Reward Remove is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.channel_points_custom_reward.remove.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.channel_points_custom_reward.remove",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337"), rewardId = Some("12345")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelPointsCustomRewardRemoveEvent(
            id = "9001",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            isEnabled = true,
            isPaused = false,
            isInStock = true,
            title = "Cool Reward",
            cost = 100,
            prompt = "reward prompt",
            isUserInputRequired = true,
            shouldRedemptionsSkipRequestQueue = false,
            maxPerStream = MaxPerStream(isEnabled = true, value = 1000),
            maxPerUserPerStream = MaxPerStream(isEnabled = true, value = 1000),
            backgroundColor = "#FA1ED2",
            image = Some(Image(
              url_1x = "https://static-cdn.jtvnw.net/image-1.png",
              url_2x = "https://static-cdn.jtvnw.net/image-2.png",
              url_4x = "https://static-cdn.jtvnw.net/image-4.png"
            )),
            defaultImage = Image(
              url_1x = "https://static-cdn.jtvnw.net/default-1.png",
              url_2x = "https://static-cdn.jtvnw.net/default-2.png",
              url_4x = "https://static-cdn.jtvnw.net/default-4.png"
            ),
            globalCooldown = GlobalCooldown(isEnabled = true, seconds = 1000),
            cooldownExpiresAt = Some(OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 123 * 1000 * 1000), ZoneOffset.UTC)),
            redemptionsRedeemedCurrentStream = Some(123)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Points Custom Reward Redemption Add is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.channel_points_custom_reward_redemption.add.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.channel_points_custom_reward_redemption.add",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337"), rewardId = Some("92af127c-7326-4483-a52b-b0da0be61c01")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelPointsCustomRewardRedemptionAddEvent(
            id = "17fa2df1-ad76-4804-bfa5-a40ef63efe63",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            userId = "9001",
            userLogin = "cooler_user",
            userName = "Cooler_User",
            userInput = "pogchamp",
            status = "unfulfilled",
            reward = Reward(
              id = "92af127c-7326-4483-a52b-b0da0be61c01",
              title = "title",
              cost = 100,
              prompt = "reward prompt"
            ),
            redeemedAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 17, 16, 3, 171067130), ZoneOffset.UTC)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Points Custom Reward Redemption Update is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.channel_points_custom_reward_redemption.update.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.channel_points_custom_reward_redemption.update",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337"), rewardId = Some("92af127c-7326-4483-a52b-b0da0be61c01")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelPointsCustomRewardRedemptionUpdateEvent(
            id = "17fa2df1-ad76-4804-bfa5-a40ef63efe63",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            userId = "9001",
            userLogin = "cooler_user",
            userName = "Cooler_User",
            userInput = "pogchamp",
            status = "fulfilled",
            reward = Reward(
              id = "92af127c-7326-4483-a52b-b0da0be61c01",
              title = "title",
              cost = 100,
              prompt = "reward prompt"
            ),
            redeemedAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 17, 16, 3, 171067130), ZoneOffset.UTC)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Poll Begin is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.poll.begin.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.poll.begin",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelPollBeginEvent(
            id = "1243456",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            title = "Aren’t shoes just really hard socks?",
            choices = List(
              PollChoice(id = "123", title = "Yeah!"),
              PollChoice(id = "124", title = "No!"),
              PollChoice(id = "125", title = "Maybe!")
            ),
            bitsVoting = BitsVoting(isEnabled = true, amountPerVote = 10),
            channelPointsVoting = ChannelPointsVoting(isEnabled = true, amountPerVote = 10),
            startedAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 17, 16, 3, 171067130), ZoneOffset.UTC),
            endsAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 17, 16, 8, 171067130), ZoneOffset.UTC),
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Poll Progress is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.poll.progress.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.poll.progress",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelPollProgressEvent(
            id = "1243456",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            title = "Aren’t shoes just really hard socks?",
            choices = List(
              StartedPollChoice(id = "123", title = "Yeah!", bitsVotes = 5, channelPointsVotes = 7, votes = 12),
              StartedPollChoice(id = "124", title = "No!", bitsVotes = 10, channelPointsVotes = 4, votes = 14),
              StartedPollChoice(id = "125", title = "Maybe!", bitsVotes = 0, channelPointsVotes = 7, votes = 7)
            ),
            bitsVoting = BitsVoting(isEnabled = true, amountPerVote = 10),
            channelPointsVoting = ChannelPointsVoting(isEnabled = true, amountPerVote = 10),
            startedAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 17, 16, 3, 171067130), ZoneOffset.UTC),
            endsAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 17, 16, 8, 171067130), ZoneOffset.UTC),
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Poll End is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.poll.end.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.poll.end",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelPollEndEvent(
            id = "1243456",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            title = "Aren’t shoes just really hard socks?",
            choices = List(
              StartedPollChoice(id = "123", title = "Yeah!", bitsVotes = 50, channelPointsVotes = 70, votes = 120),
              StartedPollChoice(id = "124", title = "No!", bitsVotes = 100, channelPointsVotes = 40, votes = 140),
              StartedPollChoice(id = "125", title = "Maybe!", bitsVotes = 10, channelPointsVotes = 70, votes = 80)
            ),
            bitsVoting = BitsVoting(isEnabled = true, amountPerVote = 10),
            channelPointsVoting = ChannelPointsVoting(isEnabled = true, amountPerVote = 10),
            status = "completed",
            startedAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 17, 16, 3, 171067130), ZoneOffset.UTC),
            endedAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 17, 16, 11, 171067130), ZoneOffset.UTC),
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Prediction Begin is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.prediction.begin.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.prediction.begin",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelPredictionBeginEvent(
            id = "1243456",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            title = "Aren’t shoes just really hard socks?",
            outcomes = List(
              PredictionOutcome(id = "1243456", title = "Yeah!", color = "blue"),
              PredictionOutcome(id = "2243456", title = "No!", color = "pink"),
            ),
            startedAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 17, 16, 3, 171067130), ZoneOffset.UTC),
            locksAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 17, 21, 3, 171067130), ZoneOffset.UTC),
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Prediction Progress is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.prediction.progress.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.prediction.progress",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelPredictionProgressEvent(
            id = "1243456",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            title = "Aren’t shoes just really hard socks?",
            outcomes = List(
              StartedPredictionOutcome(
                id = "1243456",
                title = "Yeah!",
                color = "blue",
                users = 10,
                channelPoints = 15000,
                topPredictors = List(
                  TopPredictor(
                    userId = "1234",
                    userLogin = "cool_user",
                    userName = "Cool_User",
                    channelPointsWon = None,
                    channelPointsUsed = 500
                  ),
                  TopPredictor(
                    userId = "1236",
                    userLogin = "coolest_user",
                    userName = "Coolest_User",
                    channelPointsWon = None,
                    channelPointsUsed = 200
                  )
                )
              ),
              StartedPredictionOutcome(
                id = "2243456",
                title = "No!",
                color = "pink",
                users = 8,
                channelPoints = 13000,
                topPredictors = List(
                  TopPredictor(
                    userId = "12345",
                    userLogin = "cooler_user",
                    userName = "Cooler_User",
                    channelPointsWon = None,
                    channelPointsUsed = 5000
                  )
                )
              ),
            ),
            startedAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 17, 16, 3, 171067130), ZoneOffset.UTC),
            locksAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 17, 21, 3, 171067130), ZoneOffset.UTC),
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Prediction Lock is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.prediction.lock.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.prediction.lock",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelPredictionLockEvent(
            id = "1243456",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            title = "Aren’t shoes just really hard socks?",
            outcomes = List(
              StartedPredictionOutcome(
                id = "1243456",
                title = "Yeah!",
                color = "blue",
                users = 10,
                channelPoints = 15000,
                topPredictors = List(
                  TopPredictor(
                    userId = "1234",
                    userLogin = "cool_user",
                    userName = "Cool_User",
                    channelPointsWon = None,
                    channelPointsUsed = 500
                  ),
                  TopPredictor(
                    userId = "1236",
                    userLogin = "coolest_user",
                    userName = "Coolest_User",
                    channelPointsWon = None,
                    channelPointsUsed = 200
                  )
                )
              ),
              StartedPredictionOutcome(
                id = "2243456",
                title = "No!",
                color = "pink",
                users = 8,
                channelPoints = 13000,
                topPredictors = List(
                  TopPredictor(
                    userId = "12345",
                    userLogin = "cooler_user",
                    userName = "Cooler_User",
                    channelPointsWon = None,
                    channelPointsUsed = 5000
                  )
                )
              ),
            ),
            startedAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 17, 16, 3, 171067130), ZoneOffset.UTC),
            lockedAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 17, 21, 3, 171067130), ZoneOffset.UTC),
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Prediction End is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.prediction.end.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.prediction.end",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelPredictionEndEvent(
            id = "1243456",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            title = "Aren’t shoes just really hard socks?",
            outcomes = List(
              StartedPredictionOutcome(
                id = "12345",
                title = "Yeah!",
                color = "blue",
                users = 10,
                channelPoints = 15000,
                topPredictors = List(
                  TopPredictor(
                    userId = "1234",
                    userLogin = "cool_user",
                    userName = "Cool_User",
                    channelPointsWon = Some(10000),
                    channelPointsUsed = 500
                  ),
                  TopPredictor(
                    userId = "1236",
                    userLogin = "coolest_user",
                    userName = "Coolest_User",
                    channelPointsWon = Some(5000),
                    channelPointsUsed = 200
                  )
                )
              ),
              StartedPredictionOutcome(
                id = "22435",
                title = "No!",
                color = "pink",
                users = 8,
                channelPoints = 14000,
                topPredictors = List(
                  TopPredictor(
                    userId = "12345",
                    userLogin = "cooler_user",
                    userName = "Cooler_User",
                    channelPointsWon = None,
                    channelPointsUsed = 5000
                  ),
                  TopPredictor(
                    userId = "1337",
                    userLogin = "elite_user",
                    userName = "Elite_User",
                    channelPointsWon = None,
                    channelPointsUsed = 2000
                  )
                )
              ),
            ),
            status = "resolved",
            startedAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 17, 16, 3, 171067130), ZoneOffset.UTC),
            endedAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 17, 21, 3, 171067130), ZoneOffset.UTC),
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Suspicious User Update is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.suspicious_user.update.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.suspicious_user.update",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1050263435"), moderatorUserId = Some("1050263436")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2023, 4, 11, 10, 11, 12, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(ChannelSuspiciousUserUpdateEvent(
            broadcasterUserId = "1050263435",
            broadcasterUserLogin = "77f111cbb75341449f5",
            broadcasterUserName = "77f111cbb75341449f5",
            moderatorUserId = "1050263436",
            moderatorUserLogin = "29087e59dfc441968f6",
            moderatorUserName = "29087e59dfc441968f6",
            userId = "1050263437",
            userLogin = "06fbcc75952245c5a87",
            userName = "06fbcc75952245c5a87",
            lowTrustStatus = "restricted"
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Suspicious User Message is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.suspicious_user.message.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.suspicious_user.message",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1050263432"), moderatorUserId = Some("9001")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2023, 4, 11, 10, 11, 12, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(ChannelSuspiciousUserMessageEvent(
            broadcasterUserId = "1050263432",
            broadcasterUserLogin = "dcf9dd9336034d23b65",
            broadcasterUserName = "dcf9dd9336034d23b65",
            userId = "1050263434",
            userLogin = "4a46e2cf2e2f4d6a9e6",
            userName = "4a46e2cf2e2f4d6a9e6",
            lowTrustStatus = "active_monitoring",
            sharedBanChannelIds = List("100", "200"),
            types = List("ban_evader"),
            banEvasionEvaluation = "likely",
            message = MessageWithIdAndFragments(
              messageId = "101010",
              text = "bad stuff pogchamp",
              fragments = List(
                ChatMessageFragment(
                  `type` = "emote",
                  text = "bad stuff",
                  cheermote = None,
                  emote = Some(ChatMessageEmote(id = "899", emoteSetId = "1")),
                  mention = None
                ),
                ChatMessageFragment(
                  `type` = "cheermote",
                  text = "pogchamp",
                  cheermote = Some(ChatMessageCheermote(prefix = "pogchamp", bits = 100, tier = 1)),
                  emote = None,
                  mention = None
                )
              )
            )
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel VIP Add is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.vip.add.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "7297f7eb-3bf5-461f-8ae6-7cd7781ebce3",
            status = "enabled",
            `type` = "channel.vip.add",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2024, 2, 23, 21, 12, 33, 771005262), ZoneOffset.UTC)
          )),
          event = Some(ChannelVIPAddEvent(
            userId = "1234",
            userLogin = "mod_user",
            userName = "Mod_User",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cooler_user",
            broadcasterUserName = "Cooler_User"
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel VIP Remove is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.vip.remove.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "7297f7eb-3bf5-461f-8ae6-7cd7781ebce3",
            status = "enabled",
            `type` = "channel.vip.remove",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2024, 2, 23, 21, 12, 33, 771005262), ZoneOffset.UTC)
          )),
          event = Some(ChannelVIPRemoveEvent(
            userId = "1234",
            userLogin = "mod_user",
            userName = "Mod_User",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cooler_user",
            broadcasterUserName = "Cooler_User"
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Warning Acknowledge is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.warning.acknowledge.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "7297f7eb-3bf5-461f-8ae6-7cd7781ebce3",
            status = "enabled",
            `type` = "channel.warning.acknowledge",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("423374343"), moderatorUserId = Some("424596340")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2024, 2, 23, 21, 12, 33, 771005262), ZoneOffset.UTC)
          )),
          event = Some(ChannelWarningAcknowledgeEvent(
            broadcasterUserId = "423374343",
            broadcasterUserLogin = "glowillig",
            broadcasterUserName = "glowillig",
            userId = "141981764",
            userLogin = "twitchdev",
            userName = "TwitchDev"
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Warning Send is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.warning.send.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "7297f7eb-3bf5-461f-8ae6-7cd7781ebce3",
            status = "enabled",
            `type` = "channel.warning.send",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("423374343"), moderatorUserId = Some("424596340")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2024, 2, 23, 21, 12, 33, 771005262), ZoneOffset.UTC)
          )),
          event = Some(ChannelWarningSendEvent(
            broadcasterUserId = "423374343",
            broadcasterUserLogin = "glowillig",
            broadcasterUserName = "glowillig",
            moderatorUserId = "424596340",
            moderatorUserLogin = "quotrok",
            moderatorUserName = "quotrok",
            userId = "141981764",
            userLogin = "twitchdev",
            userName = "TwitchDev",
            reason = Some("cut it out"),
            chatRulesCited = None
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Hype Train Begin is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.hype_train.begin.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.hype_train.begin",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelHypeTrainBeginEvent(
            id = "1b0AsbInCHZW2SQFQkCzqN07Ib2",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            total = 137,
            progress = 137,
            goal = 500,
            topContributions = List(
              Contribution(userId = "123", userLogin = "pogchamp", userName = "PogChamp", `type` = "bits", total = 50),
              Contribution(userId = "456", userLogin = "kappa", userName = "Kappa", `type` = "subscription", total = 45)
            ),
            lastContribution = Contribution(userId = "123", userLogin = "pogchamp", userName = "PogChamp", `type` = "bits", total = 50),
            level = 2,
            startedAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15,17, 16, 3,171067130), ZoneOffset.UTC),
            expiresAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 17, 16, 11, 171067130), ZoneOffset.UTC)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Hype Train Progress is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.hype_train.progress.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.hype_train.progress",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelHypeTrainProgressEvent(
            id = "1b0AsbInCHZW2SQFQkCzqN07Ib2",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            total = 700,
            progress = 200,
            goal = 1000,
            topContributions = List(
              Contribution(userId = "123", userLogin = "pogchamp", userName = "PogChamp", `type` = "bits", total = 50),
              Contribution(userId = "456", userLogin = "kappa", userName = "Kappa", `type` = "subscription", total = 45)
            ),
            lastContribution = Contribution(userId = "123", userLogin = "pogchamp", userName = "PogChamp", `type` = "bits", total = 50),
            level = 2,
            startedAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15,17, 16, 3,171067130), ZoneOffset.UTC),
            expiresAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 17, 16, 11, 171067130), ZoneOffset.UTC)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Hype Train End is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.hype_train.end.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.hype_train.end",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ChannelHypeTrainEndEvent(
            id = "1b0AsbInCHZW2SQFQkCzqN07Ib2",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            total = 137,
            topContributions = List(
              Contribution(userId = "123", userLogin = "pogchamp", userName = "PogChamp", `type` = "bits", total = 50),
              Contribution(userId = "456", userLogin = "kappa", userName = "Kappa", `type` = "subscription", total = 45)
            ),
            level = 2,
            startedAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15,17, 16, 3,171067130), ZoneOffset.UTC),
            endedAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 17, 16, 11, 171067130), ZoneOffset.UTC),
            cooldownEndsAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 15, 18, 16, 11, 171067130), ZoneOffset.UTC)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Charity Campaign Donate is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.charity_campaign.donate.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.charity_campaign.donate",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("123456")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2022, 7, 25, 10, 11, 12, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(ChannelCharityDonationEvent(
            id = "a1b2c3-aabb-4455-d1e2f3",
            campaignId = "123-abc-456-def",
            broadcasterUserId = "123456",
            broadcasterUserLogin = "sunnysideup",
            broadcasterUserName = "SunnySideUp",
            userId = "654321",
            userLogin = "generoususer1",
            userName = "GenerousUser1",
            charityName = "Example name",
            charityDescription = "Example description",
            charityLogo = "https://abc.cloudfront.net/ppgf/1000/100.png",
            charityWebsite = "https://www.example.com",
            amount = Amount(value = 10000, decimalPlace = 2, currency = "USD")
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Charity Campaign Start is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.charity_campaign.start.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.charity_campaign.start",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("123456")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2022, 7, 25, 10, 11, 12, 123398240), ZoneOffset.UTC)
          )),
          event = Some(ChannelCharityCampaignStartEvent(
            id = "123-abc-456-def",
            broadcasterId = "123456",
            broadcasterLogin = "sunnysideup",
            broadcasterName = "SunnySideUp",
            charityName = "Example name",
            charityDescription = "Example description",
            charityLogo = "https://abc.cloudfront.net/ppgf/1000/100.png",
            charityWebsite = "https://www.example.com",
            currentAmount = Amount(value = 0, decimalPlace = 2, currency = "USD"),
            targetAmount = Amount(value = 1500000, decimalPlace = 2, currency = "USD"),
            startedAt = OffsetDateTime.of(LocalDateTime.of(2022, 7, 26, 17, 0, 3, 171067130), ZoneOffset.UTC)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Charity Campaign Progress is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.charity_campaign.progress.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.charity_campaign.progress",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("123456")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2022, 7, 25, 10, 11, 12, 123398240), ZoneOffset.UTC)
          )),
          event = Some(ChannelCharityCampaignProgressEvent(
            id = "123-abc-456-def",
            broadcasterId = "123456",
            broadcasterLogin = "sunnysideup",
            broadcasterName = "SunnySideUp",
            charityName = "Example name",
            charityDescription = "Example description",
            charityLogo = "https://abc.cloudfront.net/ppgf/1000/100.png",
            charityWebsite = "https://www.example.com",
            currentAmount = Amount(value = 260000, decimalPlace = 2, currency = "USD"),
            targetAmount = Amount(value = 1500000, decimalPlace = 2, currency = "USD")
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Charity Campaign Stop is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.charity_campaign.stop.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.charity_campaign.stop",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("123456")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2022, 7, 25, 10, 11, 12, 123398240), ZoneOffset.UTC)
          )),
          event = Some(ChannelCharityCampaignStopEvent(
            id = "123-abc-456-def",
            broadcasterId = "123456",
            broadcasterLogin = "sunnysideup",
            broadcasterName = "SunnySideUp",
            charityName = "Example name",
            charityDescription = "Example description",
            charityLogo = "https://abc.cloudfront.net/ppgf/1000/100.png",
            charityWebsite = "https://www.example.com",
            currentAmount = Amount(value = 1450000, decimalPlace = 2, currency = "USD"),
            targetAmount = Amount(value = 1500000, decimalPlace = 2, currency = "USD"),
            stoppedAt = OffsetDateTime.of(LocalDateTime.of(2022, 7, 26, 22, 0, 3, 171067130), ZoneOffset.UTC)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Shield Mode Begin is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.shield_mode.begin.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.shield_mode.begin",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("12345"), moderatorUserId = Some("98765")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2022, 7, 25, 10, 11, 12, 123673900), ZoneOffset.UTC)
          )),
          event = Some(ChannelShieldModeBeginEvent(
            broadcasterUserId = "12345",
            broadcasterUserLogin = "simplysimple",
            broadcasterUserName = "SimplySimple",
            moderatorUserId = "98765",
            moderatorUserLogin = "particularlyparticular123",
            moderatorUserName = "ParticularlyParticular123",
            startedAt = OffsetDateTime.of(LocalDateTime.of(2022, 7, 26, 17, 0, 3, 171067130), ZoneOffset.UTC)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Shield Mode End is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.shield_mode.end.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.shield_mode.end",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("12345"), moderatorUserId = Some("98765")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2022, 7, 25, 10, 11, 12, 123673900), ZoneOffset.UTC)
          )),
          event = Some(ChannelShieldModeEndEvent(
            broadcasterUserId = "12345",
            broadcasterUserLogin = "simplysimple",
            broadcasterUserName = "SimplySimple",
            moderatorUserId = "98765",
            moderatorUserLogin = "particularlyparticular123",
            moderatorUserName = "ParticularlyParticular123",
            endedAt = OffsetDateTime.of(LocalDateTime.of(2022, 7, 27, 1, 30, 23, 171067130), ZoneOffset.UTC)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Shoutout Create is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.shoutout.create.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.shoutout.create",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("12345"), moderatorUserId = Some("98765")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2022, 7, 25, 10, 11, 12, 123673900), ZoneOffset.UTC)
          )),
          event = Some(ChannelShoutoutCreateEvent(
            broadcasterUserId = "12345",
            broadcasterUserLogin = "simplysimple",
            broadcasterUserName = "SimplySimple",
            moderatorUserId = "98765",
            moderatorUserLogin = "particularlyparticular123",
            moderatorUserName = "ParticularlyParticular123",
            toBroadcasterUserId = "626262",
            toBroadcasterUserLogin = "sandysanderman",
            toBroadcasterUserName = "SandySanderman",
            startedAt = OffsetDateTime.of(LocalDateTime.of(2022, 7, 26, 17, 0, 3, 171067130), ZoneOffset.UTC),
            viewerCount = 860,
            cooldownEndsAt = OffsetDateTime.of(LocalDateTime.of(2022, 7, 26, 17, 2, 3, 171067130), ZoneOffset.UTC),
            targetCooldownEndsAt = OffsetDateTime.of(LocalDateTime.of(2022, 7, 26, 18, 0, 3, 171067130), ZoneOffset.UTC)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Shoutout Receive is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.shoutout.receive.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.shoutout.receive",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("626262"), moderatorUserId = Some("98765")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2022, 7, 25, 10, 11, 12, 123673900), ZoneOffset.UTC)
          )),
          event = Some(ChannelShoutoutReceiveEvent(
            broadcasterUserId = "626262",
            broadcasterUserLogin = "sandysanderman",
            broadcasterUserName = "SandySanderman",
            fromBroadcasterUserId = "12345",
            fromBroadcasterUserLogin = "simplysimple",
            fromBroadcasterUserName = "SimplySimple",
            viewerCount = 860,
            startedAt = OffsetDateTime.of(LocalDateTime.of(2022, 7, 26, 17, 0, 3, 171067130), ZoneOffset.UTC)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Conduit Shard Disabled is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/conduit.shard.disabled.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "conduit.shard.disabled",
            version = "1",
            cost = 0,
            condition = Condition(clientId = Some("uo6dggojyb8d6soh92zknwmi5ej1q2")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2023, 4, 11, 10, 11, 12, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(ConduitShardDisabledEvent(
            conduit_id = "bfcfc993-26b1-b876-44d9-afe75a379dac",
            shard_id = "4",
            status = "websocket_disconnected",
            transport = Transport(
              method = "websocket",
              callback = None,
              session = Some("ad1c9fc3-0d99-4eb7-8a04-8608e8ff9ec9"),
              connectedAt = Some(OffsetDateTime.of(LocalDateTime.of(2020, 11, 10, 14, 32, 18, 730260295), ZoneOffset.UTC)),
              disconnectedAt = Some(OffsetDateTime.of(LocalDateTime.of(2020, 11, 11, 14, 32, 18, 730260295), ZoneOffset.UTC))
            )
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Drop Entitlement Grant is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/drop.entitlement.grant.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "drop.entitlement.grant",
            version = "1",
            cost = 0,
            condition = Condition(organizationId = Some("9001"), categoryId = Some("9002"), campaignId = Some("9003")),
            transport = Transport(method = "webhook", callback = Some("https://example.com/webhooks/callback")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(DropEntitlementGrantEvents(
            List(
              DropEntitlementGrantEvent(id = "bf7c8577-e3e3-4881-a78a-e9446641d45d",
                data = Entitlement(
                  organizationId = "9001",
                  categoryId = "9002",
                  categoryName = "Fortnite",
                  campaignId = "9003",
                  userId = "1234",
                  userLogin = "cool_user",
                  userName = "Cool_User",
                  entitlementId = "fb78259e-fb81-4d1b-8333-34a06ffc24c0",
                  benefitId = "74c52265-e214-48a6-91b9-23b6014e8041",
                  createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 1, 28, 4, 17, 53, 325 * 1000 * 1000), ZoneOffset.UTC)
                )
              ),
              DropEntitlementGrantEvent(id = "bf7c8577-e3e3-4881-a78a-e9446641d45c",
                data = Entitlement(
                  organizationId = "9001",
                  categoryId = "9002",
                  categoryName = "Fortnite",
                  campaignId = "9003",
                  userId = "12345",
                  userLogin = "cooler_user",
                  userName = "Cooler_User",
                  entitlementId = "fb78259e-fb81-4d1b-8333-34a06ffc24c0",
                  benefitId = "74c52265-e214-48a6-91b9-23b6014e8041",
                  createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 1, 28, 4, 17, 53, 325 * 1000 * 1000), ZoneOffset.UTC)
                )
              )
            )
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Extension Bits Transaction Create" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/extension.bits_transaction.create.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "extension.bits_transaction.create",
            version = "1",
            cost = 0,
            condition = Condition(extensionClientId = Some("deadbeef")),
            transport = Transport(method = "webhook", callback = Some("https://example.com/webhooks/callback")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(ExtensionBitsTransactionCreateEvent(
            extensionClientId = "deadbeef",
            id = "bits-tx-id",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            userId = "1236",
            userLogin = "coolest_user",
            userName = "Coolest_User",
            product = ExtensionProduct(name = "great_product", sku = "skuskusku", bits = 1234, inDevelopment = false)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Goal Begin is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.goal.begin.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.goal.begin",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("141981764")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2021, 7, 15, 10, 11, 12, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(ChannelGoalBeginEvent(
            id = "12345-cool-event",
            broadcasterUserId = "141981764",
            broadcasterUserLogin = "twitchdev",
            broadcasterUserName = "TwitchDev",
            `type` = "subscription",
            description = "Help me get partner!",
            currentAmount = 100,
            targetAmount = 220,
            startedAt = OffsetDateTime.of(LocalDateTime.of(2021, 7, 15, 17, 16, 3, 171067130), ZoneOffset.UTC)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Goal Progress is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.goal.progress.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.goal.progress",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("141981764")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2021, 7, 15, 10, 11, 12, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(ChannelGoalProgressEvent(
            id = "12345-cool-event",
            broadcasterUserId = "141981764",
            broadcasterUserLogin = "twitchdev",
            broadcasterUserName = "TwitchDev",
            `type` = "subscription",
            description = "Help me get partner!",
            currentAmount = 120,
            targetAmount = 220,
            startedAt = OffsetDateTime.of(LocalDateTime.of(2021, 7, 15, 17, 16, 3, 171067130), ZoneOffset.UTC)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Channel Goal End is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/channel.goal.end.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "channel.goal.end",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("141981764")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2021, 7, 15, 10, 11, 12, 123 * 1000 * 1000), ZoneOffset.UTC)
          )),
          event = Some(ChannelGoalEndEvent(
            id = "12345-abc-678-defgh",
            broadcasterUserId = "141981764",
            broadcasterUserLogin = "twitchdev",
            broadcasterUserName = "TwitchDev",
            `type` = "subscription",
            description = "Help me get partner!",
            isAchieved = false,
            currentAmount = 180,
            targetAmount = 220,
            startedAt = OffsetDateTime.of(LocalDateTime.of(2021, 7, 15, 17, 16, 3, 171067130), ZoneOffset.UTC),
            endedAt = OffsetDateTime.of(LocalDateTime.of(2020, 7, 16, 17, 16, 3, 171067130), ZoneOffset.UTC)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Stream Online is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/stream.online.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "stream.online",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(StreamOnlineEvent(
            id = "9001",
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
            `type` = "live",
            startedAt = OffsetDateTime.of(LocalDateTime.of(2020,10, 11, 10, 11, 12, 123 * 1000 * 1000), ZoneOffset.UTC)
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Stream Offline is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/stream.offline.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "stream.offline",
            version = "1",
            cost = 0,
            condition = Condition(broadcasterUserId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(StreamOfflineEvent(
            broadcasterUserId = "1337",
            broadcasterUserLogin = "cool_user",
            broadcasterUserName = "Cool_User",
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "User Authorization Grant is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/user.authorization.grant.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "user.authorization.grant",
            version = "1",
            cost = 1,
            condition = Condition(clientId = Some("crq72vsaoijkc83xx42hz6i37")),
            transport = Transport(method = "webhook", callback = Some("https://example.com/webhooks/callback")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(UserAuthorizationGrantEvent(
            client_id = "crq72vsaoijkc83xx42hz6i37", userId = "1337", userLogin = "cool_user", userName = "Cool_User"
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "User Authorization Revoke is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/user.authorization.revoke.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "user.authorization.revoke",
            version = "1",
            cost = 1,
            condition = Condition(clientId = Some("crq72vsaoijkc83xx42hz6i37")),
            transport = Transport(method = "webhook", callback = Some("https://example.com/webhooks/callback")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(UserAuthorizationRevokeEvent(
            client_id = "crq72vsaoijkc83xx42hz6i37", userId = "1337", userLogin = Some("cool_user"), userName = Some("Cool_User")
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "User Update is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/user.update.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "f1c2a387-161a-49f9-a165-0f21d7a4e1c4",
            status = "enabled",
            `type` = "user.update",
            version = "1",
            cost = 0,
            condition = Condition(userId = Some("1337")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2019, 11, 16, 10, 11, 12, 634234626), ZoneOffset.UTC)
          )),
          event = Some(UserUpdateEvent(
            userId = "1337",
            userLogin = "cool_user",
            userName = "Cool_User",
            email = "user@email.com",
            email_verified = true,
            description = "cool description"
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }

    "Whisper Received is received" should {
      "be correctly decoded" in {
        val json = readFile(s"$TEST_JSON_DIRECTORY/notification_payloads/user.whisper.message.v1.json")
        val expectedPayload = Payload(
          subscription = Some(Subscription(
            id = "7297f7eb-3bf5-461f-8ae6-7cd7781ebce3",
            status = "enabled",
            `type` = "user.whisper.message",
            version = "1",
            cost = 0,
            condition = Condition(userId = Some("423374343")),
            transport = Transport(method = "websocket", session = Some("AgoQHR3s6Mb4T8GFB1l3DlPfiRIGY2VsbC1h")),
            createdAt = OffsetDateTime.of(LocalDateTime.of(2024, 2, 23, 21, 12, 33, 771005262), ZoneOffset.UTC)
          )),
          event = Some(WhisperReceivedEvent(
            from_userId = "423374343",
            from_userLogin = "glowillig",
            from_userName = "glowillig",
            to_userId = "424596340",
            to_userLogin = "quotrok",
            to_userName = "quotrok",
            whisper_id = "some-whisper-id",
            whisper = Whisper(text = "a secret")
          ))
        )
        decode[Payload](json) shouldEqual Right(expectedPayload)
      }
    }
  }

  "Session encoder" when {
    "Session object is provided" should {
      "correctly encode it" in {
        val expectedJson = readFile(s"$TEST_JSON_DIRECTORY/subscription.json")
        val request = SubscriptionRequest(
          `type` = "user.update",
          version = "1",
          condition = Condition(userId = Some("1234")),
          transport = Transport(method = "websocket", session = Some("AQoQexAWVYKSTIu4ec_2VAxyuhAB"))
        )

        request.asJson.spaces2 shouldEqual expectedJson
      }
    }
  }
}
