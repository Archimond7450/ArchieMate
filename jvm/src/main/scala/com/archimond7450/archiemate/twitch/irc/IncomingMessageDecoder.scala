package com.archimond7450.archiemate.twitch.irc

import com.archimond7450.archiemate.twitch.irc.IncomingMessages.*
import org.slf4j.LoggerFactory

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
import scala.util.matching.Regex

class IncomingMessageDecoder {
  private val log = LoggerFactory.getLogger(getClass)

  private val clearChatRegex: Regex = "(@\\S+)\\s:tmi\\.twitch\\.tv\\sCLEARCHAT\\s#(\\S+)(\\s:(\\S+))?".r
  private val clearMsgRegex: Regex = "(@\\S+)\\s:tmi\\.twitch\\.tv\\sCLEARMSG\\s#(\\S+)\\s:(.*)".r
  private val globalUserStateRegex: Regex = "(@\\S+)\\s:tmi\\.twitch\\.tv\\sGLOBALUSERSTATE".r
  private val hostTargetRegex: Regex = ":tmi\\.twitch\\.tv\\sHOSTTARGET\\s#(\\S+)\\s:(\\S+)\\s(\\d+)".r
  private val joinRegex: Regex = ":(\\S+)!\\1@\\1\\.tmi\\.twitch\\.tv\\sJOIN\\s#(\\S+)".r
  private val noticeRegex: Regex = "(@\\S+)\\s:tmi\\.twitch\\.tv\\sNOTICE\\s#(\\S+)\\s:(.*)".r
  private val reconnectString: String = ":tmi.twitch.tv RECONNECT"
  private val partRegex: Regex = ":(\\S+)!\\1@\\1\\.tmi\\.twitch\\.tv\\sPART\\s#(\\S+)".r
  private val pingString: String = "PING :tmi.twitch.tv"
  private val privMsgRegex: Regex = "(@\\S+)\\s:(\\S+)!\\2@\\2\\.tmi\\.twitch\\.tv\\sPRIVMSG\\s#(\\S+)\\s:(.*)".r
  private val roomStateRegex: Regex = "(@\\S+)\\s:tmi\\.twitch\\.tv\\sROOMSTATE\\s#(\\S+)".r
  private val userNoticeRegex: Regex = "(@\\S+)\\s:tmi\\.twitch\\.tv\\sUSERNOTICE\\s#(\\S+)(\\s:(.*))?".r
  private val userStateRegex: Regex = "(@\\S+)\\s:tmi\\.twitch\\.tv\\sUSERSTATE\\s#(\\S+)".r
  private val whisperRegex: Regex = "(@\\S+)\\s:(\\S+)!\\2@\\2\\.tmi\\.twitch\\.tv\\sWHISPER\\s(\\S+)\\s:(.*)".r
  private val _353Regex: Regex = "(\\w+)\\.tmi\\.twitch\\.tv\\s353\\s\\1\\s=\\s#(\\w+)\\s:(.*)".r
  private val _366Regex: Regex = "(\\w+)\\.tmi\\.twitch\\.tv\\s366\\s\\1\\s#(\\w+)\\s:End of /NAMES list".r
  private val earlyNoticeRegex: Regex = ":tmi\\.twitch\\.tv\\sNOTICE\\s\\*\\s:(.*)".r
  private val capReqAckRegex: Regex = ":tmi\\.twitch\\.tv\\sCAP\\s\\*\\sACK\\s:(.*)".r
  private val capReqNakRegex: Regex = ":tmi\\.twitch\\.tv\\sCAP\\s\\*\\sNAK\\s:(.*)".r
  private val numericMessageRegex: Regex = ":tmi\\.twitch\\.tv\\s(\\d{3})\\s(\\S+)\\s:(.*)".r

  private object Tags {
    val banDuration = "ban-duration"
    val roomId = "room-id"
    val targetUserId = "target-user-id"
    val tmiSentTs = "tmi-sent-ts"
    val login = "login"
    val targetMsgId = "target-msg-id"
    val badgeInfo = "badge-info"
    val badges = "badges"
    val color = "color"
    val displayName = "display-name"
    val emoteSets = "emote-sets"
    val turbo = "turbo"
    val userId = "user-id"
    val userType = "user-type"
    val msgId = "msg-id"
    val emotes = "emotes"
    val mod = "mod"
    val subscriber = "subscriber"
    val vip = "vip"
    val bits = "bits"
    val id = "id"
    val pinnedChatPaidAmount = "pinned-chat-paid-amount"
    val pinnedChatPaidCurrency = "pinned-chat-paid-currency"
    val pinnedChatPaidExponent = "pinned-chat-paid-exponent"
    val pinnedChatPaidLevel = "pinned-chat-paid-level"
    val pinnedChatPaidIsSystemMessage = "pinned-chat-paid-is-system-message"
    val replyParentMsgId = "reply-parent-msg-id"
    val replyParentUserId = "reply-parent-user-id"
    val replyParentUserLogin = "reply-parent-user-login"
    val replyParentDisplayName = "reply-parent-display-name"
    val replyParentMsgBody = "reply-parent-msg-body"
    val replyThreadParentMsgId = "reply-thread-parent-msg-id"
    val replyThreadParentUserLogin = "reply-thread-parent-user-login"
    val emoteOnly = "emote-only"
    val followersOnly = "followers-only"
    val r9k = "r9k"
    val slow = "slow"
    val subsOnly = "subs-only"
    val systemMessage = "system-msg"
    val msgParamCumulativeMonths = "msg-param-cumulative-months"
    val msgParamDisplayName = "msg-param-displayName"
    val msgParamLogin = "msg-param-login"
    val msgParamMonths = "msg-param-months"
    val msgParamPromoGiftTotal = "msg-param-promo-gift-total"
    val msgParamPromoName = "msg-param-promo-name"
    val msgParamRecipientDisplayName = "msg-param-recipient-display-name"
    val msgParamRecipientId = "msg-param-recipient-id"
    val msgParamRecipientUserName = "msg-param-recipient-user-name"
    val msgParamSenderLogin = "msg-param-sender-login"
    val msgParamSenderName = "msg-param-sender-name"
    val msgParamShouldShareStreak = "msg-param-should-share-streak"
    val msgParamStreakMonths = "msg-param-streak-months"
    val msgParamSubPlan = "msg-param-sub-plan"
    val msgParamSubPlanName = "msg-param-sub-plan-name"
    val msgParamViewerCount = "msg-param-viewerCount"
    val msgParamRitualName = "msg-param-ritual-name"
    val msgParamThreshold = "msg-param-threshold"
    val msgParamGiftMonths = "msg-param-gift-months"
    val messageId = "message-id"
    val threadId = "thread-id"
    val clientNonce = "client-nonce"
    val firstMessage = "first-msg"
    val flags = "flags"
    val returningChatter = "returning-chatter"
    val pinnedChatPaidCanonicalkAmount = "pinned-chat-paid-canonical-amount"
    val rituals = "rituals"
    val msgParamCategory = "msg-param-category"
    val msgParamCopoReward = "msg-param-copoReward"
    val msgParamId = "msg-param-id"
    val msgParamValue = "msg-param-value"
    val msgParamPriorGifterAnonymous = "msg-param-prior-gifter-anonymous"
    val msgParamPriorGifterDisplayName = "msg-param-prior-gifter-display-name"
    val msgParamPriorGifterId = "msg-param-prior-gifter-id"
    val msgParamPriorGifterUserName = "msg-param-prior-gifter-user-name"
    val msgParamCommunityGiftId = "msg-param-community-gift-id"
    val msgParamGoalContributionType = "msg-param-goal-contribution-type"
    val msgParamGoalCurrentContributions = "msg-param-goal-current-contributions"
    val msgParamGoalTargetContributions = "msg-param-goal-target-contributions"
    val msgParamGoalUserContributions = "msg-param-goal-user-contributions"
    val msgParamMassGiftCount = "msg-param-mass-gift-count"
    val msgParamOriginId = "msg-param-origin-id"
    val msgParamSenderCount = "msg-param-sender-count"
  }

  private val tagsDecoder: TagsDecoder = new TagsDecoder
  private val badgeInfoDecoder: BadgeInfoDecoder = new BadgeInfoDecoder
  private val badgesDecoder: BadgesDecoder = new BadgesDecoder
  private val emoteSetsDecoder: EmoteSetsDecoder = new EmoteSetsDecoder
  private val emotesDecoder: EmotesDecoder = new EmotesDecoder

  def decode(message: String): IncomingMessage = message match {
    case message @ clearChatRegex(tags, channelName, _, null) =>
      tagsDecoder.decode(tags) match {
        case Some(decodedTags) => decodeClearChatWithoutTarget(message, decodedTags, channelName)
        case None => UnknownMessage(message)
      }
    case message @ clearChatRegex(tags, channelName, _, userName) =>
      tagsDecoder.decode(tags) match {
        case Some(decodedTags) => decodeClearChatWithTarget(message, decodedTags, channelName, userName)
        case None => UnknownMessage(message)
      }
    case message @ clearMsgRegex(tags, channelName, clearedMessage) => tagsDecoder.decode(tags) match {
      case Some(decodedTags) => decodeClearMsg(message, decodedTags, channelName, clearedMessage)
      case None => UnknownMessage(message)
    }
    case message @ globalUserStateRegex(tags) => tagsDecoder.decode(tags) match {
      case Some(decodedTags) => decodeGlobalUserState(message, decodedTags)
      case None => UnknownMessage(message)
    }
    case hostTargetRegex(hostingChannelName, hostedChannelName, viewers) =>
      decodeHostTarget(hostingChannelName, hostedChannelName, viewers.toInt)
    case joinRegex(userName: String, channelName: String) => Join(userName, channelName)
    case message @ noticeRegex(tags, channelName, noticeMessage) => tagsDecoder.decode(tags) match {
      case Some(decodedTags) => decodeNotice(message, decodedTags, channelName, noticeMessage)
      case None =>
        log.error("No tags for Twitch notice message!")
        UnknownMessage(message)
    }
    case reconnect if reconnect == reconnectString => Reconnect
    case partRegex(userName, channelName) => Part(userName, channelName)
    case ping if ping == pingString => Ping
    case message @ privMsgRegex(tags, userName, channelName, privMsgMessage) => tagsDecoder.decode(tags) match {
      case Some(decodedTags) => decodePrivMsg(message, decodedTags, userName, channelName, privMsgMessage)
      case None => UnknownMessage(message)
    }
    case message @ roomStateRegex(tags, channelName) => tagsDecoder.decode(tags) match {
      case Some(decodedTags) => decodeRoomState(decodedTags, channelName)
      case None => UnknownMessage(message)
    }
    case message @ userNoticeRegex(tags, channelName, _, null) => tagsDecoder.decode(tags) match {
      case Some(decodedTags) => decodeUserNotice(message, decodedTags, channelName, None)
      case None => UnknownMessage(message)
    }
    case message @ userNoticeRegex(tags, channelName, _, userNoticeMessage) => tagsDecoder.decode(tags) match {
      case Some(decodedTags) => decodeUserNotice(message, decodedTags, channelName, Some(userNoticeMessage))
      case None => UnknownMessage(message)
    }
    case message @ userStateRegex(tags, channelName) => tagsDecoder.decode(tags) match {
      case Some(decodedTags) => decodeUserState(message, decodedTags, channelName)
      case None => UnknownMessage(message)
    }
    case message @ whisperRegex(tags, userName, userNameFrom, whisperMessage) => tagsDecoder.decode(tags) match {
      case Some(decodedTags) => decodeWhisper(message, decodedTags, userName, userNameFrom, whisperMessage)
      case None => UnknownMessage(message)
    }
    case _353Regex(userName, channelName, users) => _353(userName, channelName, users.split("\\s").toSet)
    case _366Regex(userName, channelName) => _366(userName, channelName)
    case earlyNoticeRegex(message) => EarlyNotice(message)
    case capReqAckRegex(capabilities) => CapabilityRequestResponse(succeeded = true, capabilities.split("\\s").toSet)
    case capReqNakRegex(capabilities) => CapabilityRequestResponse(succeeded = false, capabilities.split("\\s").toSet)
    case message @ numericMessageRegex(number, user, actualMessage) => number.toIntOption match {
      case Some(correctNumber) => NumericMessage(correctNumber, user, actualMessage)
      case None => UnknownMessage(message) // Included just to prevent MatchError - the regex should take care of number
    }
    case unknownMessage => UnknownMessage(unknownMessage)
  }

  private val requiredClearChatWithoutTargetTags = List(Tags.roomId, Tags.tmiSentTs)

  private def decodeClearChatWithoutTarget(message: String, tags: Map[String, String], channelName: String): IncomingMessage =
    (tags.get(Tags.roomId), tags.get(Tags.tmiSentTs)) match {
      case (Some(roomId), Some(tmiSentTs)) =>
        (roomId.toLongOption, tmiSentTs.toLongOption) match {
          case (Some(validRoomId), Some(validTmiSentTs)) => ClearChat.Clear(
            roomId = validRoomId,
            tmiSentTimestamp = unixTimestampToOffsetDateTime(validTmiSentTs),
            channelName = channelName
          )
          case _ =>
            log.error("invalid roomId or tmiSentTs tags: {}", tags)
            UnknownMessage(message)
        }
      case _ =>
        log.error("The following required tags are missing in the CLEARCHAT message: {} | All decoded tags: {}", getMissingTagsString(requiredClearChatWithoutTargetTags, tags), tags)
        UnknownMessage(message)
    }

  private val requiredClearChatWithTargetTags = List(Tags.roomId, Tags.targetUserId, Tags.tmiSentTs)

  private def decodeClearChatWithTarget(message: String, tags: Map[String, String], channelName: String, userName: String): IncomingMessage =
    (tags.get(Tags.banDuration), tags.get(Tags.roomId), tags.get(Tags.targetUserId), tags.get(Tags.tmiSentTs)) match {
      case (Some(banDuration), Some(roomId), Some(targetUserId), Some(tmiSentTs)) =>
        (banDuration.toLongOption, roomId.toLongOption, targetUserId.toLongOption, tmiSentTs.toLongOption) match {
          case (Some(validBanDuration), Some(validRoomId), Some(validTargetUserId), Some(validTmiSentTs)) =>
            ClearChat.Timeout(
              banDurationSec = Duration(validBanDuration, TimeUnit.SECONDS),
              roomId = validRoomId,
              targetUserId = validTargetUserId,
              tmiSentTimestamp = unixTimestampToOffsetDateTime(validTmiSentTs),
              channelName = channelName,
              userName = userName
            )
          case _ =>
            log.error("Invalid banDuration, roomId, targetUserId or tmiSentTs tags: {}", tags)
            UnknownMessage(message)
        }
      case (None, Some(roomId), Some(targetUserId), Some(tmiSentTs)) =>
        (roomId.toLongOption, targetUserId.toLongOption, tmiSentTs.toLongOption) match {
          case (Some(validRoomId), Some(validTargetUserId), Some(validTmiSentTs)) =>
            ClearChat.Ban(
              roomId = validRoomId,
              targetUserId = validTargetUserId,
              tmiSentTimestamp = unixTimestampToOffsetDateTime(validTmiSentTs),
              channelName = channelName,
              userName = userName
            )
          case _ =>
            log.error("Invalid roomId, targetUserId or tmiSentTs tags: {}", tags)
            UnknownMessage(message)
        }
      case _ =>
        log.error("The following required tags are missing in the CLEARCHAT message: {} | All decoded tags: {}", getMissingTagsString(requiredClearChatWithTargetTags, tags), tags)
        UnknownMessage(message)
    }

  private val requiredClearMsgTags = List(Tags.roomId, Tags.tmiSentTs)

  private def decodeClearMsg(message: String, tags: Map[String, String], channelName: String, clearedMessage: String): IncomingMessage = {
    (tags.get(Tags.login), tags.get(Tags.roomId), tags.get(Tags.targetMsgId), tags.get(Tags.tmiSentTs)) match {
      case (Some(login), Some(roomId), Some(targetMsgId), Some(tmiSentTs)) =>
        if (roomId == "") {
          tmiSentTs.toLongOption match {
            case Some(validTmiSentTs) =>
              ClearMsg(
                login = login,
                roomId = None,
                targetMsgId = targetMsgId,
                tmiSentTimestamp = unixTimestampToOffsetDateTime(validTmiSentTs),
                channelName = channelName,
                message = clearedMessage
              )
            case _ =>
              log.error("Invalid tmiSentTs tag: {}", tmiSentTs)
              UnknownMessage(message)
          }
        } else {
          (roomId.toIntOption, tmiSentTs.toLongOption) match {
            case (Some(validRoomId), Some(validTmiSentTs)) =>
              ClearMsg(
                login = login,
                roomId = Some(validRoomId),
                targetMsgId = targetMsgId,
                tmiSentTimestamp = unixTimestampToOffsetDateTime(validTmiSentTs),
                channelName = channelName,
                message = clearedMessage)
            case _ =>
              log.error("Invalid roomId or tmiSentTs tags: {}", tags)
              UnknownMessage(message)
          }
        }
      case _ =>
        log.error("The following required tags are missing in the CLEARMSG message: {} | All decoded tags: {}", getMissingTagsString(requiredClearMsgTags, tags), tags)
        UnknownMessage(message)
    }
  }

  private val requiredGlobalUserStateTags = List(Tags.badgeInfo, Tags.badges, Tags.color, Tags.displayName, Tags.emoteSets, Tags.userId, Tags.userType)

  private def decodeGlobalUserState(message: String, tags: Map[String, String]): IncomingMessage = {
    (
      tags.get(Tags.badgeInfo),
      tags.get(Tags.badges),
      tags.get(Tags.color),
      tags.get(Tags.displayName),
      tags.get(Tags.emoteSets),
      tags.get(Tags.userId),
      tags.get(Tags.userType)
    ) match {
      case (
        Some(badgeInfo),
        Some(badges),
        Some(color),
        Some(displayName),
        Some(emoteSets),
        Some(userId),
        Some(userType)) =>
        userId.toIntOption match {
          case Some(validUserId) =>
            GlobalUserState(
              badgeInfo = badgeInfoDecoder.decode(badgeInfo),
              badges = badgesDecoder.decode(badges),
              color = color,
              displayName = displayName,
              emoteSets = emoteSetsDecoder.decode(emoteSets),
              turbo = strIntToBoolean(tags.getOrElse(Tags.turbo, "0")),
              userId = validUserId,
              userType = TwitchUserType(userType).getOrElse(TwitchUserType.NormalUser)
            )
          case _ =>
            log.error("Cannot convert userId to Int: {}", userId)
            UnknownMessage(message)
        }
      case _ =>
        log.error("The following required tags are missing in the GLOBALUSERSTATE message: {} | All decoded tags: {}", getMissingTagsString(requiredGlobalUserStateTags, tags), tags)
        UnknownMessage(message)
    }
  }

  private def decodeHostTarget(hostingChannelName: String, hostedChannelName: String, viewers: Int): IncomingMessage = {
    hostedChannelName match {
      case "-" => HostTarget.End(
        hostingChannelName = hostingChannelName,
        viewers = viewers
      )
      case _ => HostTarget.Start(
        hostingChannelName = hostingChannelName,
        hostedChannelName = hostedChannelName,
        viewers = viewers
      )
    }
  }

  private def decodeNotice(message: String, tags: Map[String, String], channelName: String, noticeMessage: String): IncomingMessage = {
    tags.get(Tags.msgId) match {
      case Some(presentMsgId) =>
        NoticeID(presentMsgId) match {
          case Some(validNoticeId) =>
            Notice(validNoticeId, channelName, noticeMessage, tags.get(Tags.targetUserId).flatMap(_.toIntOption))
          case None =>
            log.error("Unrecognized Twitch notice msg-id {} - treating as unknown message", presentMsgId)
            UnknownMessage(message)
        }
      case _ =>
        log.error("Missing tag {} for Twitch notice IRC message!", Tags.msgId)
        UnknownMessage(message)
    }
  }

  private val requiredPrivMsgHypeChatTags = List(Tags.pinnedChatPaidAmount, Tags.pinnedChatPaidCurrency, Tags.pinnedChatPaidExponent, Tags.pinnedChatPaidLevel, Tags.pinnedChatPaidIsSystemMessage)
  private val requiredPrivMsgReplyTags = List(Tags.replyParentMsgId, Tags.replyParentUserId, Tags.replyParentUserLogin, Tags.replyParentDisplayName, Tags.replyParentMsgBody, Tags.replyThreadParentMsgId, Tags.replyThreadParentUserLogin)
  private val requiredPrivMsgTags = List(Tags.badgeInfo, Tags.badges, Tags.color, Tags.emotes, Tags.roomId, Tags.tmiSentTs, Tags.userId, Tags.userType)

  private def decodePrivMsg(message: String, tags: Map[String, String], userName: String, channelName: String, privMsgMessage: String): IncomingMessage = {
    val hypeChat: Option[PrivMsg.HypeChat] = (tags.get(Tags.pinnedChatPaidAmount), tags.get(Tags.pinnedChatPaidCurrency), tags.get(Tags.pinnedChatPaidExponent), tags.get(Tags.pinnedChatPaidLevel), tags.get(Tags.pinnedChatPaidIsSystemMessage)) match {
      case (Some(presentAmount), Some(presentCurrency), Some(presentExponent), Some(presentLevel), Some(presentIsSystemMessage)) =>
        (presentAmount.toIntOption, presentExponent.toIntOption) match {
          case (Some(validAmount), Some(validExponent)) =>
            Some(PrivMsg.HypeChat(validAmount, presentCurrency, validExponent, presentLevel, strIntToBoolean(presentIsSystemMessage), tags.get(Tags.pinnedChatPaidCanonicalkAmount).flatMap(_.toIntOption)))
          case _ =>
            log.error("Invalid {} or {} tags: {}", Tags.pinnedChatPaidAmount, Tags.pinnedChatPaidExponent, tags)
            None
        }
      case _ =>
        log.info("PRIVMSG does not contain hype chat tags (these were not found: {}) | Detected tags: {}", getMissingTagsString(requiredPrivMsgHypeChatTags, tags), tags)
        None
    }

    val reply: Option[PrivMsg.Reply] = (tags.get(Tags.replyParentMsgId), tags.get(Tags.replyParentUserId), tags.get(Tags.replyParentUserLogin), tags.get(Tags.replyParentDisplayName), tags.get(Tags.replyParentMsgBody), tags.get(Tags.replyThreadParentMsgId), tags.get(Tags.replyThreadParentUserLogin)) match {
      case (Some(presentParentMessageId), Some(presentParentUserId), Some(presentParentUserLogin), Some(presentParentDisplayName), Some(presentParentMessageBody), Some(presentThreadParentMessageId), Some(presentThreadParentUserLogin)) =>
        presentParentUserId.toIntOption match {
          case Some(validParentUserId) => Some(PrivMsg.Reply(presentParentMessageId, validParentUserId, presentParentUserLogin, presentParentDisplayName, presentParentMessageBody, presentThreadParentMessageId, presentThreadParentUserLogin))
          case _ =>
            log.error("Invalid reply-parent-user-id tag: {}", presentParentUserId)
            None
        }
      case _ =>
        log.info("PRIVMSG does not contain reply tags (these were not found: {}) | Detected tags: {}", getMissingTagsString(requiredPrivMsgReplyTags, tags), tags)
        None
    }

    (
      tags.get(Tags.badgeInfo),
      tags.get(Tags.badges),
      tags.get(Tags.color),
      tags.get(Tags.emotes),
      tags.get(Tags.roomId),
      tags.get(Tags.tmiSentTs),
      tags.get(Tags.userId),
      tags.get(Tags.userType),
    ) match {
      case (
        Some(presentBadgeInfo),
        Some(presentBadges),
        Some(presentColor),
        Some(presentEmotes),
        Some(presentRoomId),
        Some(presentTmiSentTimestamp),
        Some(presentUserId),
        Some(presentUserType),
        ) =>
        (presentRoomId.toIntOption, presentTmiSentTimestamp.toLongOption, presentUserId.toIntOption) match {
          case (Some(validRoomId), Some(validTmiSentTimestamp), Some(validUserId)) =>
            PrivMsg(
              badgeInfo = badgeInfoDecoder.decode(presentBadgeInfo),
              badges = badgesDecoder.decode(presentBadges),
              color = presentColor,
              displayName = tags.getOrElse(Tags.displayName, userName),
              emoteOnly = strIntToBoolean(tags.getOrElse(Tags.emoteOnly, "0")),
              emotes = emotesDecoder.decode(presentEmotes),
              firstMessage = strIntToBoolean(tags.getOrElse(Tags.firstMessage, "0")),
              mod = strIntToBoolean(tags.getOrElse(Tags.mod, "0")),
              returningChatter = strIntToBoolean(tags.getOrElse(Tags.returningChatter, "0")),
              roomId = validRoomId,
              subscriber = strIntToBoolean(tags.getOrElse(Tags.subscriber, "0")),
              tmiSentTimestamp = unixTimestampToOffsetDateTime(validTmiSentTimestamp),
              turbo = strIntToBoolean(tags.getOrElse(Tags.turbo, "0")),
              userId = validUserId,
              userType = TwitchUserType(presentUserType).getOrElse(TwitchUserType.NormalUser),
              vip = strIntToBoolean(tags.getOrElse(Tags.vip, "0")),
              userName = userName,
              channelName = channelName,
              message = privMsgMessage,
              bits = tags.getOrElse(Tags.bits, "0").toInt,
              clientNonce = tags.get(Tags.clientNonce),
              id = tags.get(Tags.id),
              flags = tags.get(Tags.flags),
              hypeChat = hypeChat,
              reply = reply)
          case _ =>
            log.error("One or more tags could not be converted to number in PRIVMSG message: {}", tags)
            UnknownMessage(message)
        }
      case _ =>
        log.error("The following required tags are missing in the PRIVMSG message: {} | All decoded tags: {}", getMissingTagsString(requiredPrivMsgTags, tags), tags)
        UnknownMessage(message)
    }
  }

  private def decodeRoomState(tags: Map[String, String], channelName: String): IncomingMessage = {
    RoomState(
      channelName = channelName,
      emoteOnly = tags.get(Tags.emoteOnly).map(strIntToBoolean),
      followersOnly = tags.get(Tags.followersOnly).flatMap(_.toIntOption),
      r9k = tags.get(Tags.r9k).map(strIntToBoolean),
      roomId = tags.get(Tags.roomId).flatMap(_.toIntOption),
      slow = tags.get(Tags.slow).flatMap(_.toIntOption),
      subsOnly = tags.get(Tags.subsOnly).map(strIntToBoolean),
      rituals = tags.get(Tags.rituals).map(strIntToBoolean))
  }

  private val requiredUserNoticeTags = List(
    Tags.badgeInfo,
    Tags.badges,
    Tags.color,
    Tags.emotes,
    Tags.id,
    Tags.login,
    Tags.roomId,
    Tags.systemMessage,
    Tags.tmiSentTs,
    Tags.userId,
    Tags.userType
  )
  private val requiredUserNoticeSubTags = List(
    Tags.msgParamCumulativeMonths,
    Tags.msgParamShouldShareStreak,
    Tags.msgParamStreakMonths,
    Tags.msgParamSubPlan,
    Tags.msgParamSubPlanName
  )
  private val requiredUserNoticeResubTags = List(
    Tags.msgParamCumulativeMonths,
    Tags.msgParamShouldShareStreak,
    Tags.msgParamStreakMonths,
    Tags.msgParamSubPlan,
    Tags.msgParamSubPlanName
  )
  private val requiredUserNoticeSubGiftTags = List(
    Tags.msgParamMonths,
    Tags.msgParamRecipientDisplayName,
    Tags.msgParamRecipientId,
    Tags.msgParamRecipientUserName,
    Tags.msgParamSubPlan,
    Tags.msgParamSubPlanName
  )
  private val requiredUserNoticeSubMysteryGiftTags = List(
    Tags.msgParamCommunityGiftId,
    Tags.msgParamGoalContributionType,
    Tags.msgParamGoalCurrentContributions,
    Tags.msgParamGoalTargetContributions,
    Tags.msgParamGoalUserContributions,
    Tags.msgParamMassGiftCount,
    Tags.msgParamOriginId,
    Tags.msgParamSenderCount,
    Tags.msgParamSubPlan
  )
  private val requiredUserNoticeGiftPaidUpgradeTags = List(
    Tags.msgParamPromoGiftTotal,
    Tags.msgParamPromoName,
    Tags.msgParamSenderLogin,
    Tags.msgParamSenderName
  )
  private val requiredUserNoticeAnonGiftPaidUpgradeTags = List(
    Tags.msgParamPromoGiftTotal,
    Tags.msgParamPromoName
  )
  private val requiredUserNoticeRaidTags = List(
    Tags.msgParamDisplayName,
    Tags.msgParamLogin,
    Tags.msgParamViewerCount
  )
  private val requiredUserNoticeBitsBadgeTierTags = List(
    Tags.msgParamThreshold
  )
  private val requiredUserNoticeViewerMilestoneWatchStreakTags = List(
    Tags.msgParamCopoReward,
    Tags.msgParamId,
    Tags.msgParamValue
  )
  private val requiredUserNoticeStandardPayForwardTags = List(
    Tags.msgParamPriorGifterAnonymous,
    Tags.msgParamPriorGifterDisplayName,
    Tags.msgParamPriorGifterId,
    Tags.msgParamPriorGifterUserName,
    Tags.msgParamRecipientDisplayName,
    Tags.msgParamRecipientId,
    Tags.msgParamRecipientUserName
  )

  private def decodeUserNotice(message: String, tags: Map[String, String], channelName: String, userNoticeMessage: Option[String]): IncomingMessage = {
    val common: Option[UserNotice.Common] = (
      tags.get(Tags.badgeInfo),
      tags.get(Tags.badges),
      tags.get(Tags.color),
      tags.get(Tags.emotes),
      tags.get(Tags.id),
      tags.get(Tags.login),
      tags.get(Tags.roomId),
      tags.get(Tags.systemMessage),
      tags.get(Tags.tmiSentTs),
      tags.get(Tags.userId),
      tags.get(Tags.userType)
    ) match {
      case (
        Some(presentBadgeInfo),
        Some(presentBadges),
        Some(presentColor),
        Some(presentEmotes),
        Some(presentId),
        Some(presentLogin),
        Some(presentRoomId),
        Some(presentSystemMessage),
        Some(presentTmiSentTimestamp),
        Some(presentUserId),
        Some(presentUserType)
        ) =>
        (
          presentRoomId.toIntOption,
          presentTmiSentTimestamp.toLongOption,
          presentUserId.toIntOption
        ) match {
          case (
            Some(validRoomId),
            Some(validTmiSentTimestamp),
            Some(validUserId)
            ) =>
            Some(UserNotice.Common(
              badgeInfo = badgeInfoDecoder.decode(presentBadgeInfo),
              badges = badgesDecoder.decode(presentBadges),
              color = presentColor,
              displayName = tags.getOrElse(Tags.displayName, presentLogin),
              emotes = emotesDecoder.decode(presentEmotes),
              id = presentId,
              login = presentLogin,
              mod = strIntToBoolean(tags.getOrElse(Tags.mod, "0")),
              roomId = validRoomId,
              subscriber = strIntToBoolean(tags.getOrElse(Tags.subscriber, "0")),
              systemMessage = presentSystemMessage,
              tmiSentTimestamp = unixTimestampToOffsetDateTime(validTmiSentTimestamp),
              turbo = strIntToBoolean(tags.getOrElse(Tags.turbo, "0")),
              userId = validUserId,
              userType = TwitchUserType(presentUserType).getOrElse(TwitchUserType.NormalUser),
              vip = strIntToBoolean(tags.getOrElse(Tags.vip, "0")),
              channelName = channelName
            ))
          case _ =>
            log.error("Cannot convert {}, {} or {} tag: {}", Tags.roomId, Tags.tmiSentTs, Tags.userId, tags)
            None
        }
      case _ =>
        log.error("The following required tags are missing in the USERNOTICE message: {} | All decoded tags: {}", getMissingTagsString(requiredUserNoticeTags, tags), tags)
        None
    }

    common match {
      case Some(presentCommon) =>
        tags.get(Tags.msgId) match {
          case Some("sub") =>
            (
              tags.get(Tags.msgParamCumulativeMonths),
              tags.get(Tags.msgParamShouldShareStreak),
              tags.get(Tags.msgParamStreakMonths),
              tags.get(Tags.msgParamSubPlan),
              tags.get(Tags.msgParamSubPlanName)
            ) match {
              case (
                Some(presentCumulativeMonths),
                Some(presentShouldShareStreak),
                Some(presentStreakMonths),
                Some(presentSubPlan),
                Some(presentSubPlanName)
                ) =>
                (presentCumulativeMonths.toIntOption, presentStreakMonths.toIntOption) match {
                  case (Some(validCumulativeMonths), Some(validStreakMonths)) =>
                    UserNotice.Sub(
                      cumulativeMonths = validCumulativeMonths,
                      shouldShareStreak = strIntToBoolean(presentShouldShareStreak),
                      streakMonths = validStreakMonths,
                      subPlan = presentSubPlan,
                      subPlanName = presentSubPlanName,
                      userNotice = presentCommon
                    )
                  case _ =>
                    log.error("Cannot convert tag {} or {} to number: {}", Tags.msgParamCumulativeMonths, Tags.msgParamStreakMonths, tags)
                    UnknownMessage(message)
                }
              case _ =>
                log.error("The following required tags are missing in the USERNOTICE message with {}=sub: {} | All decoded tags: {}", Tags.msgId, getMissingTagsString(requiredUserNoticeSubTags, tags), tags)
                UnknownMessage(message)
            }
          case Some("resub") =>
            (
              tags.get(Tags.msgParamCumulativeMonths),
              tags.get(Tags.msgParamShouldShareStreak),
              tags.get(Tags.msgParamStreakMonths),
              tags.get(Tags.msgParamSubPlan),
              tags.get(Tags.msgParamSubPlanName)
            ) match {
              case (
                Some(presentCumulativeMonths),
                Some(presentShouldShareStreak),
                Some(presentStreakMonths),
                Some(presentSubPlan),
                Some(presentSubPlanName)
                ) =>
                (presentCumulativeMonths.toIntOption, presentStreakMonths.toIntOption) match {
                  case (Some(validCumulativeMonths), Some(validStreakMonths)) =>
                    UserNotice.Resub(
                      cumulativeMonths = validCumulativeMonths,
                      shouldShareStreak = strIntToBoolean(presentShouldShareStreak),
                      streakMonths = validStreakMonths,
                      subPlan = presentSubPlan,
                      subPlanName = presentSubPlanName,
                      userNotice = presentCommon,
                      message = userNoticeMessage match {
                        case Some(presentUserNoticeMessage) => presentUserNoticeMessage
                        case _ => ""
                      }
                    )
                  case _ =>
                    log.error("Cannot convert tag {} or {} to number: {}", Tags.msgParamCumulativeMonths, Tags.msgParamStreakMonths, tags)
                    UnknownMessage(message)
                }
              case _ =>
                log.error("The following required tags are missing in the USERNOTICE message with {}=resub: {} | All decoded tags: {}", Tags.msgId, getMissingTagsString(requiredUserNoticeResubTags, tags), tags)
                UnknownMessage(message)
            }
          case Some("subgift") =>
            (
              tags.get(Tags.msgParamMonths),
              tags.get(Tags.msgParamRecipientDisplayName),
              tags.get(Tags.msgParamRecipientId),
              tags.get(Tags.msgParamRecipientUserName),
              tags.get(Tags.msgParamSubPlan),
              tags.get(Tags.msgParamSubPlanName)
            ) match {
              case (
                Some(presentMonths),
                Some(presentRecipientDisplayName),
                Some(presentRecipientId),
                Some(presentRecipientUserName),
                Some(presentSubPlan),
                Some(presentSubPlanName)
                ) =>
                (presentMonths.toIntOption, presentRecipientId.toIntOption) match {
                  case (Some(validMonths), Some(validRecipientId)) =>
                    UserNotice.SubGift(
                      months = validMonths,
                      recipientDisplayName = presentRecipientDisplayName,
                      recipientId = validRecipientId,
                      recipientUserName = presentRecipientUserName,
                      subPlan = presentSubPlan,
                      subPlanName = presentSubPlanName,
                      giftMonths = tags.get(Tags.msgParamGiftMonths).flatMap(_.toIntOption).getOrElse(1),
                      userNotice = presentCommon
                    )
                  case _ =>
                    log.error("Cannot convert tag {} or {} to number: {}", Tags.msgParamMonths, Tags.msgParamRecipientId, tags)
                    UnknownMessage(message)
                }
              case _ =>
                log.error("The following required tags are missing in the USERNOTICE message with {}=subgift: {} | All decoded tags: {}", Tags.msgId, getMissingTagsString(requiredUserNoticeSubGiftTags, tags), tags)
                UnknownMessage(message)
            }
          case Some("submysterygift") =>
            (
              tags.get(Tags.msgParamCommunityGiftId),
              tags.get(Tags.msgParamGoalContributionType),
              tags.get(Tags.msgParamGoalCurrentContributions),
              tags.get(Tags.msgParamGoalTargetContributions),
              tags.get(Tags.msgParamGoalUserContributions),
              tags.get(Tags.msgParamMassGiftCount),
              tags.get(Tags.msgParamOriginId),
              tags.get(Tags.msgParamSenderCount),
              tags.get(Tags.msgParamSubPlan)
            ) match {
              case (
                Some(presentCommunityGiftId),
                Some(presentGoalContributionType),
                Some(presentGoalCurrentContributions),
                Some(presentGoalTargetContributions),
                Some(presentGoalUserContributions),
                Some(presentMassGiftCount),
                Some(presentOriginId),
                Some(presentSenderCount),
                Some(presentSubPlan)
                ) =>
                (
                  presentCommunityGiftId.toLongOption,
                  presentGoalCurrentContributions.toIntOption,
                  presentGoalTargetContributions.toIntOption,
                  presentGoalUserContributions.toIntOption,
                  presentMassGiftCount.toIntOption,
                  presentOriginId.toLongOption,
                  presentSenderCount.toIntOption
                ) match {
                  case (
                    Some(validCommunityGiftId),
                    Some(validGoalCurrentContributions),
                    Some(validGoalTargetContributions),
                    Some(validGoalUserContributions),
                    Some(validMassGiftCount),
                    Some(validOriginId),
                    Some(validSenderCount)
                    ) =>
                    UserNotice.SubMysteryGift(
                      communityGiftId = validCommunityGiftId,
                      goalContributionType = presentGoalContributionType,
                      goalCurrentContributions = validGoalCurrentContributions,
                      goalTargetContributions = validGoalTargetContributions,
                      goalUserContributions = validGoalUserContributions,
                      massGiftCount = validMassGiftCount,
                      originId = validOriginId,
                      senderCount = validSenderCount,
                      subPlan = presentSubPlan,
                      userNotice = presentCommon
                    )
                  case _ =>
                    log.error("Cannot convert one or more of the tags to numbers in USERNOTICE message with {}=submysterygift: {}", Tags.msgId, tags)
                    UnknownMessage(message)
                }
              case _ =>
                log.error("The following required tags are missing in the USERNOTICE message with {}=subgift: {} | All decoded tags: {}", Tags.msgId, getMissingTagsString(requiredUserNoticeSubMysteryGiftTags, tags), tags)
                UnknownMessage(message)
            }
          case Some("giftpaidupgrade") =>
            (
              tags.get(Tags.msgParamPromoGiftTotal),
              tags.get(Tags.msgParamPromoName),
              tags.get(Tags.msgParamSenderLogin),
              tags.get(Tags.msgParamSenderName)
            ) match {
              case (
                Some(presentPromoGiftTotal),
                Some(presentPromoName),
                Some(presentSenderLogin),
                Some(presentSenderName)
                ) =>
                presentPromoGiftTotal.toIntOption match {
                  case Some(validPromoGiftTotal) =>
                    UserNotice.GiftPaidUpgrade(
                      promoGiftTotal = validPromoGiftTotal,
                      promoName = presentPromoName,
                      senderLogin = presentSenderLogin,
                      senderDisplayName = presentSenderName,
                      userNotice = presentCommon
                    )
                  case _ =>
                    log.error("Could not convert tag {} to number: {}", Tags.msgParamPromoGiftTotal, tags)
                    UnknownMessage(message)
                }
              case _ =>
                log.error("The following required tags are missing in the USERNOTICE message with {}=giftpaidupgrade: {} | All decoded tags: {}", Tags.msgId, getMissingTagsString(requiredUserNoticeGiftPaidUpgradeTags, tags), tags)
                UnknownMessage(message)
            }
          case Some("rewardgift") => UserNotice.RewardGift(presentCommon)
          case Some("anongiftpaidupgrade") =>
            (
              tags.get(Tags.msgParamPromoGiftTotal),
              tags.get(Tags.msgParamPromoName)
            ) match {
              case (
                Some(presentPromoGiftTotal),
                Some(presentPromoName),
                ) =>
                presentPromoGiftTotal.toIntOption match {
                  case Some(validPromoGiftTotal) =>
                    UserNotice.AnonGiftPaidUpgrade(
                      promoGiftTotal = validPromoGiftTotal,
                      promoName = presentPromoName,
                      userNotice = presentCommon
                    )
                  case _ =>
                    log.error("Could not convert tag {} to number: {}", Tags.msgParamPromoGiftTotal, tags)
                    UnknownMessage(message)
                }
              case _ =>
                log.error("The following required tags are missing in the USERNOTICE message with {}=anongiftpaidupgrade: {} | All decoded tags: {}", Tags.msgId, getMissingTagsString(requiredUserNoticeAnonGiftPaidUpgradeTags, tags), tags)
                UnknownMessage(message)
            }
          case Some("raid") =>
            (
              tags.get(Tags.msgParamDisplayName),
              tags.get(Tags.msgParamLogin),
              tags.get(Tags.msgParamViewerCount)
            ) match {
              case (
                Some(presentDisplayName),
                Some(presentLogin),
                Some(presentViewerCount)
                ) =>
                presentViewerCount.toIntOption match {
                  case Some(validViewerCount) =>
                    UserNotice.Raid(
                      displayName = presentDisplayName,
                      login = presentLogin,
                      viewerCount = validViewerCount,
                      userNotice = presentCommon
                    )
                  case _ =>
                    log.error("Could not convert tag {} to number: {}", Tags.msgParamViewerCount, tags)
                    UnknownMessage(message)
                }
              case _ =>
                log.error("The following required tags are missing in the USERNOTICE message with {}=raid: {} | All decoded tags: {}", Tags.msgId, getMissingTagsString(requiredUserNoticeRaidTags, tags), tags)
                UnknownMessage(message)
            }
          case Some("unraid") => UserNotice.Unraid(presentCommon)
          case Some("ritual") =>
            tags.get(Tags.msgParamRitualName) match {
              case Some("new_chatter") =>
                UserNotice.Ritual.NewChatter(
                  message = userNoticeMessage match {
                    case Some(presentUserNoticeMessage) => presentUserNoticeMessage
                    case _ => ""
                  },
                  userNotice = presentCommon
                )
              case Some(_) =>
                log.error("Tag {} in USERNOTICE message with {}=ritual had unknown value: {}", Tags.msgParamRitualName, Tags.msgId, tags)
                UnknownMessage(message)
              case _ =>
                log.error("Tag {} not was not present in USERNOTICE message with {}=ritual: {}", Tags.msgParamRitualName, Tags.msgId, tags)
                UnknownMessage(message)
            }
          case Some("bitsbadgetier") =>
            tags.get(Tags.msgParamThreshold) match {
              case Some(presentThreshold) => presentThreshold.toIntOption match {
                case Some(validThreshold) =>
                  UserNotice.BitsBadgeTier(
                    threshold = validThreshold,
                    userNotice = presentCommon
                  )
                case _ =>
                  log.error("Could not convert tag {} to number: {}", Tags.msgParamThreshold, tags)
                  UnknownMessage(message)
              }
              case _ =>
                log.error("The following required tags are missing in the USERNOTICE message with {}=bitsbadgetier: {} | All decoded tags: {}", Tags.msgId, getMissingTagsString(requiredUserNoticeBitsBadgeTierTags, tags), tags)
                UnknownMessage(message)
            }
          case Some("viewermilestone") =>
            tags.get(Tags.msgParamCategory) match {
              case Some("watch-streak") =>
                (
                  tags.get(Tags.msgParamCopoReward),
                  tags.get(Tags.msgParamId),
                  tags.get(Tags.msgParamValue)
                ) match {
                  case (
                    Some(presentCopoReward),
                    Some(presentId),
                    Some(presentValue)
                    ) =>
                    (presentCopoReward.toIntOption, presentValue.toIntOption) match {
                      case (Some(validCopoReward), Some(validValue)) =>
                        UserNotice.ViewerMilestone.WatchStreak(
                          copoReward = validCopoReward,
                          id = presentId,
                          value = validValue,
                          message = userNoticeMessage.getOrElse(""),
                          userNotice = presentCommon
                        )
                      case _ =>
                        log.error("Could not convert tag {} or {} to number in USERNOTICE message with {}=viewermilestone and {}=watch-streak: {}", Tags.msgParamCopoReward, Tags.msgParamValue, Tags.msgId, Tags.msgParamCategory, tags)
                        UnknownMessage(message)
                    }
                  case _ =>
                    log.error("The following required tags are missing in the USERNOTICE message with {}=viewermilestone and {}=watch-streak: {} | All decoded tags: {}", Tags.msgId, Tags.msgParamCategory, getMissingTagsString(requiredUserNoticeViewerMilestoneWatchStreakTags, tags), tags)
                    UnknownMessage(message)
                }
              case _ =>
                log.error("Unknown tag {} value in USERNOTICE message with {}=viewermilestone: {}", Tags.msgParamCategory, Tags.msgId, tags)
                UnknownMessage(message)
            }
          case Some("standardpayforward") =>
            (
              tags.get(Tags.msgParamPriorGifterAnonymous),
              tags.get(Tags.msgParamPriorGifterDisplayName),
              tags.get(Tags.msgParamPriorGifterId),
              tags.get(Tags.msgParamPriorGifterUserName),
              tags.get(Tags.msgParamRecipientDisplayName),
              tags.get(Tags.msgParamRecipientId),
              tags.get(Tags.msgParamRecipientUserName)
            ) match {
              case (
                Some(presentPriorGifterAnonymous),
                Some(presentPriorGifterDisplayName),
                Some(presentPriorGifterId),
                Some(presentPriorGifterUserName),
                Some(presentRecipientDisplayName),
                Some(presentRecipientId),
                Some(presentRecipientUserName)
                ) =>
                (presentPriorGifterId.toIntOption, presentRecipientId.toIntOption) match {
                  case (Some(validPriorGifterId), Some(validRecipientId)) =>
                    UserNotice.StandardPayForward(
                      priorGifterAnonymous = strIntToBoolean(presentPriorGifterAnonymous),
                      priorGifterDisplayname = presentPriorGifterDisplayName,
                      priorGifterId = validPriorGifterId,
                      priorGifterUserName = presentPriorGifterUserName,
                      recipientDisplayName = presentRecipientDisplayName,
                      recipientId = validRecipientId,
                      recipientUserName = presentRecipientUserName,
                      userNotice = presentCommon
                    )
                  case _ =>
                    log.error("Could not convert tag {} or {} to number in USERNOTICE message with {}=standardpayforward: {}", Tags.msgParamPriorGifterId, Tags.msgParamRecipientId, Tags.msgId, tags)
                    UnknownMessage(message)
                }
              case _ =>
                log.error("The following required tags are missing in the USERNOTICE message with {}=standardpayforward: {} | All decoded tags: {}", Tags.msgId, getMissingTagsString(requiredUserNoticeStandardPayForwardTags, tags), tags)
                UnknownMessage(message)
            }
          case _ =>
            log.error("Unknown {} tag value in USERNOTICE message: {}", Tags.msgId, tags)
            UnknownMessage(message)
        }
      case _ =>
        UnknownMessage(message)
    }
  }

  private val requiredUserStateTags = List(
    Tags.badgeInfo,
    Tags.badges,
    Tags.color,
    Tags.displayName,
    Tags.emoteSets,
    Tags.userType
  )

  private def decodeUserState(message: String, tags: Map[String, String], channelName: String): IncomingMessage = {
    (
      tags.get(Tags.badgeInfo),
      tags.get(Tags.badges),
      tags.get(Tags.color),
      tags.get(Tags.displayName),
      tags.get(Tags.emoteSets),
      tags.get(Tags.userType)
    ) match {
      case (
        Some(presentBadgeInfo),
        Some(presentBadges),
        Some(presentColor),
        Some(presentDisplayName),
        Some(presentEmoteSets),
        Some(presentUserType)) =>
        UserState(
          badgeInfo = badgeInfoDecoder.decode(presentBadgeInfo),
          badges = badgesDecoder.decode(presentBadges),
          color = presentColor,
          displayName = presentDisplayName,
          emoteSets = emoteSetsDecoder.decode(presentEmoteSets),
          mod = strIntToBoolean(tags.getOrElse(Tags.mod, "0")),
          subscriber = strIntToBoolean(tags.getOrElse(Tags.subscriber, "0")),
          turbo = strIntToBoolean(tags.getOrElse(Tags.turbo, "0")),
          userType = TwitchUserType(presentUserType).getOrElse(TwitchUserType.NormalUser),
          channelName = channelName,
          id = tags.get(Tags.id),
        )
      case _ =>
        log.error("The following required tags are missing in the USERSTATE message: {} | All decoded tags: {}", getMissingTagsString(requiredUserStateTags, tags), tags)
        UnknownMessage(message)
    }
  }

  private val requiredWhisperTags = List(
    Tags.badges,
    Tags.color,
    Tags.displayName,
    Tags.emotes,
    Tags.messageId,
    Tags.threadId,
    Tags.userId,
    Tags.userType
  )

  private def decodeWhisper(message: String, tags: Map[String, String], userName: String, userNameFrom: String, whisperMessage: String): IncomingMessage = {
    (
      tags.get(Tags.badges),
      tags.get(Tags.color),
      tags.get(Tags.displayName),
      tags.get(Tags.emotes),
      tags.get(Tags.messageId),
      tags.get(Tags.threadId),
      tags.get(Tags.userId),
      tags.get(Tags.userType)
    ) match {
      case (
        Some(presentBadges),
        Some(presentColor),
        Some(presentDisplayName),
        Some(presentEmotes),
        Some(presentMessageId),
        Some(presentThreadId),
        Some(presentUserId),
        Some(presentUserType)
        ) =>
        (presentMessageId.toIntOption, presentUserId.toIntOption) match {
          case (Some(validMessageId), Some(validUserId)) =>
            Whisper(
              badges = badgesDecoder.decode(presentBadges),
              color = presentColor,
              displayName = presentDisplayName,
              emotes = emotesDecoder.decode(presentEmotes),
              messageId = validMessageId,
              threadId = presentThreadId,
              turbo = strIntToBoolean(tags.getOrElse(Tags.turbo, "0")),
              userId = validUserId,
              userType = TwitchUserType(presentUserType).getOrElse(TwitchUserType.NormalUser),
              fromUser = userNameFrom,
              toUser = userName,
              message = whisperMessage
            )
          case _ =>
            log.error("Could not convert tag {} or {} to number: {}", Tags.messageId, Tags.userId, tags)
            UnknownMessage(message)
        }
      case _ =>
        log.error("The following required tags are missing in the WHISPER message: {} | All decoded tags: {}", getMissingTagsString(requiredWhisperTags, tags), tags)
        UnknownMessage(message)
    }
  }

  private def unixTimestampToOffsetDateTime(timestamp: Long): OffsetDateTime = {
    val instant = Instant.ofEpochMilli(timestamp)
    OffsetDateTime.ofInstant(instant, ZoneOffset.UTC)
  }

  private def strIntToBoolean(str: String): Boolean = str match {
    case "0" | "false" => false
    case "1" | "true" => true
    case _ =>
      log.error("Cannot convert {} to int - returning false", str)
      false
  }

  private def getMissingTagsString(expectedTagsList: List[String], actualTags: Map[String, String]): String = {
    expectedTagsList.diff(actualTags.keys.toList).mkString(", ")
  }
}