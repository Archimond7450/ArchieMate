using Xunit;
using ArchieMate.TwitchIRC.Messages.Incoming;

namespace ArchieMate.TwitchIRC.UnitTests.Messages.Incoming;

public class WelcomeTests
{
    internal static readonly string Message001 = ":tmi.twitch.tv 001 <user> :Welcome, GLHF!";
    internal static readonly string Message002 = ":tmi.twitch.tv 002 <user> :Your host is tmi.twitch.tv";
    internal static readonly string Message003 = ":tmi.twitch.tv 003 <user> :This server is rather new";
    internal static readonly string Message004 = ":tmi.twitch.tv 004 <user> :-";
    internal static readonly string Message375 = ":tmi.twitch.tv 375 <user> :-";
    internal static readonly string Message372 = ":tmi.twitch.tv 372 <user> :You are in a maze of twisty passages.";
    internal static readonly string Message376 = ":tmi.twitch.tv 376 <user> :>";

    [Fact]
    public void NullMessageCorrectlyThrowsException()
    {
        GenericMessageTestsHelper.TestNullArgumentException<Welcome>();
    }

    [Fact]
    public void WelcomeMessage001IsSuccessfullyRecognized()
    {
        // Arrange
        string message = Message001;
        var expectedProps = new { Code = 1, User = "<user>", Message = "Welcome, GLHF!" };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<Welcome>(message, expectedProps);
    }

    [Fact]
    public void WelcomeMessage002IsSuccessfullyRecognized()
    {
        // Arrange
        string message = Message002;
        var expectedProps = new { Code = 2, User = "<user>", Message = "Your host is tmi.twitch.tv" };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<Welcome>(message, expectedProps);
    }

    [Fact]
    public void WelcomeMessage003IsSuccessfullyRecognized()
    {
        // Arrange
        string message = Message003;
        var expectedProps = new { Code = 3, User = "<user>", Message = "This server is rather new" };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<Welcome>(message, expectedProps);
    }

    [Fact]
    public void WelcomeMessage004IsSuccessfullyRecognized()
    {
        // Arrange
        string message = Message004;
        var expectedProps = new { Code = 4, User = "<user>", Message = "-" };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<Welcome>(message, expectedProps);
    }

    [Fact]
    public void WelcomeMessage375IsSuccessfullyRecognized()
    {
        // Arrange
        string message = Message375;
        var expectedProps = new { Code = 375, User = "<user>", Message = "-" };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<Welcome>(message, expectedProps);
    }

    [Fact]
    public void WelcomeMessage372IsSuccessfullyRecognized()
    {
        // Arrange
        string message = Message372;
        var expectedProps = new { Code = 372, User = "<user>", Message = "You are in a maze of twisty passages." };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<Welcome>(message, expectedProps);
    }

    [Fact]
    public void WelcomeMessage376IsSuccessfullyRecognized()
    {
        // Arrange
        string message = Message376;
        var expectedProps = new { Code = 376, User = "<user>", Message = ">" };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<Welcome>(message, expectedProps);
    }
}
