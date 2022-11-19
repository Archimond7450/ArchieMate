using System.Text.RegularExpressions;

namespace ArchieMate.TwitchIRC.Messages.Incoming;

public class Ping : Message
{
    private static readonly Regex regularExpression = new Regex(@"PING :tmi.twitch.tv");

    public static GroupCollection? Matches(string message)
    {
        Match match = regularExpression.Match(message);

        if (match.Success)
        {
            return match.Groups;
        }

        return null;
    }

    public Ping(GroupCollection groups) : base(groups)
    {

    }
}