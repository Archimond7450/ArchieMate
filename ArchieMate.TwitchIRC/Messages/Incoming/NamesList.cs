namespace ArchieMate.TwitchIRC.Messages.Incoming;

using System.Text.RegularExpressions;

public class NamesList : Message
{
    private static readonly Regex regularExpression = new Regex(@":(?'bot'\S+)\.tmi\.twitch\.tv\s353\s\S+\s=\s#?(?'channel'\S+)\s:(?'users'.*)");

    public string ChatbotName { get; init; }
    public string Channel { get; init; }
    public string[] Users { get; init; }

    public static GroupCollection? Matches(string message)
    {
        Match match = regularExpression.Match(message);

        if (match.Success)
        {
            return match.Groups;
        }

        return null;
    }

    public NamesList(GroupCollection groups) : base(groups)
    {
        this.ChatbotName = groups["bot"].Value;
        this.Channel = groups["channel"].Value;
        this.Users = groups["users"].Value.Split(' ');
    }
}

