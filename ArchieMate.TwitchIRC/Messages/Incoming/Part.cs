namespace ArchieMate.TwitchIRC.Messages.Incoming;


using System.Text.RegularExpressions;

public class Part : Message
{
    private static readonly Regex regularExpression = new Regex(@":(?'user'\S+)!\S+@\S+\.tmi\.twitch\.tv\sPART\s#(?'channel'.*)");

    public string User { get; init; }
    public string Channel { get; init; }

    public static GroupCollection? Matches(string message)
    {
        Match match = regularExpression.Match(message);
        
        if (match.Success)
        {
            return match.Groups;
        }

        return null;
    }

    public Part(GroupCollection groups) : base(groups)
    {
        this.User = groups["user"].Value;
        this.Channel = groups["channel"].Value;
    }
}
