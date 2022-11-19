using Xunit;
using ArchieMate.TwitchIRC.Messages.Incoming;
using BadgesClass = ArchieMate.TwitchIRC.Badges;
using System.Collections.Generic;
using System;
using UserTypesClass = ArchieMate.TwitchIRC.UserTypes.Types;

namespace ArchieMate.TwitchIRC.UnitTests.Messages.Incoming;

public class UserNoticeTests
{
    internal static readonly string GenericResub = "@badge-info=;badges=staff/1,broadcaster/1,turbo/1;color=#008000;display-name=ronni;emotes=;id=db25007f-7a18-43eb-9379-80131e44d633;login=ronni;mod=0;msg-id=resub;msg-param-cumulative-months=6;msg-param-streak-months=2;msg-param-should-share-streak=1;msg-param-sub-plan=Prime;msg-param-sub-plan-name=Prime;room-id=12345678;subscriber=1;system-msg=ronni\\shas\\ssubscribed\\sfor\\s6\\smonths!;tmi-sent-ts=1507246572675;turbo=1;user-id=87654321;user-type=staff :tmi.twitch.tv USERNOTICE #dallas :Great stream -- keep it up!";
    internal static readonly string GenericGiftSub = "@badge-info=;badges=staff/1,premium/1;color=#0000FF;display-name=TWW2;emotes=;id=e9176cd8-5e22-4684-ad40-ce53c2561c5e;login=tww2;mod=0;msg-id=subgift;msg-param-months=1;msg-param-recipient-display-name=Mr_Woodchuck;msg-param-recipient-id=55554444;msg-param-recipient-user-name=mr_woodchuck;msg-param-sub-plan-name=House\\sof\\sNyoro~n;msg-param-sub-plan=1000;room-id=19571752;subscriber=0;system-msg=TWW2\\sgifted\\sa\\sTier\\s1\\ssub\\sto\\sMr_Woodchuck!;tmi-sent-ts=1521159445153;turbo=0;user-id=87654321;user-type=staff :tmi.twitch.tv USERNOTICE #forstycup";
    internal static readonly string GenericRaid = "@badge-info=;badges=turbo/1;color=#9ACD32;display-name=TestChannel;emotes=;id=3d830f12-795c-447d-af3c-ea05e40fbddb;login=testchannel;mod=0;msg-id=raid;msg-param-displayName=TestChannel;msg-param-login=testchannel;msg-param-viewerCount=15;room-id=33332222;subscriber=0;system-msg=15\\sraiders\\sfrom\\sTestChannel\\shave\\sjoined\\n!;tmi-sent-ts=1507246572675;turbo=1;user-id=123456;user-type= :tmi.twitch.tv USERNOTICE #othertestchannel";
    internal static readonly string GenericRitual = "@badge-info=;badges=;color=;display-name=SevenTest1;emotes=30259:0-6;id=37feed0f-b9c7-4c3a-b475-21c6c6d21c3d;login=seventest1;mod=0;msg-id=ritual;msg-param-ritual-name=new_chatter;room-id=87654321;subscriber=0;system-msg=Seventoes\\sis\\snew\\shere!;tmi-sent-ts=1508363903826;turbo=0;user-id=77776666;user-type= :tmi.twitch.tv USERNOTICE #seventoes :HeyGuys";
    internal static readonly string RealSub = "@badge-info=subscriber/1;badges=subscriber/0,premium/1;color=#FF0000;display-name=ffez_xd;emotes=;flags=;id=bea7f871-05fb-4827-bfe7-d3a0790d0819;login=ffez_xd;mod=0;msg-id=sub;msg-param-cumulative-months=1;msg-param-goal-contribution-type=SUB_POINTS;msg-param-goal-current-contributions=424;msg-param-goal-target-contributions=430;msg-param-goal-user-contributions=1;msg-param-months=0;msg-param-multimonth-duration=0;msg-param-multimonth-tenure=0;msg-param-should-share-streak=0;msg-param-sub-plan-name=Channel\\sSubscription\\s(WTii);msg-param-sub-plan=Prime;msg-param-was-gifted=false;room-id=23693840;subscriber=1;system-msg=ffez_xd\\ssubscribed\\swith\\sPrime.;tmi-sent-ts=1649340405346;user-id=600173591;user-type= :tmi.twitch.tv USERNOTICE #wtii";
    internal static readonly string RealResub = "@badge-info=subscriber/55;badges=moderator/1,subscriber/24;color=#1E90FF;display-name=frederikct112;emotes=;flags=;id=e2c0603d-696a-42ef-8d95-082ca8c64cb0;login=frederikct112;mod=1;msg-id=resub;msg-param-cumulative-months=55;msg-param-months=0;msg-param-multimonth-duration=0;msg-param-multimonth-tenure=0;msg-param-should-share-streak=0;msg-param-sub-plan-name=Channel\\sSubscription\\s(WTii);msg-param-sub-plan=1000;msg-param-was-gifted=false;room-id=23693840;subscriber=1;system-msg=frederikct112\\ssubscribed\\sat\\sTier\\s1.\\sThey've\\ssubscribed\\sfor\\s55\\smonths!;tmi-sent-ts=1649344276182;user-id=23489192;user-type=mod :tmi.twitch.tv USERNOTICE #wtii :Xdxdxdxdxdxdxd 4v4 allies lol lol";
    internal static readonly string RealGiftSub = "@badge-info=subscriber/30;badges=subscriber/24,premium/1;color=#BECCF2;display-name=the_whiteagle;emotes=;flags=;id=a03d07d4-98b3-4a87-8ac9-cb12841225bd;login=the_whiteagle;mod=0;msg-id=subgift;msg-param-gift-months=1;msg-param-goal-contribution-type=SUB_POINTS;msg-param-goal-current-contributions=425;msg-param-goal-target-contributions=430;msg-param-goal-user-contributions=1;msg-param-months=7;msg-param-origin-id=b4\\sb7\\scf\\sb7\\s42\\s31\\sbc\\se0\\saf\\sb1\\sc2\\s71\\sc3\\s86\\s8c\\sf4\\s1c\\se3\\sf8\\s92;msg-param-recipient-display-name=BaledorTheBold;msg-param-recipient-id=158464075;msg-param-recipient-user-name=baledorthebold;msg-param-sender-count=3;msg-param-sub-plan-name=Channel\\sSubscription\\s(WTii);msg-param-sub-plan=1000;room-id=23693840;subscriber=1;system-msg=the_whiteagle\\sgifted\\sa\\sTier\\s1\\ssub\\sto\\sBaledorTheBold!\\sThey\\shave\\sgiven\\s3\\sGift\\sSubs\\sin\\sthe\\schannel!;tmi-sent-ts=1649351974273;user-id=105745602;user-type= :tmi.twitch.tv USERNOTICE #wtii";
    internal static readonly string RealThijsSub = "@badge-info=;badges=premium/1;color=;display-name=Jibbies512;emotes=;flags=;id=72f1fe68-9430-474a-809c-17b30838577a;login=jibbies512;mod=0;msg-id=sub;msg-param-cumulative-months=1;msg-param-goal-contribution-type=SUBS;msg-param-goal-current-contributions=1573;msg-param-goal-description=Fall\\sGuys\\swith\\schat;msg-param-goal-target-contributions=2000;msg-param-goal-user-contributions=1;msg-param-months=0;msg-param-multimonth-duration=1;msg-param-multimonth-tenure=0;msg-param-should-share-streak=0;msg-param-sub-plan-name=Channel\\sSubscription\\s(thijshs);msg-param-sub-plan=Prime;msg-param-was-gifted=false;room-id=57025612;subscriber=1;system-msg=Jibbies512\\ssubscribed\\swith\\sPrime.;tmi-sent-ts=1660206347025;user-id=145112849;user-type= :tmi.twitch.tv USERNOTICE #thijs";

    [Fact]
    public void NullMessageCorrectlyThrowsException()
    {
        GenericMessageTestsHelper.TestNullArgumentException<UserNotice>();
    }

    [Fact]
    public void GenericUserNoticeResubMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = GenericResub;
        var expectedProps = new
        {
            BadgeInfo = new Dictionary<string, string>(),
            Badges = new Dictionary<string, string>
            {
                {BadgesClass.Staff, "1"},
                {BadgesClass.Broadcaster, "1"},
                {BadgesClass.Turbo, "1"}
            },
            Color = "#008000",
            DisplayName = "ronni",
            Emotes = new List<Emotes.Info>(),
            MessageId = new Guid("db25007f-7a18-43eb-9379-80131e44d633"),
            Login = "ronni",
            Moderator = false,
            RoomId = 12345678,
            Subscriber = true,
            SystemMessage = "ronni has subscribed for 6 months!",
            Timestamp = new DateTimeOffset(2017, 10, 5, 23, 36, 12, 675, TimeSpan.Zero),
            Turbo = true,
            UserId = 87654321,
            UserType = UserTypesClass.TwitchEmployee,
            Channel = "dallas",
            Message = "Great stream -- keep it up!",
            Details = new
            {
                CumulativeMonths = 6,
                StreakMonths = 2,
                ShouldShareStreak = true,
                Plan = UserNotice.SubPlanType.Prime,
                PlanName = "Prime"
            }
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<UserNotice>(message, expectedProps);
    }

    [Fact]
    public void GenericUserNoticeGiftSubMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = GenericGiftSub;
        var expectedProps = new
        {
            BadgeInfo = new Dictionary<string, string>(),
            Badges = new Dictionary<string, string>
            {
                {BadgesClass.Staff, "1"},
                {BadgesClass.Premium, "1"}
            },
            Color = "#0000FF",
            DisplayName = "TWW2",
            Emotes = new List<Emotes.Info>(),
            MessageId = new Guid("e9176cd8-5e22-4684-ad40-ce53c2561c5e"),
            Login = "tww2",
            Moderator = false,
            RoomId = 19571752,
            Subscriber = false,
            SystemMessage = "TWW2 gifted a Tier 1 sub to Mr_Woodchuck!",
            Timestamp = new DateTimeOffset(2018, 3, 16, 0, 17, 25, 153, TimeSpan.Zero),
            Turbo = false,
            UserId = 87654321,
            UserType = UserTypesClass.TwitchEmployee,
            Channel = "forstycup",
            Message = string.Empty,
            Details = new
            {
                CumulativeMonths = 1,
                RecipientDisplayName = "Mr_Woodchuck",
                RecipientId = 55554444,
                RecipientUserName = "mr_woodchuck",
                Plan = UserNotice.SubPlanType.Tier1,
                PlanName = "House of Nyoro~n",
                MonthsGifted = 1
            }
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<UserNotice>(message, expectedProps);
    }

    [Fact]
    public void GenericUserNoticeRaidMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = GenericRaid;
        var expectedProps = new
        {
            BadgeInfo = new Dictionary<string, string>(),
            Badges = new Dictionary<string, string>
            {
                {BadgesClass.Turbo, "1"}
            },
            Color = "#9ACD32",
            DisplayName = "TestChannel",
            Emotes = new List<Emotes.Info>(),
            MessageId = new Guid("3d830f12-795c-447d-af3c-ea05e40fbddb"),
            Login = "testchannel",
            Moderator = false,
            RoomId = 33332222,
            Subscriber = false,
            SystemMessage = "15 raiders from TestChannel have joined\n!",
            Timestamp = new DateTimeOffset(2017, 10, 5, 23, 36, 12, 675, TimeSpan.Zero),
            Turbo = true,
            UserId = 123456,
            UserType = UserTypesClass.NormalUser,
            Channel = "othertestchannel",
            Message = string.Empty,
            Details = new
            {
                RaiderDisplayName = "TestChannel",
                RaiderLogin = "testchannel",
                ViewerCount = 15
            }
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<UserNotice>(message, expectedProps);
    }

    [Fact]
    public void GenericUserNoticeRitualMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = GenericRitual;
        var expectedProps = new
        {
            BadgeInfo = new Dictionary<string, string>(),
            Badges = new Dictionary<string, string>(),
            Color = "",
            DisplayName = "SevenTest1",
            Emotes = new List<Emotes.Info>()
            {
                new()
                {
                    Id = "30259",
                    Position = 0,
                    EmoteTextLength = 7
                }
            },
            MessageId = new Guid("37feed0f-b9c7-4c3a-b475-21c6c6d21c3d"),
            Login = "seventest1",
            Moderator = false,
            RoomId = 87654321,
            Subscriber = false,
            SystemMessage = "Seventoes is new here!",
            Timestamp = new DateTimeOffset(2017, 10, 18, 21, 58, 23, 826, TimeSpan.Zero),
            Turbo = false,
            UserId = 77776666,
            UserType = UserTypesClass.NormalUser,
            Channel = "seventoes",
            Message = "HeyGuys",
            Details = new
            {
                RitualName = "new_chatter"
            }
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<UserNotice>(message, expectedProps);
    }

    [Fact]
    public void RealUserNoticeSubMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = RealSub;
        var expectedProps = new
        {
            BadgeInfo = new Dictionary<string, string>
            {
                {BadgesClass.Subscriber, "1"}
            },
            Badges = new Dictionary<string, string>
            {
                {BadgesClass.Subscriber, "0"},
                {BadgesClass.Premium, "1"}
            },
            Color = "#FF0000",
            DisplayName = "ffez_xd",
            Emotes = new List<Emotes.Info>(),
            MessageId = new Guid("bea7f871-05fb-4827-bfe7-d3a0790d0819"),
            Login = "ffez_xd",
            Moderator = false,
            RoomId = 23693840,
            Subscriber = true,
            SystemMessage = "ffez_xd subscribed with Prime.",
            Timestamp = new DateTimeOffset(2022, 4, 7, 14, 06, 45, 346, TimeSpan.Zero),
            Turbo = false,
            UserId = 600173591,
            UserType = UserTypesClass.NormalUser,
            Channel = "wtii",
            Message = string.Empty,
            Details = new
            {
                CumulativeMonths = 1,
                StreakMonths = 1,
                ShouldShareStreak = false,
                Plan = UserNotice.SubPlanType.Prime,
                PlanName = "Channel Subscription (WTii)"
            }
        };
        // TODO msg-param-goal-contribution-type=SUB_POINTS;msg-param-goal-current-contributions=424;msg-param-goal-target-contributions=430;msg-param-goal-user-contributions=1;
        // TODO what is msg-param-months=0; doing in sub???
        // TODO msg-param-multimonth-duration=0;msg-param-multimonth-tenure=0;msg-param-was-gifted=false;

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<UserNotice>(message, expectedProps);
    }

    [Fact]
    public void RealUserNoticeResubMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = RealResub;
        var expectedProps = new
        {
            BadgeInfo = new Dictionary<string, string>
            {
                {BadgesClass.Subscriber, "55"}
            },
            Badges = new Dictionary<string, string>
            {
                {BadgesClass.Moderator, "1"},
                {BadgesClass.Subscriber, "24"}
            },
            Color = "#1E90FF",
            DisplayName = "frederikct112",
            Emotes = new List<Emotes.Info>(),
            MessageId = new Guid("e2c0603d-696a-42ef-8d95-082ca8c64cb0"),
            Login = "frederikct112",
            Moderator = true,
            RoomId = 23693840,
            Subscriber = true,
            SystemMessage = "frederikct112 subscribed at Tier 1. They've subscribed for 55 months!",
            Timestamp = new DateTimeOffset(2022, 4, 7, 15, 11, 16, 182, TimeSpan.Zero),
            Turbo = false,
            UserId = 23489192,
            UserType = UserTypesClass.Moderator,
            Channel = "wtii",
            Message = "Xdxdxdxdxdxdxd 4v4 allies lol lol",
            Details = new
            {
                CumulativeMonths = 55,
                StreakMonths = 1,
                ShouldShareStreak = false,
                Plan = UserNotice.SubPlanType.Tier1,
                PlanName = "Channel Subscription (WTii)"
            }
        };
        // TODO what is msg-param-months=0; doing in resub???
        // TODO msg-param-multimonth-duration=0;msg-param-multimonth-tenure=0;
        // TODO msg-param-was-gifted=false;

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<UserNotice>(message, expectedProps);
    }

    [Fact]
    public void RealUserNoticeGiftSubMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = RealGiftSub;
        var expectedProps = new
        {
            BadgeInfo = new Dictionary<string, string>
            {
                {BadgesClass.Subscriber, "30"}
            },
            Badges = new Dictionary<string, string>
            {
                {BadgesClass.Subscriber, "24"},
                {BadgesClass.Premium, "1"}
            },
            Color = "#BECCF2",
            DisplayName = "the_whiteagle",
            Emotes = new List<Emotes.Info>(),
            MessageId = new Guid("a03d07d4-98b3-4a87-8ac9-cb12841225bd"),
            Login = "the_whiteagle",
            Moderator = false,
            RoomId = 23693840,
            Subscriber = true,
            SystemMessage = "the_whiteagle gifted a Tier 1 sub to BaledorTheBold! They have given 3 Gift Subs in the channel!",
            Timestamp = new DateTimeOffset(2022, 4, 7, 17, 19, 34, 273, TimeSpan.Zero),
            Turbo = false,
            UserId = 105745602,
            UserType = UserTypesClass.NormalUser,
            Channel = "wtii",
            Message = string.Empty,
            Details = new
            {
                CumulativeMonths = 7,
                RecipientDisplayName = "BaledorTheBold",
                RecipientId = 158464075,
                RecipientUserName = "baledorthebold",
                Plan = UserNotice.SubPlanType.Tier1,
                PlanName = "Channel Subscription (WTii)",
                MonthsGifted = 1,
            }
        };

        // TODO msg-param-goal-contribution-type=SUB_POINTS;msg-param-goal-current-contributions=425;msg-param-goal-target-contributions=430;msg-param-goal-user-contributions=1;
        // TODO msg-param-origin-id=b4\\sb7\\scf\\sb7\\s42\\s31\\sbc\\se0\\saf\\sb1\\sc2\\s71\\sc3\\s86\\s8c\\sf4\\s1c\\se3\\sf8\\s92;
        // TODO msg-param-sender-count=3;

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<UserNotice>(message, expectedProps);
    }

    [Fact]
    public void RealUserNoticeThijsSubMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = RealThijsSub;
        var expectedProps = new
        {
            BadgeInfo = new Dictionary<string, string>(),
            Badges = new Dictionary<string, string>
            {
                {BadgesClass.Premium, "1"}
            },
            Color = "",
            DisplayName = "Jibbies512",
            Emotes = new List<Emotes.Info>(),
            MessageId = new Guid("72f1fe68-9430-474a-809c-17b30838577a"),
            Login = "jibbies512",
            Moderator = false,
            RoomId = 57025612,
            Subscriber = true,
            SystemMessage = "Jibbies512 subscribed with Prime.",
            Timestamp = new DateTimeOffset(2022, 8, 11, 8, 25, 47, 25, TimeSpan.Zero),
            Turbo = false,
            UserId = 145112849,
            UserType = UserTypesClass.NormalUser,
            Channel = "thijs",
            Message = string.Empty,
            Details = new
            {
                CumulativeMonths = 1,
                ShouldShareStreak = false,
                StreakMonths = 1,
                Plan = UserNotice.SubPlanType.Prime,
                PlanName = "Channel Subscription (thijshs)"
            }
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<UserNotice>(message, expectedProps);
    }
}
