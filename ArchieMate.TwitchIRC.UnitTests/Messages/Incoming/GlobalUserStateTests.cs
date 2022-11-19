using System.Reflection;
using Xunit;
using ArchieMate.TwitchIRC.Messages.Incoming;
using BadgesClass = ArchieMate.TwitchIRC.Badges;
using System.Collections.Generic;
using ArchieMate.TwitchIRC.UserTypes;

namespace ArchieMate.TwitchIRC.UnitTests.Messages.Incoming;

public class GlobalUserStateTests
{
    internal static readonly string Example = "@badge-info=subscriber/8;badges=subscriber/6;color=#0D4200;display-name=dallas;emote-sets=0,33,50,237,793,2126,3517,4578,5569,9400,10337,12239;turbo=0;user-id=12345678;user-type=admin :tmi.twitch.tv GLOBALUSERSTATE";
    // TODO: Add real message when actually received on the bot


    [Fact]
    public void NullMessageCorrectlyThrowsException()
    {
        GenericMessageTestsHelper.TestNullArgumentException<GlobalUserState>();
    }

    [Fact]
    public void ExampleMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = Example;
        var expectedProps = new
        {
            BadgeInfo = new Dictionary<string, string>
            {
                {BadgesClass.Subscriber, "8"}
            },
            Badges = new Dictionary<string, string>
            {
                {BadgesClass.Subscriber, "6"}
            },
            Color = "#0D4200",
            DisplayName = "dallas",
            EmoteSets = new int[] { 0, 33, 50, 237, 793, 2126, 3517, 4578, 5569, 9400, 10337, 12239 },
            Turbo = false,
            UserId = 12345678,
            UserType = Types.TwitchAdmin
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<GlobalUserState>(message, expectedProps);
    }
}
