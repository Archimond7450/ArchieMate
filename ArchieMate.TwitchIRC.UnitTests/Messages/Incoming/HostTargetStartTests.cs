using Xunit;
using ArchieMate.TwitchIRC.Messages.Incoming;

namespace ArchieMate.TwitchIRC.UnitTests.Messages.Incoming;

public class HostTargetStartTests
{
    internal static readonly string Generic = ":tmi.twitch.tv HOSTTARGET #hosting_channel :<channel> 0";
    internal static readonly string Real = ":tmi.twitch.tv HOSTTARGET #wtii :sp4rta 107";

    [Fact]
    public void NullMessageCorrectlyThrowsException()
    {
        GenericMessageTestsHelper.TestNullArgumentException<HostTargetStart>();
    }

    [Fact]
    public void GenericHostTargetStartMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = Generic;
        var expectedProps = new
        {
            Channel = "hosting_channel",
            HostedChannel = "<channel>",
            NumViewers = 0
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<HostTargetStart>(message, expectedProps);
    }

    [Fact]
    public void RealHostTargetStartMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = Real;
        var expectedProps = new
        {
            Channel = "wtii",
            HostedChannel = "sp4rta",
            NumViewers = 107
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<HostTargetStart>(message, expectedProps);
    }
}
