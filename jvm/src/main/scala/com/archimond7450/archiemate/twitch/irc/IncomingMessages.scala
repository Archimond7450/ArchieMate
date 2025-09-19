package com.archimond7450.archiemate.twitch.irc

import java.time.OffsetDateTime
import scala.concurrent.duration.Duration

sealed trait IncomingMessage

object IncomingMessages {
  object ClearChat {
    /**
     * Message which is sent after a user was banned in a chat room
     *
     * @param roomId           The ID of the channel where the banned user's messages were removed from
     * @param targetUserId     The ID of the user that was banned
     * @param tmiSentTimestamp Datetime when Twitch sent the message
     * @param channelName      The name of the channel where the banned user's messages were removed from
     * @param userName         The login name of the user who was banned
     */
    case class Ban(
                    roomId: Long,
                    targetUserId: Long,
                    tmiSentTimestamp: OffsetDateTime,
                    channelName: String,
                    userName: String) extends IncomingMessage

    /**
     * Message which is sent after a user is timed out in a chat room
     *
     * @param banDurationSec   The duration of the timeout, in seconds
     * @param roomId           The ID of the channel where the messages were removed from
     * @param targetUserId     The ID of the user that was timed out
     * @param tmiSentTimestamp Datetime when Twitch sent the message
     * @param channelName      The name of the channel where the messages were removed from
     * @param userName         The login name of the user who was timed out
     */
    case class Timeout(
                        banDurationSec: Duration,
                        roomId: Long,
                        targetUserId: Long,
                        tmiSentTimestamp: OffsetDateTime,
                        channelName: String,
                        userName: String) extends IncomingMessage

    /**
     * Message which is sent after all messages are cleared in a chat room
     *
     * @param roomId           The ID of the channel where the messages were removed from
     * @param tmiSentTimestamp Datetime when Twitch sent the message
     * @param channelName      The name of the channel where the messages were removed from
     */
    case class Clear(
                      roomId: Long,
                      tmiSentTimestamp: OffsetDateTime,
                      channelName: String) extends IncomingMessage
  }

  /**
   * Message which is sent when a single message is deleted from the chat room
   *
   * @param login            The name of the user who sent the message
   * @param roomId           Optional. The ID of the channel where the message was removed from
   * @param targetMsgId      A UUID that identifies the message that was removed
   * @param tmiSentTimestamp Datetime when Twitch sent the message
   * @param channelName      The name of the channel where the message was removed from
   * @param message          The chat message that was removed
   */
  case class ClearMsg(
                       login: String,
                       roomId: Option[Int],
                       targetMsgId: String,
                       tmiSentTimestamp: OffsetDateTime,
                       channelName: String,
                       message: String) extends IncomingMessage

  /**
   * Message which is sent after the chatbot authenticates with the server
   *
   * @param badgeInfo   Metadata related to the chat badges. Currently this only contains metadata for subscriber badges to indicate the number of months the user has been a subscriber
   * @param badges      Map between badges and their versions, for example: Map("broadcaster" -> 1)
   * @param color       The color of the user's name in the chat room. This is a hexadecimal RGB color code in the form #RGB. May be empty if it is never set
   * @param displayName The user's display name. This may be empty if it is never set.
   * @param emoteSets   List of IDs that identify the emote sets that the user has access to. Is always at least zero (0).
   * @param turbo       Indicates whether the user has site-wide commercial free mode enabled
   * @param userId      The user's ID
   * @param userType    The type of user
   */
  case class GlobalUserState(
                              badgeInfo: Map[String, String],
                              badges: Map[String, String],
                              color: String,
                              displayName: String,
                              emoteSets: List[Int],
                              turbo: Boolean,
                              userId: Int,
                              userType: TwitchUserType) extends IncomingMessage

  object HostTarget {
    /**
     * Message which is sent when a channel starts hosting viewers from another channel
     *
     * @param hostingChannelName The channel that's hosting the viewers
     * @param hostedChannelName  The channel being hosted
     * @param viewers            The number of viewers watching the broadcast
     */
    case class Start(
                      hostingChannelName: String,
                      hostedChannelName: String,
                      viewers: Int) extends IncomingMessage

    /**
     * Message which is sent when a channel stops hosting viewers from another channel
     *
     * @param hostingChannelName The channel that's hosting the viewers
     * @param viewers            The number of viewers watching the broadcast
     */
    case class End(
                    hostingChannelName: String,
                    viewers: Int) extends IncomingMessage
  }

  /**
   * Message which is sent when a user joins the chat room
   *
   * @param userName    The login name of the user that joined the chat room
   * @param channelName The name of the channel that the user joined
   */
  case class Join(
                   userName: String,
                   channelName: String) extends IncomingMessage

  /**
   * Message which is sent to indicate the outcome of an action
   *
   * @param noticeMessageId An ID that can be used to determine the action's outcome using a pattern match against the constants in the NoticeIDs object.
   * @param targetUserId    An ID of the user to whom whisper message could not be sent - only set when noticeMessageId is NoticeIDs.WHISPER_RESTRICTED
   * @param channelName     The channel where the action occurred
   * @param message         A message that describes the outcome of the action
   */
  case class Notice(
                     noticeMessageId: NoticeID,
                     channelName: String,
                     message: String,
                     targetUserId: Option[Int] = None) extends IncomingMessage

  /**
   * Message which is sent when the Twitch IRC server needs to perform maintenance and is about to disconnect the chatbot.
   */
  case object Reconnect extends IncomingMessage

  /**
   * Message which is sent when a user leaves the chat room
   *
   * @param userName    The name of the channel that the user left
   * @param channelName The login name of the user that left the chat room
   */
  case class Part(
                   userName: String,
                   channelName: String) extends IncomingMessage

  /**
   * Message which is sent to ensure that the chatbot is still alive and able to respond to Twitch IRC messages
   */
  case object Ping extends IncomingMessage

  object PrivMsg {
    /**
     * Tags which are sent after an user posts a message with Hype Chat to the chat room
     *
     * @param amount          The value of the Hype Chat sent by the user
     * @param currency        The ISO 4217 alphabetic currency code the user has sent the Hype Chat in (EUR, USD, CZK, ...)
     * @param exponent        Indicates how many decimal points this currency represents partial amounts in. Decimal points start from the right side of the value defined in amount parameter.
     * @param level           The level of the Hype Chat, in English
     * @param isSystemMessage Determines if the message sent with the Hype Chat was filled in by the system
     */
    case class HypeChat(
                         amount: Int,
                         currency: String,
                         exponent: Int,
                         level: String,
                         isSystemMessage: Boolean,
                         paidCanonicalAmount: Option[Int] = None) extends IncomingMessage

    /**
     * Tags which are sent after an user posts a reply message to the chat room
     *
     * @param parentMessageId       An ID that uniquely identifies the parent message that this message is replying to
     * @param parentUserId          An ID that uniquely identifies the sender of the direct parent message
     * @param parentUserLogin       The login name of the sender of the direct parent message
     * @param parentDisplayName     The display name of the sender of the direct parent message
     * @param parentMessageBody     The text of the direct parent message
     * @param threadParentMessageId An ID that uniquely identifies the top-level parent message of the reply thread that this message is replying to
     * @param threadParentUserLogin The login name of the sender of the top-level parent message
     */
    case class Reply(
                      parentMessageId: String,
                      parentUserId: Int,
                      parentUserLogin: String,
                      parentDisplayName: String,
                      parentMessageBody: String,
                      threadParentMessageId: String,
                      threadParentUserLogin: String) extends IncomingMessage
  }

  /**
   * Message which is sent after an user posts a message to the chat room
   *
   * @param badgeInfo        Metadata related to the chat badges. Currently this only contains metadata for subscriber badges to indicate the number of months the user has been a subscriber
   * @param badges           Map between badges and their versions, for example: Map("broadcaster" -> 1)
   * @param color            The color of the user's name in the chat room. This is a hexadecimal RGB color code in the form #RGB. May be empty if it is never set
   * @param displayName      The user's display name. This may be empty if it is never set.
   * @param emotes           Map between used emote IDs and their start and end positions in the message
   * @param mod              Determines whether the user is a moderator
   * @param roomId           An ID that identifies the chat room
   * @param subscriber       Determines whether the user is a subscriber
   * @param tmiSentTimestamp Datetime when Twitch sent the message
   * @param turbo            Indicates whether the user has site-wide commercial free mode enabled
   * @param userId           The user's ID
   * @param userType         The type of user
   * @param vip              Determines whether the user that sent the chat message is a VIP
   * @param userName         The user name of the user that sent the chat message
   * @param channelName      Name of the channel where the message was sent
   * @param bits             The amount of Bits the user cheered
   * @param id               An optional ID that uniquely identifies the message
   * @param message          The message itself
   */
  case class PrivMsg(
                      badgeInfo: Map[String, String],
                      badges: Map[String, String],
                      color: String,
                      displayName: String,
                      emoteOnly: Boolean,
                      emotes: Map[String, List[(Int, Int)]],
                      firstMessage: Boolean,
                      mod: Boolean,
                      returningChatter: Boolean,
                      roomId: Int,
                      subscriber: Boolean,
                      tmiSentTimestamp: OffsetDateTime,
                      turbo: Boolean,
                      userId: Int,
                      userType: TwitchUserType,
                      vip: Boolean,
                      userName: String,
                      channelName: String,
                      message: String,
                      bits: Int = 0,
                      clientNonce: Option[String] = None,
                      id: Option[String] = None,
                      flags: Option[String] = None,
                      hypeChat: Option[PrivMsg.HypeChat] = None,
                      reply: Option[PrivMsg.Reply] = None) extends IncomingMessage

  /**
   * Message which is sent after the chatbot joins a channel or when the channel's chat room settings change
   *
   * @param emoteOnly     Determines whether the chat room allows only messages with emotes
   * @param followersOnly Determines whether only followers can post messages in the chat room. The value indicates how long, in minutes, the user must have followed the broadcaster before posting chat messages. If the value is -1, the chat room is not restricted to followers only.
   * @param r9k           Determines whether a user's messages must be unique. Applies only to messages with more than 9 characters.
   * @param roomId        An ID that identifies the chat room
   * @param slow          Determines how long, in seconds, users must wait between sending messages
   * @param subsOnly      Determines whether only subscribers and moderators can chat in the chat room
   * @param channelName   Name of the channel for which these settings apply
   */
  case class RoomState(
                        channelName: String,
                        emoteOnly: Option[Boolean] = None,
                        followersOnly: Option[Int] = None,
                        r9k: Option[Boolean] = None,
                        roomId: Option[Int] = None,
                        slow: Option[Int] = None,
                        subsOnly: Option[Boolean] = None,
                        rituals: Option[Boolean] = None) extends IncomingMessage

  object UserNotice {
    /**
     * Common part of any UserNotice message
     *
     * @param badgeInfo        Metadata related to the chat badges. Currently this only contains metadata for subscriber badges to indicate the number of months the user has been a subscriber
     * @param badges           Map between badges and their versions, for example: Map("broadcaster" -> 1)
     * @param color            The color of the user's name in the chat room. This is a hexadecimal RGB color code in the form #RGB. May be empty if it is never set
     * @param displayName      The user's display name. This may be empty if it is never set.
     * @param emotes           Map between used emote IDs and their start and end positions in the message
     * @param id               An ID that uniquely identifies this message
     * @param login            The login name of the user whose action generated the message
     * @param mod              Determines whether the user is a moderator
     * @param roomId           An ID that identifies the chat room
     * @param subscriber       Determines whether the user is a subscriber
     * @param systemMessage    The message Twitch shows in the chat room for this notice
     * @param tmiSentTimestamp Datetime when Twitch sent the message
     * @param turbo            Indicates whether the user has site-wide commercial free mode enabled
     * @param userId           The user's ID
     * @param userType         The type of user
     * @param vip              Determines whether the user that sent the chat message is a VIP
     * @param channelName      The name of the chat room for this notice
     */
    case class Common(
                       badgeInfo: Map[String, String],
                       badges: Map[String, String],
                       color: String,
                       displayName: String,
                       emotes: Map[String, List[(Int, Int)]],
                       id: String,
                       login: String,
                       mod: Boolean,
                       roomId: Int,
                       subscriber: Boolean,
                       systemMessage: String,
                       tmiSentTimestamp: OffsetDateTime,
                       turbo: Boolean,
                       userId: Int,
                       userType: TwitchUserType,
                       vip: Boolean,
                       channelName: String)

    /**
     * Message which is sent after a user subscribes to the channel
     *
     * @param cumulativeMonths  The total number of months the user has subscribed
     * @param shouldShareStreak Indicates whether the user wants their streaks shared
     * @param streakMonths      The number of consecutive months the user has subscribed. This is zero (0) if shouldShareStreak parameter is false
     * @param subPlan           The type of subscription plan being used
     * @param subPlanName       The display name of the subscription plan. This may be a default name or one created by the channel owner
     * @param userNotice        The common parts of UserNotice message
     */
    case class Sub(
                    cumulativeMonths: Int,
                    shouldShareStreak: Boolean,
                    streakMonths: Int,
                    subPlan: String,
                    subPlanName: String,
                    userNotice: UserNotice.Common) extends IncomingMessage

    /**
     * Message which is sent after a user re-subscribes to the channel
     *
     * @param cumulativeMonths  The total number of months the user has subscribed
     * @param shouldShareStreak Indicates whether the user wants their streaks shared
     * @param streakMonths      The number of consecutive months the user has subscribed. This is zero (0) if shouldShareStreak parameter is false
     * @param subPlan           The type of subscription plan being used
     * @param subPlanName       The display name of the subscription plan. This may be a default name or one created by the channel owner
     * @param message           The message sent by the subscribing user
     * @param userNotice        The common parts of UserNotice message
     */
    case class Resub(
                      cumulativeMonths: Int,
                      shouldShareStreak: Boolean,
                      streakMonths: Int,
                      subPlan: String,
                      subPlanName: String,
                      message: String,
                      userNotice: UserNotice.Common) extends IncomingMessage

    /**
     * Message which is sent after a user gifts a subscription to another user in the channel
     *
     * @param months               The total number of months the user has subscribed
     * @param recipientDisplayName The display name of the subscription gift recipient
     * @param recipientId          The user ID of the subscription gift recipient
     * @param recipientUserName    The user name of the subscription gift recipient
     * @param subPlan              The type of subscription plan being used
     * @param subPlanName          The display name of the subscription plan. This may be a default name or one created by the channel owner
     * @param giftMonths           The number of months gifted as part of a single, multi-month gift
     * @param userNotice           The common parts of UserNotice message
     */
    case class SubGift(
                        months: Int,
                        recipientDisplayName: String,
                        recipientId: Int,
                        recipientUserName: String,
                        subPlan: String,
                        subPlanName: String,
                        giftMonths: Int,
                        userNotice: UserNotice.Common) extends IncomingMessage

    /**
     *
     * @param userNotice The common parts of UserNotice message
     */
    case class SubMysteryGift(
                               communityGiftId: Long,
                               goalContributionType: String,
                               goalCurrentContributions: Int,
                               goalTargetContributions: Int,
                               goalUserContributions: Int,
                               massGiftCount: Int,
                               originId: Long,
                               senderCount: Int,
                               subPlan: String,
                               userNotice: UserNotice.Common) extends IncomingMessage

    /**
     *
     * @param promoGiftTotal    The number of gifts the gifter has given during the promo indicated by promoName parameter
     * @param promoName         The subscriptions promo, if any, that is ongoing (for example, Subtember 2018)
     * @param senderLogin       The login name of the user who gifted the subscription
     * @param senderDisplayName The display name of the user who gifted the subscription
     * @param userNotice        The common parts of UserNotice message
     */
    case class GiftPaidUpgrade(
                                promoGiftTotal: Int,
                                promoName: String,
                                senderLogin: String,
                                senderDisplayName: String,
                                userNotice: UserNotice.Common) extends IncomingMessage

    /**
     *
     * @param userNotice The common parts of UserNotice message
     */
    case class RewardGift(
                           userNotice: UserNotice.Common) extends IncomingMessage

    /**
     *
     * @param promoGiftTotal The number of gifts the gifter has given during the promo indicated by promoName parameter
     * @param promoName      The subscriptions promo, if any, that is ongoing (for example, Subtember 2018)
     * @param userNotice     The common parts of UserNotice message
     */
    case class AnonGiftPaidUpgrade(
                                    promoGiftTotal: Int,
                                    promoName: String,
                                    userNotice: UserNotice.Common) extends IncomingMessage

    /**
     * Message which is sent after a broadcaster raids the channel
     *
     * @param displayName The display name of the broadcaster raiding this channel
     * @param login       The login name of the broadcaster raiding this channel
     * @param viewerCount The number of viewers raiding this channel from the broadcaster's channel
     * @param userNotice  The common parts of UserNotice message
     */
    case class Raid(
                     displayName: String,
                     login: String,
                     viewerCount: Int,
                     userNotice: UserNotice.Common) extends IncomingMessage

    /**
     *
     * @param userNotice The common parts of UserNotice message
     */
    case class Unraid(
                       userNotice: UserNotice.Common) extends IncomingMessage

    object Ritual {
      /**
       * Message which is sent after a user posts their first message in the channel
       *
       * @param message    The message sent by the new chatter
       * @param userNotice The common parts of UserNotice message
       */
      case class NewChatter(
                             message: String,
                             userNotice: UserNotice.Common) extends IncomingMessage
    }

    /**
     * Message which is sent after a user earns a higher tier of Bits badge
     *
     * @param threshold  The tier of the Bits badge the user just earned. For example, 100, 1000, 10000.
     * @param userNotice The common parts of UserNotice message
     */
    case class BitsBadgeTier(
                              threshold: Int,
                              userNotice: UserNotice.Common) extends IncomingMessage

    object ViewerMilestone {
      /**
       * Tags which are sent after a user reaches a watch streak
       *
       * @param copoReward - amount of channel points earned
       * @param id         - An ID that uniquely identifies this streak
       * @param value      - amount of consecutive streams watched in a row
       * @param userNotice The common parts of UserNotice message
       */
      case class WatchStreak(
                              copoReward: Int,
                              id: String,
                              value: Int,
                              message: String,
                              userNotice: UserNotice.Common
                            ) extends IncomingMessage
    }

    case class StandardPayForward(
                                   priorGifterAnonymous: Boolean,
                                   priorGifterDisplayname: String,
                                   priorGifterId: Int,
                                   priorGifterUserName: String,
                                   recipientDisplayName: String,
                                   recipientId: Int,
                                   recipientUserName: String,
                                   userNotice: UserNotice.Common
                                 ) extends IncomingMessage
  }

  /**
   * Message which is sent after the chatbot joins a channel or sends a PRIVMSG message
   *
   * @param badgeInfo   Metadata related to the chat badges. Currently this only contains metadata for subscriber badges to indicate the number of months the user has been a subscriber
   * @param badges      Map between badges and their versions, for example: Map("broadcaster" -> 1)
   * @param color       The color of the user's name in the chat room. This is a hexadecimal RGB color code in the form #RGB. May be empty if it is never set
   * @param displayName The user's display name. This may be empty if it is never set.
   * @param emoteSets   List of IDs that identify the emote sets that the user has access to. Is always at least zero (0).
   * @param id          If a PRIVMSG was sent, an ID that uniquely identifies the message
   * @param mod         Determines whether the user is a moderator
   * @param subscriber  Determines whether the user is a subscriber
   * @param turbo       Indicates whether the user has site-wide commercial free mode enabled
   * @param userType    The type of user
   * @param channelName The name of the channel that the bot joined or sent a PRIVMSG in
   */
  case class UserState(
                        badgeInfo: Map[String, String],
                        badges: Map[String, String],
                        color: String,
                        displayName: String,
                        emoteSets: List[Int],
                        mod: Boolean,
                        subscriber: Boolean,
                        turbo: Boolean,
                        userType: TwitchUserType,
                        channelName: String,
                        id: Option[String] = None) extends IncomingMessage

  /**
   * Message which is sent when a WHISPER message is directed specifically to the chatbot
   *
   * @param badges      Map between badges and their versions, for example: Map("broadcaster" -> 1)
   * @param color       The color of the user's name in the chat room. This is a hexadecimal RGB color code in the form #RGB. May be empty if it is never set
   * @param displayName The user's display name. This may be empty if it is never set.
   * @param emotes      Map between used emote IDs and their start and end positions in the message
   * @param messageId   An ID that uniquely identifies the whisper message
   * @param threadId    An ID that uniquely identifies the whisper thread. The ID is in the form, [smaller-value-user-id]_[larger-value-user-id]
   * @param turbo       Indicates whether the user has site-wide commercial free mode enabled
   * @param userId      The ID of the user sending the whisper message
   * @param userType    The type of user sending the whisper message
   * @param fromUser    The user that's sending the whisper message
   * @param toUser      The user that's receiving the whisper message
   * @param message     The whisper message
   */
  case class Whisper(
                      badges: Map[String, String],
                      color: String,
                      displayName: String,
                      emotes: Map[String, List[(Int, Int)]],
                      messageId: Int,
                      threadId: String,
                      turbo: Boolean,
                      userId: Int,
                      userType: TwitchUserType,
                      fromUser: String,
                      toUser: String,
                      message: String) extends IncomingMessage

  /**
   * Message which is sent after bot joins a channel. Contains a list of users already in the channel.
   * These are usually split in multiple messages.
   *
   * @param userName    The chatbot's username
   * @param channelName The channel which was joined
   * @param users       Set of users
   */
  case class _353(
                   userName: String,
                   channelName: String,
                   users: Set[String]) extends IncomingMessage

  /**
   * Message which is sent after bot joins a channel. This message is received after all 353 messages.
   *
   * @param userName    The chatbot's username
   * @param channelName The channel which was joined
   */
  case class _366(
                   userName: String,
                   channelName: String) extends IncomingMessage

  /**
   * Early Notice that contains an error message
   * @param message The error message
   */
  case class EarlyNotice(message: String) extends IncomingMessage

  /**
   * Response to Capability Request message
   * @param succeeded    Determines whether the capabilities have been requested successfully
   * @param capabilities The requested capabilities
   */
  case class CapabilityRequestResponse(succeeded: Boolean, capabilities: Set[String]) extends IncomingMessage

  /**
   * Part of the response to the successful authentication
   * @param number   Message number
   * @param userName Chatbot's user name
   * @param message  The actual message
   */
  case class NumericMessage(number: Int, userName: String, message: String) extends IncomingMessage

  /**
   * A message that the chatbot was unable to decode
   *
   * @param message Unmodified message
   */
  case class UnknownMessage(message: String) extends IncomingMessage
}
