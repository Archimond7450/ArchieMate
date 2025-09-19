package com.archimond7450.archiemate.twitch.irc

enum NoticeID(raw: String) {
  def rawValue: String = raw
  
  /**
   * [user] is already banned in this channel
   */
  case AlreadyBanned extends NoticeID("already_banned")

  /**
   * This room is not in emote-only mode.
   */
  case AlreadyEmoteOnlyOff extends NoticeID("already_emote_only_off")

  /**
   * This room is already in emote-only mode.
   */
  case AlreadyEmoteOnlyOn extends NoticeID("already_emote_only_on")

  /**
   * This room is not in followers-only mode.
   */
  case AlreadyFollowersOff extends NoticeID("already_followers_off")

  /**
   * This room is already in [duration] followers-only mode.
   */
  case AlreadyFollowersOn extends NoticeID("already_followers_on")

  /**
   * This room is not in unique-chat mode.
   */
  case AlreadyR9KOff extends NoticeID("already_r9k_off")

  /**
   * This room is already in unique-chat mode.
   */
  case AlreadyR9KOn extends NoticeID("already_r9k_on")

  /**
   * This room is not in slow mode.
   */
  case AlreadySlowOff extends NoticeID("already_slow_off")

  /**
   * This room is already in [duration]-second slow mode.
   */
  case AlreadySlowOn extends NoticeID("already_slow_on")

  /**
   * This room is not in subscribers-only mode.
   */
  case AlreadySubsOff extends NoticeID("already_subs_off")

  /**
   * This room is already in subscribers-only mode.
   */
  case AlreadySubsOn extends NoticeID("already_subs_on")

  /**
   * [user] is now auto hosting you for up to [number] viewers.
   */
  case AutohostReceive extends NoticeID("autohost_receive")

  /**
   * You cannot ban admin [user]. Please email support@twitch.tv if an admin is being abusive.
   */
  case BadBanAdmin extends NoticeID("bad_ban_admin")

  /**
   * You cannot ban anonymous users.
   */
  case BadBanAnon extends NoticeID("bad_ban_anon")

  /**
   * You cannot ban the broadcaster.
   */
  case BadBanBroadcaster extends NoticeID("bad_ban_broadcaster")

  /**
   * You cannot ban moderator [user] unless you are the owner of this channel.
   */
  case BadBanMod extends NoticeID("bad_ban_mod")

  /**
   * You cannot ban yourself.
   */
  case BadBanSelf extends NoticeID("bad_ban_self")

  /**
   * You cannot ban a staff [user]. Please email support@twitch.tv if a staff member is being abusive.
   */
  case BadBanStaff extends NoticeID("bad_ban_staff")

  /**
   * Failed to start the commercial.
   */
  case BadCommercialError extends NoticeID("bad_commercial_error")

  /**
   * You cannot delete the broadcaster’s messages.
   */
  case BadDeleteMessageBroadcaster extends NoticeID("bad_delete_message_broadcaster")

  /**
   * You cannot delete messages from another moderator [user].
   */
  case BadDeleteMessageMod extends NoticeID("bad_delete_message_mod")

  /**
   * There was a problem hosting [channel]. Please try again in a minute.
   */
  case BadHostError extends NoticeID("bad_host_error")

  /**
   * This channel is already hosting [channel].
   */
  case BadHostHosting extends NoticeID("bad_host_hosting")

  /**
   * Host target cannot be changed more than [number] times every half hour.
   */
  case BadHostRateExceeded extends NoticeID("bad_host_rate_exceeded")

  /**
   * This channel is unable to be hosted.
   */
  case BadHostRejected extends NoticeID("bad_host_rejected")

  /**
   * A channel cannot host itself.
   */
  case BadHostSelf extends NoticeID("bad_host_self")

  /**
   * [user] is banned in this channel. You must unban this user before granting mod status.
   */
  case BadModBanned extends NoticeID("bad_mod_banned")

  /**
   * [user] is already a moderator of this channel.
   */
  case BadModMod extends NoticeID("bad_mod_mod")

  /**
   * You cannot set slow delay to more than [number] seconds.
   */
  case BadSlowDuration extends NoticeID("bad_slow_duration")

  /**
   * You cannot timeout admin [user]. Please email support@twitch.tv if an admin is being abusive.
   */
  case BadTimeoutAdmin extends NoticeID("bad_timeout_admin")

  /**
   * You cannot timeout anonymous users.
   */
  case BadTimeoutAnon extends NoticeID("bad_timeout_anon")

  /**
   * You cannot timeout the broadcaster.
   */
  case BadTimeoutBroadcaster extends NoticeID("bad_timeout_broadcaster")

  /**
   * You cannot time a user out for more than [seconds].
   */
  case BadTimeoutDuration extends NoticeID("bad_timeout_duration")

  /**
   * You cannot timeout moderator [user] unless you are the owner of this channel.
   */
  case BadTimeoutMod extends NoticeID("bad_timeout_mod")

  /**
   * You cannot timeout yourself.
   */
  case BadTimeoutSelf extends NoticeID("bad_timeout_self")

  /**
   * You cannot timeout staff [user]. Please email support@twitch.tv if a staff member is being abusive.
   */
  case BadTimeoutStaff extends NoticeID("bad_timeout_staff")

  /**
   * [user] is not banned from this channel.
   */
  case BadUnbanNoBan extends NoticeID("bad_unban_no_ban")

  /**
   * There was a problem exiting host mode. Please try again in a minute.
   */
  case BadUnhostError extends NoticeID("bad_unhost_error")

  /**
   * [user] is not a moderator of this channel.
   */
  case BadUnmodMod extends NoticeID("bad_unmod_mod")

  /**
   * [user] is banned in this channel. You must unban this user before granting VIP status.
   */
  case BadVipGranteeBanned extends NoticeID("bad_vip_grantee_banned")

  /**
   * [user] is already a VIP of this channel.
   */
  case BadVipGranteeAlreadyVip extends NoticeID("bad_vip_grantee_already_vip")

  /**
   * Unable to add VIP. Visit the Achievements page on your dashboard to learn how to unlock additional VIP slots.
   */
  case BadVipMaxVipsReached extends NoticeID("bad_vip_max_vips_reached")

  /**
   * Unable to add VIP. Visit the Achievements page on your dashboard to learn how to unlock this feature.
   */
  case BadVipAchievementIncomplete extends NoticeID("bad_vip_achievement_incomplete")

  /**
   * [user] is not a VIP of this channel.
   */
  case BadUnvipGranteeNotVip extends NoticeID("bad_unvip_grantee_not_vip")

  /**
   * [user] is now banned from this channel.
   */
  case BanSuccess extends NoticeID("ban_success")

  /**
   * Commands available to you in this room (use /help for details): [list of commands] More help: https://help.twitch.tv/s/article/chat-commands.
   */
  case CmdsAvailable extends NoticeID("cmds_available")

  /**
   * Your color has been changed.
   */
  case ColorChanged extends NoticeID("color_changed")

  /**
   * Initiating [number] second commercial break. Keep in mind that your stream is still live and not everyone will get a commercial.
   */
  case CommercialSuccess extends NoticeID("commercial_success")

  /**
   * The message from [user] is now deleted.
   */
  case DeleteMessageSuccess extends NoticeID("delete_message_success")

  /**
   * You deleted a message from staff [user]. Please email support@twitch.tv if a staff member is being abusive.
   */
  case DeleteStaffMessageSuccess extends NoticeID("delete_staff_message_success")

  /**
   * This room is no longer in emote-only mode.
   */
  case EmoteOnlyOff extends NoticeID("emote_only_off")

  /**
   * This room is now in emote-only mode.
   */
  case EmoteOnlyOn extends NoticeID("emote_only_on")

  /**
   * This room is no longer in followers-only mode.
   */
  case FollowersOff extends NoticeID("followers_off")

  /**
   * This room is now in [duration] followers-only mode.
   */
  case FollowersOn extends NoticeID("followers_on")

  /**
   * This room is now in followers-only mode.
   */
  case FollowersOnZero extends NoticeID("followers_on_zero")

  /**
   * Exited host mode.
   */
  case HostOff extends NoticeID("host_off")

  /**
   * Now hosting [channel].
   */
  case HostOn extends NoticeID("host_on")

  /**
   * [channel] is now hosting you for up to [number] viewers.
   */
  case HostReceive extends NoticeID("host_receive")

  /**
   * [channel] is now hosting you.
   */
  case HostReceiveNoCount extends NoticeID("host_receive_no_count")

  /**
   * [channel] has gone offline. Exiting host mode.
   */
  case HostTargetWentOffline extends NoticeID("host_target_went_offline")

  /**
   * [number] host commands remaining this half hour.
   */
  case HostsRemaining extends NoticeID("hosts_remaining")

  /**
   * Invalid username: [user]
   */
  case InvalidUser extends NoticeID("invalid_user")

  /**
   * You have added [user] as a moderator of this channel.
   */
  case ModSuccess extends NoticeID("mod_success")

  /**
   * You are permanently banned from talking in [channel].
   */
  case MsgBanned extends NoticeID("msg_banned")

  /**
   * Your message was not sent because it contained too many unprocessable characters. If you believe this is an error, please rephrase and try again.
   */
  case MsgBadCharacters extends NoticeID("msg_bad_characters")

  /**
   * Your message was not sent because your account is not in good standing in this channel.
   */
  case MsgChannelBlocked extends NoticeID("msg_channel_blocked")

  /**
   * This channel does not exist or has been suspended.
   */
  case MsgChannelSuspended extends NoticeID("msg_channel_suspended")

  /**
   * Your message was not sent because it is identical to the previous one you sent, less than 30 seconds ago.
   */
  case MsgDuplicate extends NoticeID("msg_duplicate")

  /**
   * This room is in emote-only mode. You can find your currently available emoticons using the smiley in the chat text area.
   */
  case MsgEmoteOnly extends NoticeID("msg_emoteonly")

  /**
   * This room is in [duration] followers-only mode. Follow [channel] to join the community! Note: These msg_followers tags are kickbacks to a user who does not meet the criteria; that is, does not follow or has not followed long enough.
   */
  case MsgFollowersOnly extends NoticeID("msg_followersonly")

  /**
   * This room is in [duration1] followers-only mode. You have been following for [duration2]. Continue following to chat!
   */
  case MsgFollowersOnlyFollowed extends NoticeID("msg_followersonly_followed")

  /**
   * This room is in followers-only mode. Follow [channel] to join the community!
   */
  case MsgFollowersOnlyZero extends NoticeID("msg_followersonly_zero")

  /**
   * This room is in unique-chat mode and the message you attempted to send is not unique.
   */
  case MsgR9K extends NoticeID("msg_r9k")

  /**
   * Your message was not sent because you are sending messages too quickly.
   */
  case MsgRateLimit extends NoticeID("msg_ratelimit")

  /**
   * Hey! Your message is being checked by mods and has not been sent.
   */
  case MsgRejected extends NoticeID("msg_rejected")

  /**
   * Your message wasn’t posted due to conflicts with the channel’s moderation settings.
   */
  case MsgRejectedMandatory extends NoticeID("msg_rejected_mandatory")

  /**
   * A verified phone number is required to chat in this channel. Please visit https://www.twitch.tv/settings/security to verify your phone number.
   */
  case MsgRequiresVerifiedPhoneNumber extends NoticeID("msg_requires_verified_phone_number")

  /**
   * This room is in slow mode and you are sending messages too quickly. You will be able to talk again in [number] seconds.
   */
  case MsgSlowMode extends NoticeID("msg_slowmode")

  /**
   * This room is in subscribers only mode. To talk, purchase a channel subscription at https://www.twitch.tv/products/[broadcaster login name]/ticket?ref=subscriber_only_mode_chat.
   */
  case MsgSubsOnly extends NoticeID("msg_subsonly")

  /**
   * You don’t have permission to perform that action.
   */
  case MsgSuspended extends NoticeID("msg_suspended")

  /**
   * You are timed out for [number] more seconds.
   */
  case MsgTimedOut extends NoticeID("msg_timedout")

  /**
   * This room requires a verified account to chat. Please verify your account at https://www.twitch.tv/settings/security.
   */
  case MsgVerifiedEmail extends NoticeID("msg_verified_email")

  /**
   * No help available.
   */
  case NoHelp extends NoticeID("no_help")

  /**
   * There are no moderators of this channel.
   */
  case NoMods extends NoticeID("no_mods")

  /**
   * This channel does not have any VIPs.
   */
  case NoVips extends NoticeID("no_vips")

  /**
   * No channel is currently being hosted.
   */
  case NotHosting extends NoticeID("not_hosting")

  /**
   * You don’t have permission to perform that action.
   */
  case NoPermission extends NoticeID("no_permission")

  /**
   * This room is no longer in unique-chat mode.
   */
  case R9KOff extends NoticeID("r9k_off")

  /**
   * This room is now in unique-chat mode.
   */
  case R9KOn extends NoticeID("r9k_on")

  /**
   * You already have a raid in progress.
   */
  case RaidErrorAlreadyRaiding extends NoticeID("raid_error_already_raiding")

  /**
   * You cannot raid this channel.
   */
  case RaidErrorForbidden extends NoticeID("raid_error_forbidden")

  /**
   * A channel cannot raid itself.
   */
  case RaidErrorSelf extends NoticeID("raid_error_self")

  /**
   * Sorry, you have more viewers than the maximum currently supported by raids right now.
   */
  case RaidErrorTooManyViewers extends NoticeID("raid_error_too_many_viewers")

  /**
   * There was a problem raiding [channel]. Please try again in a minute.
   */
  case RaidErrorUnexpected extends NoticeID("raid_error_unexpected")

  /**
   * This channel is intended for mature audiences.
   */
  case RaidNoticeMature extends NoticeID("raid_notice_mature")

  /**
   * This channel has follower- or subscriber-only chat.
   */
  case RaidNoticeRestrictedChat extends NoticeID("raid_notice_restricted_chat")

  /**
   * The moderators of this channel are: [list of users]
   */
  case RoomMods extends NoticeID("room_mods")

  /**
   * This room is no longer in slow mode.
   */
  case SlowOff extends NoticeID("slow_off")

  /**
   * This room is now in slow mode. You may send messages every [number] seconds.
   */
  case SlowOn extends NoticeID("slow_on")

  /**
   * This room is no longer in subscribers-only mode.
   */
  case SubsOff extends NoticeID("subs_off")

  /**
   * This room is now in subscribers-only mode.
   */
  case SubsOn extends NoticeID("subs_on")

  /**
   * [user] is not timed out from this channel.
   */
  case TimeoutNoTimeout extends NoticeID("timeout_no_timeout")

  /**
   * [user] has been timed out for [duration].
   */
  case TimeoutSuccess extends NoticeID("timeout_success")

  /**
   * The community has closed channel [channel] due to Terms of Service violations.
   */
  case TosBan extends NoticeID("tos_ban")

  /**
   * Only turbo users can specify an arbitrary hex color. Use one of the following instead: <list of colors>.
   */
  case TurboOnlyColor extends NoticeID("turbo_only_color")

  /**
   * Sorry, “[command]” is not available through this client.
   */
  case UnavailableCommand extends NoticeID("unavailable_command")

  /**
   * [user] is no longer banned from this channel.
   */
  case UnbanSuccess extends NoticeID("unban_success")

  /**
   * You have removed [user] as a moderator of this channel.
   */
  case UnmodSuccess extends NoticeID("unmod_success")

  /**
   * You do not have an active raid.
   */
  case UnraidErrorNoActiveRaid extends NoticeID("unraid_error_no_active_raid")

  /**
   * There was a problem stopping the raid. Please try again in a minute.
   */
  case UnraidErrorUnexpected extends NoticeID("unraid_error_unexpected")

  /**
   * The raid has been canceled.
   */
  case UnraidSuccess extends NoticeID("unraid_success")

  /**
   * Unrecognized command: [command]
   */
  case UnrecognizedCmd extends NoticeID("unrecognized_cmd")

  /**
   * [user] is permanently banned. Use “/unban” to remove a ban.
   */
  case UntimeoutBanned extends NoticeID("untimeout_banned")

  /**
   * [user] is no longer timed out in this channel.
   */
  case UntimeoutSuccess extends NoticeID("untimeout_success")

  /**
   * You have removed [user] as a VIP of this channel.
   */
  case UnvipSuccess extends NoticeID("unvip_success")

  /**
   * Usage: “/ban [username] [reason]” Permanently prevent a user from chatting. Reason is optional and will be shown to the target and other moderators. Use “/unban” to remove a ban.
   */
  case UsageBan extends NoticeID("usage_ban")

  /**
   * Usage: “/clear”
   * Clear chat history for all users in this room.
   */
  case UsageClear extends NoticeID("usage_clear")

  /**
   * Usage: “/color” [color]
   * Change your username color. Color must be in hex (#000000) or one of the following: <list of colors>.
   */
  case UsageColor extends NoticeID("usage_color")

  /**
   * Usage: “/commercial [length]”
   * Triggers a commercial. Length (optional) must be a positive number of seconds.
   */
  case UsageCommercial extends NoticeID("usage_commercial")

  /**
   * Usage: “/disconnect”
   * Reconnects to chat.
   */
  case UsageDisconnect extends NoticeID("usage_disconnect")

  /**
   * Usage: “/delete <msg id>” - Deletes the specified message. For more information, see https://dev.twitch.tv/docs/irc/commands/#clearmsg-twitch-commands.
   */
  case UsageDelete extends NoticeID("usage_delete")

  /**
   * Usage: /emoteonlyoff”
   * Disables emote-only mode.
   */
  case UsageEmoteOnlyOff extends NoticeID("usage_emote_only_off")

  /**
   * Usage: “/emoteonly”
   * Enables emote-only mode (only emoticons may be used in chat). Use /emoteonlyoff to disable.
   */
  case UsageEmoteOnlyOn extends NoticeID("usage_emote_only_on")

  /**
   * Usage: /followersoff”
   * Disables followers-only mode.
   */
  case UsageFollowersOff extends NoticeID("usage_followers_off")

  /**
   * Usage: “/followers"
   * Enables followers-only mode (only users who have followed for “duration” may chat). Examples: “30m”, “1 week”, “5 days 12 hours”. Must be less than 3 months.
   */
  case UsageFollowersOn extends NoticeID("usage_followers_on")

  /**
   * Usage: “/help”
   * Lists the commands available to you in this room.
   */
  case UsageHelp extends NoticeID("usage_help")

  /**
   * Usage: “/host [channel]“
   * Host another channel. Use “/unhost” to unset host mode.
   */
  case UsageHost extends NoticeID("usage_host")

  /**
   * Usage: “/marker <optional comment>“
   * Adds a stream marker (with an optional comment, max 140 characters) at the current timestamp. You can use markers in the Highlighter for easier editing.
   */
  case UsageMarker extends NoticeID("usage_marker")

  /**
   * Usage: “/me <message>” - Express an action in the third-person.
   */
  case UsageMe extends NoticeID("usage_me")

  /**
   * Usage: “/mod [username]” - Grant moderator status to a user. Use “/mods” to list the moderators of this channel.
   */
  case UsageMod extends NoticeID("usage_mod")

  /**
   * Usage: “/mods”
   * Lists the moderators of this channel.
   */
  case UsageMods extends NoticeID("usage_mods")

  /**
   * Usage: “/uniquechatoff” - Disables unique-chat mode.
   */
  case UsageR9KOff extends NoticeID("usage_r9k_off")

  /**
   * Usage: “/uniquechat” - Enables unique-chat mode. Use “/uniquechatoff” to disable.
   */
  case UsageR9KOn extends NoticeID("usage_r9k_on")

  /**
   * Usage: “/raid [channel]“
   * Raid another channel.
   * Use “/unraid” to cancel the Raid.
   */
  case UsageRaid extends NoticeID("usage_raid")

  /**
   * Usage: “/slowoff”
   * Disables slow mode.
   */
  case UsageSlowOff extends NoticeID("usage_slow_off")

  /**
   * Usage: “/slow” [duration]
   * Enables slow mode (limit how often users may send messages). Duration (optional, default=[number]) must be a positive integer number of seconds.
   * Use “/slowoff” to disable.
   */
  case UsageSlowOn extends NoticeID("usage_slow_on")

  /**
   * Usage: “/subscribersoff”
   * Disables subscribers-only mode.
   */
  case UsageSubsOff extends NoticeID("usage_subs_off")

  /**
   * Usage: “/subscribers”
   * Enables subscribers-only mode (only subscribers may chat in this channel).
   * Use “/subscribersoff” to disable.
   */
  case UsageSubsOn extends NoticeID("usage_subs_on")

  /**
   * Usage: “/timeout [username] [duration][time unit] [reason]”
   * Temporarily prevent a user from chatting. Duration (optional, default=10 minutes) must be a positive integer; time unit (optional, default=s) must be one of s, m, h, d, w; maximum duration is 2 weeks. Combinations like 1d2h are also allowed. Reason is optional and will be shown to the target user and other moderators.
   * Use “untimeout” to remove a timeout.
   */
  case UsageTimeout extends NoticeID("usage_timeout")

  /**
   * Usage: “/unban [username]“
   * Removes a ban on a user.
   */
  case UsageUnban extends NoticeID("usage_unban")

  /**
   * Usage: “/unhost”
   * Stop hosting another channel.
   */
  case UsageUnhost extends NoticeID("usage_unhost")

  /**
   * Usage: “/unmod [username]” - Revoke moderator status from a user. Use “/mods” to list the moderators of this channel.
   */
  case UsageUnmod extends NoticeID("usage_unmod")

  /**
   * Usage: “/unraid”
   * Cancel the Raid.
   */
  case UsageUnraid extends NoticeID("usage_unraid")

  /**
   * Usage: “/untimeout [username]“
   * Removes a timeout on a user.
   */
  case UsageUntimeout extends NoticeID("usage_untimeout")

  /**
   * Usage: “/unvip [username]” - Revoke VIP status from a user. Use “/vips” to list the VIPs of this channel.
   */
  case UsageUnvip extends NoticeID("usage_unvip")

  /**
   * Usage: “/user” [username] - Display information about a specific user on this channel.
   */
  case UsageUser extends NoticeID("usage_user")

  /**
   * Usage: “/vip [username]” - Grant VIP status to a user. Use “/vips” to list the VIPs of this channel.
   */
  case UsageVip extends NoticeID("usage_vip")

  /**
   * Usage: “/vips” - Lists the VIPs of this channel.
   */
  case UsageVips extends NoticeID("usage_vips")

  /**
   * Usage: “/w [username] [message]”
   */
  case UsageWhisper extends NoticeID("usage_whisper")

  /**
   * You have added [user] as a vip of this channel.
   */
  case VipSuccess extends NoticeID("vip_success")

  /**
   * The VIPs of this channel are: [list of users].
   */
  case VipsSuccess extends NoticeID("vips_success")

  /**
   * You have been banned from sending whispers.
   */
  case WhisperBanned extends NoticeID("whisper_banned")

  /**
   * That user has been banned from receiving whispers.
   */
  case WhisperBannedRecipient extends NoticeID("whisper_banned_recipient")

  /**
   * No user matching that username.
   */
  case WhisperInvalidLogin extends NoticeID("whisper_invalid_login")

  /**
   * You cannot whisper to yourself.
   */
  case WhisperInvalidSelf extends NoticeID("whisper_invalid_self")

  /**
   * You are sending whispers too fast. Try again in a minute.
   */
  case WhisperLimitPerMin extends NoticeID("whisper_limit_per_min")

  /**
   * You are sending whispers too fast. Try again in a second.
   */
  case WhisperLimitPerSec extends NoticeID("whisper_limit_per_sec")

  /**
   * Your settings prevent you from sending this whisper.
   */
  case WhisperRestricted extends NoticeID("whisper_restricted")

  /**
   * That user’s settings prevent them from receiving this whisper.
   */
  case WhisperRestrictedRecipient extends NoticeID("whisper_restricted_recipient")
}

object NoticeID {
  def apply(raw: String): Option[NoticeID] = NoticeID.values.find(x => x.rawValue == raw)
}