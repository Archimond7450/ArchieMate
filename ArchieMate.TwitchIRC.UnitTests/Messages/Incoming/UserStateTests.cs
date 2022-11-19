using System.Reflection;
using Xunit;
using ArchieMate.TwitchIRC.Messages.Incoming;
using BadgesClass = ArchieMate.TwitchIRC.Badges;
using System.Collections.Generic;
using ArchieMate.TwitchIRC.UserTypes;
using System;

namespace ArchieMate.TwitchIRC.UnitTests.Messages.Incoming;

public class UserStateTests
{
    internal static readonly string Example = "@badge-info=;badges=staff/1;color=#0D4200;display-name=ronni;emote-sets=0,33,50,237,793,2126,3517,4578,5569,9400,10337,12239;mod=1;subscriber=1;turbo=1;user-type=staff :tmi.twitch.tv USERSTATE #dallas";
    internal static readonly string WithoutId = "@badge-info=;badges=moderator/1;color=#0000FF;display-name=ArchieMate;emote-sets=0,610186276;mod=1;subscriber=0;user-type=mod :tmi.twitch.tv USERSTATE #archimond7450";
    internal static readonly string WithId = "@badge-info=;badges=moderator/1;color=#0000FF;display-name=ArchieMate;emote-sets=0,610186276;id=581b6702-10fd-41d3-bd84-060d8878e098;mod=1;subscriber=0;user-type=mod :tmi.twitch.tv USERSTATE #archimond7450";


    [Fact]
    public void NullMessageCorrectlyThrowsException()
    {
        GenericMessageTestsHelper.TestNullArgumentException<UserState>();
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
                {BadgesClass.Staff, "1"}
            },
            Color = "#0D4200",
            DisplayName = "ronni",
            EmoteSets = new int[] { 0, 33, 50, 237, 793, 2126, 3517, 4578, 5569, 9400, 10337, 12239 },
            MessageId = (Guid?)null,
            Moderator = true,
            Subscriber = true,
            Turbo = true,
            UserType = Types.TwitchEmployee,
            Channel = "dallas"
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<UserState>(message, expectedProps);
    }

    [Fact]
    public void WithoutIdMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = WithoutId;
        var expectedProps = new
        {
            BadgeInfo = new Dictionary<string, string>(),
            Badges = new Dictionary<string, string>
            {
                {BadgesClass.Moderator, "1"}
            },
            Color = "#0000FF",
            DisplayName = "ArchieMate",
            EmoteSets = new int[] { 0, 610186276 },
            MessageId = (Guid?)null,
            Moderator = true,
            Subscriber = false,
            Turbo = false,
            UserType = Types.Moderator,
            Channel = "archimond7450"
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<UserState>(message, expectedProps);
    }

    [Fact]
    public void WithIdMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = WithId;
        var expectedProps = new
        {
            BadgeInfo = new Dictionary<string, string>(),
            Badges = new Dictionary<string, string>
            {
                {BadgesClass.Moderator, "1"}
            },
            Color = "#0000FF",
            DisplayName = "ArchieMate",
            EmoteSets = new int[] { 0, 610186276 },
            MessageId = new Guid("581b6702-10fd-41d3-bd84-060d8878e098"),
            Moderator = true,
            Subscriber = false,
            Turbo = false,
            UserType = Types.Moderator,
            Channel = "archimond7450"
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<UserState>(message, expectedProps);
    }
}
