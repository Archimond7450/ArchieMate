using Xunit;
using ArchieMate.TwitchIRC.Messages.Incoming;

namespace ArchieMate.TwitchIRC.UnitTests.Messages.Incoming;

public class JoinTests
{
    internal static readonly string Generic = ":<user>!<user>@<user>.tmi.twitch.tv JOIN #<channel>";
    internal static readonly string Real = ":archiemate!archiemate@archiemate.tmi.twitch.tv JOIN #wtii";
    internal static readonly string OnChatbotJoin = ":archiemate!archiemate@archiemate.tmi.twitch.tv JOIN archiemate";

    [Fact]
    public void NullMessageCorrectlyThrowsException()
    {
        GenericMessageTestsHelper.TestNullArgumentException<Join>();
    }

    [Fact]
    public void GenericJoinMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = Generic;
        var expectedProps = new { User = "<user>", Channel = "<channel>" };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<Join>(message, expectedProps);
    }

    [Fact]
    public void RealJoinMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = Real;
        var expectedProps = new { User = "archiemate", Channel = "wtii" };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<Join>(message, expectedProps);
    }

    [Fact]
    public void OnChatbotJoinMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = OnChatbotJoin;
        var expectedProps = new { User = "archiemate", Channel = "archiemate" };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<Join>(message, expectedProps);
    }
}
