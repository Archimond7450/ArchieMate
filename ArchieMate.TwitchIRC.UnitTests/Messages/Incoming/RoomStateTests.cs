using Xunit;
using ArchieMate.TwitchIRC.Messages.Incoming;


namespace ArchieMate.TwitchIRC.UnitTests.Messages.Incoming;

public class RoomStateTests
{
    internal static readonly string ExampleJoin = "@emote-only=0;followers-only=-1;r9k=0;rituals=0;room-id=12345678;slow=0;subs-only=0 :tmi.twitch.tv ROOMSTATE #bar";
    internal static readonly string RealJoin = "@emote-only=0;followers-only=-1;r9k=0;room-id=147113965;slow=0;subs-only=0 :tmi.twitch.tv ROOMSTATE #archimond7450";
    internal static readonly string RealSubsOnlyOn = "@room-id=147113965;subs-only=1 :tmi.twitch.tv ROOMSTATE #archimond7450";
    internal static readonly string RealSubsOnlyOff = "@room-id=147113965;subs-only=0 :tmi.twitch.tv ROOMSTATE #archimond7450";
    internal static readonly string RealEmoteOnlyOn = "@emote-only=1;room-id=147113965 :tmi.twitch.tv ROOMSTATE #archimond7450";
    internal static readonly string RealEmoteOnlyOff = "@emote-only=0;room-id=147113965 :tmi.twitch.tv ROOMSTATE #archimond7450";
    internal static readonly string RealFollowersOnlyOn = "@followers-only=10;room-id=147113965 :tmi.twitch.tv ROOMSTATE #archimond7450";
    internal static readonly string RealFollowersOnlyOff = "@followers-only=0;room-id=147113965 :tmi.twitch.tv ROOMSTATE #archimond7450";
    internal static readonly string RealSlowModeOn = "@room-id=147113965;slow=30 :tmi.twitch.tv ROOMSTATE #archimond7450";
    internal static readonly string RealSlowModeOff = "@room-id=147113965;slow=0 :tmi.twitch.tv ROOMSTATE #archimond7450";

    [Fact]
    public void NullMessageCorrectlyThrowsException()
    {
        GenericMessageTestsHelper.TestNullArgumentException<RoomState>();
    }

    [Fact]
    public void GenericClearChatMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = ExampleJoin;
        var expectedProps = new
        {
            EmoteOnly = false,
            FollowForMinutesToChat = -1,
            MustHaveUniqueMessages = false,
            Rituals = false,
            RoomId = 12345678,
            WaitBetweenMessages = 0,
            SubscribersOnly = false,
            Channel = "bar"
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<RoomState>(message, expectedProps);
    }

    [Fact]
    public void RealJoinRoomStateMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = RealJoin;
        var expectedProps = new
        {
            EmoteOnly = false,
            FollowForMinutesToChat = -1,
            MustHaveUniqueMessages = false,
            Rituals = (bool?)null,
            RoomId = 147113965,
            WaitBetweenMessages = 0,
            SubscribersOnly = false,
            Channel = "archimond7450"
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<RoomState>(message, expectedProps);
    }

    [Fact]
    public void RealSubsOnlyOnRoomStateMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = RealSubsOnlyOn;
        var expectedProps = new
        {
            EmoteOnly = (bool?)null,
            FollowForMinutesToChat = (int?)null,
            MustHaveUniqueMessages = (bool?)null,
            Rituals = (bool?)null,
            RoomId = 147113965,
            WaitBetweenMessages = (int?)null,
            SubscribersOnly = true,
            Channel = "archimond7450"
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<RoomState>(message, expectedProps);
    }

    [Fact]
    public void RealSubsOnlyOffRoomStateMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = RealSubsOnlyOff;
        var expectedProps = new
        {
            EmoteOnly = (bool?)null,
            FollowForMinutesToChat = (int?)null,
            MustHaveUniqueMessages = (bool?)null,
            Rituals = (bool?)null,
            RoomId = 147113965,
            WaitBetweenMessages = (int?)null,
            SubscribersOnly = false,
            Channel = "archimond7450"
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<RoomState>(message, expectedProps);
    }

    [Fact]
    public void RealEmoteOnlyOnRoomStateMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = RealEmoteOnlyOn;
        var expectedProps = new
        {
            EmoteOnly = true,
            FollowForMinutesToChat = (int?)null,
            MustHaveUniqueMessages = (bool?)null,
            Rituals = (bool?)null,
            RoomId = 147113965,
            WaitBetweenMessages = (int?)null,
            SubscribersOnly = (bool?)null,
            Channel = "archimond7450"
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<RoomState>(message, expectedProps);
    }

    [Fact]
    public void RealEmoteOnlyOffRoomStateMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = RealEmoteOnlyOff;
        var expectedProps = new
        {
            EmoteOnly = false,
            FollowForMinutesToChat = (int?)null,
            MustHaveUniqueMessages = (bool?)null,
            Rituals = (bool?)null,
            RoomId = 147113965,
            WaitBetweenMessages = (int?)null,
            SubscribersOnly = (bool?)null,
            Channel = "archimond7450"
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<RoomState>(message, expectedProps);
    }

    [Fact]
    public void RealFollowersOnlyOnRoomStateMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = RealFollowersOnlyOn;
        var expectedProps = new
        {
            EmoteOnly = (bool?)null,
            FollowForMinutesToChat = 10,
            MustHaveUniqueMessages = (bool?)null,
            Rituals = (bool?)null,
            RoomId = 147113965,
            WaitBetweenMessages = (int?)null,
            SubscribersOnly = (bool?)null,
            Channel = "archimond7450"
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<RoomState>(message, expectedProps);
    }

    [Fact]
    public void RealFollowersOnlyOffRoomStateMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = RealFollowersOnlyOff;
        var expectedProps = new
        {
            EmoteOnly = (bool?)null,
            FollowForMinutesToChat = 0,
            MustHaveUniqueMessages = (bool?)null,
            Rituals = (bool?)null,
            RoomId = 147113965,
            WaitBetweenMessages = (int?)null,
            SubscribersOnly = (bool?)null,
            Channel = "archimond7450"
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<RoomState>(message, expectedProps);
    }

    [Fact]
    public void RealSlowModeOnRoomStateMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = RealSlowModeOn;
        var expectedProps = new
        {
            EmoteOnly = (bool?)null,
            FollowForMinutesToChat = (int?)null,
            MustHaveUniqueMessages = (bool?)null,
            Rituals = (bool?)null,
            RoomId = 147113965,
            WaitBetweenMessages = 30,
            SubscribersOnly = (bool?)null,
            Channel = "archimond7450"
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<RoomState>(message, expectedProps);
    }

    [Fact]
    public void RealSlowModeOffRoomStateMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = RealSlowModeOff;
        var expectedProps = new
        {
            EmoteOnly = (bool?)null,
            FollowForMinutesToChat = (int?)null,
            MustHaveUniqueMessages = (bool?)null,
            Rituals = (bool?)null,
            RoomId = 147113965,
            WaitBetweenMessages = 0,
            SubscribersOnly = (bool?)null,
            Channel = "archimond7450"
        };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<RoomState>(message, expectedProps);
    }
}
