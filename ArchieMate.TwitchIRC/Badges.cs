namespace ArchieMate.TwitchIRC;

public static class Badges
{
    public const string Admin = "admin";
    public const string Bits = "bits";
    public const string Charity = "bits-charity";
    public const string Broadcaster = "broadcaster";
    public const string Founder = "founder";
    public const string GLHFPledge = "glhf-pledge";
    public const string GlitchCon2020 = "glitchcon2020";
    public const string HypeTrain = "hype-train";
    public const string Moderator = "moderator";
    public const string Moments = "moments";
    public const string NoAudio = "no_audio";
    public const string NoVideo = "no_video";
    public const string OverwatchLeagueInsider2019A = "overwatch-league-insider_2019A";
    public const string Partner = "partner";
    public const string Predictions = "predictions";
    public const string Premium = "premium";
    public const string Staff = "staff";
    public const string SubGifter = "sub-gifter";
    public const string Subscriber = "subscriber";
    public const string Turbo = "turbo";
    public const string Vip = "vip";

    public static readonly string[] StringBadges =
    { Predictions };
    public static readonly string[] BooleanBadges =
    { Admin, Broadcaster, Charity, GLHFPledge, GlitchCon2020, Moderator, NoAudio, NoVideo, OverwatchLeagueInsider2019A, Partner, Premium, Staff, Turbo, Vip };
    public static readonly string[] IntegerBadges =
    { Bits, Founder, HypeTrain, Moments, SubGifter, Subscriber };

    public static Dictionary<string, string> Decode(string badges)
    {
        var dict = new Dictionary<string, string>();

        if (badges.Length > 0)
        {
            foreach (string badge in badges.Split(','))
            {
                int splitPosition = badge.IndexOf('/');

                string key = badge.Substring(0, splitPosition);
                string value = badge.Substring(splitPosition + 1);

                dict[key] = value;

                if (Array.IndexOf(StringBadges, key) == -1
                    && Array.IndexOf(BooleanBadges, key) == -1
                    && Array.IndexOf(IntegerBadges, key) == -1)
                {
                    // TODO log this message: $"Unknown badge '{key}'! Value: {value}");
                }
            }
        }

        return dict;
    }
}