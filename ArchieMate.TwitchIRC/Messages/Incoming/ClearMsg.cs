namespace ArchieMate.TwitchIRC.Messages.Incoming;

using System.Text.RegularExpressions;


public class ClearMsg : Message
{
    private static readonly Regex regularExpression = new Regex(@"@(?'tags'\S+)\s:tmi\.twitch\.tv\sCLEARMSG\s#(?'channel'\S+)\s:(?'message'.*)");

    public string User { get; init; }
    public string MessageId { get; init; }
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

    public ClearMsg(GroupCollection groups) : base(groups)
    {
        Dictionary<string, string> tags = Tags.Decode(groups["tags"].Value);

        this.User = tags["login"];
        this.MessageId = tags["target-msg-id"];
        this.Channel = groups["channel"].Value;
        this.Message = groups["message"].Value;
    }
}