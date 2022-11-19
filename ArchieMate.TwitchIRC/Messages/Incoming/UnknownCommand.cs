namespace ArchieMate.TwitchIRC.Messages.Incoming;

using System.Text.RegularExpressions;

public class UnknownCommand : Message
{
    private static readonly Regex regularExpression = new Regex(@":tmi\.twitch\.tv\s421\s(?'user'\S+)\s(?'command'\S+)\s:(?'message'.*)");

    public string User { get; init; }
    public string Command { get; init; }
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

    public UnknownCommand(GroupCollection groups) : base(groups)
    {
        this.User = groups["user"].Value;
        this.Command = groups["command"].Value;
        this.Message = groups["message"].Value;
    }
}

