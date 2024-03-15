using ArchieMate.TwitchIRC.Messages.Incoming;
using FluentAssertions;
using Xunit;

namespace ArchieMate.TwitchIRC.UnitTests.Messages.Incoming;

public class MessagesTests
{
    // TODO PrivMsg

    [Fact]
    public void JoinMessageIsSuccessfullyDecoded()
    {
        // Arrange
        string message = JoinTests.Real;

        // Act
        IMessage decodedMessage = Message.Decode(message);

        // Assert
        decodedMessage.Should().BeOfType<Join>();
    }

    [Fact]
    public void PartMessageIsSuccessfullyDecoded()
    {
        // Arrange
        string message = PartTests.Real;

        // Act
        IMessage decodedMessage = Message.Decode(message);

        // Assert
        decodedMessage.Should().BeOfType<Part>();
    }

    [Fact]
    public void PingMessageIsSuccessfullyDecoded()
    {
        // Arrange
        string message = PingTests.Message;

        // Act
        IMessage decodedMessage = Message.Decode(message);

        // Assert
        decodedMessage.Should().BeOfType<Ping>();
    }

    [Fact]
    public void ClearChatMessageIsSuccessfullyDecoded()
    {
        // Arrange
        string message = ClearChatTests.RealBan;

        // Act
        IMessage decodedMessage = Message.Decode(message);

        // Assert
        decodedMessage.Should().BeOfType<ClearChat>();
    }

    [Fact]
    public void NoticeMessageIsSuccessfullyDecoded()
    {
        // Arrange
        string message = NoticeTests.HostOn;

        // Act
        IMessage decodedMessage = Message.Decode(message);

        // Assert
        decodedMessage.Should().BeOfType<Notice>();
    }

    [Fact]
    public void ClearMsgMessageIsSuccessfullyDecoded()
    {
        // Arrange
        string message = ClearMsgTests.Example; // TODO When real data is available, change to the real data

        // Act
        IMessage decodedMessage = Message.Decode(message);

        // Assert
        decodedMessage.Should().BeOfType<ClearMsg>();
    }

    [Fact]
    public void HostTargetStartMessageIsSuccessfullyDecoded()
    {
        // Arrange
        string message = HostTargetStartTests.Real;

        // Act
        IMessage decodedMessage = Message.Decode(message);

        // Assert
        decodedMessage.Should().BeOfType<HostTargetStart>();
    }

    [Fact]
    public void HostTargetEndMessageIsSuccessfullyDecoded()
    {
        // Arrange
        string message = HostTargetEndTests.Real;

        // Act
        IMessage decodedMessage = Message.Decode(message);

        // Assert
        decodedMessage.Should().BeOfType<HostTargetEnd>();
    }

    [Fact]
    public void RoomStateMessageIsSuccessfullyDecoded()
    {
        // Arrange
        string message = RoomStateTests.RealJoin;

        // Act
        IMessage decodedMessage = Message.Decode(message);

        // Assert
        decodedMessage.Should().BeOfType<RoomState>();
    }

    [Fact]
    public void UserStateMessageIsSuccessfullyDecoded()
    {
        // Arrange
        string message = UserStateTests.WithId;

        // Act
        IMessage decodedMessage = Message.Decode(message);

        // Assert
        decodedMessage.Should().BeOfType<UserState>();
    }

    [Fact]
    public void GlobalUserStateMessageIsSuccessfullyDecoded()
    {
        // Arrange
        string message = GlobalUserStateTests.Example; // TODO: Change to real when available

        // Act
        IMessage decodedMessage = Message.Decode(message);

        // Assert
        decodedMessage.Should().BeOfType<GlobalUserState>();
    }

    [Fact]
    public void ReconnectMessageIsSuccessfullyDecoded()
    {
        // Arrange
        string message = ReconnectTests.Message;

        // Act
        IMessage decodedMessage = Message.Decode(message);

        // Assert
        decodedMessage.Should().BeOfType<Reconnect>();
    }

    [Fact]
    public void UnknownCommandMessageIsSuccessfullyDecoded()
    {
        // Arrange
        string message = UnknownCommandTests.Generic;

        // Act
        IMessage decodedMessage = Message.Decode(message);

        // Assert
        decodedMessage.Should().BeOfType<UnknownCommand>();
    }

    [Fact]
    public void NamesListMessageIsSuccessfullyDecoded()
    {
        // Arrange
        string message = NamesListTests.Real;

        // Act
        IMessage decodedMessage = Message.Decode(message);

        // Assert
        decodedMessage.Should().BeOfType<NamesList>();
    }

    [Fact]
    public void EndOfNamesListMessageIsSuccessfullyDecoded()
    {
        // Arrange
        string message = EndOfNamesTests.Real;

        // Act
        IMessage decodedMessage = Message.Decode(message);

        // Assert
        decodedMessage.Should().BeOfType<EndOfNamesList>();
    }

    [Fact]
    public void WelcomeMessageIsSuccessfullyDecoded()
    {
        // Arrange
        string message = WelcomeTests.Message001;

        // Act
        IMessage decodedMessage = Message.Decode(message);

        // Assert
        decodedMessage.Should().BeOfType<Welcome>();
    }

    [Fact]
    public void CapabilityAcknowledgeMessageIsSuccessfullyDecoded()
    {
        // Arrange
        string message = CapabilityAcknowledgeTests.Full;

        // Act
        IMessage decodedMessage = Message.Decode(message);

        // Assert
        decodedMessage.Should().BeOfType<CapabilityAcknowledge>();
    }
}