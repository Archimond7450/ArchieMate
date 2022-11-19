using Xunit;
using ArchieMate.TwitchIRC.Messages.Incoming;

namespace ArchieMate.TwitchIRC.UnitTests.Messages.Incoming;

public class ClearMsgTests
{
    internal static readonly string Generic = "@login=<login>;target-msg-id=<target-msg-id> :tmi.twitch.tv CLEARMSG #<channel> :<message>";
    internal static readonly string Example = "@login=ronni;target-msg-id=abc-123-def :tmi.twitch.tv CLEARMSG #dallas :HeyGuys";
    // TODO: Add unit test for real CLEARMSG

    [Fact]
    public void NullMessageCorrectlyThrowsException()
    {
        GenericMessageTestsHelper.TestNullArgumentException<ClearMsg>();
    }

    [Fact]
    public void GenericClearMsgMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = Generic;
        var expectedProps = new
        {
            User = "<login>",
            MessageId = "<target-msg-id>",
            Channel = "<channel>",
            Message = "<message>"
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<ClearMsg>(message, expectedProps);
    }

    [Fact]
    public void ExampleClearMsgMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = Example;
        var expectedProps = new
        {
            User = "ronni",
            MessageId = "abc-123-def",
            Channel = "dallas",
            Message = "HeyGuys"
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<ClearMsg>(message, expectedProps);
    }
}
