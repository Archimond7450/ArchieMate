using Xunit;
using ArchieMate.TwitchIRC.Messages.Incoming;

namespace ArchieMate.TwitchIRC.UnitTests.Messages.Incoming;

public class HostTargetEndTests
{
    internal static readonly string Generic = ":tmi.twitch.tv HOSTTARGET #hosting_channel :- 0";
    internal static readonly string Real = ":tmi.twitch.tv HOSTTARGET #wtii :- 0";

    [Fact]
    public void NullMessageCorrectlyThrowsException()
    {
        GenericMessageTestsHelper.TestNullArgumentException<HostTargetEnd>();
    }

    [Fact]
    public void GenericHostTargetEndMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = Generic;
        var expectedProps = new
        {
            Channel = "hosting_channel",
            NumViewers = 0
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<HostTargetEnd>(message, expectedProps);
    }

    [Fact]
    public void RealHostTargetEndMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = Real;
        var expectedProps = new
        {
            Channel = "wtii",
            NumViewers = 0
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<HostTargetEnd>(message, expectedProps);
    }
}
