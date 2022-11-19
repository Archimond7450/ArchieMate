namespace ArchieMate.TwitchIRC.Messages.Incoming;

using System.Text.RegularExpressions;
using BadgesClass = Badges;
using UserTypes = UserTypes.Types;
using UserTypesDecoder = UserTypes.Decoder;
using EmoteSetsClass = TwitchIRC.EmoteSets;

public class UserState : Message
{
    private static readonly Regex regularExpression = new Regex(@"@(?'tags'.*)\s:tmi\.twitch\.tv\sUSERSTATE\s#(?'channel'\S+)");

    public Dictionary<string, string> BadgeInfo { get; init; }
    public Dictionary<string, string> Badges { get; init; }
    public string Color { get; init; }
    public string DisplayName { get; init; }
    public int[] EmoteSets { get; init; }
    public Guid? MessageId { get; init; }
    public bool Moderator { get; init; }
    public bool Subscriber { get; init; }
    public bool Turbo { get; init; }
    public UserTypes UserType { get; init; }
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

    public UserState(GroupCollection groups) : base(groups)
    {
        Dictionary<string, string> tags = Tags.Decode(groups["tags"].Value);

        this.BadgeInfo = BadgesClass.Decode(tags.GetValueOrDefault("badge-info", ""));
        this.Badges = BadgesClass.Decode(tags.GetValueOrDefault("badges", ""));
        this.Color = tags.GetValueOrDefault("color", "");
        this.DisplayName = Tags.ParseStringValue(tags["display-name"]);
        this.EmoteSets = EmoteSetsClass.Decode(tags.GetValueOrDefault("emote-sets", ""));
        if (tags.ContainsKey("id"))
        {
            this.MessageId = new Guid(tags["id"]);
        }
        this.Moderator = Tags.ParseBooleanValue(tags.GetValueOrDefault("mod", "0"));
        this.Subscriber = Tags.ParseBooleanValue(tags.GetValueOrDefault("subscriber", ""));
        this.Turbo = Tags.ParseBooleanValue(tags.GetValueOrDefault("turbo", ""));
        this.UserType = UserTypesDecoder.Decode(tags["user-type"]);
        this.Channel = groups["channel"].Value;
    }
}