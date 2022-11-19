using Xunit;
using ArchieMate.TwitchIRC.Messages.Incoming;

namespace ArchieMate.TwitchIRC.UnitTests.Messages.Incoming;

public class CapabilityAcknowledgeTests
{
    public static readonly string Full = ":tmi.twitch.tv CAP * ACK :twitch.tv/membership twitch.tv/tags twitch.tv/commands";
    public static readonly string One = ":tmi.twitch.tv CAP * ACK :twitch.tv/commands";
    public static readonly string Two = ":tmi.twitch.tv CAP * ACK :twitch.tv/tags twitch.tv/membership";

    [Fact]
    public void NullMessageCorrectlyThrowsException()
    {
        GenericMessageTestsHelper.TestNullArgumentException<CapabilityAcknowledge>();
    }

    [Fact]
    public void CapabilityAcknowledgeFullMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = Full;
        var expectedProps = new { Capabilities = new[] {"twitch.tv/membership", "twitch.tv/tags", "twitch.tv/commands"} };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<CapabilityAcknowledge>(message, expectedProps);
    }

    [Fact]
    public void CapabilityAcknowledgeOneMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = One;
        var expectedProps = new { Capabilities = new[] {"twitch.tv/commands"} };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<CapabilityAcknowledge>(message, expectedProps);
    }

    [Fact]
    public void CapabilityAcknowledgeTwoMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = Two;
        var expectedProps = new { Capabilities = new[] {"twitch.tv/tags", "twitch.tv/membership"} };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<CapabilityAcknowledge>(message, expectedProps);
    }
}
