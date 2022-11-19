namespace ArchieMate.TwitchIRC.Messages.Incoming;

using BadgesClass = Badges;
using System.Text.RegularExpressions;
using EmotesClass = TwitchIRC.Emotes;
using UserTypes = UserTypes.Types;
using UserTypesDecoder = UserTypes.Decoder;


public class UserNotice : Message
{
    private static readonly Regex regularExpression = new Regex(@"@(?'tags'.*)\s:tmi\.twitch\.tv\sUSERNOTICE\s#(?'channel'\S+)(\s:(?'message'.*))?");

    private enum Types
    {
        Sub,
        Resub,
        SubGift,
        GiftPaidUpgrade,
        // RewardGift,
        AnonGiftPaidUpgrade,
        Raid,
        // Unraid,
        Ritual,
        BitsBadgeTier
    }

    private static class TypesDecoder
    {
        internal static Types Decode(string userNoticeType)
        {
            switch (userNoticeType)
            {
                case "sub":
                    return Types.Sub;
                case "resub":
                    return Types.Resub;
                case "subgift":
                    return Types.SubGift;
                case "giftpaidupgrade":
                    return Types.GiftPaidUpgrade;
                // case "rewardgift":
                //     return Types.RewardGift;
                case "anongiftpaidupgrade":
                    return Types.AnonGiftPaidUpgrade;
                case "raid":
                    return Types.Raid;
                // case "unraid":
                //     return Types.Unraid;
                case "ritual":
                    return Types.Ritual;
                case "bitsbadgetier":
                    return Types.BitsBadgeTier;
            }
            throw new ArgumentException($"Invalid userNoticeType: {userNoticeType}");
        }
    }

    public abstract class Type
    {
        public Type(Dictionary<string, string> tags)
        {
            if (tags is null)
            {
                throw new ArgumentNullException("tags");
            }
        }

        public static Type Decode(Dictionary<string, string> tags)
        {
            var userNoticeType = TypesDecoder.Decode(tags["msg-id"]);

            switch (userNoticeType)
            {
                case Types.Sub:
                case Types.Resub:
                    return new Sub(tags);
                case Types.SubGift:
                    return new SubGift(tags);
                case Types.GiftPaidUpgrade:
                    return new GiftPaidUpgrade(tags);
                // case Types.RewardGift:
                //     return new RewardGift(tags);
                case Types.AnonGiftPaidUpgrade:
                    return new AnonGiftPaidUpgrade(tags);
                case Types.Raid:
                    return new Raid(tags);
                // case Types.Unraid:
                //     return new Unraid(tags);
                case Types.Ritual:
                    return new Ritual(tags);
                case Types.BitsBadgeTier:
                    return new BitsBadgeTier(tags);
            }

            throw new ArgumentException($"Invalid tag msg-id in tags: {tags}");
        }
    }

    public enum SubPlanType
    {
        Prime,
        Tier1,
        Tier2,
        Tier3
    }

    private static class SubPlanTypeDecoder
    {
        internal static SubPlanType Decode(string strPlanType)
        {
            switch (strPlanType)
            {
                case "Prime":
                    return SubPlanType.Prime;
                case "1000":
                    return SubPlanType.Tier1;
                case "2000":
                    return SubPlanType.Tier2;
                case "3000":
                    return SubPlanType.Tier3;
                default:
                    throw new ArgumentException($"Invalid plan type: {strPlanType}");
            }
        }
    }

    public class Sub : Type
    {
        public int CumulativeMonths { get; init; }
        public bool ShouldShareStreak { get; init; }
        public int StreakMonths { get; init; }
        public SubPlanType Plan { get; init; }
        public string PlanName { get; init; }

        public Sub(Dictionary<string, string> tags) : base(tags)
        {
            this.CumulativeMonths = Convert.ToInt32(tags["msg-param-cumulative-months"]);
            this.ShouldShareStreak = Tags.ParseBooleanValue(tags["msg-param-should-share-streak"]);
            this.StreakMonths = Convert.ToInt32(tags.GetValueOrDefault("msg-param-streak-months", "1"));
            this.Plan = SubPlanTypeDecoder.Decode(tags["msg-param-sub-plan"]);
            this.PlanName = Tags.ParseStringValue(tags["msg-param-sub-plan-name"]);
        }
    }

    public class SubGift : Type
    {
        public int CumulativeMonths { get; init; }
        public string RecipientDisplayName { get; init; }
        public int RecipientId { get; init; }
        public string RecipientUserName { get; init; }
        public SubPlanType Plan { get; init; }
        public string PlanName { get; init; }
        public int MonthsGifted { get; init; }

        public SubGift(Dictionary<string, string> tags) : base(tags)
        {
            this.CumulativeMonths = Convert.ToInt32(tags["msg-param-months"]);
            this.RecipientDisplayName = tags["msg-param-recipient-display-name"];
            this.RecipientId = Convert.ToInt32(tags["msg-param-recipient-id"]);
            this.RecipientUserName = tags["msg-param-recipient-user-name"];
            this.Plan = SubPlanTypeDecoder.Decode(tags["msg-param-sub-plan"]);
            this.PlanName = Tags.ParseStringValue(tags["msg-param-sub-plan-name"]);
            this.MonthsGifted = Convert.ToInt32(tags.GetValueOrDefault("msg-param-gift-months", "1"));
        }
    }

    public class GiftPaidUpgrade : Type
    {
        public int TotalSubsGifted { get; set; }
        public string PromoName { get; init; }
        public string GifterLogin { get; init; }
        public string GifterDisplayName { get; init; }

        public GiftPaidUpgrade(Dictionary<string, string> tags) : base(tags)
        {
            this.TotalSubsGifted = Convert.ToInt32(tags["msg-param-promo-gift-total"]);
            this.PromoName = tags["msg-param-promo-name"];
            this.GifterLogin = tags["msg-param-sender-login"];
            this.GifterDisplayName = tags["msg-param-sender-name"];
        }
    }

    /*
    public class RewardGift : Type
    {
        public RewardGift(Dictionary<string, string> tags) : base(tags)
        {
            // TODO: ???
        }
    }
    */

    public class AnonGiftPaidUpgrade : Type
    {
        public int TotalSubsGifted { get; set; }
        public string PromoName { get; init; }

        public AnonGiftPaidUpgrade(Dictionary<string, string> tags) : base(tags)
        {
            this.TotalSubsGifted = Convert.ToInt32(tags["msg-param-promo-gift-total"]);
            this.PromoName = tags["msg-param-promo-name"];
        }
    }

    public class Raid : Type
    {
        public string RaiderDisplayName { get; init; }
        public string RaiderLogin { get; init; }
        public int ViewerCount { get; init; }

        public Raid(Dictionary<string, string> tags) : base(tags)
        {
            this.RaiderDisplayName = tags["msg-param-displayName"];
            this.RaiderLogin = tags["msg-param-login"];
            this.ViewerCount = Convert.ToInt32(tags["msg-param-viewerCount"]);
        }
    }

    /*
    public class Unraid : Type
    {
        public Unraid(Dictionary<string, string> tags) : base(tags)
        {
            /// TODO: ???
        }
    }
    */

    public class Ritual : Type
    {
        public string RitualName { get; init; }

        public Ritual(Dictionary<string, string> tags) : base(tags)
        {
            this.RitualName = tags["msg-param-ritual-name"];
        }
    }

    public class BitsBadgeTier : Type
    {
        public int Threshold { get; init; }

        public BitsBadgeTier(Dictionary<string, string> tags) : base(tags)
        {
            this.Threshold = Convert.ToInt32(tags["msg-param-threshold"]);
        }
    }

    public Dictionary<string, string> BadgeInfo { get; init; }
    public Dictionary<string, string> Badges { get; init; }
    public string Color { get; init; }
    public string DisplayName { get; init; }
    public List<EmotesClass.Info> Emotes { get; init; }
    public Guid MessageId { get; init; }
    public string Login { get; init; }
    public bool Moderator { get; init; }
    public int RoomId { get; init; }
    public bool Subscriber { get; init; }
    public string SystemMessage { get; init; }
    public DateTimeOffset Timestamp { get; init; }
    public bool Turbo { get; init; }
    public int UserId { get; init; }
    public UserTypes UserType { get; init; }
    public string Channel { get; init; }
    public string Message { get; init; }
    public Type Details { get; init; }

    public static GroupCollection? Matches(string message)
    {
        Match match = regularExpression.Match(message);

        if (match.Success)
        {
            return match.Groups;
        }

        return null;
    }

    public UserNotice(GroupCollection groups) : base(groups)
    {
        Dictionary<string, string> tags = Tags.Decode(groups["tags"].Value);

        this.BadgeInfo = BadgesClass.Decode(tags.GetValueOrDefault("badge-info", ""));
        this.Badges = BadgesClass.Decode(tags.GetValueOrDefault("badges", ""));
        this.Color = tags.GetValueOrDefault("color", "");
        this.DisplayName = Tags.ParseStringValue(tags["display-name"]);
        this.Emotes = EmotesClass.Decode(tags.GetValueOrDefault("emotes", ""));
        this.MessageId = new Guid(tags["id"]);
        this.Login = tags["login"];
        this.Moderator = Tags.ParseBooleanValue(tags.GetValueOrDefault("mods", "0"));
        this.RoomId = Convert.ToInt32(tags["room-id"]);
        this.Subscriber = Tags.ParseBooleanValue(tags.GetValueOrDefault("subscriber", ""));
        this.SystemMessage = Tags.ParseStringValue(tags["system-msg"]);
        this.Timestamp = DateTimeOffset.FromUnixTimeMilliseconds(Convert.ToInt64(tags["tmi-sent-ts"]));
        this.Turbo = Tags.ParseBooleanValue(tags.GetValueOrDefault("turbo", ""));
        this.UserId = Convert.ToInt32(tags["user-id"]);
        this.UserType = UserTypesDecoder.Decode(tags["user-type"]);
        this.Channel = groups["channel"].Value;
        this.Message = groups["message"].Value;
        this.Details = Type.Decode(tags);

        if (this.Badges.ContainsKey("moderator") || this.UserType == UserTypes.Moderator)
        {
            this.Moderator = true;
        }
    }
}