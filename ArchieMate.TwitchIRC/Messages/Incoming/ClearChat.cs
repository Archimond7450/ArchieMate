namespace ArchieMate.TwitchIRC.Messages.Incoming;

using System.Text.RegularExpressions;


// TODO: Check if clearing the entire chat sends this message as well
public class ClearChat : Message
{
    private static readonly Regex regularExpression = new Regex(@"@(?'tags'\S+)\s:tmi\.twitch\.tv\sCLEARCHAT\s#(?'channel'\S+)\s:(?'user'.*)");

    public int BanDuration { get; init; } // In seconds
    public int RoomId { get; init; }
    public int TargetUserId { get; init; }
    public DateTimeOffset Timestamp { get; init; }
    public int UserId { get; init; }
    public string Channel { get; init; }
    public string User { get; init; }
    

    public static GroupCollection? Matches(string message)
    {
        Match match = regularExpression.Match(message);
        
        if (match.Success)
        {
            return match.Groups;
        }

        return null;
    }

    public ClearChat(GroupCollection groups) : base(groups)
    {
        Dictionary<string, string> tags = Tags.Decode(groups["tags"].Value);

        this.BanDuration = Convert.ToInt32(tags.GetValueOrDefault("ban-duration", "-1"));
        this.RoomId = Convert.ToInt32(tags["room-id"]);
        this.TargetUserId = Convert.ToInt32(tags.GetValueOrDefault("target-user-id", "0"));
        this.Timestamp = DateTimeOffset.FromUnixTimeMilliseconds(Convert.ToInt64(tags["tmi-sent-ts"]));
        this.UserId = Convert.ToInt32(tags["target-user-id"]);
        this.Channel = groups["channel"].Value;
        this.User = groups["user"].Value;
    }
}