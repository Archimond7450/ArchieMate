namespace ArchieMate.TwitchIRC.Messages.Incoming;

using System.Reflection;
using System.Text.RegularExpressions;
using System.Linq;


internal static class NoticeTypeString
{
    public const string AlreadyBanned = "already_banned";
    public const string AlreadyEmoteOnlyOff = "already_emote_only_off";
    public const string AlreadyEmoteOnlyOn = "already_emote_only_on";
    public const string AlreadyFollowersOff = "already_followers_off";
    public const string AlreadyFollowersOn = "already_followers_on";
    public const string AlreadyR9KOff = "already_r9k_off";
    public const string AlreadyR9KOn = "already_r9k_on";
    public const string AlreadySlowOff = "already_slow_off";
    public const string AlreadySlowOn = "already_slow_on";
    public const string AlreadySubsOff = "already_subs_off";
    public const string AlreadySubsOn = "already_subs_on";
    public const string AutohostReceive = "autohost_receive";
    public const string BadBanAdmin = "bad_ban_admin";
    public const string BadBanAnon = "bad_ban_anon";
    public const string BadBanBroadcaster = "bad_ban_broadcaster";
    public const string BadBanMod = "bad_ban_mod";
    public const string BadBanSelf = "bad_ban_self";
    public const string BadBanStaff = "bad_ban_staff";
    public const string BadCommercialError = "bad_commercial_error";
    public const string BadDeleteMessageBroadcaster = "bad_delete_message_broadcaster";
    public const string BadDeleteMessageMod = "bad_delete_message_mod";
    public const string BadHostError = "bad_host_error";
    public const string BadHostHosting = "bad_host_hosting";
    public const string BadHostRateExceeded = "bad_host_rate_exceeded";
    public const string BadHostRejected = "bad_host_rejected";
    public const string BadHostSelf = "bad_host_self";
    public const string BadModBanned = "bad_mod_banned";
    public const string BadModMod = "bad_mod_mod";
    public const string BadSlowDuration = "bad_slow_duration";
    public const string BadTimeoutAdmin = "bad_timeout_admin";
    public const string BadTimeoutAnon = "bad_timeout_anon";
    public const string BadTimeoutBroadcaster = "bad_timeout_broadcaster";
    public const string BadTimeoutDuration = "bad_timeout_duration";
    public const string BadTimeoutMod = "bad_timeout_mod";
    public const string BadTimeoutSelf = "bad_timeout_self";
    public const string BadTimeoutStaff = "bad_timeout_staff";
    public const string BadUnbanNoBan = "bad_unban_no_ban";
    public const string BadUnhostError = "bad_unhost_error";
    public const string BadUnmodMod = "bad_unmod_mod";
    public const string BadVipGranteeBanned = "bad_vip_grantee_banned";
    public const string BadVipGranteeAlreadyVip = "bad_vip_grantee_already_vip";
    public const string BadVipMaxVipsReached = "bad_vip_max_vips_reached";
    public const string BadVipAchievementIncomplete = "bad_vip_achievement_incomplete";
    public const string BadUnvipGranteeNotVip = "bad_unvip_grantee_not_vip";
    public const string BanSuccess = "ban_success";
    public const string CommandsAvailable = "cmds_available";
    public const string ColorChanged = "color_changed";
    public const string CommercialSuccess = "commercial_success";
    public const string DeleteMessageSuccess = "delete_message_success";
    public const string DeleteStaffMessageSuccess = "delete_staff_message_success";
    public const string EmoteOnlyOff = "emote_only_off";
    public const string EmoteOnlyOn = "emote_only_on";
    public const string FollowersOff = "followers_off";
    public const string FollowersOn = "followers_on";
    public const string FollowersOnZero = "followers_on_zero";
    public const string HostOff = "host_off";
    public const string HostOn = "host_on";
    public const string HostReceive = "host_receive";
    public const string HostReceiveNoCount = "host_receive_no_count";
    public const string HostTargetWentOffline = "host_target_went_offline";
    public const string HostsRemaining = "hosts_remaining";
    public const string InvalidUser = "invalid_user";
    public const string ModSuccess = "mod_success";
    public const string MessageBanned = "msg_banned";
    public const string MessageBadCharacters = "msg_bad_characters";
    public const string MessageChannelBlocked = "msg_channel_blocked";
    public const string MessageChannelSuspended = "msg_channel_suspended";
    public const string MessageDuplicate = "msg_duplicate";
    public const string MessageEmoteOnly = "msg_emoteonly";
    public const string MessageFollowersOnly = "msg_followersonly";
    public const string MessageFollowersOnlyFollowed = "msg_followersonly_followed";
    public const string MessageFollowersOnlyZero = "msg_followersonly_zero";
    public const string MessageR9K = "msg_r9k";
    public const string MessageRateLimit = "msg_ratelimit";
    public const string MessageRejected = "msg_rejected";
    public const string MessageRejectedMandatory = "message_rejected_mandatory";
    public const string MessageSlowMode = "message_slowmode";
    public const string MessageSubsOnly = "message_subsonly";
    public const string MessageSuspended = "msg_suspended";
    public const string MessageTimedOut = "msg_timedout";
    public const string MessageVerifiedEmail = "msg_verified_email";
    public const string NoHelp = "no_help";
    public const string NoMods = "no_mods";
    public const string NoVips = "no_vips";
    public const string NotHosting = "not_hosting";
    public const string NoPermission = "no_permission";
    public const string R9KOff = "r9k_off";
    public const string R9KOn = "r9k_on";
    public const string RaidErrorAlreadyRaiding = "raid_error_already_raiding";
    public const string RaidErrorForbidden = "raid_error_forbidden";
    public const string RaidErrorSelf = "raid_error_self";
    public const string RaidErrorTooManyViewers = "raid_error_too_many_viewers";
    public const string RaidErrorUnexpected = "raid_error_unexpected";
    public const string RaidNoticeMature = "raid_notice_mature";
    public const string RaidNoticeRestrictedChat = "raid_notice_restricted_chat";
    public const string RoomMods = "room_mods";
    public const string SlowOff = "slow_off";
    public const string SlowOn = "slow_on";
    public const string SubsOff = "subs_off";
    public const string SubsOn = "subs_on";
    public const string TimeoutNoTimeout = "timeout_no_timeout";
    public const string TimeoutSuccess = "timeout_success";
    public const string TosBan = "tos_ban";
    public const string TurboOnlyColor = "turbo_only_color";
    public const string UnavailableCommand = "unavailable_command";
    public const string UnbanSuccess = "unban_success";
    public const string UnmodSuccess = "unmod_success";
    public const string UnraidErrorNoActiveRaid = "unraid_error_no_active_raid";
    public const string UnraidErrorUnexpected = "unraid_error_unexpected";
    public const string UnraidSuccess = "unraid_success";
    public const string UnrecognizedCommand = "unrecognized_cmd";
    public const string UntimeoutBanned = "untimeout_banned";
    public const string UntimeoutSuccess = "untimeout_success";
    public const string UnvipSuccess = "unvip_success";
    public const string UsageBan = "usage_ban";
    public const string UsageClear = "usage_clear";
    public const string UsageColor = "usage_color";
    public const string UsageCommercial = "usage_commercial";
    public const string UsageDisconnect = "usage_disconnect";
    public const string UsageDelete = "usage_delete";
    public const string UsageEmoteOnlyOff = "usage_emote_only_off";
    public const string UsageEmoteOnlyOn = "usage_emote_only_on";
    public const string UsageFollowersOff = "usage_followers_off";
    public const string UsageFollowersOn = "usage_followers_on";
    public const string UsageHelp = "usage_help";
    public const string UsageHost = "usage_host";
    public const string UsageMarker = "usage_marker";
    public const string UsageMe = "usage_me";
    public const string UsageMod = "usage_mod";
    public const string UsageMods = "usage_mods";
    public const string UsageR9KOff = "usage_r9k_off";
    public const string UsageR9KOn = "usage_r9k_on";
    public const string UsageRaid = "usage_raid";
    public const string UsageSlowOff = "usage_slow_off";
    public const string UsageSlowOn = "usage_slow_on";
    public const string UsageSubsOff = "usage_subs_off";
    public const string UsageSubsOn = "usage_subs_on";
    public const string UsageTimeout = "usage_timeout";
    public const string UsageUnban = "usage_unban";
    public const string UsageUnhost = "usage_unhost";
    public const string UsageUnmod = "usage_unmod";
    public const string UsageUnraid = "usage_unraid";
    public const string UsageUntimeout = "usage_untimeout";
    public const string UsageUnvip = "usage_unvip";
    public const string UsageUser = "usage_user";
    public const string UsageVip = "usage_vip";
    public const string UsageVips = "usage_vips";
    public const string UsageWhisper = "usage_whisper";
    public const string VipSuccess = "vip_success";
    public const string VipsSuccess = "vips_success";
    public const string WhisperBanned = "whisper_banned";
    public const string WhisperBannedRecipient = "whisper_banned_recipient";
    public const string WhisperInvalidLogin = "whisper_invalid_login";
    public const string WhisperInvalidSelf = "whisper_invalid_self";
    public const string WhisperLimitPerMinute = "whisper_limit_per_min";
    public const string WhisperLimitPerSecond = "whisper_limit_per_sec";
    public const string WhisperRestricted = "whisper_restricted";
    public const string WhisperRestrictedRecipient = "whisper_restricted_recipient";
}

public enum NoticeType
{
    AlreadyBanned,
    AlreadyEmoteOnlyOff,
    AlreadyEmoteOnlyOn,
    AlreadyFollowersOff,
    AlreadyFollowersOn,
    AlreadyR9KOff,
    AlreadyR9KOn,
    AlreadySlowOff,
    AlreadySlowOn,
    AlreadySubsOff,
    AlreadySubsOn,
    AutohostReceive,
    BadBanAdmin,
    BadBanAnon,
    BadBanBroadcaster,
    BadBanMod,
    BadBanSelf,
    BadBanStaff,
    BadCommercialError,
    BadDeleteMessageBroadcaster,
    BadDeleteMessageMod,
    BadHostError,
    BadHostHosting,
    BadHostRateExceeded,
    BadHostRejected,
    BadHostSelf,
    BadModBanned,
    BadModMod,
    BadSlowDuration,
    BadTimeoutAdmin,
    BadTimeoutAnon,
    BadTimeoutBroadcaster,
    BadTimeoutDuration,
    BadTimeoutMod,
    BadTimeoutSelf,
    BadTimeoutStaff,
    BadUnbanNoBan,
    BadUnhostError,
    BadUnmodMod,
    BadVipGranteeBanned,
    BadVipGranteeAlreadyVip,
    BadVipMaxVipsReached,
    BadVipAchievementIncomplete,
    BadUnvipGranteeNotVip,
    BanSuccess,
    CommandsAvailable,
    ColorChanged,
    CommercialSuccess,
    DeleteMessageSuccess,
    DeleteStaffMessageSuccess,
    EmoteOnlyOff,
    EmoteOnlyOn,
    FollowersOff,
    FollowersOn,
    FollowersOnZero,
    HostOff,
    HostOn,
    HostReceive,
    HostReceiveNoCount,
    HostTargetWentOffline,
    HostsRemaining,
    InvalidUser,
    ModSuccess,
    MessageBanned,
    MessageBadCharacters,
    MessageChannelBlocked,
    MessageChannelSuspended,
    MessageDuplicate,
    MessageEmoteOnly,
    MessageFollowersOnly,
    MessageFollowersOnlyFollowed,
    MessageFollowersOnlyZero,
    MessageR9K,
    MessageRateLimit,
    MessageRejected,
    MessageRejectedMandatory,
    MessageSlowMode,
    MessageSubsOnly,
    MessageSuspended,
    MessageTimedOut,
    MessageVerifiedEmail,
    NoHelp,
    NoMods,
    NoVips,
    NotHosting,
    NoPermission,
    R9KOff,
    R9KOn,
    RaidErrorAlreadyRaiding,
    RaidErrorForbidden,
    RaidErrorSelf,
    RaidErrorTooManyViewers,
    RaidErrorUnexpected,
    RaidNoticeMature,
    RaidNoticeRestrictedChat,
    RoomMods,
    SlowOff,
    SlowOn,
    SubsOff,
    SubsOn,
    TimeoutNoTimeout,
    TimeoutSuccess,
    TosBan,
    TurboOnlyColor,
    UnavailableCommand,
    UnbanSuccess,
    UnmodSuccess,
    UnraidErrorNoActiveRaid,
    UnraidErrorUnexpected,
    UnraidSuccess,
    UnrecognizedCommand,
    UntimeoutBanned,
    UntimeoutSuccess,
    UnvipSuccess,
    UsageBan,
    UsageClear,
    UsageColor,
    UsageCommercial,
    UsageDisconnect,
    UsageDelete,
    UsageEmoteOnlyOff,
    UsageEmoteOnlyOn,
    UsageFollowersOff,
    UsageFollowersOn,
    UsageHelp,
    UsageHost,
    UsageMarker,
    UsageMe,
    UsageMod,
    UsageMods,
    UsageR9KOff,
    UsageR9KOn,
    UsageRaid,
    UsageSlowOff,
    UsageSlowOn,
    UsageSubsOff,
    UsageSubsOn,
    UsageTimeout,
    UsageUnban,
    UsageUnhost,
    UsageUnmod,
    UsageUnraid,
    UsageUntimeout,
    UsageUnvip,
    UsageUser,
    UsageVip,
    UsageVips,
    UsageWhisper,
    VipSuccess,
    VipsSuccess,
    WhisperBanned,
    WhisperBannedRecipient,
    WhisperInvalidLogin,
    WhisperInvalidSelf,
    WhisperLimitPerMinute,
    WhisperLimitPerSecond,
    WhisperRestricted,
    WhisperRestrictedRecipient,
}

public class Notice : Message
{
    private static NoticeType NoticeTypeFromTag(string tag)
    {
        var members = typeof(NoticeTypeString)
            .GetFields(BindingFlags.Static | BindingFlags.Public)
            .Where(f => f.FieldType == typeof(string))
            .ToDictionary(f => f.Name, f => (string?)f.GetValue(null));

        foreach (var member in members.Keys)
        {
            if (member is string sMember)
            {
                if (tag == members[member])
                {
                    return Enum.Parse<NoticeType>(sMember);
                }
            }
        }

        throw new ArgumentException("Unknown notice type");
    }

    private static readonly Regex regularExpression = new Regex(@"@(?'tags'\S+)\s:tmi\.twitch\.tv\sNOTICE\s#(?'channel'\S+)\s:(?'message'.*)");

    public NoticeType Type { get; init; }
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

    public Notice(GroupCollection groups) : base(groups)
    {
        Dictionary<string, string> tags = Tags.Decode(groups["tags"].Value);

        this.Type = NoticeTypeFromTag(tags["msg-id"]);
        this.Channel = groups["channel"].Value;
        this.Message = groups["message"].Value;
    }
}