namespace ArchieMate.TwitchIRC.Messages.Incoming;

using System.Text.RegularExpressions;


// TODO: Check if clearing the entire chat sends this message as well
public class RoomState : Message
{
    private static readonly Regex regularExpression = new Regex(@"@(?'tags'\S+)\s:tmi\.twitch\.tv\sROOMSTATE\s#(?'channel'.*)");

    public bool? EmoteOnly { get; init; }
    public int? FollowForMinutesToChat { get; init; }
    public bool? MustHaveUniqueMessages { get; init; }
    public bool? Rituals { get; init; }
    public int RoomId { get; init; }
    public int? WaitBetweenMessages { get; init; }
    public bool? SubscribersOnly { get; init; }
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

    public RoomState(GroupCollection groups) : base(groups)
    {
        Dictionary<string, string> tags = Tags.Decode(groups["tags"].Value);

        string emoteOnly = tags.GetValueOrDefault("emote-only", "");
        string followersOnly = tags.GetValueOrDefault("followers-only", "");
        string r9k = tags.GetValueOrDefault("r9k", "");
        string rituals = tags.GetValueOrDefault("rituals", "");
        string slow = tags.GetValueOrDefault("slow", "");
        string subsOnly = tags.GetValueOrDefault("subs-only", "");

        this.EmoteOnly = (emoteOnly == string.Empty) ? null : Tags.ParseBooleanValue(emoteOnly);
        this.FollowForMinutesToChat = (followersOnly == string.Empty) ? null : Convert.ToInt32(followersOnly);
        this.MustHaveUniqueMessages = (r9k == string.Empty) ? null : Tags.ParseBooleanValue(r9k);
        this.Rituals = (rituals == string.Empty) ? null : Tags.ParseBooleanValue(rituals);
        this.RoomId = Convert.ToInt32(tags["room-id"]);
        this.WaitBetweenMessages = (slow == string.Empty) ? null : Convert.ToInt32(slow);
        this.SubscribersOnly = (subsOnly == string.Empty) ? null : Tags.ParseBooleanValue(subsOnly);
        this.Channel = groups["channel"].Value;
    }
}