using Xunit;
using ArchieMate.TwitchIRC.Messages.Incoming;

namespace ArchieMate.TwitchIRC.UnitTests.Messages.Incoming;

public class NoticeTests
{
    internal static readonly string HostOn = "@msg-id=host_on :tmi.twitch.tv NOTICE #wtii :Now hosting sp4rta.";
    internal static readonly string HostOff = "@msg-id=host_target_went_offline :tmi.twitch.tv NOTICE #wtii :sp4rta has gone offline. Exiting host mode.";

    [Fact]
    public void NullMessageCorrectlyThrowsException()
    {
        GenericMessageTestsHelper.TestNullArgumentException<Notice>();
    }

    [Fact]
    public void HostOnNoticeMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = HostOn;
        var expectedProps = new
        {
            Type = NoticeType.HostOn,
            Channel = "wtii",
            Message = "Now hosting sp4rta."
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<Notice>(message, expectedProps);
    }

    [Fact]
    public void HostOffNoticeMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = HostOff;
        var expectedProps = new
        {
            Type = NoticeType.HostTargetWentOffline,
            Channel = "wtii",
            Message = "sp4rta has gone offline. Exiting host mode."
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<Notice>(message, expectedProps);
    }
}
