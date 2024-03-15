namespace ArchieMate.TwitchIRC.Messages.Incoming;

using BadgesClass = Badges;
using EmotesClass = Emotes;
using System.Text.RegularExpressions;

public class PrivMsg : Message
{
    private static readonly Regex regularExpression = new Regex(@"@(?'tags'.*)\s:(?'username'\S+)!\S+@\S+\.tmi\.twitch\.tv\sPRIVMSG\s#(?'channel'\S+)\s:(?'message'.*)");

    public Dictionary<string, string> BadgeInfo { get; init; }
    public Dictionary<string, string> Badges { get; init; }
    public int Bits { get; init; }
    public string? ClientNonce { get; init; }
    public string Color { get; init; }
    public string DisplayName { get; init; }
    public bool EmoteOnly { get; init; }
    public List<EmotesClass.Info> Emotes { get; init; }
    public bool FirstMessage { get; init; }
    public string Username { get; init; }
    public string Channel { get; init; }
    public string Message { get; init; }
    public Guid MessageId { get; init; }
    public bool Moderator { get; init; }
    public bool ReturningChatter { get; init; }
    public int RoomId { get; init; }
    public bool Subscriber { get; init; }
    public DateTimeOffset Timestamp { get; init; }
    public bool Turbo { get; init; }
    public int UserId { get; init; }
    public UserTypes.Types UserType { get; init; }

    public static GroupCollection? Matches(string message)
    {
        Match match = regularExpression.Match(message);

        if (match.Success)
        {
            return match.Groups;
        }

        return null;
    }

    public PrivMsg(GroupCollection groups) : base(groups)
    {
        Dictionary<string, string> tags = Tags.Decode(groups["tags"].Value);

        this.BadgeInfo = BadgesClass.Decode(tags.GetValueOrDefault("badge-info", ""));
        this.Badges = BadgesClass.Decode(tags.GetValueOrDefault("badges", ""));
        this.Bits = Convert.ToInt32(tags.GetValueOrDefault("bits", "0"));
        if (tags.ContainsKey("client-nonce"))
        {
            this.ClientNonce = tags["client-nonce"];
        }
        this.Color = tags.GetValueOrDefault("color", "");
        this.DisplayName = Tags.ParseStringValue(tags["display-name"]);
        this.EmoteOnly = Tags.ParseBooleanValue(tags.GetValueOrDefault("emote-only", ""));
        this.Emotes = EmotesClass.Decode(tags.GetValueOrDefault("emotes", ""));
        this.FirstMessage = Tags.ParseBooleanValue(tags.GetValueOrDefault("first-msg", ""));
        this.MessageId = new Guid(tags["id"]);
        this.Moderator = Tags.ParseBooleanValue(tags.GetValueOrDefault("mods", "0"));
        this.ReturningChatter = Tags.ParseBooleanValue(tags.GetValueOrDefault("returning-chatter", "0"));
        this.RoomId = Convert.ToInt32(tags["room-id"]);
        this.Subscriber = Tags.ParseBooleanValue(tags.GetValueOrDefault("subscriber", ""));
        this.Timestamp = DateTimeOffset.FromUnixTimeMilliseconds(Convert.ToInt64(tags["tmi-sent-ts"]));
        this.Turbo = Tags.ParseBooleanValue(tags.GetValueOrDefault("turbo", ""));
        this.UserId = Convert.ToInt32(tags["user-id"]);
        this.UserType = UserTypes.Decoder.Decode(tags.GetValueOrDefault("user-type", ""));
        this.Username = groups["username"].Value;
        this.Channel = groups["channel"].Value;
        this.Message = groups["message"].Value;
    }
}