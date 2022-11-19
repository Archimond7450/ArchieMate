using Xunit;
using ArchieMate.TwitchIRC.Messages.Incoming;
using System.Text.RegularExpressions;
using FluentAssertions;

namespace ArchieMate.TwitchIRC.UnitTests.Messages.Incoming;

public class PartTests
{
    internal static readonly string Generic = ":<user>!<user>@<user>.tmi.twitch.tv PART #<channel>";
    internal static readonly string Real = ":millocz!millocz@millocz.tmi.twitch.tv PART #wtii";

    [Fact]
    public void NullMessageCorrectlyThrowsException()
    {
        GenericMessageTestsHelper.TestNullArgumentException<Part>();
    }
    
    [Fact]
    public void GenericPartMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = Generic;
        var expectedProps = new { User = "<user>", Channel = "<channel>" };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<Part>(message, expectedProps);
    }

    [Fact]
    public void RealPartMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = Real;
        var expectedProps = new { User = "millocz", Channel = "wtii" };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<Part>(message, expectedProps);
    }
}
