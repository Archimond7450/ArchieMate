namespace ArchieMate.TwitchIRC.Messages.Incoming;


using System.Text.RegularExpressions;

public class Reconnect : Message
{
    private static readonly Regex regularExpression = new Regex(@":tmi.twitch.tv RECONNECT");

    public static GroupCollection? Matches(string message)
    {
        Match match = regularExpression.Match(message);

        if (match.Success)
        {
            return match.Groups;
        }

        return null;
    }

    public Reconnect(GroupCollection groups) : base(groups)
    {

    }
}
