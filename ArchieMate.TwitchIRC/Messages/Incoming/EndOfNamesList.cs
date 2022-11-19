namespace ArchieMate.TwitchIRC.Messages.Incoming;

using System.Text.RegularExpressions;

public class EndOfNamesList : Message
{
    private static readonly Regex regularExpression = new Regex(@":(?'bot'\S+)\.tmi\.twitch\.tv\s366\s\S+\s#?(?'channel'\S+)\s:(?'end_of_names_list'.*)");

    public string ChatbotName { get; init; }
    public string Channel { get; init; }
    public string Message { get; init; }

    public static GroupCollection? Matches(string message)
    {
        Match match = regularExpression.Match(message);

        if (match.Success)
        {
            return match.Groups;
        }

        return null;
    }

    public EndOfNamesList(GroupCollection groups) : base(groups)
    {
        this.ChatbotName = groups["bot"].Value;
        this.Channel = groups["channel"].Value;
        this.Message = groups["end_of_names_list"].Value;
    }
}

