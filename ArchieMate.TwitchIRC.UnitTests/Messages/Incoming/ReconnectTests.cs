using Xunit;
using ArchieMate.TwitchIRC.Messages.Incoming;

namespace ArchieMate.TwitchIRC.UnitTests.Messages.Incoming;

public class ReconnectTests
{
    internal static readonly string Message = ":tmi.twitch.tv RECONNECT";

    [Fact]
    public void NullMessageCorrectlyThrowsException()
    {
        GenericMessageTestsHelper.TestNullArgumentException<Reconnect>();
    }

    [Fact]
    public void RealReconnectMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = Message;

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingSimpleMessage<Reconnect>(message);
    }
}
