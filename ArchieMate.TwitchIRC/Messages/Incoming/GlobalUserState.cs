namespace ArchieMate.TwitchIRC.Messages.Incoming;

using System.Text.RegularExpressions;
using BadgesClass = Badges;
using UserTypes = UserTypes.Types;
using UserTypesDecoder = UserTypes.Decoder;
using EmoteSetsClass = TwitchIRC.EmoteSets;

/// <summary>
/// This message is received after authenticating to Twitch IRC with NICK/PASS
/// </summary>
public class GlobalUserState : Message
{
    private static readonly Regex regularExpression = new Regex(@"@(?'tags'\S+)\s:tmi\.twitch\.tv\sGLOBALUSERSTATE");

    public Dictionary<string, string> BadgeInfo { get; init; }
    public Dictionary<string, string> Badges { get; init; }
    public string Color { get; init; }
    public string DisplayName { get; init; }
    public int[] EmoteSets { get; init; }
    public bool Turbo { get; init; }
    public int UserId { get; init; }
    public UserTypes UserType { get; init; }

    public static GroupCollection? Matches(string message)
    {
        Match match = regularExpression.Match(message);

        if (match.Success)
        {
            return match.Groups;
        }

        return null;
    }

    public GlobalUserState(GroupCollection groups) : base(groups)
    {
        Dictionary<string, string> tags = Tags.Decode(groups["tags"].Value);

        this.BadgeInfo = BadgesClass.Decode(tags.GetValueOrDefault("badge-info", ""));
        this.Badges = BadgesClass.Decode(tags.GetValueOrDefault("badges", ""));
        this.Color = tags.GetValueOrDefault("color", "");
        this.DisplayName = tags["display-name"];
        this.EmoteSets = EmoteSetsClass.Decode(tags.GetValueOrDefault("emote-sets", ""));
        this.Turbo = Tags.ParseBooleanValue(tags.GetValueOrDefault("turbo", ""));
        this.UserId = Convert.ToInt32(tags["user-id"]);
        this.UserType = UserTypesDecoder.Decode(tags["user-type"]);
    }
}