package com.archimond7450.archiemate.twitch.irc

import com.archimond7450.archiemate.twitch.irc.IncomingMessages.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

class IncomingMessageDecoderSpec extends AnyWordSpecLike with Matchers {
  val incomingMessageDecoder = new IncomingMessageDecoder

  "Incoming Message Decoder's decode method" should {
    "correctly decode the ClearChat message" when {
      "dallas permanently banned ronni from the chat room and removed all of ronni’s messages" in {
        val message = "@room-id=12345678;target-user-id=87654321;tmi-sent-ts=1642715756806 :tmi.twitch.tv CLEARCHAT #dallas :ronni"
        val expected = ClearChat.Ban(
          roomId = 12345678L,
          targetUserId = 87654321L,
          tmiSentTimestamp = OffsetDateTime.of(LocalDateTime.of(2022, 1, 20, 21, 55, 56, 806 * 1000 * 1000), ZoneOffset.UTC),
          channelName = "dallas",
          userName = "ronni"
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "dallas removed all messages from the chat room" in {
        val message = "@room-id=12345678;tmi-sent-ts=1642715695392 :tmi.twitch.tv CLEARCHAT #dallas"
        val expected = ClearChat.Clear(
          roomId = 12345678L,
          tmiSentTimestamp = OffsetDateTime.of(LocalDateTime.of(2022, 1, 20, 21, 54, 55, 392 * 1000 * 1000), ZoneOffset.UTC),
          channelName = "dallas"
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "dallas put ronni in a timeout and removed all of ronni’s messages from the chat room" in {
        val message = "@ban-duration=350;room-id=12345678;target-user-id=87654321;tmi-sent-ts=1642719320727 :tmi.twitch.tv CLEARCHAT #dallas :ronni"
        val expected = ClearChat.Timeout(
          banDurationSec = Duration(350, TimeUnit.SECONDS),
          roomId = 12345678L,
          targetUserId = 87654321L,
          tmiSentTimestamp = OffsetDateTime.of(LocalDateTime.of(2022, 1, 20, 22, 55, 20, 727 * 1000 * 1000), ZoneOffset.UTC),
          channelName = "dallas",
          userName = "ronni"
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }
    }

    "correctly decode the ClearMsg message" when {
      "moderator deleted ronni’s HeyGuys message from the dallas chat room" in {
        val message = "@login=ronni;room-id=;target-msg-id=abc-123-def;tmi-sent-ts=1642720582342 :tmi.twitch.tv CLEARMSG #dallas :HeyGuys"
        val expected = ClearMsg(
          login = "ronni",
          roomId = None,
          targetMsgId = "abc-123-def",
          tmiSentTimestamp = OffsetDateTime.of(LocalDateTime.of(2022, 1, 20, 23, 16, 22, 342 * 1000 * 1000), ZoneOffset.UTC),
          channelName = "dallas",
          message = "HeyGuys"
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }
    }

    "correctly decode the GlobalUserState message" when {
      "the user state of dallas, an admin user, after logging in" in {
        val message = "@badge-info=subscriber/8;badges=subscriber/6;color=#0D4200;display-name=dallas;emote-sets=0,33,50,237,793,2126,3517,4578,5569,9400,10337,12239;turbo=0;user-id=12345678;user-type=admin :tmi.twitch.tv GLOBALUSERSTATE"
        val expected = GlobalUserState(
          badgeInfo = Map("subscriber" -> "8"),
          badges = Map("subscriber" -> "6"),
          color = "#0D4200",
          displayName = "dallas",
          emoteSets = List(0, 33, 50, 237, 793, 2126, 3517, 4578, 5569, 9400, 10337, 12239),
          turbo = false,
          userId = 12345678,
          userType = TwitchUserType.Administrator
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }
    }

    "correctly decode the HostTarget message" when {
      "channel abc is hosting channel xyz who has 10 viewers watching its broadcast" in {
        val message = ":tmi.twitch.tv HOSTTARGET #abc :xyz 10"
        val expected = HostTarget.Start(
          hostingChannelName = "abc",
          hostedChannelName = "xyz",
          viewers = 10
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "channel abc is no longer hosting another channel" in {
        val message = ":tmi.twitch.tv HOSTTARGET #abc :- 10"
        val expected = HostTarget.End(
          hostingChannelName = "abc",
          viewers = 10
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }
    }

    "correctly decode the Join message" when {
      "ronni joined the dallas chat room" in {
        val message = ":ronni!ronni@ronni.tmi.twitch.tv JOIN #dallas"
        val expected = Join(
          userName = "ronni",
          channelName = "dallas"
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }
    }

    "correctly decode the Notice message" when {
      "foo’s message was successfully deleted" in {
        val message = "@msg-id=delete_message_success :tmi.twitch.tv NOTICE #bar :The message from foo is now deleted."
        val expected = Notice(
          noticeMessageId = NoticeID.DeleteMessageSuccess,
          channelName = "bar",
          message = "The message from foo is now deleted."
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "chatbot can’t send whisper messages" in {
        val message = "@msg-id=whisper_restricted;target-user-id=12345678 :tmi.twitch.tv NOTICE #bar :Your settings prevent you from sending this whisper."
        val expected = Notice(
          noticeMessageId = NoticeID.WhisperRestricted,
          channelName = "bar",
          message = "Your settings prevent you from sending this whisper.",
          targetUserId = Some(12345678)
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }
    }

    "correctly decode the Part message" when {
      "ronni left the dallas chat room" in {
        val message = ":ronni!ronni@ronni.tmi.twitch.tv PART #dallas"
        val expected = Part(
          userName = "ronni",
          channelName = "dallas"
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }
    }

    "correctly decode the PrivMsg message" when {
      "ronni posted a message in the chat room" in {
        val message = "@badge-info=;badges=turbo/1;color=#0D4200;display-name=ronni;emotes=25:0-4,12-16/1902:6-10;id=b34ccfc7-4977-403a-8a94-33c6bac34fb8;mod=0;room-id=1337;subscriber=0;tmi-sent-ts=1507246572675;turbo=1;user-id=1337;user-type=global_mod :ronni!ronni@ronni.tmi.twitch.tv PRIVMSG #ronni :Kappa Keepo Kappa"
        val expected = PrivMsg(
          badgeInfo = Map.empty,
          badges = Map("turbo" -> "1"),
          color = "#0D4200",
          displayName = "ronni",
          emoteOnly = false,
          emotes = Map(
            "25" -> List(
              (0, 4),
              (12, 16)),
            "1902" -> List(
              (6, 10))
          ),
          firstMessage = false,
          mod = false,
          returningChatter = false,
          roomId = 1337,
          subscriber = false,
          tmiSentTimestamp = OffsetDateTime.of(LocalDateTime.of(2017, 10, 5, 23, 36, 12, 675 * 1000 * 1000), ZoneOffset.UTC),
          turbo = true,
          userId = 1337,
          userType = TwitchUserType.GlobalModerator,
          vip = false,
          userName = "ronni",
          channelName = "ronni",
          message = "Kappa Keepo Kappa",
          id = Some("b34ccfc7-4977-403a-8a94-33c6bac34fb8")
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "ronni cheered 100 Bits" in {
        val message = "@badge-info=;badges=staff/1,bits/1000;bits=100;color=;display-name=ronni;emotes=;id=b34ccfc7-4977-403a-8a94-33c6bac34fb8;mod=0;room-id=12345678;subscriber=0;tmi-sent-ts=1507246572675;turbo=0;user-id=12345678;user-type=staff :ronni!ronni@ronni.tmi.twitch.tv PRIVMSG #ronni :cheer100"
        val expected = PrivMsg(
          badgeInfo = Map.empty,
          badges = Map("staff" -> "1", "bits" -> "1000"),
          color = "",
          displayName = "ronni",
          emoteOnly = false,
          emotes = Map.empty,
          firstMessage = false,
          mod = false,
          returningChatter = false,
          roomId = 12345678,
          subscriber = false,
          tmiSentTimestamp = OffsetDateTime.of(LocalDateTime.of(2017, 10, 5, 23, 36, 12, 675 * 1000 * 1000), ZoneOffset.UTC),
          turbo = false,
          userId = 12345678,
          userType = TwitchUserType.TwitchEmployee,
          vip = false,
          userName = "ronni",
          channelName = "ronni",
          message = "cheer100",
          bits = 100,
          id = Some("b34ccfc7-4977-403a-8a94-33c6bac34fb8")
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "A VIP sent the message" in {
        val message = "@badge-info=;badges=vip/1,partner/1;client-nonce=cd15335a5e2059c3b087e22612de485e;color=;display-name=fun2bfun;emotes=;first-msg=0;flags=;id=1fd20412-965f-4c96-beb3-52266448f564;mod=0;returning-chatter=0;room-id=102336968;subscriber=0;tmi-sent-ts=1661372052425;turbo=0;user-id=12345678;user-type=;vip=1 :ronni!ronni@ronni.tmi.twitch.tv PRIVMSG #dallas :PogChamp"
        val expected = PrivMsg(
          badgeInfo = Map.empty,
          badges = Map("vip" -> "1", "partner" -> "1"),
          color = "",
          displayName = "fun2bfun",
          emoteOnly = false,
          emotes = Map.empty,
          firstMessage = false,
          mod = false,
          returningChatter = false,
          roomId = 102336968,
          subscriber = false,
          tmiSentTimestamp = OffsetDateTime.of(LocalDateTime.of(2022, 8, 24, 20, 14, 12, 425 * 1000 * 1000), ZoneOffset.UTC),
          turbo = false,
          userId = 12345678,
          userType = TwitchUserType.NormalUser,
          vip = true,
          userName = "ronni",
          channelName = "dallas",
          message = "PogChamp",
          clientNonce = Some("cd15335a5e2059c3b087e22612de485e"),
          id = Some("1fd20412-965f-4c96-beb3-52266448f564"),
          flags = Some("")
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "A user sent a Hype Chat of $2 USD" in {
        val message = "@badge-info=;badges=glhf-pledge/1;color=;emotes=;first-msg=0;flags=;id=f6fb34f8-562f-4b4d-b628-32113d0ef4b0;mod=0;pinned-chat-paid-amount=200;pinned-chat-paid-canonical-amount=200;pinned-chat-paid-currency=USD;pinned-chat-paid-exponent=2;pinned-chat-paid-is-system-message=0;pinned-chat-paid-level=ONE;returning-chatter=0;room-id=12345678;subscriber=0;tmi-sent-ts=1687471984306;turbo=0;user-id=12345678;user-type= :ronni!ronni@ronni.tmi.twitch.tv PRIVMSG #dallas :Great job! SeemsGood"
        val expected = PrivMsg(
          badgeInfo = Map.empty,
          badges = Map("glhf-pledge" -> "1"),
          color = "",
          displayName = "ronni",
          emoteOnly = false,
          emotes = Map.empty,
          firstMessage = false,
          mod = false,
          returningChatter = false,
          roomId = 12345678,
          subscriber = false,
          tmiSentTimestamp = OffsetDateTime.of(LocalDateTime.of(2023, 6, 22, 22, 13, 4, 306 * 1000 * 1000), ZoneOffset.UTC),
          turbo = false,
          userId = 12345678,
          userType = TwitchUserType.NormalUser,
          vip = false,
          userName = "ronni",
          channelName = "dallas",
          message = "Great job! SeemsGood",
          flags = Some(""),
          id = Some("f6fb34f8-562f-4b4d-b628-32113d0ef4b0"),
          hypeChat = Some(PrivMsg.HypeChat(
            amount = 200,
            currency = "USD",
            exponent = 2,
            level = "ONE",
            isSystemMessage = false,
            paidCanonicalAmount = Some(200)
          ))
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "A user sent a Hype Chat of ₩1500 KRW" in {
        val message = "@badge-info=;badges=partner/1;color=;emotes=;first-msg=0;flags=;id=bf4a779b-e26f-4688-8dcc-c41f221d8bbe;mod=0;pinned-chat-paid-amount=1500;pinned-chat-paid-canonical-amount=1500;pinned-chat-paid-currency=KRW;pinned-chat-paid-exponent=0;pinned-chat-paid-is-system-message=0;pinned-chat-paid-level=ONE;returning-chatter=0;room-id=12345678;subscriber=0;tmi-sent-ts=1687474201232;turbo=0;user-id=12345678;user-type= :ronni!ronni@ronni.tmi.twitch.tv PRIVMSG #dallas :POGGERS"
        val expected = PrivMsg(
          badgeInfo = Map.empty,
          badges = Map("partner" -> "1"),
          color = "",
          displayName = "ronni",
          emoteOnly = false,
          emotes = Map.empty,
          firstMessage = false,
          mod = false,
          returningChatter = false,
          roomId = 12345678,
          subscriber = false,
          tmiSentTimestamp = OffsetDateTime.of(LocalDateTime.of(2023, 6, 22, 22, 50, 1, 232 * 1000 * 1000), ZoneOffset.UTC),
          turbo = false,
          userId = 12345678,
          userType = TwitchUserType.NormalUser,
          vip = false,
          userName = "ronni",
          channelName = "dallas",
          message = "POGGERS",
          flags = Some(""),
          id = Some("bf4a779b-e26f-4688-8dcc-c41f221d8bbe"),
          hypeChat = Some(PrivMsg.HypeChat(
            amount = 1500,
            currency = "KRW",
            exponent = 0,
            level = "ONE",
            isSystemMessage = false,
            paidCanonicalAmount = Some(1500)
          ))
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "Real IRC emote only message (pedWave) from HivePhaser" in {
        val message = "@badge-info=;badges=moderator/1,bits-leader/2;client-nonce=aa307b6c4561ecdf888ea79a5357761f;color=#1E90FF;display-name=HivePhaser;emote-only=1;emotes=emotesv2_ee553b5d420d4e059b10c4ceae0ac9af:0-6;first-msg=0;flags=;id=7ced725b-05f7-4ba8-9bd1-a53eb8adb27e;mod=1;returning-chatter=0;room-id=147113965;subscriber=0;tmi-sent-ts=1720125031509;turbo=0;user-id=143401071;user-type=mod :hivephaser!hivephaser@hivephaser.tmi.twitch.tv PRIVMSG #archimond7450 :pedWave"
        val expected = PrivMsg(
          badgeInfo = Map.empty,
          badges = Map("moderator" -> "1", "bits-leader" -> "2"),
          color = "#1E90FF",
          displayName = "HivePhaser",
          emoteOnly = true,
          emotes = Map("emotesv2_ee553b5d420d4e059b10c4ceae0ac9af" -> List((0, 6))),
          firstMessage = false,
          mod = true,
          returningChatter = false,
          roomId = 147113965,
          subscriber = false,
          tmiSentTimestamp = OffsetDateTime.of(LocalDateTime.of(2024, 7, 4, 20, 30, 31, 509 * 1000 * 1000), ZoneOffset.UTC),
          turbo = false,
          userId = 143401071,
          userType = TwitchUserType.Moderator,
          vip = false,
          userName = "hivephaser",
          channelName = "archimond7450",
          message = "pedWave",
          clientNonce = Some("aa307b6c4561ecdf888ea79a5357761f"),
          id = Some("7ced725b-05f7-4ba8-9bd1-a53eb8adb27e"),
          flags = Some(""),
          hypeChat = None,
          reply = None
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "Real IRC message with an interesting badge" in {
        val message = "@badge-info=predictions/Tyrall;badges=predictions/blue-2;client-nonce=13ba73618cef6998f7a6aa2b88427a9f;color=#B22222;display-name=kekvit;emotes=;first-msg=0;flags=;id=3a88305c-1f08-4e82-abed-a1d3c40f8b69;mod=0;returning-chatter=0;room-id=23693840;subscriber=0;tmi-sent-ts=1720189737349;turbo=0;user-id=73480864;user-type= :kekvit!kekvit@kekvit.tmi.twitch.tv PRIVMSG #wtii :@tyrrall 20k points on you, make me proud"
        val expected = PrivMsg(
          badgeInfo = Map("predictions" -> "Tyrall"),
          badges = Map("predictions" -> "blue-2"),
          color = "#B22222",
          displayName = "kekvit",
          emoteOnly = false,
          emotes = Map.empty,
          firstMessage = false,
          mod = false,
          returningChatter = false,
          roomId = 23693840,
          subscriber = false,
          tmiSentTimestamp = OffsetDateTime.of(LocalDateTime.of(2024, 7, 5, 14, 28, 57, 349 * 1000 * 1000), ZoneOffset.UTC),
          turbo = false,
          userId = 73480864,
          userType = TwitchUserType.NormalUser,
          vip = false,
          userName = "kekvit",
          channelName = "wtii",
          message = "@tyrrall 20k points on you, make me proud",
          clientNonce = Some("13ba73618cef6998f7a6aa2b88427a9f"),
          id = Some("3a88305c-1f08-4e82-abed-a1d3c40f8b69"),
          flags = Some(""),
          hypeChat = None,
          reply = None
        )
        val actual = incomingMessageDecoder.decode(message)
        expected shouldEqual actual
      }
    }

    "correctly decode the Reconnect message" in {
      incomingMessageDecoder.decode(":tmi.twitch.tv RECONNECT") shouldEqual Reconnect
    }

    "correctly decode the RoomState message" when {
      "Received chat room's settings after the chatbot joined" in {
        val message = "@emote-only=0;followers-only=0;r9k=0;slow=0;subs-only=0 :tmi.twitch.tv ROOMSTATE #dallas"
        val expected = RoomState(
          emoteOnly = Some(false),
          followersOnly = Some(0),
          r9k = Some(false),
          roomId = None,
          slow = Some(0),
          subsOnly = Some(false),
          channelName = "dallas"
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "Moderator turned on slow mode requiring users to wait 10 seconds between sending messages" in {
        val message = "@slow=10 :tmi.twitch.tv ROOMSTATE #dallas"
        val expected = RoomState(
          channelName = "dallas",
          slow = Some(10),
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }
    }

    "correctly decode the UserNotice message" when {
      "ronni resubscribed to the dallas channel" in {
        val message = "@badge-info=;badges=staff/1,broadcaster/1,turbo/1;color=#008000;display-name=ronni;emotes=;id=db25007f-7a18-43eb-9379-80131e44d633;login=ronni;mod=0;msg-id=resub;msg-param-cumulative-months=6;msg-param-streak-months=2;msg-param-should-share-streak=1;msg-param-sub-plan=Prime;msg-param-sub-plan-name=Prime;room-id=12345678;subscriber=1;system-msg=ronni\\shas\\ssubscribed\\sfor\\s6\\smonths!;tmi-sent-ts=1507246572675;turbo=1;user-id=87654321;user-type=staff :tmi.twitch.tv USERNOTICE #dallas :Great stream -- keep it up!"
        val expected = UserNotice.Resub(
          cumulativeMonths = 6,
          streakMonths = 2,
          shouldShareStreak = true,
          subPlan = "Prime",
          subPlanName = "Prime",
          message = "Great stream -- keep it up!",
          userNotice = UserNotice.Common(
            badgeInfo = Map.empty,
            badges = Map("staff" -> "1", "broadcaster" -> "1", "turbo" -> "1"),
            color = "#008000",
            displayName = "ronni",
            emotes = Map.empty,
            id = "db25007f-7a18-43eb-9379-80131e44d633",
            login = "ronni",
            mod = false,
            roomId = 12345678,
            subscriber = true,
            systemMessage = "ronni has subscribed for 6 months!",
            tmiSentTimestamp = OffsetDateTime.of(LocalDateTime.of(2017, 10, 5, 23, 36, 12, 675 * 1000 * 1000), ZoneOffset.UTC),
            turbo = true,
            userId = 87654321,
            userType = TwitchUserType.TwitchEmployee,
            vip = false,
            channelName = "dallas"
          )
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "tww2 gifted a subscription to Mr_Woodchuck in forstycup’s channel" in {
        val message = "@badge-info=;badges=staff/1,premium/1;color=#0000FF;display-name=TWW2;emotes=;id=e9176cd8-5e22-4684-ad40-ce53c2561c5e;login=tww2;mod=0;msg-id=subgift;msg-param-months=1;msg-param-recipient-display-name=Mr_Woodchuck;msg-param-recipient-id=55554444;msg-param-recipient-user-name=mr_woodchuck;msg-param-sub-plan-name=House\\sof\\sNyoro~n;msg-param-sub-plan=1000;room-id=19571752;subscriber=0;system-msg=TWW2\\sgifted\\sa\\sTier\\s1\\ssub\\sto\\sMr_Woodchuck!;tmi-sent-ts=1521159445153;turbo=0;user-id=87654321;user-type=staff :tmi.twitch.tv USERNOTICE #forstycup"
        val expected = UserNotice.SubGift(
          months = 1,
          recipientDisplayName = "Mr_Woodchuck",
          recipientId = 55554444,
          recipientUserName = "mr_woodchuck",
          subPlan = "1000",
          subPlanName = "House of Nyoro~n",
          giftMonths = 1,
          userNotice = UserNotice.Common(
            badgeInfo = Map.empty,
            badges = Map("staff" -> "1", "premium" -> "1"),
            color = "#0000FF",
            displayName = "TWW2",
            emotes = Map.empty,
            id = "e9176cd8-5e22-4684-ad40-ce53c2561c5e",
            login = "tww2",
            mod = false,
            roomId = 19571752,
            subscriber = false,
            systemMessage = "TWW2 gifted a Tier 1 sub to Mr_Woodchuck!",
            tmiSentTimestamp = OffsetDateTime.of(LocalDateTime.of(2018, 3, 16, 0, 17, 25, 153 * 1000 * 1000), ZoneOffset.UTC),
            turbo = false,
            userId = 87654321,
            userType = TwitchUserType.TwitchEmployee,
            vip = false,
            channelName = "forstycup",
          )
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "broadcaster from TestChannel raided the channel OtherTestChannel" in {
        val message = "@badge-info=;badges=turbo/1;color=#9ACD32;display-name=TestChannel;emotes=;id=3d830f12-795c-447d-af3c-ea05e40fbddb;login=testchannel;mod=0;msg-id=raid;msg-param-displayName=TestChannel;msg-param-login=testchannel;msg-param-viewerCount=15;room-id=33332222;subscriber=0;system-msg=15\\sraiders\\sfrom\\sTestChannel\\shave\\sjoined\\n!;tmi-sent-ts=1507246572675;turbo=1;user-id=123456;user-type= :tmi.twitch.tv USERNOTICE #othertestchannel"
        val expected = UserNotice.Raid(
          displayName = "TestChannel",
          login = "testchannel",
          viewerCount = 15,
          userNotice = UserNotice.Common(
            badgeInfo = Map.empty,
            badges = Map("turbo" -> "1"),
            color = "#9ACD32",
            displayName = "TestChannel",
            emotes = Map.empty,
            id = "3d830f12-795c-447d-af3c-ea05e40fbddb",
            login = "testchannel",
            mod = false,
            roomId = 33332222,
            subscriber = false,
            systemMessage = "15 raiders from TestChannel have joined\n!",
            tmiSentTimestamp = OffsetDateTime.of(LocalDateTime.of(2017, 10, 5, 23, 36, 12, 675 * 1000 * 1000), ZoneOffset.UTC),
            turbo = true,
            userId = 123456,
            userType = TwitchUserType.NormalUser,
            vip = false,
            channelName = "othertestchannel"
          )
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "new_chatter ritual for SevenTest1 in seventoes channel" in {
        val message = "@badge-info=;badges=;color=;display-name=SevenTest1;emotes=30259:0-6;id=37feed0f-b9c7-4c3a-b475-21c6c6d21c3d;login=seventest1;mod=0;msg-id=ritual;msg-param-ritual-name=new_chatter;room-id=87654321;subscriber=0;system-msg=Seventoes\\sis\\snew\\shere!;tmi-sent-ts=1508363903826;turbo=0;user-id=77776666;user-type= :tmi.twitch.tv USERNOTICE #seventoes :HeyGuys"
        val expected = UserNotice.Ritual.NewChatter(
          message = "HeyGuys",
          userNotice = UserNotice.Common(
            badgeInfo = Map.empty,
            badges = Map.empty,
            color = "",
            displayName = "SevenTest1",
            emotes = Map("30259" -> List((0, 6))),
            id = "37feed0f-b9c7-4c3a-b475-21c6c6d21c3d",
            login = "seventest1",
            mod = false,
            roomId = 87654321,
            subscriber = false,
            systemMessage = "Seventoes is new here!",
            tmiSentTimestamp = OffsetDateTime.of(LocalDateTime.of(2017, 10, 18, 21, 58, 23, 826 * 1000 * 1000), ZoneOffset.UTC),
            turbo = false,
            userId = 77776666,
            userType = TwitchUserType.NormalUser,
            vip = false,
            channelName = "seventoes"
          )
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "Real_Poke is gifting 3 Tier 1 Subs to Wtii's community" in {
        val message = "@badge-info=subscriber/3;badges=subscriber/0,hype-train/1;color=#008000;display-name=Real_Poke;emotes=;flags=;id=ba53e16d-1daa-458e-b7c5-1db2e712d3af;login=real_poke;mod=0;msg-id=submysterygift;msg-param-community-gift-id=994662594283314594;msg-param-goal-contribution-type=SUB_POINTS;msg-param-goal-current-contributions=420;msg-param-goal-target-contributions=400;msg-param-goal-user-contributions=3;msg-param-mass-gift-count=3;msg-param-origin-id=994662594283314594;msg-param-sender-count=45;msg-param-sub-plan=1000;room-id=23693840;subscriber=1;system-msg=Real_Poke\\sis\\sgifting\\s3\\sTier\\s1\\sSubs\\sto\\sWTii's\\scommunity!\\sThey've\\sgifted\\sa\\stotal\\sof\\s45\\sin\\sthe\\schannel!;tmi-sent-ts=1711548773585;user-id=44296768;user-type=;vip=0 :tmi.twitch.tv USERNOTICE #wtii"
        val expected = UserNotice.SubMysteryGift(
          communityGiftId = 994662594283314594L,
          goalContributionType = "SUB_POINTS",
          goalCurrentContributions = 420,
          goalTargetContributions = 400,
          goalUserContributions = 3,
          massGiftCount = 3,
          originId = 994662594283314594L,
          senderCount = 45,
          subPlan = "1000",
          userNotice = UserNotice.Common(
            badgeInfo = Map("subscriber" -> "3"),
            badges = Map("subscriber" -> "0", "hype-train" -> "1"),
            color = "#008000",
            displayName = "Real_Poke",
            emotes = Map.empty,
            id = "ba53e16d-1daa-458e-b7c5-1db2e712d3af",
            login = "real_poke",
            mod = false,
            roomId = 23693840,
            subscriber = true,
            systemMessage = "Real_Poke is gifting 3 Tier 1 Subs to WTii's community! They've gifted a total of 45 in the channel!",
            tmiSentTimestamp = OffsetDateTime.of(LocalDateTime.of(2024, 3, 27, 14, 12, 53, 585 * 1000 * 1000), ZoneOffset.UTC),
            turbo = false,
            userId = 44296768,
            userType = TwitchUserType.NormalUser,
            vip = false,
            channelName = "wtii"
          )
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "aDith_0 watched 35 consecutive streams" in {
        val message = "@badge-info=subscriber/5;badges=subscriber/0,rplace-2023/1;color=#00FF7F;display-name=aDith_0;emotes=970925:0-6/1042896:8-18,20-30,32-42;flags=;id=14de0dfe-735f-4594-9108-90a32bf11ceb;login=adith_0;mod=0;msg-id=viewermilestone;msg-param-category=watch-streak;msg-param-copoReward=450;msg-param-id=f4dc6fe8-4009-4e2f-9fc7-e92cba56691b;msg-param-value=35;room-id=23693840;subscriber=1;system-msg=aDith_0\\swatched\\s35\\sconsecutive\\sstreams\\sthis\\smonth\\sand\\ssparked\\sa\\swatch\\sstreak!;tmi-sent-ts=1711293179643;user-id=548448379;user-type=;vip=0 :tmi.twitch.tv USERNOTICE #wtii :wtiiSpy wtiiTabaho1 wtiiTabaho1 wtiiTabaho1"
        val expected = UserNotice.ViewerMilestone.WatchStreak(
          copoReward = 450,
          id = "f4dc6fe8-4009-4e2f-9fc7-e92cba56691b",
          value = 35,
          message = "wtiiSpy wtiiTabaho1 wtiiTabaho1 wtiiTabaho1",
          userNotice = UserNotice.Common(
            badgeInfo = Map("subscriber" -> "5"),
            badges = Map("subscriber" -> "0", "rplace-2023" -> "1"),
            color = "#00FF7F",
            displayName = "aDith_0",
            emotes = Map("970925" -> List((0, 6)), "1042896" -> List((8, 18), (20, 30), (32, 42))),
            id = "14de0dfe-735f-4594-9108-90a32bf11ceb",
            login = "adith_0",
            mod = false,
            roomId = 23693840,
            subscriber = true,
            systemMessage = "aDith_0 watched 35 consecutive streams this month and sparked a watch streak!",
            tmiSentTimestamp = OffsetDateTime.of(LocalDateTime.of(2024, 3, 24, 15, 12, 59, 643 * 1000 * 1000), ZoneOffset.UTC),
            turbo = false,
            userId = 548448379,
            userType = TwitchUserType.NormalUser,
            vip = false,
            channelName = "wtii"
          )
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "Real_Poke is paying forward the Gift they got from croyvile to maxrightder" in {
        val message = "@badge-info=subscriber/3;badges=subscriber/0;color=#008000;display-name=Real_Poke;emotes=;flags=;id=0d4e779c-11e7-44ad-8aab-cea7fbdf4b02;login=real_poke;mod=0;msg-id=standardpayforward;msg-param-prior-gifter-anonymous=false;msg-param-prior-gifter-display-name=croyvile;msg-param-prior-gifter-id=113804728;msg-param-prior-gifter-user-name=croyvile;msg-param-recipient-display-name=maxrightder;msg-param-recipient-id=733645508;msg-param-recipient-user-name=maxrightder;room-id=23693840;subscriber=1;system-msg=Real_Poke\\sis\\spaying\\sforward\\sthe\\sGift\\sthey\\sgot\\sfrom\\scroyvile\\sto\\smaxrightder!;tmi-sent-ts=1711206920479;user-id=44296768;user-type=;vip=0 :tmi.twitch.tv USERNOTICE #wtii"
        val expected = UserNotice.StandardPayForward(
          priorGifterAnonymous = false,
          priorGifterDisplayname = "croyvile",
          priorGifterId = 113804728,
          priorGifterUserName = "croyvile",
          recipientDisplayName = "maxrightder",
          recipientId = 733645508,
          recipientUserName = "maxrightder",
          userNotice = UserNotice.Common(
            badgeInfo = Map("subscriber" -> "3"),
            badges = Map("subscriber" -> "0"),
            color = "#008000",
            displayName = "Real_Poke",
            emotes = Map.empty,
            id = "0d4e779c-11e7-44ad-8aab-cea7fbdf4b02",
            login = "real_poke",
            mod = false,
            roomId = 23693840,
            subscriber = true,
            systemMessage = "Real_Poke is paying forward the Gift they got from croyvile to maxrightder!",
            tmiSentTimestamp = OffsetDateTime.of(LocalDateTime.of(2024, 3, 23, 15, 15, 20, 479 * 1000 * 1000), ZoneOffset.UTC),
            turbo = false,
            userId = 44296768,
            userType = TwitchUserType.NormalUser,
            vip = false,
            channelName = "wtii"
          )
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }
    }

    "correctly decode the UserState message" when {
      "ronni joined the dallas channel" in {
        val message = "@badge-info=;badges=staff/1;color=#0D4200;display-name=ronni;emote-sets=0,33,50,237,793,2126,3517,4578,5569,9400,10337,12239;mod=1;subscriber=1;turbo=1;user-type=staff :tmi.twitch.tv USERSTATE #dallas"
        val expected = UserState(
          badgeInfo = Map.empty,
          badges = Map("staff" -> "1"),
          color = "#0D4200",
          displayName = "ronni",
          emoteSets = List(0, 33, 50, 237, 793, 2126, 3517, 4578, 5569, 9400, 10337, 12239),
          mod = true,
          subscriber = true,
          turbo = true,
          userType = TwitchUserType.TwitchEmployee,
          channelName = "dallas"
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }
    }

    "correctly decode the Whisper message" when {
      "foo sent a hello whisper message to petsgomoo (chatbot)" in {
        val message = "@badges=staff/1,bits-charity/1;color=#8A2BE2;display-name=PetsgomOO;emotes=;message-id=306;thread-id=12345678_87654321;turbo=0;user-id=87654321;user-type=staff :petsgomoo!petsgomoo@petsgomoo.tmi.twitch.tv WHISPER foo :hello"
        val expected = Whisper(
          badges = Map("staff" -> "1", "bits-charity" -> "1"),
          color = "#8A2BE2",
          displayName = "PetsgomOO",
          emotes = Map.empty,
          messageId = 306,
          threadId = "12345678_87654321",
          turbo = false,
          userId = 87654321,
          userType = TwitchUserType.TwitchEmployee,
          toUser = "petsgomoo",
          fromUser = "foo",
          message = "hello"
        )
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }
    }

    "correctly decode the Early Notice message" when {
      "login authentication failed" in {
        val message = ":tmi.twitch.tv NOTICE * :Login authentication failed"
        val expected = EarlyNotice("Login authentication failed")
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "authentication was not formatted properly" in {
        val message = ":tmi.twitch.tv NOTICE * :Improperly formatted auth"
        val expected = EarlyNotice("Improperly formatted auth")
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }
    }

    "correctly decode the response to Capability Request message" when {
      "capabilities are acknowledged" in {
        val message = ":tmi.twitch.tv CAP * ACK :twitch.tv/membership twitch.tv/tags twitch.tv/commands"
        val expected = CapabilityRequestResponse(succeeded = true, Set("twitch.tv/membership", "twitch.tv/tags", "twitch.tv/commands"))
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "capabilities are not acknowledged" in {
        val message = ":tmi.twitch.tv CAP * NAK :twitch.tv/foo"
        val expected = CapabilityRequestResponse(succeeded = false, Set("twitch.tv/foo"))
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }
    }

    "correctly decode the parts of response to successful authentication" when {
      "Numeric Message 001 is received" in {
        val message = ":tmi.twitch.tv 001 <user> :Welcome, GLHF!"
        val expected = NumericMessage(1, "<user>", "Welcome, GLHF!")
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "Numeric Message 002 is received" in {
        val message = ":tmi.twitch.tv 002 <user> :Your host is tmi.twitch.tv"
        val expected = NumericMessage(2, "<user>", "Your host is tmi.twitch.tv")
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "Numeric Message 003 is received" in {
        val message = ":tmi.twitch.tv 003 <user> :This server is rather new"
        val expected = NumericMessage(3, "<user>", "This server is rather new")
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "Numeric Message 004 is received" in {
        val message = ":tmi.twitch.tv 004 <user> :-"
        val expected = NumericMessage(4, "<user>", "-")
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "Numeric Message 375 is received" in {
        val message = ":tmi.twitch.tv 375 <user> :-"
        val expected = NumericMessage(375, "<user>", "-")
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "Numeric Message 372 is received" in {
        val message = ":tmi.twitch.tv 372 <user> :You are in a maze of twisty passages."
        val expected = NumericMessage(372, "<user>", "You are in a maze of twisty passages.")
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }

      "Numeric Message 376 is received" in {
        val message = ":tmi.twitch.tv 376 <user> :>"
        val expected = NumericMessage(376, "<user>", ">")
        val actual = incomingMessageDecoder.decode(message)
        actual shouldEqual expected
      }
    }
  }
}