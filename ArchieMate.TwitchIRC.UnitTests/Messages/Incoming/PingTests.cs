using Xunit;
using ArchieMate.TwitchIRC.Messages.Incoming;

namespace ArchieMate.TwitchIRC.UnitTests.Messages.Incoming;

public class PingTests
{
    public static readonly string Message = "PING :tmi.twitch.tv";

    [Fact]
    public void NullMessageCorrectlyThrowsException()
    {
        GenericMessageTestsHelper.TestNullArgumentException<Ping>();
    }

    [Fact]
    public void PingMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = Message;

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingSimpleMessage<Ping>(message);
    }
}