using System;
using Xunit;
using ArchieMate.TwitchIRC.Messages.Incoming;


namespace ArchieMate.TwitchIRC.UnitTests.Messages.Incoming;

public class ClearChatTests
{
    internal static readonly string Generic = "@ban-duration=1;room-id=2;target-user-id=3;tmi-sent-ts=1650489791000 :tmi.twitch.tv CLEARCHAT #<channel> :<user>";
    internal static readonly string RealBan = "@room-id=23693840;target-user-id=140167043;tmi-sent-ts=1649340649566 :tmi.twitch.tv CLEARCHAT #wtii :djavan61";
    // TODO: Add unit test for real timeout

    [Fact]
    public void NullMessageCorrectlyThrowsException()
    {
        GenericMessageTestsHelper.TestNullArgumentException<ClearChat>();
    }

    [Fact]
    public void GenericClearChatMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = Generic;
        var expectedProps = new
        {
            BanDuration = 1,
            RoomId = 2,
            TargetUserId = 3,
            Timestamp = new DateTimeOffset(2022, 4, 20, 21, 23, 11, TimeSpan.Zero),
            Channel = "<channel>",
            User = "<user>"
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<ClearChat>(message, expectedProps);
    }

    [Fact]
    public void RealBanClearChatMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = RealBan;
        var expectedProps = new
        {
            BanDuration = -1,
            RoomId = 23693840,
            TargetUserId = 140167043,
            Timestamp = new DateTimeOffset(2022, 4, 7, 14, 10, 49, 566, TimeSpan.Zero),
            Channel = "wtii",
            User = "djavan61"
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<ClearChat>(message, expectedProps);
    }
}
