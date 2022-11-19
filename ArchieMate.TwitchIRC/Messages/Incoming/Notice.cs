namespace ArchieMate.TwitchIRC.Messages.Incoming;

using System.Reflection;
using System.Text.RegularExpressions;
using System.Linq;


internal static class NoticeTypeString
{
    public static readonly string AlreadyBanned = "already_banned";
    public static readonly string AlreadyEmoteOnlyOff = "already_emote_only_off";
    public static readonly string AlreadyEmoteOnlyOn = "already_emote_only_on";
    public static readonly string AlreadyFollowersOff = "already_followers_off";
    public static readonly string AlreadyFollowersOn = "already_followers_on";
    public static readonly string AlreadyR9KOff = "already_r9k_off";
    public static readonly string AlreadyR9KOn = "already_r9k_on";
    public static readonly string AlreadySlowOff = "already_slow_off";
    public static readonly string AlreadySlowOn = "already_slow_on";
    public static readonly string AlreadySubsOff = "already_subs_off";
    public static readonly string AlreadySubsOn = "already_subs_on";
    public static readonly string AutohostReceive = "autohost_receive";
    public static readonly string BadBanAdmin = "bad_ban_admin";
    public static readonly string BadBanAnon = "bad_ban_anon";
    public static readonly string BadBanBroadcaster = "bad_ban_broadcaster";
    public static readonly string BadBanMod = "bad_ban_mod";
    public static readonly string BadBanSelf = "bad_ban_self";
    public static readonly string BadBanStaff = "bad_ban_staff";
    public static readonly string BadCommercialError = "bad_commercial_error";
    public static readonly string BadDeleteMessageBroadcaster = "bad_delete_message_broadcaster";
    public static readonly string BadDeleteMessageMod = "bad_delete_message_mod";
    public static readonly string BadHostError = "bad_host_error";
    public static readonly string BadHostHosting = "bad_host_hosting";
    public static readonly string BadHostRateExceeded = "bad_host_rate_exceeded";
    public static readonly string BadHostRejected = "bad_host_rejected";
    public static readonly string BadHostSelf = "bad_host_self";
    public static readonly string BadModBanned = "bad_mod_banned";
    public static readonly string BadModMod = "bad_mod_mod";
    public static readonly string BadSlowDuration = "bad_slow_duration";
    public static readonly string BadTimeoutAdmin = "bad_timeout_admin";
    public static readonly string BadTimeoutAnon = "bad_timeout_anon";
    public static readonly string BadTimeoutBroadcaster = "bad_timeout_broadcaster";
    public static readonly string BadTimeoutDuration = "bad_timeout_duration";
    public static readonly string BadTimeoutMod = "bad_timeout_mod";
    public static readonly string BadTimeoutSelf = "bad_timeout_self";
    public static readonly string BadTimeoutStaff = "bad_timeout_staff";
    public static readonly string BadUnbanNoBan = "bad_unban_no_ban";
    public static readonly string BadUnhostError = "bad_unhost_error";
    public static readonly string BadUnmodMod = "bad_unmod_mod";
    public static readonly string BadVipGranteeBanned = "bad_vip_grantee_banned";
    public static readonly string BadVipGranteeAlreadyVip = "bad_vip_grantee_already_vip";
    public static readonly string BadVipMaxVipsReached = "bad_vip_max_vips_reached";
    public static readonly string BadVipAchievementIncomplete = "bad_vip_achievement_incomplete";
    public static readonly string BadUnvipGranteeNotVip = "bad_unvip_grantee_not_vip";
    public static readonly string BanSuccess = "ban_success";
    public static readonly string CommandsAvailable = "cmds_available";
    public static readonly string ColorChanged = "color_changed";
    public static readonly string CommercialSuccess = "commercial_success";
    public static readonly string DeleteMessageSuccess = "delete_message_success";
    public static readonly string DeleteStaffMessageSuccess = "delete_staff_message_success";
    public static readonly string EmoteOnlyOff = "emote_only_off";
    public static readonly string EmoteOnlyOn = "emote_only_on";
    public static readonly string FollowersOff = "followers_off";
    public static readonly string FollowersOn = "followers_on";
    public static readonly string FollowersOnZero = "followers_on_zero";
    public static readonly string HostOff = "host_off";
    public static readonly string HostOn = "host_on";
    public static readonly string HostReceive = "host_receive";
    public static readonly string HostReceiveNoCount = "host_receive_no_count";
    public static readonly string HostTargetWentOffline = "host_target_went_offline";
    public static readonly string HostsRemaining = "hosts_remaining";
    public static readonly string InvalidUser = "invalid_user";
    public static readonly string ModSuccess = "mod_success";
    public static readonly string MessageBanned = "msg_banned";
    public static readonly string MessageBadCharacters = "msg_bad_characters";
    public static readonly string MessageChannelBlocked = "msg_channel_blocked";
    public static readonly string MessageChannelSuspended = "msg_channel_suspended";
    public static readonly string MessageDuplicate = "msg_duplicate";
    public static readonly string MessageEmoteOnly = "msg_emoteonly";
    public static readonly string MessageFollowersOnly = "msg_followersonly";
    public static readonly string MessageFollowersOnlyFollowed = "msg_followersonly_followed";
    public static readonly string MessageFollowersOnlyZero = "msg_followersonly_zero";
    public static readonly string MessageR9K = "msg_r9k";
    public static readonly string MessageRateLimit = "msg_ratelimit";
    public static readonly string MessageRejected = "msg_rejected";
    public static readonly string MessageRejectedMandatory = "message_rejected_mandatory";
    public static readonly string MessageSlowMode = "message_slowmode";
    public static readonly string MessageSubsOnly = "message_subsonly";
    public static readonly string MessageSuspended = "msg_suspended";
    public static readonly string MessageTimedOut = "msg_timedout";
    public static readonly string MessageVerifiedEmail = "msg_verified_email";
    public static readonly string NoHelp = "no_help";
    public static readonly string NoMods = "no_mods";
    public static readonly string NoVips = "no_vips";
    public static readonly string NotHosting = "not_hosting";
    public static readonly string NoPermission = "no_permission";
    public static readonly string R9KOff = "r9k_off";
    public static readonly string R9KOn = "r9k_on";
    public static readonly string RaidErrorAlreadyRaiding = "raid_error_already_raiding";
    public static readonly string RaidErrorForbidden = "raid_error_forbidden";
    public static readonly string RaidErrorSelf = "raid_error_self";
    public static readonly string RaidErrorTooManyViewers = "raid_error_too_many_viewers";
    public static readonly string RaidErrorUnexpected = "raid_error_unexpected";
    public static readonly string RaidNoticeMature = "raid_notice_mature";
    public static readonly string RaidNoticeRestrictedChat = "raid_notice_restricted_chat";
    public static readonly string RoomMods = "room_mods";
    public static readonly string SlowOff = "slow_off";
    public static readonly string SlowOn = "slow_on";
    public static readonly string SubsOff = "subs_off";
    public static readonly string SubsOn = "subs_on";
    public static readonly string TimeoutNoTimeout = "timeout_no_timeout";
    public static readonly string TimeoutSuccess = "timeout_success";
    public static readonly string TosBan = "tos_ban";
    public static readonly string TurboOnlyColor = "turbo_only_color";
    public static readonly string UnavailableCommand = "unavailable_command";
    public static readonly string UnbanSuccess = "unban_success";
    public static readonly string UnmodSuccess = "unmod_success";
    public static readonly string UnraidErrorNoActiveRaid = "unraid_error_no_active_raid";
    public static readonly string UnraidErrorUnexpected = "unraid_error_unexpected";
    public static readonly string UnraidSuccess = "unraid_success";
    public static readonly string UnrecognizedCommand = "unrecognized_cmd";
    public static readonly string UntimeoutBanned = "untimeout_banned";
    public static readonly string UntimeoutSuccess = "untimeout_success";
    public static readonly string UnvipSuccess = "unvip_success";
    public static readonly string UsageBan = "usage_ban";
    public static readonly string UsageClear = "usage_clear";
    public static readonly string UsageColor = "usage_color";
    public static readonly string UsageCommercial = "usage_commercial";
    public static readonly string UsageDisconnect = "usage_disconnect";
    public static readonly string UsageDelete = "usage_delete";
    public static readonly string UsageEmoteOnlyOff = "usage_emote_only_off";
    public static readonly string UsageEmoteOnlyOn = "usage_emote_only_on";
    public static readonly string UsageFollowersOff = "usage_followers_off";
    public static readonly string UsageFollowersOn = "usage_followers_on";
    public static readonly string UsageHelp = "usage_help";
    public static readonly string UsageHost = "usage_host";
    public static readonly string UsageMarker = "usage_marker";
    public static readonly string UsageMe = "usage_me";
    public static readonly string UsageMod = "usage_mod";
    public static readonly string UsageMods = "usage_mods";
    public static readonly string UsageR9KOff = "usage_r9k_off";
    public static readonly string UsageR9KOn = "usage_r9k_on";
    public static readonly string UsageRaid = "usage_raid";
    public static readonly string UsageSlowOff = "usage_slow_off";
    public static readonly string UsageSlowOn = "usage_slow_on";
    public static readonly string UsageSubsOff = "usage_subs_off";
    public static readonly string UsageSubsOn = "usage_subs_on";
    public static readonly string UsageTimeout = "usage_timeout";
    public static readonly string UsageUnban = "usage_unban";
    public static readonly string UsageUnhost = "usage_unhost";
    public static readonly string UsageUnmod = "usage_unmod";
    public static readonly string UsageUnraid = "usage_unraid";
    public static readonly string UsageUntimeout = "usage_untimeout";
    public static readonly string UsageUnvip = "usage_unvip";
    public static readonly string UsageUser = "usage_user";
    public static readonly string UsageVip = "usage_vip";
    public static readonly string UsageVips = "usage_vips";
    public static readonly string UsageWhisper = "usage_whisper";
    public static readonly string VipSuccess = "vip_success";
    public static readonly string VipsSuccess = "vips_success";
    public static readonly string WhisperBanned = "whisper_banned";
    public static readonly string WhisperBannedRecipient = "whisper_banned_recipient";
    public static readonly string WhisperInvalidLogin = "whisper_invalid_login";
    public static readonly string WhisperInvalidSelf = "whisper_invalid_self";
    public static readonly string WhisperLimitPerMinute = "whisper_limit_per_min";
    public static readonly string WhisperLimitPerSecond = "whisper_limit_per_sec";
    public static readonly string WhisperRestricted = "whisper_restricted";
    public static readonly string WhisperRestrictedRecipient = "whisper_restricted_recipient";
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