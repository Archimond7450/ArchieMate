using System.Reflection;
using Xunit;
using ArchieMate.TwitchIRC.Messages.Incoming;
using BadgesClass = ArchieMate.TwitchIRC.Badges;
using System.Collections.Generic;
using ArchieMate.TwitchIRC.UserTypes;
using System;

namespace ArchieMate.TwitchIRC.UnitTests.Messages.Incoming;

public class PrivMsgTests
{
    internal static readonly string Example = "@badge-info=;badges=turbo/1;color=#0D4200;display-name=ronni;emotes=25:0-4,12-16/1902:6-10;id=b34ccfc7-4977-403a-8a94-33c6bac34fb8;mod=0;room-id=1337;subscriber=0;tmi-sent-ts=1507246572675;turbo=1;user-id=1337;user-type=global_mod :ronni!ronni@ronni.tmi.twitch.tv PRIVMSG #ronni :Kappa Keepo Kappa";
    internal static readonly string ExampleBits = "@badge-info=;badges=staff/1,bits/1000;bits=100;color=;display-name=ronni;emotes=;id=b34ccfc7-4977-403a-8a94-33c6bac34fb8;mod=0;room-id=12345678;subscriber=0;tmi-sent-ts=1507246572675;turbo=1;user-id=12345678;user-type=staff :ronni!ronni@ronni.tmi.twitch.tv PRIVMSG #ronni :cheer100";
    internal static readonly string RealThijs = "@badge-info=predictions/NOP,subscriber/7;badges=predictions/pink-2,vip/1,subscriber/6,moments/2;client-nonce=fc5141860ef33263da67df94b38a6e7e;color=;display-name=got_bg;emotes=;first-msg=0;flags=;id=3a63328c-496f-40ec-a4fb-02b08bf3f9bb;mod=0;returning-chatter=0;room-id=57025612;subscriber=1;tmi-sent-ts=1660210949055;turbo=0;user-id=612686581;user-type= :got_bg!got_bg@got_bg.tmi.twitch.tv PRIVMSG #thijs :bottomRight Sadge";


    [Fact]
    public void NullMessageCorrectlyThrowsException()
    {
        GenericMessageTestsHelper.TestNullArgumentException<PrivMsg>();
    }

    [Fact]
    public void ExampleMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = Example;
        var expectedProps = new
        {
            BadgeInfo = new Dictionary<string, string>(),
            Badges = new Dictionary<string, string>
            {
                {BadgesClass.Turbo, "1"}
            },
            Bits = 0,
            ClientNonce = (string?)null,
            Color = "#0D4200",
            DisplayName = "ronni",
            EmoteOnly = false,
            Emotes = new List<Emotes.Info>
            {
                new Emotes.Info
                {
                    Id = "25",
                    Position = 0,
                    EmoteTextLength = 5
                },
                new Emotes.Info
                {
                    Id = "25",
                    Position = 12,
                    EmoteTextLength = 5
                },
                new Emotes.Info
                {
                    Id = "1902",
                    Position = 6,
                    EmoteTextLength = 5
                }
            },
            FirstMessage = false,
            MessageId = new Guid("b34ccfc7-4977-403a-8a94-33c6bac34fb8"),
            Moderator = false,
            ReturningChatter = false,
            RoomId = 1337,
            Subscriber = false,
            Timestamp = new DateTimeOffset(2017, 10, 5, 23, 36, 12, 675, TimeSpan.Zero),
            Turbo = true,
            UserId = 1337,
            UserType = Types.GlobalModerator,
            Username = "ronni",
            Channel = "ronni",
            Message = "Kappa Keepo Kappa"
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<PrivMsg>(message, expectedProps);
    }

    [Fact]
    public void ExampleBitsMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = ExampleBits;
        var expectedProps = new
        {
            BadgeInfo = new Dictionary<string, string>(),
            Badges = new Dictionary<string, string>
            {
                {BadgesClass.Staff, "1"},
                {BadgesClass.Bits, "1000"}
            },
            Bits = 100,
            ClientNonce = (string?)null,
            Color = "",
            DisplayName = "ronni",
            EmoteOnly = false,
            Emotes = new List<Emotes.Info>(),
            FirstMessage = false,
            MessageId = new Guid("b34ccfc7-4977-403a-8a94-33c6bac34fb8"),
            Moderator = false,
            ReturningChatter = false,
            RoomId = 12345678,
            Subscriber = false,
            Timestamp = new DateTimeOffset(2017, 10, 5, 23, 36, 12, 675, TimeSpan.Zero),
            Turbo = true,
            UserId = 12345678,
            UserType = Types.TwitchEmployee,
            Username = "ronni",
            Channel = "ronni",
            Message = "cheer100"
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<PrivMsg>(message, expectedProps);
    }

    [Fact]
    public void RealThijsMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = RealThijs;
        var expectedProps = new
        {
            BadgeInfo = new Dictionary<string, string>
            {
                {Badges.Predictions, "NOP"},
                {Badges.Subscriber, "7"}
            },
            Badges = new Dictionary<string, string>
            {
                {Badges.Predictions, "pink-2"},
                {Badges.Vip, "1"},
                {Badges.Subscriber, "6"},
                {Badges.Moments, "2"}
            },
            Bits = 0,
            ClientNonce = "fc5141860ef33263da67df94b38a6e7e",
            Color = "",
            DisplayName = "got_bg",
            EmoteOnly = false,
            Emotes = new List<Emotes.Info>(),
            FirstMessage = false,
            MessageId = new Guid("3a63328c-496f-40ec-a4fb-02b08bf3f9bb"),
            Moderator = false,
            ReturningChatter = false,
            RoomId = 57025612,
            Subscriber = true,
            Timestamp = new DateTimeOffset(2022, 8, 11, 9, 42, 29, 55, TimeSpan.Zero),
            Turbo = false,
            UserId = 612686581,
            UserType = Types.NormalUser,
            Username = "got_bg",
            Channel = "thijs",
            Message = "bottomRight Sadge"
        };

        // @badge-info=predictions/NOP,subscriber/7;
        // badges=predictions/pink-2,vip/1,subscriber/6,moments/2;
        // client-nonce=fc5141860ef33263da67df94b38a6e7e;color=;
        // display-name=got_bg;emotes=;first-msg=0;flags=;
        // id=3a63328c-496f-40ec-a4fb-02b08bf3f9bb;mod=0;
        // returning-chatter=0;room-id=57025612;subscriber=1;
        // tmi-sent-ts=1660210949055;turbo=0;user-id=612686581;user-type= 
        // :got_bg!got_bg@got_bg.tmi.twitch.tv PRIVMSG #thijs 
        // :bottomRight Sadge

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<PrivMsg>(message, expectedProps);
    }
}
