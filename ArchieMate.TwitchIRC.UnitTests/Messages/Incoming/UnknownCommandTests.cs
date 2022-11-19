using Xunit;
using ArchieMate.TwitchIRC.Messages.Incoming;

namespace ArchieMate.TwitchIRC.UnitTests.Messages.Incoming;

public class UnknownCommandTests
{
    internal static readonly string Generic = ":tmi.twitch.tv 421 <user> WHO :Unknown command";

    [Fact]
    public void NullMessageCorrectlyThrowsException()
    {
        GenericMessageTestsHelper.TestNullArgumentException<UnknownCommand>();
    }

    [Fact]
    public void GenericUnknownCommandMessageIsRecognized()
    {
        // Arrange
        string message = Generic;
        var expectedProps = new { User = "<user>", Command = "WHO", Message = "Unknown command" };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<UnknownCommand>(message, expectedProps);
    }
}
