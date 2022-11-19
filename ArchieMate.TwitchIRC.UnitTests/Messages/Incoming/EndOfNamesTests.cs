using Xunit;
using ArchieMate.TwitchIRC.Messages.Incoming;

namespace ArchieMate.TwitchIRC.UnitTests.Messages.Incoming;

public class EndOfNamesTests
{
    internal static readonly string Generic = ":<user>.tmi.twitch.tv 366 <user> #<channel> :End of /NAMES list";
    internal static readonly string Real = ":archiemate.tmi.twitch.tv 366 archiemate #wtii :End of /NAMES list";

    internal static readonly string ConstantMessage = "End of /NAMES list";

    [Fact]
    public void NullMessageCorrectlyThrowsException()
    {
        GenericMessageTestsHelper.TestNullArgumentException<EndOfNamesList>();
    }

    [Fact]
    public void GenericEndOfNamesListMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = Generic;
        var expectedProps = new { ChatbotName = "<user>", Channel = "<channel>", Message = ConstantMessage };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<EndOfNamesList>(message, expectedProps);
    }

    [Fact]
    public void RealEndOfNamesListMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = Real;
        var expectedProps = new { ChatbotName = "archiemate", Channel = "wtii", Message = ConstantMessage };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<EndOfNamesList>(message, expectedProps);
    }
}
