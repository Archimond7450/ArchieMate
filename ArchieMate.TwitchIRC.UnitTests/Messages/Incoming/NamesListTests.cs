using Xunit;
using ArchieMate.TwitchIRC.Messages.Incoming;

namespace ArchieMate.TwitchIRC.UnitTests.Messages.Incoming;

public class NamesListTests
{
    internal static readonly string Generic = ":<user>.tmi.twitch.tv 353 <user> = #<channel> :<user>";
    internal static readonly string Real = ":archiemate.tmi.twitch.tv 353 archiemate = #wtii :rifmopen4 babypills ordof22 withoutequal v_and_k vo1d_lord bobbysinger94 kasvisliemikuutio colossalfish srcookiemonstr";
    internal static readonly string RealWithoutHashtag = ":archiemate.tmi.twitch.tv 353 archiemate = archiemate :archiemate";

    [Fact]
    public void NullMessageCorrectlyThrowsException()
    {
        GenericMessageTestsHelper.TestNullArgumentException<NamesList>();
    }

    [Fact]
    public void GenericNamesListMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = Generic;
        var expectedProps = new { ChatbotName = "<user>", Channel = "<channel>", Users = new[] { "<user>" } };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<NamesList>(message, expectedProps);
    }

    [Fact]
    public void RealNamesListMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = Real;
        string[] allUsers = new[] { "rifmopen4", "babypills", "ordof22", "withoutequal", "v_and_k", "vo1d_lord", "bobbysinger94", "kasvisliemikuutio", "colossalfish", "srcookiemonstr" };
        var expectedProps = new { ChatbotName = "archiemate", Channel = "wtii", Users = allUsers };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<NamesList>(message, expectedProps);
    }

    [Fact]
    public void RealWithoutHashtagNamesListMessageIsSuccessfullyRecognized()
    {
        // Arrange
        string message = RealWithoutHashtag;
        string[] allUsers = new[] { "archiemate" };
        var expectedProps = new { ChatbotName = "archiemate", Channel = "archiemate", Users = allUsers };

        // Arrange the rest, then Act and Assert
        GenericMessageTestsHelper.TestIncomingMessage<NamesList>(message, expectedProps);
    }
}
