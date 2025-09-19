package com.archimond7450.archiemate.twitch.eventsub

import com.archimond7450.archiemate.helpers.JsonHelper.{OffsetDateTimeJson, dropNulls}
import com.archimond7450.archiemate.CirceConfiguration.twitchConfiguration
import io.circe.{Decoder, Encoder, Json, JsonObject, HCursor, ACursor, DecodingFailure}
import io.circe.derivation.{Configuration, ConfiguredDecoder, ConfiguredEncoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


case class IncomingMessage(metadata: Metadata, payload: Payload)
object IncomingMessage {
  given Decoder[IncomingMessage] = (c: HCursor) => {
    for {
      metadata <- c.downField("metadata").as[Metadata]
      payload <- c.downField("payload").as[Payload]
    } yield IncomingMessage(metadata, payload)
  }
  given Encoder[IncomingMessage] = (m: IncomingMessage) => {
    Json.fromJsonObject(JsonObject(
      "metadata" -> m.metadata.asJson,
      "payload" -> m.payload.asJson
    ))
  }
}

case class Metadata(messageId: String,
                    messageType: String,
                    messageTimestamp: OffsetDateTime,
                    subscriptionType: Option[String] = None,
                    subscriptionVersion: Option[String] = None)
object Metadata {
  given Decoder[Metadata] = ConfiguredDecoder.derived
  given Encoder[Metadata] = dropNulls(ConfiguredEncoder.derived)
}

case class Payload(session: Option[Session] = None,
                   subscription: Option[Subscription] = None,
                   event: Option[Event] = None)
object Payload {
  given Decoder[Payload] = (c: HCursor) => {
    for {
      session <- c.downField("session").as[Option[Session]]
      subscription <- c.downField("subscription").as[Option[Subscription]]
      event <- subscription match {
        case Some(_) => for {
          event <- c.get[Option[Event]]("event")
          events <- c.get[Option[Event]]("events")
        } yield event.orElse(events)
        case None => Right(None)
      }
    } yield Payload(session, subscription, event)
  }

  given Encoder[Payload] = dropNulls((p: Payload) => {
    Json.fromJsonObject(JsonObject(
      "session" -> p.session.map(_.asJson).getOrElse(Json.Null),
      "subscription" -> p.subscription.map(_.asJson).getOrElse(Json.Null),
      "event" -> p.event.map {
        case DropEntitlementGrantEvents(_) => Json.Null
        case otherEvent: Event => otherEvent.asJson
      }.getOrElse(Json.Null),
      "events" -> p.event.map {
        case event: DropEntitlementGrantEvents => event.asJson
        case _ => Json.Null
      }.getOrElse(Json.Null)
    ))
  })
}

case class Session(id: String,
                   status: String,
                   keepaliveTimeoutSeconds: Option[Int],
                   reconnectUrl: Option[String],
                   connectedAt: OffsetDateTime)
object Session {
  given Decoder[Session] = ConfiguredDecoder.derived
  given Encoder[Session] = dropNulls(ConfiguredEncoder.derived)
}

case class Subscription(id: String,
                        status: String,
                        `type`: String,
                        version: String,
                        cost: Int,
                        condition: Condition,
                        transport: Transport,
                        createdAt: OffsetDateTime)
object Subscription {
  given Decoder[Subscription] = ConfiguredDecoder.derived
  given Encoder[Subscription] = ConfiguredEncoder.derived
}

case class SubscriptionRequest(`type`: String,
                               version: String,
                               condition: Condition,
                               transport: Transport)
object SubscriptionRequest {
  given Decoder[SubscriptionRequest] = ConfiguredDecoder.derived
  given Encoder[SubscriptionRequest] = ConfiguredEncoder.derived
}

case class Transport(method: String,
                     callback: Option[String] = None,
                     session: Option[String] = None,
                     connectedAt: Option[OffsetDateTime] = None,
                     disconnectedAt: Option[OffsetDateTime] = None)
object Transport {
  given Decoder[Transport] = (c: HCursor) => {
    for {
      method <- c.get[String]("method")
      callback <- c.get[Option[String]]("callback")
      session <- c.get[Option[String]]("session")
      sessionId <- c.get[Option[String]]("session_id")
      connectedAt <- c.get[Option[OffsetDateTime]]("connected_at")
      disconnectedAt <- c.get[Option[OffsetDateTime]]("disconnected_at")
    } yield Transport(method, callback, session.orElse(sessionId), connectedAt, disconnectedAt)
  }
  given Encoder[Transport] = dropNulls((t: Transport) => {
    Json.fromJsonObject(JsonObject(
      "method" -> Json.fromString(t.method),
      "callback" -> Json.fromStringOrNull(t.callback),
      "session_id" -> Json.fromStringOrNull(t.session),
      "connected_at" -> t.connectedAt.map(c => c.asJson).getOrElse(Json.Null),
      "disconnected_at" -> t.disconnectedAt.map(d => d.asJson).getOrElse(Json.Null),
    ))
  })
}

case class Condition(broadcasterUserId: Option[String] = None,
                     moderatorUserId: Option[String] = None,
                     broadcasterId: Option[String] = None,
                     userId: Option[String] = None,
                     fromBroadcasterUserId: Option[String] = None,
                     toBroadcasterUserId: Option[String] = None,
                     rewardId: Option[String] = None,
                     clientId: Option[String] = None,
                     conduitId: Option[String] = None,
                     organizationId: Option[String] = None,
                     categoryId: Option[String] = None,
                     campaignId: Option[String] = None,
                     extensionClientId: Option[String] = None)
object Condition {
  given Decoder[Condition] = ConfiguredDecoder.derived
  given Encoder[Condition] = dropNulls(ConfiguredEncoder.derived)
}

sealed trait Event
object Event {
  given Decoder[Event] = (c: HCursor) => {
    for {
      subscription <- c.up.downField("subscription").as[Subscription]
      subscriptionType = subscription.`type`
      subscriptionVersion = subscription.version
      event <- SubscriptionIds.get(subscriptionType, subscriptionVersion)(c)
    } yield event
  }
  given Encoder[Event] = (e: Event) => SubscriptionIds.encodeEvent(e)
}

private case class SubscriptionId(subType: String, version: String)
object SubscriptionId {
  given Decoder[SubscriptionId] = ConfiguredDecoder.derived
  given Encoder[SubscriptionId] = ConfiguredEncoder.derived
}

private object SubscriptionIds {
  private val decoderMap: Map[SubscriptionId, ACursor => Either[DecodingFailure, Event]] = Map(
    SubscriptionId("automod.message.hold", "1") -> (json => json.as[AutomodMessageHoldEvent]),
    SubscriptionId("automod.message.update", "1") -> (json => json.as[AutomodMessageUpdateEvent]),
    SubscriptionId("automod.settings.update", "1") -> (json => json.as[AutomodSettingsUpdateEvent]),
    SubscriptionId("automod.terms.update", "1") -> (json => json.as[AutomodTermsUpdateEvent]),
    SubscriptionId("channel.update", "2") -> (json => json.as[ChannelUpdateEvent]),
    SubscriptionId("channel.follow", "1") -> (json => json.as[ChannelFollowEvent]),
    SubscriptionId("channel.follow", "2") -> (json => json.as[ChannelFollowEvent]),
    SubscriptionId("channel.ad_break.begin", "1") -> (json => json.as[ChannelAdBreakBeginEvent]),
    SubscriptionId("channel.chat.clear", "1") -> (json => json.as[ChannelChatClearEvent]),
    SubscriptionId("channel.chat.clear_user_messages", "1") -> (json => json.as[ChannelChatClearUserMessagesEvent]),
    SubscriptionId("channel.chat.message", "1") -> (json => json.as[ChannelChatMessageEvent]),
    SubscriptionId("channel.chat.message_delete", "1") -> (json => json.as[ChannelChatMessageDeleteEvent]),
    SubscriptionId("channel.chat.notification", "1") -> (json => json.as[ChannelChatNotificationEvent]),
    SubscriptionId("channel.chat_settings.update", "1") -> (json => json.as[ChannelChatSettingsUpdateEvent]),
    SubscriptionId("channel.chat.user_message_hold", "1") -> (json => json.as[ChannelChatUserMessageHoldEvent]),
    SubscriptionId("channel.chat.user_message_update", "1") -> (json => json.as[ChannelChatUserMessageUpdateEvent]),
    SubscriptionId("channel.subscribe", "1") -> (json => json.as[ChannelSubscribeEvent]),
    SubscriptionId("channel.subscription.end", "1") -> (json => json.as[ChannelSubscriptionEndEvent]),
    SubscriptionId("channel.subscription.gift", "1") -> (json => json.as[ChannelSubscriptionGiftEvent]),
    SubscriptionId("channel.subscription.message", "1") -> (json => json.as[ChannelSubscriptionMessageEvent]),
    SubscriptionId("channel.cheer", "1") -> (json => json.as[ChannelCheerEvent]),
    SubscriptionId("channel.raid", "1") -> (json => json.as[ChannelRaidEvent]),
    SubscriptionId("channel.ban", "1") -> (json => json.as[ChannelBanEvent]),
    SubscriptionId("channel.unban", "1") -> (json => json.as[ChannelUnbanEvent]),
    SubscriptionId("channel.unban_request.create", "1") -> (json => json.as[ChannelUnbanRequestCreateEvent]),
    SubscriptionId("channel.unban_request.resolve", "1") -> (json => json.as[ChannelUnbanRequestResolveEvent]),
    SubscriptionId("channel.moderate", "1") -> (json => json.as[ChannelModerateEvent]),
    SubscriptionId("channel.moderate", "2") -> (json => json.as[ChannelModerateV2Event]),
    SubscriptionId("channel.moderator.add", "1") -> (json => json.as[ChannelModeratorAddEvent]),
    SubscriptionId("channel.moderator.remove", "1") -> (json => json.as[ChannelModeratorRemoveEvent]),
    SubscriptionId("channel.guest_star_session.begin", "beta") -> (json => json.as[ChannelGuestStarSessionBeginEvent]),
    SubscriptionId("channel.guest_star_session.end", "beta") -> (json => json.as[ChannelGuestStarSessionEndEvent]),
    SubscriptionId("channel.guest_star_guest.update", "beta") -> (json => json.as[ChannelGuestStarGuestUpdateEvent]),
    SubscriptionId("channel.guest_star_settings.update", "beta") -> (json => json.as[ChannelGuestStarSettingsUpdateEvent]),
    SubscriptionId("channel.channel_points_automatic_reward_redemption.add", "1") -> (json => json.as[ChannelPointsAutomaticRewardRedemptionAddEvent]),
    SubscriptionId("channel.channel_points_custom_reward.add", "1") -> (json => json.as[ChannelPointsCustomRewardAddEvent]),
    SubscriptionId("channel.channel_points_custom_reward.update", "1") -> (json => json.as[ChannelPointsCustomRewardUpdateEvent]),
    SubscriptionId("channel.channel_points_custom_reward.remove", "1") -> (json => json.as[ChannelPointsCustomRewardRemoveEvent]),
    SubscriptionId("channel.channel_points_custom_reward_redemption.add", "1") -> (json => json.as[ChannelPointsCustomRewardRedemptionAddEvent]),
    SubscriptionId("channel.channel_points_custom_reward_redemption.update", "1") -> (json => json.as[ChannelPointsCustomRewardRedemptionUpdateEvent]),
    SubscriptionId("channel.poll.begin", "1") -> (json => json.as[ChannelPollBeginEvent]),
    SubscriptionId("channel.poll.progress", "1") -> (json => json.as[ChannelPollProgressEvent]),
    SubscriptionId("channel.poll.end", "1") -> (json => json.as[ChannelPollEndEvent]),
    SubscriptionId("channel.prediction.begin", "1") -> (json => json.as[ChannelPredictionBeginEvent]),
    SubscriptionId("channel.prediction.progress", "1") -> (json => json.as[ChannelPredictionProgressEvent]),
    SubscriptionId("channel.prediction.lock", "1") -> (json => json.as[ChannelPredictionLockEvent]),
    SubscriptionId("channel.prediction.end", "1") -> (json => json.as[ChannelPredictionEndEvent]),
    SubscriptionId("channel.suspicious_user.update", "1") -> (json => json.as[ChannelSuspiciousUserUpdateEvent]),
    SubscriptionId("channel.suspicious_user.message", "1") -> (json => json.as[ChannelSuspiciousUserMessageEvent]),
    SubscriptionId("channel.vip.add", "1") -> (json => json.as[ChannelVIPAddEvent]),
    SubscriptionId("channel.vip.remove", "1") -> (json => json.as[ChannelVIPRemoveEvent]),
    SubscriptionId("channel.warning.acknowledge", "1") -> (json => json.as[ChannelWarningAcknowledgeEvent]),
    SubscriptionId("channel.warning.send", "1") -> (json => json.as[ChannelWarningSendEvent]),
    SubscriptionId("channel.hype_train.begin", "1") -> (json => json.as[ChannelHypeTrainBeginEvent]),
    SubscriptionId("channel.hype_train.progress", "1") -> (json => json.as[ChannelHypeTrainProgressEvent]),
    SubscriptionId("channel.hype_train.end", "1") -> (json => json.as[ChannelHypeTrainEndEvent]),
    SubscriptionId("channel.charity_campaign.donate", "1") -> (json => json.as[ChannelCharityDonationEvent]),
    SubscriptionId("channel.charity_campaign.start", "1") -> (json => json.as[ChannelCharityCampaignStartEvent]),
    SubscriptionId("channel.charity_campaign.progress", "1") -> (json => json.as[ChannelCharityCampaignProgressEvent]),
    SubscriptionId("channel.charity_campaign.stop", "1") -> (json => json.as[ChannelCharityCampaignStopEvent]),
    SubscriptionId("channel.shield_mode.begin", "1") -> (json => json.as[ChannelShieldModeBeginEvent]),
    SubscriptionId("channel.shield_mode.end", "1") -> (json => json.as[ChannelShieldModeEndEvent]),
    SubscriptionId("channel.shoutout.create", "1") -> (json => json.as[ChannelShoutoutCreateEvent]),
    SubscriptionId("channel.shoutout.receive", "1") -> (json => json.as[ChannelShoutoutReceiveEvent]),
    SubscriptionId("conduit.shard.disabled", "1") -> (json => json.as[ConduitShardDisabledEvent]),
    SubscriptionId("drop.entitlement.grant", "1") -> (json => json.as[DropEntitlementGrantEvents]),
    SubscriptionId("extension.bits_transaction.create", "1") -> (json => json.as[ExtensionBitsTransactionCreateEvent]),
    SubscriptionId("channel.goal.begin", "1") -> (json => json.as[ChannelGoalBeginEvent]),
    SubscriptionId("channel.goal.progress", "1") -> (json => json.as[ChannelGoalProgressEvent]),
    SubscriptionId("channel.goal.end", "1") -> (json => json.as[ChannelGoalEndEvent]),
    SubscriptionId("stream.online", "1") -> (json => json.as[StreamOnlineEvent]),
    SubscriptionId("stream.offline", "1") -> (json => json.as[StreamOfflineEvent]),
    SubscriptionId("user.authorization.grant", "1") -> (json => json.as[UserAuthorizationGrantEvent]),
    SubscriptionId("user.authorization.revoke", "1") -> (json => json.as[UserAuthorizationRevokeEvent]),
    SubscriptionId("user.update", "1") -> (json => json.as[UserUpdateEvent]),
    SubscriptionId("user.whisper.message", "1") -> (json => json.as[WhisperReceivedEvent])
  )

  def encodeEvent: PartialFunction[Event, Json] = {
    case e: AutomodMessageHoldEvent => e.asJson
    case e: AutomodMessageUpdateEvent => e.asJson
    case e: AutomodSettingsUpdateEvent => e.asJson
    case e: AutomodTermsUpdateEvent => e.asJson
    case e: ChannelUpdateEvent => e.asJson
    case e: ChannelFollowEvent => e.asJson
    case e: ChannelAdBreakBeginEvent => e.asJson
    case e: ChannelChatClearEvent => e.asJson
    case e: ChannelChatClearUserMessagesEvent => e.asJson
    case e: ChannelChatMessageEvent => e.asJson
    case e: ChannelChatMessageDeleteEvent => e.asJson
    case e: ChannelChatNotificationEvent => e.asJson
    case e: ChannelChatSettingsUpdateEvent => e.asJson
    case e: ChannelChatUserMessageHoldEvent => e.asJson
    case e: ChannelChatUserMessageUpdateEvent => e.asJson
    case e: ChannelSubscribeEvent => e.asJson
    case e: ChannelSubscriptionEndEvent => e.asJson
    case e: ChannelSubscriptionGiftEvent => e.asJson
    case e: ChannelSubscriptionMessageEvent => e.asJson
    case e: ChannelCheerEvent => e.asJson
    case e: ChannelRaidEvent => e.asJson
    case e: ChannelBanEvent => e.asJson
    case e: ChannelUnbanEvent => e.asJson
    case e: ChannelUnbanRequestCreateEvent => e.asJson
    case e: ChannelUnbanRequestResolveEvent => e.asJson
    case e: ChannelModerateEvent => e.asJson
    case e: ChannelModerateV2Event => e.asJson
    case e: ChannelModeratorAddEvent => e.asJson
    case e: ChannelModeratorRemoveEvent => e.asJson
    case e: ChannelGuestStarSessionBeginEvent => e.asJson
    case e: ChannelGuestStarSessionEndEvent => e.asJson
    case e: ChannelGuestStarGuestUpdateEvent => e.asJson
    case e: ChannelGuestStarSettingsUpdateEvent => e.asJson
    case e: ChannelPointsAutomaticRewardRedemptionAddEvent => e.asJson
    case e: ChannelPointsCustomRewardAddEvent => e.asJson
    case e: ChannelPointsCustomRewardUpdateEvent => e.asJson
    case e: ChannelPointsCustomRewardRemoveEvent => e.asJson
    case e: ChannelPointsCustomRewardRedemptionAddEvent => e.asJson
    case e: ChannelPointsCustomRewardRedemptionUpdateEvent => e.asJson
    case e: ChannelPollBeginEvent => e.asJson
    case e: ChannelPollProgressEvent => e.asJson
    case e: ChannelPollEndEvent => e.asJson
    case e: ChannelPredictionBeginEvent => e.asJson
    case e: ChannelPredictionProgressEvent => e.asJson
    case e: ChannelPredictionLockEvent => e.asJson
    case e: ChannelPredictionEndEvent => e.asJson
    case e: ChannelSuspiciousUserUpdateEvent => e.asJson
    case e: ChannelSuspiciousUserMessageEvent => e.asJson
    case e: ChannelVIPAddEvent => e.asJson
    case e: ChannelVIPRemoveEvent => e.asJson
    case e: ChannelWarningAcknowledgeEvent => e.asJson
    case e: ChannelWarningSendEvent => e.asJson
    case e: ChannelHypeTrainBeginEvent => e.asJson
    case e: ChannelHypeTrainProgressEvent => e.asJson
    case e: ChannelHypeTrainEndEvent => e.asJson
    case e: ChannelCharityDonationEvent => e.asJson
    case e: ChannelCharityCampaignStartEvent => e.asJson
    case e: ChannelCharityCampaignProgressEvent => e.asJson
    case e: ChannelCharityCampaignStopEvent => e.asJson
    case e: ChannelShieldModeBeginEvent => e.asJson
    case e: ChannelShieldModeEndEvent => e.asJson
    case e: ChannelShoutoutCreateEvent => e.asJson
    case e: ChannelShoutoutReceiveEvent => e.asJson
    case e: ConduitShardDisabledEvent => e.asJson
    case e: DropEntitlementGrantEvents => e.asJson
    case e: ExtensionBitsTransactionCreateEvent => e.asJson
    case e: ChannelGoalBeginEvent => e.asJson
    case e: ChannelGoalProgressEvent => e.asJson
    case e: ChannelGoalEndEvent => e.asJson
    case e: StreamOnlineEvent => e.asJson
    case e: StreamOfflineEvent => e.asJson
    case e: UserAuthorizationGrantEvent => e.asJson
    case e: UserAuthorizationRevokeEvent => e.asJson
    case e: UserUpdateEvent => e.asJson
    case e: WhisperReceivedEvent => e.asJson
  }

  def get(subType: String, version: String): ACursor => Either[DecodingFailure, Event] =
    decoderMap.getOrElse(
      SubscriptionId(subType, version),
      json => Left(DecodingFailure(s"Unknown event type/version: $subType/$version", json.history))
    )
}

case class AutomodMessageHoldEvent(broadcasterUserId: String,
                                   broadcasterUserLogin: String,
                                   broadcasterUserName: String,
                                   userId: String,
                                   userLogin: String,
                                   userName: String,
                                   messageId: String,
                                   message: String,
                                   level: Int,
                                   category: String,
                                   heldAt: OffsetDateTime,
                                   fragments: MessageFragments) extends Event
object AutomodMessageHoldEvent {
  given Decoder[AutomodMessageHoldEvent] = ConfiguredDecoder.derived
  given Encoder[AutomodMessageHoldEvent] = ConfiguredEncoder.derived
}

case class AutomodMessageUpdateEvent(broadcasterUserId: String,
                                     broadcasterUserLogin: String,
                                     broadcasterUserName: String,
                                     userId: String,
                                     userLogin: String,
                                     userName: String,
                                     moderatorUserId: String,
                                     moderatorUserLogin: String,
                                     moderatorUserName: String,
                                     messageId: String,
                                     message: String,
                                     category: String,
                                     level: Int,
                                     status: String,
                                     heldAt: OffsetDateTime,
                                     fragments: MessageFragments) extends Event
object AutomodMessageUpdateEvent {
  given Decoder[AutomodMessageUpdateEvent] = ConfiguredDecoder.derived
  given Encoder[AutomodMessageUpdateEvent] = ConfiguredEncoder.derived
}

case class AutomodSettingsUpdateEvent(data: List[AutomodSettingsData]) extends Event
object AutomodSettingsUpdateEvent {
  given Decoder[AutomodSettingsUpdateEvent] = ConfiguredDecoder.derived
  given Encoder[AutomodSettingsUpdateEvent] = ConfiguredEncoder.derived
}

case class AutomodTermsUpdateEvent(broadcasterUserId: String,
                                   broadcasterUserLogin: String,
                                   broadcasterUserName: String,
                                   moderatorUserId: String,
                                   moderatorUserLogin: String,
                                   moderatorUserName: String,
                                   action: String,
                                   fromAutomod: Boolean,
                                   terms: List[String]) extends Event
object AutomodTermsUpdateEvent {
  given Decoder[AutomodTermsUpdateEvent] = ConfiguredDecoder.derived
  given Encoder[AutomodTermsUpdateEvent] = ConfiguredEncoder.derived
}

case class ChannelUpdateEvent(broadcasterUserId: String,
                              broadcasterUserLogin: String,
                              broadcasterUserName: String,
                              title: String,
                              language: String,
                              categoryId: String,
                              categoryName: String,
                              contentClassificationLabels: List[String]) extends Event
object ChannelUpdateEvent {
  given Decoder[ChannelUpdateEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelUpdateEvent] = ConfiguredEncoder.derived
}

case class ChannelFollowEvent(userId: String,
                              userLogin: String,
                              userName: String,
                              broadcasterUserId: String,
                              broadcasterUserLogin: String,
                              broadcasterUserName: String,
                              followedAt: OffsetDateTime) extends Event
object ChannelFollowEvent {
  given Decoder[ChannelFollowEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelFollowEvent] = ConfiguredEncoder.derived
}

case class ChannelAdBreakBeginEvent(durationSeconds: Int,
                                    startedAt: OffsetDateTime,
                                    isAutomatic: Boolean,
                                    broadcasterUserId: String,
                                    broadcasterUserLogin: String,
                                    broadcasterUserName: String,
                                    requesterUserId: String,
                                    requesterUserLogin: String,
                                    requesterUserName: String) extends Event
object ChannelAdBreakBeginEvent {
  given Decoder[ChannelAdBreakBeginEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelAdBreakBeginEvent] = ConfiguredEncoder.derived
}

case class ChannelChatClearEvent(broadcasterUserId: String,
                                 broadcasterUserLogin: String,
                                 broadcasterUserName: String) extends Event
object ChannelChatClearEvent {
  given Decoder[ChannelChatClearEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelChatClearEvent] = ConfiguredEncoder.derived
}

case class ChannelChatClearUserMessagesEvent(broadcasterUserId: String,
                                             broadcasterUserLogin: String,
                                             broadcasterUserName: String,
                                             targetUserId: String,
                                             targetUserLogin: String,
                                             targetUserName: String) extends Event
object ChannelChatClearUserMessagesEvent {
  given Decoder[ChannelChatClearUserMessagesEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelChatClearUserMessagesEvent] = ConfiguredEncoder.derived
}

case class ChannelChatMessageEvent(broadcasterUserId: String,
                                   broadcasterUserLogin: String,
                                   broadcasterUserName: String,
                                   chatterUserId: String,
                                   chatterUserLogin: String,
                                   chatterUserName: String,
                                   messageId: String,
                                   message: ChatMessage,
                                   message_type: String,
                                   badges: List[Badge],
                                   cheer: Option[Cheer],
                                   color: String,
                                   reply: Option[Reply],
                                   channel_points_custom_reward_id: Option[String],
                                   channel_points_animation_id: Option[String]) extends Event
object ChannelChatMessageEvent {
  given Decoder[ChannelChatMessageEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelChatMessageEvent] = ConfiguredEncoder.derived
}

case class ChannelChatMessageDeleteEvent(broadcasterUserId: String,
                                         broadcasterUserLogin: String,
                                         broadcasterUserName: String,
                                         targetUserId: String,
                                         targetUserLogin: String,
                                         targetUserName: String,
                                         messageId: String) extends Event
object ChannelChatMessageDeleteEvent {
  given Decoder[ChannelChatMessageDeleteEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelChatMessageDeleteEvent] = ConfiguredEncoder.derived
}

case class ChannelChatNotificationEvent(broadcasterUserId: String,
                                        broadcasterUserLogin: String,
                                        broadcasterUserName: String,
                                        chatterUserId: String,
                                        chatterUserLogin: String,
                                        chatterUserName: String,
                                        chatterIsAnonymous: Boolean,
                                        color: String,
                                        badges: List[Badge],
                                        systemMessage: String,
                                        messageId: String,
                                        message: ChatMessage,
                                        noticeType: String,
                                        sub: Option[NoticeSub],
                                        resub: Option[NoticeResub],
                                        subGift: Option[NoticeSubGift],
                                        communitySubGift: Option[NoticeCommunitySubGift],
                                        giftPaidUpgrade: Option[NoticeGiftPaidUpgrade],
                                        primePaidUpgrade: Option[NoticePrimePaidUpgrade],
                                        raid: Option[NoticeRaid],
                                        unraid: Option[NoticeUnraid],
                                        payItForward: Option[NoticePayItForward],
                                        announcement: Option[NoticeAnnouncement],
                                        charityDonation: Option[NoticeCharityDonation],
                                        bitsBadgeTier: Option[NoticeBitsBadgeTier]) extends Event
object ChannelChatNotificationEvent {
  given Decoder[ChannelChatNotificationEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelChatNotificationEvent] = ConfiguredEncoder.derived
}

case class ChannelChatSettingsUpdateEvent(broadcasterUserId: String,
                                          broadcasterUserLogin: String,
                                          broadcasterUserName: String,
                                          emoteMode: Boolean,
                                          followerMode: Boolean,
                                          followerModeDurationMinutes: Option[Int],
                                          slowMode: Boolean,
                                          slowModeWaitTimeSeconds: Option[Int],
                                          subscriberMode: Boolean,
                                          uniqueChatMode: Boolean) extends Event
object ChannelChatSettingsUpdateEvent {
  given Decoder[ChannelChatSettingsUpdateEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelChatSettingsUpdateEvent] = ConfiguredEncoder.derived
}

case class ChannelChatUserMessageHoldEvent(broadcasterUserId: String,
                                           broadcasterUserLogin: String,
                                           broadcasterUserName: String,
                                           userId: String,
                                           userLogin: String,
                                           userName: String,
                                           messageId: String,
                                           message: ChatMessage) extends Event
object ChannelChatUserMessageHoldEvent {
  given Decoder[ChannelChatUserMessageHoldEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelChatUserMessageHoldEvent] = ConfiguredEncoder.derived
}

case class ChannelChatUserMessageUpdateEvent(broadcasterUserId: String,
                                             broadcasterUserLogin: String,
                                             broadcasterUserName: String,
                                             userId: String,
                                             userLogin: String,
                                             userName: String,
                                             status: String,
                                             messageId: String,
                                             message: ChatMessage) extends Event
object ChannelChatUserMessageUpdateEvent {
  given Decoder[ChannelChatUserMessageUpdateEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelChatUserMessageUpdateEvent] = ConfiguredEncoder.derived
}

case class ChannelSubscribeEvent(userId: String,
                                 userLogin: String,
                                 userName: String,
                                 broadcasterUserId: String,
                                 broadcasterUserLogin: String,
                                 broadcasterUserName: String,
                                 tier: String,
                                 isGift: Boolean) extends Event
object ChannelSubscribeEvent {
  given Decoder[ChannelSubscribeEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelSubscribeEvent] = ConfiguredEncoder.derived
}

case class ChannelSubscriptionEndEvent(userId: String,
                                       userLogin: String,
                                       userName: String,
                                       broadcasterUserId: String,
                                       broadcasterUserLogin: String,
                                       broadcasterUserName: String,
                                       tier: String,
                                       isGift: Boolean) extends Event
object ChannelSubscriptionEndEvent {
  given Decoder[ChannelSubscriptionEndEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelSubscriptionEndEvent] = ConfiguredEncoder.derived
}

case class ChannelSubscriptionGiftEvent(userId: Option[String],
                                        userLogin: Option[String],
                                        userName: Option[String],
                                        broadcasterUserId: String,
                                        broadcasterUserLogin: String,
                                        broadcasterUserName: String,
                                        total: Int,
                                        tier: String,
                                        cumulativeTotal: Option[Int],
                                        isAnonymous: Boolean) extends Event
object ChannelSubscriptionGiftEvent {
  given Decoder[ChannelSubscriptionGiftEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelSubscriptionGiftEvent] = ConfiguredEncoder.derived
}

case class ChannelSubscriptionMessageEvent(userId: String,
                                           userLogin: String,
                                           userName: String,
                                           broadcasterUserId: String,
                                           broadcasterUserLogin: String,
                                           broadcasterUserName: String,
                                           tier: String,
                                           message: Message,
                                           cumulativeMonths: Int,
                                           streakMonths: Option[Int],
                                           durationMonths: Int) extends Event
object ChannelSubscriptionMessageEvent {
  given Decoder[ChannelSubscriptionMessageEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelSubscriptionMessageEvent] = ConfiguredEncoder.derived
}

case class ChannelCheerEvent(isAnonymous: Boolean,
                             userId: Option[String],
                             userLogin: Option[String],
                             userName: Option[String],
                             broadcasterUserId: String,
                             broadcasterUserLogin: String,
                             broadcasterUserName: String,
                             message: String,
                             bits: Int) extends Event
object ChannelCheerEvent {
  given Decoder[ChannelCheerEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelCheerEvent] = ConfiguredEncoder.derived
}

case class ChannelRaidEvent(fromBroadcasterUserId: String,
                            fromBroadcasterUserLogin: String,
                            fromBroadcasterUserName: String,
                            toBroadcasterUserId: String,
                            toBroadcasterUserLogin: String,
                            toBroadcasterUserName: String,
                            viewers: Int) extends Event
object ChannelRaidEvent {
  given Decoder[ChannelRaidEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelRaidEvent] = ConfiguredEncoder.derived
}

case class ChannelBanEvent(userId: String,
                           userLogin: String,
                           userName: String,
                           broadcasterUserId: String,
                           broadcasterUserLogin: String,
                           broadcasterUserName: String,
                           moderatorUserId: String,
                           moderatorUserLogin: String,
                           moderatorUserName: String,
                           reason: String,
                           bannedAt: OffsetDateTime,
                           endsAt: Option[OffsetDateTime],
                           isPermanent: Boolean) extends Event
object ChannelBanEvent {
  given Decoder[ChannelBanEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelBanEvent] = ConfiguredEncoder.derived
}

case class ChannelUnbanEvent(userId: String,
                             userLogin: String,
                             userName: String,
                             broadcasterUserId: String,
                             broadcasterUserLogin: String,
                             broadcasterUserName: String,
                             moderatorUserId: String,
                             moderatorUserLogin: String,
                             moderatorUserName: String) extends Event
object ChannelUnbanEvent {
  given Decoder[ChannelUnbanEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelUnbanEvent] = ConfiguredEncoder.derived
}

case class ChannelUnbanRequestCreateEvent(id: String,
                                          broadcasterUserId: String,
                                          broadcasterUserLogin: String,
                                          broadcasterUserName: String,
                                          userId: String,
                                          userLogin: String,
                                          userName: String,
                                          text: String,
                                          createdAt: OffsetDateTime) extends Event
object ChannelUnbanRequestCreateEvent {
  given Decoder[ChannelUnbanRequestCreateEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelUnbanRequestCreateEvent] = ConfiguredEncoder.derived
}

case class ChannelUnbanRequestResolveEvent(id: String,
                                           broadcasterUserId: String,
                                           broadcasterUserLogin: String,
                                           broadcasterUserName: String,
                                           moderatorUserId: Option[String],
                                           moderatorUserLogin: Option[String],
                                           moderatorUserName: Option[String],
                                           userId: String,
                                           userLogin: String,
                                           userName: String,
                                           resolutionText: Option[String],
                                           status: String)  extends Event
object ChannelUnbanRequestResolveEvent {
  given Decoder[ChannelUnbanRequestResolveEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelUnbanRequestResolveEvent] = ConfiguredEncoder.derived
}

case class ChannelModerateEvent(broadcasterUserId: String,
                                broadcasterUserLogin: String,
                                broadcasterUserName: String,
                                moderatorUserId: String,
                                moderatorUserLogin: String,
                                moderatorUserName: String,
                                action: String,
                                followers: Option[Followers] = None,
                                slow: Option[Slow] = None,
                                vip: Option[Vip] = None,
                                unvip: Option[Unvip] = None,
                                mod: Option[Mod] = None,
                                unmod: Option[Unmod] = None,
                                ban: Option[Ban] = None,
                                unban: Option[Unban] = None,
                                timeout: Option[Timeout] = None,
                                untimeout: Option[Untimeout] = None,
                                raid: Option[Raid] = None,
                                unraid: Option[Unraid] = None,
                                delete: Option[Delete] = None,
                                automodTerms: Option[AutomodTerms] = None,
                                unbanRequest: Option[UnbanRequest] = None) extends Event
object ChannelModerateEvent {
  given Decoder[ChannelModerateEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelModerateEvent] = ConfiguredEncoder.derived
}

case class ChannelModerateV2Event(broadcasterUserId: String,
                                  broadcasterUserLogin: String,
                                  broadcasterUserName: String,
                                  moderatorUserId: String,
                                  moderatorUserLogin: String,
                                  moderatorUserName: String,
                                  action: String,
                                  followers: Option[Followers] = None,
                                  slow: Option[Slow] = None,
                                  vip: Option[Vip] = None,
                                  unvip: Option[Unvip] = None,
                                  mod: Option[Mod] = None,
                                  unmod: Option[Unmod] = None,
                                  ban: Option[Ban] = None,
                                  unban: Option[Unban] = None,
                                  timeout: Option[Timeout] = None,
                                  untimeout: Option[Untimeout] = None,
                                  raid: Option[Raid] = None,
                                  unraid: Option[Unraid] = None,
                                  delete: Option[Delete] = None,
                                  automodTerms: Option[AutomodTerms] = None,
                                  unbanRequest: Option[UnbanRequest] = None,
                                  warn: Option[Warn] = None) extends Event
object ChannelModerateV2Event {
  given Decoder[ChannelModerateV2Event] = ConfiguredDecoder.derived
  given Encoder[ChannelModerateV2Event] = ConfiguredEncoder.derived
}

case class ChannelModeratorAddEvent(broadcasterUserId: String,
                                    broadcasterUserLogin: String,
                                    broadcasterUserName: String,
                                    userId: String,
                                    userLogin: String,
                                    userName: String) extends Event
object ChannelModeratorAddEvent {
  given Decoder[ChannelModeratorAddEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelModeratorAddEvent] = ConfiguredEncoder.derived
}

case class ChannelModeratorRemoveEvent(broadcasterUserId: String,
                                       broadcasterUserLogin: String,
                                       broadcasterUserName: String,
                                       userId: String,
                                       userLogin: String,
                                       userName: String) extends Event
object ChannelModeratorRemoveEvent {
  given Decoder[ChannelModeratorRemoveEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelModeratorRemoveEvent] = ConfiguredEncoder.derived
}

case class ChannelGuestStarSessionBeginEvent(broadcasterUserId: String,
                                             broadcasterUserLogin: String,
                                             broadcasterUserName: String,
                                             moderatorUserId: Option[String],
                                             moderatorUserLogin: Option[String],
                                             moderatorUserName: Option[String],
                                             sessionId: String,
                                             startedAt: OffsetDateTime) extends Event
object ChannelGuestStarSessionBeginEvent {
  given Decoder[ChannelGuestStarSessionBeginEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelGuestStarSessionBeginEvent] = ConfiguredEncoder.derived
}

case class ChannelGuestStarSessionEndEvent(broadcasterUserId: String,
                                           broadcasterUserLogin: String,
                                           broadcasterUserName: String,
                                           moderatorUserId: Option[String],
                                           moderatorUserLogin: Option[String],
                                           moderatorUserName: Option[String],
                                           sessionId: String,
                                           startedAt: OffsetDateTime,
                                           endedAt: OffsetDateTime) extends Event
object ChannelGuestStarSessionEndEvent {
  given Decoder[ChannelGuestStarSessionEndEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelGuestStarSessionEndEvent] = ConfiguredEncoder.derived
}

case class ChannelGuestStarGuestUpdateEvent(broadcasterUserId: String,
                                            broadcasterUserLogin: String,
                                            broadcasterUserName: String,
                                            sessionId: String,
                                            moderatorUserId: Option[String],
                                            moderatorUserLogin: Option[String],
                                            moderatorUserName: Option[String],
                                            guestUserId: Option[String],
                                            guestUserLogin: Option[String],
                                            guestUserName: Option[String],
                                            slotId: Option[String],
                                            state: Option[String],
                                            hostVideoEnabled: Option[Boolean],
                                            hostAudioEnabled: Option[Boolean],
                                            hostVolume: Option[Int]) extends Event
object ChannelGuestStarGuestUpdateEvent {
  given Decoder[ChannelGuestStarGuestUpdateEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelGuestStarGuestUpdateEvent] = ConfiguredEncoder.derived
}

case class ChannelGuestStarSettingsUpdateEvent(broadcasterUserId: String,
                                               broadcasterUserLogin: String,
                                               broadcasterUserName: String,
                                               isModeratorSendLiveEnabled: Boolean,
                                               moderatorUserId: Option[String],
                                               moderatorUserLogin: Option[String],
                                               moderatorUserName: Option[String],
                                               slotCount: Int,
                                               isBrowserSourceAudioEnabled: Boolean,
                                               groupLayout: String) extends Event
object ChannelGuestStarSettingsUpdateEvent {
  given Decoder[ChannelGuestStarSettingsUpdateEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelGuestStarSettingsUpdateEvent] = ConfiguredEncoder.derived
}

case class ChannelPointsAutomaticRewardRedemptionAddEvent(broadcasterUserId: String,
                                                          broadcasterUserLogin: String,
                                                          broadcasterUserName: String,
                                                          userId: String,
                                                          userLogin: String,
                                                          userName: String,
                                                          id: String,
                                                          reward: RewardInformation,
                                                          message: Message,
                                                          userInput: Option[String],
                                                          redeemedAt: OffsetDateTime) extends Event
object ChannelPointsAutomaticRewardRedemptionAddEvent {
  given Decoder[ChannelPointsAutomaticRewardRedemptionAddEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelPointsAutomaticRewardRedemptionAddEvent] = ConfiguredEncoder.derived
}

case class ChannelPointsCustomRewardAddEvent(id: String,
                                             broadcasterUserId: String,
                                             broadcasterUserLogin: String,
                                             broadcasterUserName: String,
                                             isEnabled: Boolean,
                                             isPaused: Boolean,
                                             isInStock: Boolean,
                                             title: String,
                                             cost: Int,
                                             prompt: String,
                                             isUserInputRequired: Boolean,
                                             shouldRedemptionsSkipRequestQueue: Boolean,
                                             maxPerStream: MaxPerStream,
                                             maxPerUserPerStream: MaxPerStream,
                                             backgroundColor: String,
                                             image: Option[Image],
                                             defaultImage: Image,
                                             globalCooldown: GlobalCooldown,
                                             cooldownExpiresAt: Option[OffsetDateTime],
                                             redemptionsRedeemedCurrentStream: Option[Int]) extends Event
object ChannelPointsCustomRewardAddEvent {
  given Decoder[ChannelPointsCustomRewardAddEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelPointsCustomRewardAddEvent] = ConfiguredEncoder.derived
}

case class ChannelPointsCustomRewardUpdateEvent(id: String,
                                                broadcasterUserId: String,
                                                broadcasterUserLogin: String,
                                                broadcasterUserName: String,
                                                isEnabled: Boolean,
                                                isPaused: Boolean,
                                                isInStock: Boolean,
                                                title: String,
                                                cost: Int,
                                                prompt: String,
                                                isUserInputRequired: Boolean,
                                                shouldRedemptionsSkipRequestQueue: Boolean,
                                                maxPerStream: MaxPerStream,
                                                maxPerUserPerStream: MaxPerStream,
                                                backgroundColor: String,
                                                image: Option[Image],
                                                defaultImage: Image,
                                                globalCooldown: GlobalCooldown,
                                                cooldownExpiresAt: Option[OffsetDateTime],
                                                redemptionsRedeemedCurrentStream: Option[Int]) extends Event
object ChannelPointsCustomRewardUpdateEvent {
  given Decoder[ChannelPointsCustomRewardUpdateEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelPointsCustomRewardUpdateEvent] = ConfiguredEncoder.derived
}

case class ChannelPointsCustomRewardRemoveEvent(id: String,
                                                broadcasterUserId: String,
                                                broadcasterUserLogin: String,
                                                broadcasterUserName: String,
                                                isEnabled: Boolean,
                                                isPaused: Boolean,
                                                isInStock: Boolean,
                                                title: String,
                                                cost: Int,
                                                prompt: String,
                                                isUserInputRequired: Boolean,
                                                shouldRedemptionsSkipRequestQueue: Boolean,
                                                maxPerStream: MaxPerStream,
                                                maxPerUserPerStream: MaxPerStream,
                                                backgroundColor: String,
                                                image: Option[Image],
                                                defaultImage: Image,
                                                globalCooldown: GlobalCooldown,
                                                cooldownExpiresAt: Option[OffsetDateTime],
                                                redemptionsRedeemedCurrentStream: Option[Int]) extends Event
object ChannelPointsCustomRewardRemoveEvent {
  given Decoder[ChannelPointsCustomRewardRemoveEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelPointsCustomRewardRemoveEvent] = ConfiguredEncoder.derived
}

case class ChannelPointsCustomRewardRedemptionAddEvent(id: String,
                                                       broadcasterUserId: String,
                                                       broadcasterUserLogin: String,
                                                       broadcasterUserName: String,
                                                       userId: String,
                                                       userLogin: String,
                                                       userName: String,
                                                       userInput: String,
                                                       status: String,
                                                       reward: Reward,
                                                       redeemedAt: OffsetDateTime) extends Event
object ChannelPointsCustomRewardRedemptionAddEvent {
  given Decoder[ChannelPointsCustomRewardRedemptionAddEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelPointsCustomRewardRedemptionAddEvent] = ConfiguredEncoder.derived
}

case class ChannelPointsCustomRewardRedemptionUpdateEvent(id: String,
                                                          broadcasterUserId: String,
                                                          broadcasterUserLogin: String,
                                                          broadcasterUserName: String,
                                                          userId: String,
                                                          userLogin: String,
                                                          userName: String,
                                                          userInput: String,
                                                          status: String,
                                                          reward: Reward,
                                                          redeemedAt: OffsetDateTime) extends Event
object ChannelPointsCustomRewardRedemptionUpdateEvent {
  given Decoder[ChannelPointsCustomRewardRedemptionUpdateEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelPointsCustomRewardRedemptionUpdateEvent] = ConfiguredEncoder.derived
}

case class ChannelPollBeginEvent(id: String,
                                 broadcasterUserId: String,
                                 broadcasterUserLogin: String,
                                 broadcasterUserName: String,
                                 title: String,
                                 choices: List[PollChoice],
                                 bitsVoting: BitsVoting,
                                 channelPointsVoting: ChannelPointsVoting,
                                 startedAt: OffsetDateTime,
                                 endsAt: OffsetDateTime) extends Event
object ChannelPollBeginEvent {
  given Decoder[ChannelPollBeginEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelPollBeginEvent] = ConfiguredEncoder.derived
}

case class ChannelPollProgressEvent(id: String,
                                    broadcasterUserId: String,
                                    broadcasterUserLogin: String,
                                    broadcasterUserName: String,
                                    title: String,
                                    choices: List[StartedPollChoice],
                                    bitsVoting: BitsVoting,
                                    channelPointsVoting: ChannelPointsVoting,
                                    startedAt: OffsetDateTime,
                                    endsAt: OffsetDateTime) extends Event
object ChannelPollProgressEvent {
  given Decoder[ChannelPollProgressEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelPollProgressEvent] = ConfiguredEncoder.derived
}

case class ChannelPollEndEvent(id: String,
                               broadcasterUserId: String,
                               broadcasterUserLogin: String,
                               broadcasterUserName: String,
                               title: String,
                               choices: List[StartedPollChoice],
                               bitsVoting: BitsVoting,
                               channelPointsVoting: ChannelPointsVoting,
                               status: String,
                               startedAt: OffsetDateTime,
                               endedAt: OffsetDateTime) extends Event
object ChannelPollEndEvent {
  given Decoder[ChannelPollEndEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelPollEndEvent] = ConfiguredEncoder.derived
}

case class ChannelPredictionBeginEvent(id: String,
                                       broadcasterUserId: String,
                                       broadcasterUserLogin: String,
                                       broadcasterUserName: String,
                                       title: String,
                                       outcomes: List[PredictionOutcome],
                                       startedAt: OffsetDateTime,
                                       locksAt: OffsetDateTime) extends Event
object ChannelPredictionBeginEvent {
  given Decoder[ChannelPredictionBeginEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelPredictionBeginEvent] = ConfiguredEncoder.derived
}

case class ChannelPredictionProgressEvent(id: String,
                                          broadcasterUserId: String,
                                          broadcasterUserLogin: String,
                                          broadcasterUserName: String,
                                          title: String,
                                          outcomes: List[StartedPredictionOutcome],
                                          startedAt: OffsetDateTime,
                                          locksAt: OffsetDateTime) extends Event
object ChannelPredictionProgressEvent {
  given Decoder[ChannelPredictionProgressEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelPredictionProgressEvent] = ConfiguredEncoder.derived
}

case class ChannelPredictionLockEvent(id: String,
                                      broadcasterUserId: String,
                                      broadcasterUserLogin: String,
                                      broadcasterUserName: String,
                                      title: String,
                                      outcomes: List[StartedPredictionOutcome],
                                      startedAt: OffsetDateTime,
                                      lockedAt: OffsetDateTime) extends Event
object ChannelPredictionLockEvent {
  given Decoder[ChannelPredictionLockEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelPredictionLockEvent] = ConfiguredEncoder.derived
}

case class ChannelPredictionEndEvent(id: String,
                                     broadcasterUserId: String,
                                     broadcasterUserLogin: String,
                                     broadcasterUserName: String,
                                     title: String,
                                     outcomes: List[StartedPredictionOutcome],
                                     status: String,
                                     startedAt: OffsetDateTime,
                                     endedAt: OffsetDateTime) extends Event
object ChannelPredictionEndEvent {
  given Decoder[ChannelPredictionEndEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelPredictionEndEvent] = ConfiguredEncoder.derived
}

case class ChannelSuspiciousUserUpdateEvent(broadcasterUserId: String,
                                            broadcasterUserLogin: String,
                                            broadcasterUserName: String,
                                            moderatorUserId: String,
                                            moderatorUserLogin: String,
                                            moderatorUserName: String,
                                            userId: String,
                                            userLogin: String,
                                            userName: String,
                                            lowTrustStatus: String) extends Event
object ChannelSuspiciousUserUpdateEvent {
  given Decoder[ChannelSuspiciousUserUpdateEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelSuspiciousUserUpdateEvent] = ConfiguredEncoder.derived
}

case class ChannelSuspiciousUserMessageEvent(broadcasterUserId: String,
                                             broadcasterUserLogin: String,
                                             broadcasterUserName: String,
                                             userId: String,
                                             userLogin: String,
                                             userName: String,
                                             lowTrustStatus: String,
                                             sharedBanChannelIds: List[String],
                                             types: List[String],
                                             banEvasionEvaluation: String,
                                             message: MessageWithIdAndFragments) extends Event
object ChannelSuspiciousUserMessageEvent {
  given Decoder[ChannelSuspiciousUserMessageEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelSuspiciousUserMessageEvent] = ConfiguredEncoder.derived
}

case class ChannelVIPAddEvent(userId: String,
                              userLogin: String,
                              userName: String,
                              broadcasterUserId: String,
                              broadcasterUserLogin: String,
                              broadcasterUserName: String) extends Event
object ChannelVIPAddEvent {
  given Decoder[ChannelVIPAddEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelVIPAddEvent] = ConfiguredEncoder.derived
}

case class ChannelVIPRemoveEvent(userId: String,
                                 userLogin: String,
                                 userName: String,
                                 broadcasterUserId: String,
                                 broadcasterUserLogin: String,
                                 broadcasterUserName: String) extends Event
object ChannelVIPRemoveEvent {
  given Decoder[ChannelVIPRemoveEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelVIPRemoveEvent] = ConfiguredEncoder.derived
}

case class ChannelWarningAcknowledgeEvent(broadcasterUserId: String,
                                          broadcasterUserLogin: String,
                                          broadcasterUserName: String,
                                          userId: String,
                                          userLogin: String,
                                          userName: String) extends Event
object ChannelWarningAcknowledgeEvent {
  given Decoder[ChannelWarningAcknowledgeEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelWarningAcknowledgeEvent] = ConfiguredEncoder.derived
}

case class ChannelWarningSendEvent(broadcasterUserId: String,
                                   broadcasterUserLogin: String,
                                   broadcasterUserName: String,
                                   moderatorUserId: String,
                                   moderatorUserLogin: String,
                                   moderatorUserName: String,
                                   userId: String,
                                   userLogin: String,
                                   userName: String,
                                   reason: Option[String],
                                   chatRulesCited: Option[List[String]]) extends Event
object ChannelWarningSendEvent {
  given Decoder[ChannelWarningSendEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelWarningSendEvent] = ConfiguredEncoder.derived
}

case class ChannelHypeTrainBeginEvent(id: String,
                                      broadcasterUserId: String,
                                      broadcasterUserLogin: String,
                                      broadcasterUserName: String,
                                      total: Int,
                                      progress: Int,
                                      goal: Int,
                                      topContributions: List[Contribution],
                                      lastContribution: Contribution,
                                      level: Int,
                                      startedAt: OffsetDateTime,
                                      expiresAt: OffsetDateTime) extends Event
object ChannelHypeTrainBeginEvent {
  given Decoder[ChannelHypeTrainBeginEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelHypeTrainBeginEvent] = ConfiguredEncoder.derived
}

case class ChannelHypeTrainProgressEvent(id: String,
                                         broadcasterUserId: String,
                                         broadcasterUserLogin: String,
                                         broadcasterUserName: String,
                                         total: Int,
                                         progress: Int,
                                         goal: Int,
                                         topContributions: List[Contribution],
                                         lastContribution: Contribution,
                                         level: Int,
                                         startedAt: OffsetDateTime,
                                         expiresAt: OffsetDateTime) extends Event
object ChannelHypeTrainProgressEvent {
  given Decoder[ChannelHypeTrainProgressEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelHypeTrainProgressEvent] = ConfiguredEncoder.derived
}

case class ChannelHypeTrainEndEvent(id: String,
                                    broadcasterUserId: String,
                                    broadcasterUserLogin: String,
                                    broadcasterUserName: String,
                                    total: Int,
                                    topContributions: List[Contribution],
                                    level: Int,
                                    startedAt: OffsetDateTime,
                                    endedAt: OffsetDateTime,
                                    cooldownEndsAt: OffsetDateTime) extends Event
object ChannelHypeTrainEndEvent {
  given Decoder[ChannelHypeTrainEndEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelHypeTrainEndEvent] = ConfiguredEncoder.derived
}

case class ChannelCharityDonationEvent(id: String,
                                       campaignId: String,
                                       broadcasterUserId: String,
                                       broadcasterUserLogin: String,
                                       broadcasterUserName: String,
                                       userId: String,
                                       userLogin: String,
                                       userName: String,
                                       charityName: String,
                                       charityDescription: String,
                                       charityLogo: String,
                                       charityWebsite: String,
                                       amount: Amount) extends Event
object ChannelCharityDonationEvent {
  given Decoder[ChannelCharityDonationEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelCharityDonationEvent] = ConfiguredEncoder.derived
}

case class ChannelCharityCampaignStartEvent(id: String,
                                            broadcasterId: String,
                                            broadcasterLogin: String,
                                            broadcasterName: String,
                                            charityName: String,
                                            charityDescription: String,
                                            charityLogo: String,
                                            charityWebsite: String,
                                            currentAmount: Amount,
                                            targetAmount: Amount,
                                            startedAt: OffsetDateTime) extends Event
object ChannelCharityCampaignStartEvent {
  given Decoder[ChannelCharityCampaignStartEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelCharityCampaignStartEvent] = ConfiguredEncoder.derived
}

case class ChannelCharityCampaignProgressEvent(id: String,
                                               broadcasterId: String,
                                               broadcasterLogin: String,
                                               broadcasterName: String,
                                               charityName: String,
                                               charityDescription: String,
                                               charityLogo: String,
                                               charityWebsite: String,
                                               currentAmount: Amount,
                                               targetAmount: Amount) extends Event
object ChannelCharityCampaignProgressEvent {
  given Decoder[ChannelCharityCampaignProgressEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelCharityCampaignProgressEvent] = ConfiguredEncoder.derived
}

case class ChannelCharityCampaignStopEvent(id: String,
                                           broadcasterId: String,
                                           broadcasterLogin: String,
                                           broadcasterName: String,
                                           charityName: String,
                                           charityDescription: String,
                                           charityLogo: String,
                                           charityWebsite: String,
                                           currentAmount: Amount,
                                           targetAmount: Amount,
                                           stoppedAt: OffsetDateTime) extends Event
object ChannelCharityCampaignStopEvent {
  given Decoder[ChannelCharityCampaignStopEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelCharityCampaignStopEvent] = ConfiguredEncoder.derived
}

case class ChannelShieldModeBeginEvent(broadcasterUserId: String,
                                       broadcasterUserLogin: String,
                                       broadcasterUserName: String,
                                       moderatorUserId: String,
                                       moderatorUserLogin: String,
                                       moderatorUserName: String,
                                       startedAt: OffsetDateTime) extends Event
object ChannelShieldModeBeginEvent {
  given Decoder[ChannelShieldModeBeginEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelShieldModeBeginEvent] = ConfiguredEncoder.derived
}

case class ChannelShieldModeEndEvent(broadcasterUserId: String,
                                     broadcasterUserLogin: String,
                                     broadcasterUserName: String,
                                     moderatorUserId: String,
                                     moderatorUserLogin: String,
                                     moderatorUserName: String,
                                     endedAt: OffsetDateTime) extends Event
object ChannelShieldModeEndEvent {
  given Decoder[ChannelShieldModeEndEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelShieldModeEndEvent] = ConfiguredEncoder.derived
}

case class ChannelShoutoutCreateEvent(broadcasterUserId: String,
                                      broadcasterUserLogin: String,
                                      broadcasterUserName: String,
                                      moderatorUserId: String,
                                      moderatorUserLogin: String,
                                      moderatorUserName: String,
                                      toBroadcasterUserId: String,
                                      toBroadcasterUserLogin: String,
                                      toBroadcasterUserName: String,
                                      startedAt: OffsetDateTime,
                                      viewerCount: Int,
                                      cooldownEndsAt: OffsetDateTime,
                                      targetCooldownEndsAt: OffsetDateTime) extends Event
object ChannelShoutoutCreateEvent {
  given Decoder[ChannelShoutoutCreateEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelShoutoutCreateEvent] = ConfiguredEncoder.derived
}

case class ChannelShoutoutReceiveEvent(broadcasterUserId: String,
                                       broadcasterUserLogin: String,
                                       broadcasterUserName: String,
                                       fromBroadcasterUserId: String,
                                       fromBroadcasterUserLogin: String,
                                       fromBroadcasterUserName: String,
                                       viewerCount: Int,
                                       startedAt: OffsetDateTime) extends Event
object ChannelShoutoutReceiveEvent {
  given Decoder[ChannelShoutoutReceiveEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelShoutoutReceiveEvent] = ConfiguredEncoder.derived
}

case class ConduitShardDisabledEvent(conduit_id: String,
                                     shard_id: String,
                                     status: String,
                                     transport: Transport) extends Event
object ConduitShardDisabledEvent {
  given Decoder[ConduitShardDisabledEvent] = ConfiguredDecoder.derived
  given Encoder[ConduitShardDisabledEvent] = ConfiguredEncoder.derived
}

case class DropEntitlementGrantEvents(events: List[DropEntitlementGrantEvent]) extends Event
object DropEntitlementGrantEvents {
  given Decoder[DropEntitlementGrantEvents] = (c: HCursor) => {
    for {
      events <- c.as[List[DropEntitlementGrantEvent]]
    } yield DropEntitlementGrantEvents(events)
  }
  given Encoder[DropEntitlementGrantEvents] = (e: DropEntitlementGrantEvents) => {
    Json.fromValues(e.events.map(_.asJson))
  }
}

case class DropEntitlementGrantEvent(id: String, data: Entitlement)
object DropEntitlementGrantEvent {
  given Decoder[DropEntitlementGrantEvent] = ConfiguredDecoder.derived
  given Encoder[DropEntitlementGrantEvent] = ConfiguredEncoder.derived
}

// NOT SUPPORTED - required webhooks instead of websockets
case class ExtensionBitsTransactionCreateEvent(extensionClientId: String,
                                               id: String,
                                               broadcasterUserId: String,
                                               broadcasterUserLogin: String,
                                               broadcasterUserName: String,
                                               userId: String,
                                               userLogin: String,
                                               userName: String,
                                               product: ExtensionProduct) extends Event
object ExtensionBitsTransactionCreateEvent {
  given Decoder[ExtensionBitsTransactionCreateEvent] = ConfiguredDecoder.derived
  given Encoder[ExtensionBitsTransactionCreateEvent] = ConfiguredEncoder.derived
}

case class ChannelGoalBeginEvent(id: String,
                                 broadcasterUserId: String,
                                 broadcasterUserLogin: String,
                                 broadcasterUserName: String,
                                 `type`: String,
                                 description: String,
                                 currentAmount: Int,
                                 targetAmount: Int,
                                 startedAt: OffsetDateTime) extends Event
object ChannelGoalBeginEvent {
  given Decoder[ChannelGoalBeginEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelGoalBeginEvent] = ConfiguredEncoder.derived
}

case class ChannelGoalProgressEvent(id: String,
                                    broadcasterUserId: String,
                                    broadcasterUserLogin: String,
                                    broadcasterUserName: String,
                                    `type`: String,
                                    description: String,
                                    currentAmount: Int,
                                    targetAmount: Int,
                                    startedAt: OffsetDateTime) extends Event
object ChannelGoalProgressEvent {
  given Decoder[ChannelGoalProgressEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelGoalProgressEvent] = ConfiguredEncoder.derived
}

case class ChannelGoalEndEvent(id: String,
                               broadcasterUserId: String,
                               broadcasterUserLogin: String,
                               broadcasterUserName: String,
                               `type`: String,
                               description: String,
                               isAchieved: Boolean,
                               currentAmount: Int,
                               targetAmount: Int,
                               startedAt: OffsetDateTime,
                               endedAt: OffsetDateTime) extends Event
object ChannelGoalEndEvent {
  given Decoder[ChannelGoalEndEvent] = ConfiguredDecoder.derived
  given Encoder[ChannelGoalEndEvent] = ConfiguredEncoder.derived
}

case class StreamOnlineEvent(id: String,
                             broadcasterUserId: String,
                             broadcasterUserLogin: String,
                             broadcasterUserName: String,
                             `type`: String,
                             startedAt: OffsetDateTime) extends Event
object StreamOnlineEvent {
  given Decoder[StreamOnlineEvent] = ConfiguredDecoder.derived
  given Encoder[StreamOnlineEvent] = ConfiguredEncoder.derived
}

case class StreamOfflineEvent(broadcasterUserId: String,
                              broadcasterUserLogin: String,
                              broadcasterUserName: String) extends Event
object StreamOfflineEvent {
  given Decoder[StreamOfflineEvent] = ConfiguredDecoder.derived
  given Encoder[StreamOfflineEvent] = ConfiguredEncoder.derived
}

case class UserAuthorizationGrantEvent(client_id: String,
                                       userId: String,
                                       userLogin: String,
                                       userName: String) extends Event
object UserAuthorizationGrantEvent {
  given Decoder[UserAuthorizationGrantEvent] = ConfiguredDecoder.derived
  given Encoder[UserAuthorizationGrantEvent] = ConfiguredEncoder.derived
}

case class UserAuthorizationRevokeEvent(client_id: String,
                                        userId: String,
                                        userLogin: Option[String],
                                        userName: Option[String]) extends Event
object UserAuthorizationRevokeEvent {
  given Decoder[UserAuthorizationRevokeEvent] = ConfiguredDecoder.derived
  given Encoder[UserAuthorizationRevokeEvent] = ConfiguredEncoder.derived
}

case class UserUpdateEvent(userId: String,
                           userLogin: String,
                           userName: String,
                           email: String,
                           email_verified: Boolean,
                           description: String) extends Event
object UserUpdateEvent {
  given Decoder[UserUpdateEvent] = ConfiguredDecoder.derived
  given Encoder[UserUpdateEvent] = ConfiguredEncoder.derived
}

case class WhisperReceivedEvent(from_userId: String,
                                from_userLogin: String,
                                from_userName: String,
                                to_userId: String,
                                to_userLogin: String,
                                to_userName: String,
                                whisper_id: String,
                                whisper: Whisper) extends Event
object WhisperReceivedEvent {
  given Decoder[WhisperReceivedEvent] = ConfiguredDecoder.derived
  given Encoder[WhisperReceivedEvent] = ConfiguredEncoder.derived
}

case class MessageFragments(emotes: List[EmoteFragment], cheermotes: List[CheermoteFragment])
object MessageFragments {
  given Decoder[MessageFragments] = ConfiguredDecoder.derived
  given Encoder[MessageFragments] = ConfiguredEncoder.derived
}

case class EmoteFragment(text: String, id: String, `set-id`: String)
object EmoteFragment {
  given Decoder[EmoteFragment] = ConfiguredDecoder.derived
  given Encoder[EmoteFragment] = ConfiguredEncoder.derived
}

case class CheermoteFragment(text: String, amount: Int, prefix: String, tier: Int)
object CheermoteFragment {
  given Decoder[CheermoteFragment] = ConfiguredDecoder.derived
  given Encoder[CheermoteFragment] = ConfiguredEncoder.derived
}

case class AutomodSettingsData(broadcasterUserId: String,
                               broadcasterUserLogin: String,
                               broadcasterUserName: String,
                               moderatorUserId: String,
                               moderatorUserLogin: String,
                               moderatorUserName: String,
                               bullying: Int,
                               overallLevel: Option[Int],
                               disability: Int,
                               raceEthnicityOrReligion: Int,
                               misogyny: Int,
                               sexualitySexOrGender: Int,
                               aggression: Int,
                               sexBasedTerms: Int,
                               swearing: Int)
object AutomodSettingsData {
  given Decoder[AutomodSettingsData] = ConfiguredDecoder.derived
  given Encoder[AutomodSettingsData] = ConfiguredEncoder.derived
}

case class ChatMessage(text: String,
                       fragments: List[ChatMessageFragment])
object ChatMessage {
  given Decoder[ChatMessage] = ConfiguredDecoder.derived
  given Encoder[ChatMessage] = ConfiguredEncoder.derived
}

case class ChatMessageFragment(`type`: String,
                               text: String,
                               cheermote: Option[ChatMessageCheermote],
                               emote: Option[ChatMessageEmote],
                               mention: Option[ChatMessageMention])
object ChatMessageFragment {
  given Decoder[ChatMessageFragment] = ConfiguredDecoder.derived
  given Encoder[ChatMessageFragment] = ConfiguredEncoder.derived
}

case class ChatMessageCheermote(prefix: String, bits: Int, tier: Int)
object ChatMessageCheermote {
  given Decoder[ChatMessageCheermote] = ConfiguredDecoder.derived
  given Encoder[ChatMessageCheermote] = ConfiguredEncoder.derived
}

case class ChatMessageEmote(id: String, emoteSetId: String, ownerId: Option[String] = None, format: Option[List[String]] = None)
object ChatMessageEmote {
  given Decoder[ChatMessageEmote] = ConfiguredDecoder.derived
  given Encoder[ChatMessageEmote] = ConfiguredEncoder.derived
}

case class ChatMessageMention(userId: String, userLogin: String, userName: String)
object ChatMessageMention {
  given Decoder[ChatMessageMention] = ConfiguredDecoder.derived
  given Encoder[ChatMessageMention] = ConfiguredEncoder.derived
}

case class Badge(setId: String, id: String, info: String)
object Badge {
  given Decoder[Badge] = ConfiguredDecoder.derived
  given Encoder[Badge] = ConfiguredEncoder.derived
}

case class Cheer(bits: Int)
object Cheer {
  given Decoder[Cheer] = ConfiguredDecoder.derived
  given Encoder[Cheer] = ConfiguredEncoder.derived
}

case class Reply(parentMessageId: String,
                 parentMessageBody: String,
                 parentUserId: String,
                 parentUserLogin: String,
                 parentUserName: String,
                 threadMessageId: String,
                 threadUserId: String,
                 threadUserLogin: String,
                 threadUserName: String)
object Reply {
  given Decoder[Reply] = ConfiguredDecoder.derived
  given Encoder[Reply] = ConfiguredEncoder.derived
}

case class NoticeSub(subTier: String, isPrime: Boolean, durationMonths: Int)
object NoticeSub {
  given Decoder[NoticeSub] = ConfiguredDecoder.derived
  given Encoder[NoticeSub] = ConfiguredEncoder.derived
}

case class NoticeResub(cumulativeMonths: Int,
                       durationMonths: Int,
                       streakMonths: Int,
                       subTier: String,
                       isPrime: Boolean,
                       isGift: Boolean,
                       gifterIsAnonymous: Option[Boolean],
                       gifterUserId: Option[String],
                       gifterUserLogin: Option[String],
                       gifterUserName: Option[String])
object NoticeResub {
  given Decoder[NoticeResub] = ConfiguredDecoder.derived
  given Encoder[NoticeResub] = ConfiguredEncoder.derived
}

case class NoticeSubGift(durationMonths: Int,
                         cumulativeTotal: Option[Int],
                         recipientUserId: String,
                         recipientUserLogin: String,
                         recipientUserName: String,
                         subTier: String,
                         communityGiftId: Option[String])
object NoticeSubGift {
  given Decoder[NoticeSubGift] = ConfiguredDecoder.derived
  given Encoder[NoticeSubGift] = ConfiguredEncoder.derived
}

case class NoticeCommunitySubGift(id: String,
                                  total: Int,
                                  subTier: String,
                                  cumulativeTotal: Option[Int])
object NoticeCommunitySubGift {
  given Decoder[NoticeCommunitySubGift] = ConfiguredDecoder.derived
  given Encoder[NoticeCommunitySubGift] = ConfiguredEncoder.derived
}

case class NoticeGiftPaidUpgrade(gifterIsAnonymous: Boolean,
                                 gifterUserId: Option[String],
                                 gifterUserLogin: Option[String],
                                 gifterUserName: Option[String])
object NoticeGiftPaidUpgrade {
  given Decoder[NoticeGiftPaidUpgrade] = ConfiguredDecoder.derived
  given Encoder[NoticeGiftPaidUpgrade] = ConfiguredEncoder.derived
}

case class NoticePrimePaidUpgrade(subTier: String)
object NoticePrimePaidUpgrade {
  given Decoder[NoticePrimePaidUpgrade] = ConfiguredDecoder.derived
  given Encoder[NoticePrimePaidUpgrade] = ConfiguredEncoder.derived
}

case class NoticeRaid(userId: String,
                      userLogin: String,
                      userName: String,
                      viewerCount: Int,
                      profileImageUrl: String)
object NoticeRaid {
  given Decoder[NoticeRaid] = ConfiguredDecoder.derived
  given Encoder[NoticeRaid] = ConfiguredEncoder.derived
}

case class NoticeUnraid(_empty: Option[String] = None)
object NoticeUnraid {
  given Decoder[NoticeUnraid] = ConfiguredDecoder.derived
  given Encoder[NoticeUnraid] = ConfiguredEncoder.derived
}

case class NoticePayItForward(gifterIsAnonymous: Boolean,
                              gifterUserId: Option[String],
                              gifterUserLogin: Option[String],
                              gifterUserName: Option[String])
object NoticePayItForward {
  given Decoder[NoticePayItForward] = ConfiguredDecoder.derived
  given Encoder[NoticePayItForward] = ConfiguredEncoder.derived
}

case class NoticeAnnouncement(color: String)
object NoticeAnnouncement {
  given Decoder[NoticeAnnouncement] = ConfiguredDecoder.derived
  given Encoder[NoticeAnnouncement] = ConfiguredEncoder.derived
}

case class NoticeCharityDonation(charityName: String, amount: Amount)
object NoticeCharityDonation {
  given Decoder[NoticeCharityDonation] = ConfiguredDecoder.derived
  given Encoder[NoticeCharityDonation] = ConfiguredEncoder.derived
}

case class NoticeBitsBadgeTier(tier: Int)
object NoticeBitsBadgeTier {
  given Decoder[NoticeBitsBadgeTier] = ConfiguredDecoder.derived
  given Encoder[NoticeBitsBadgeTier] = ConfiguredEncoder.derived
}

case class Amount(value: Int, decimalPlace: Int, currency: String)
object Amount {
  given Decoder[Amount] = (c: HCursor) => {
    for {
      value <- c.get[Int]("value")
      decimalPlace <- c.get[Int]("decimal_place").orElse(c.get[Int]("decimal_places"))
      currency <- c.get[String]("currency")
    } yield {
      Amount(value, decimalPlace, currency)
    }
  }
  given Encoder[Amount] = ConfiguredEncoder.derived
}

case class Message(text: String, emotes: List[EmotePosition])
object Message {
  given Decoder[Message] = ConfiguredDecoder.derived
  given Encoder[Message] = ConfiguredEncoder.derived
}

case class EmotePosition(begin: Int, end: Int, id: String)
object EmotePosition {
  given Decoder[EmotePosition] = ConfiguredDecoder.derived
  given Encoder[EmotePosition] = ConfiguredEncoder.derived
}

case class Followers(followDurationMinutes: Int)
object Followers {
  given Decoder[Followers] = ConfiguredDecoder.derived
  given Encoder[Followers] = ConfiguredEncoder.derived
}

case class Slow(waitTimeSeconds: Int)
object Slow {
  given Decoder[Slow] = ConfiguredDecoder.derived
  given Encoder[Slow] = ConfiguredEncoder.derived
}

case class Vip(userId: String, userLogin: String, userName: String)
object Vip {
  given Decoder[Vip] = ConfiguredDecoder.derived
  given Encoder[Vip] = ConfiguredEncoder.derived
}

case class Unvip(userId: String, userLogin: String, userName: String)
object Unvip {
  given Decoder[Unvip] = ConfiguredDecoder.derived
  given Encoder[Unvip] = ConfiguredEncoder.derived
}

case class Mod(userId: String, userLogin: String, userName: String)
object Mod {
  given Decoder[Mod] = ConfiguredDecoder.derived
  given Encoder[Mod] = ConfiguredEncoder.derived
}

case class Unmod(userId: String, userLogin: String, userName: String)
object Unmod {
  given Decoder[Unmod] = ConfiguredDecoder.derived
  given Encoder[Unmod] = ConfiguredEncoder.derived
}

case class Ban(userId: String, userLogin: String, userName: String, reason: Option[String])
object Ban {
  given Decoder[Ban] = ConfiguredDecoder.derived
  given Encoder[Ban] = ConfiguredEncoder.derived
}

case class Unban(userId: String, userLogin: String, userName: String)
object Unban {
  given Decoder[Unban] = ConfiguredDecoder.derived
  given Encoder[Unban] = ConfiguredEncoder.derived
}

case class Timeout(userId: String, userLogin: String, userName: String, reason: Option[String], expiresAt: OffsetDateTime)
object Timeout {
  given Decoder[Timeout] = ConfiguredDecoder.derived
  given Encoder[Timeout] = ConfiguredEncoder.derived
}

case class Untimeout(userId: String, userLogin: String, userName: String)
object Untimeout {
  given Decoder[Untimeout] = ConfiguredDecoder.derived
  given Encoder[Untimeout] = ConfiguredEncoder.derived
}

case class Raid(userId: String, userLogin: String, userName: String, viewerCount: Int)
object Raid {
  given Decoder[Raid] = ConfiguredDecoder.derived
  given Encoder[Raid] = ConfiguredEncoder.derived
}

case class Unraid(userId: String, userLogin: String, userName: String)
object Unraid {
  given Decoder[Unraid] = ConfiguredDecoder.derived
  given Encoder[Unraid] = ConfiguredEncoder.derived
}

case class Delete(userId: String, userLogin: String, userName: String, messageId: String, messageBody: String)
object Delete {
  given Decoder[Delete] = ConfiguredDecoder.derived
  given Encoder[Delete] = ConfiguredEncoder.derived
}

case class AutomodTerms(action: String, list: String, terms: List[String], fromAutomod: Boolean)
object AutomodTerms {
  given Decoder[AutomodTerms] = ConfiguredDecoder.derived
  given Encoder[AutomodTerms] = ConfiguredEncoder.derived
}

case class UnbanRequest(isApproved: Boolean, userId: String, userLogin: String, userName: String, moderatorMessage: String)
object UnbanRequest {
  given Decoder[UnbanRequest] = ConfiguredDecoder.derived
  given Encoder[UnbanRequest] = ConfiguredEncoder.derived
}

case class Warn(userId: String, userLogin: String, userName: String, reason: Option[String], chatRulesCited: Option[List[String]])
object Warn {
  given Decoder[Warn] = ConfiguredDecoder.derived
  given Encoder[Warn] = ConfiguredEncoder.derived
}

case class RewardInformation(`type`: String, cost: Int, unlockedEmote: Option[UnlockedEmote])
object RewardInformation {
  given Decoder[RewardInformation] = ConfiguredDecoder.derived
  given Encoder[RewardInformation] = ConfiguredEncoder.derived
}

case class UnlockedEmote(id: String, name: String)
object UnlockedEmote {
  given Decoder[UnlockedEmote] = ConfiguredDecoder.derived
  given Encoder[UnlockedEmote] = ConfiguredEncoder.derived
}

case class MaxPerStream(isEnabled: Boolean, value: Int)
object MaxPerStream {
  given Decoder[MaxPerStream] = ConfiguredDecoder.derived
  given Encoder[MaxPerStream] = ConfiguredEncoder.derived
}

case class Image(url_1x: String, url_2x: String, url_4x: String)
object Image {
  given Decoder[Image] = ConfiguredDecoder.derived
  given Encoder[Image] = ConfiguredEncoder.derived
}

case class GlobalCooldown(isEnabled: Boolean, seconds: Int)
object GlobalCooldown {
  given Decoder[GlobalCooldown] = ConfiguredDecoder.derived
  given Encoder[GlobalCooldown] = ConfiguredEncoder.derived
}

case class Reward(id: String, title: String, cost: Int, prompt: String)
object Reward {
  given Decoder[Reward] = ConfiguredDecoder.derived
  given Encoder[Reward] = ConfiguredEncoder.derived
}

case class PollChoice(id: String, title: String)
object PollChoice {
  given Decoder[PollChoice] = ConfiguredDecoder.derived
  given Encoder[PollChoice] = ConfiguredEncoder.derived
}

case class StartedPollChoice(id: String, title: String, bitsVotes: Int, channelPointsVotes: Int, votes: Int)
object StartedPollChoice {
  given Decoder[StartedPollChoice] = ConfiguredDecoder.derived
  given Encoder[StartedPollChoice] = ConfiguredEncoder.derived
}

case class BitsVoting(isEnabled: Boolean, amountPerVote: Int)
object BitsVoting {
  given Decoder[BitsVoting] = ConfiguredDecoder.derived
  given Encoder[BitsVoting] = ConfiguredEncoder.derived
}

case class ChannelPointsVoting(isEnabled: Boolean, amountPerVote: Int)
object ChannelPointsVoting {
  given Decoder[ChannelPointsVoting] = ConfiguredDecoder.derived
  given Encoder[ChannelPointsVoting] = ConfiguredEncoder.derived
}

case class PredictionOutcome(id: String, title: String, color: String)
object PredictionOutcome {
  given Decoder[PredictionOutcome] = ConfiguredDecoder.derived
  given Encoder[PredictionOutcome] = ConfiguredEncoder.derived
}

case class StartedPredictionOutcome(id: String, title: String, color: String, users: Int, channelPoints: Int, topPredictors: List[TopPredictor])
object StartedPredictionOutcome {
  given Decoder[StartedPredictionOutcome] = ConfiguredDecoder.derived
  given Encoder[StartedPredictionOutcome] = ConfiguredEncoder.derived
}

case class TopPredictor(userId: String, userLogin: String, userName: String, channelPointsWon: Option[Int], channelPointsUsed: Int)
object TopPredictor {
  given Decoder[TopPredictor] = ConfiguredDecoder.derived
  given Encoder[TopPredictor] = ConfiguredEncoder.derived
}

case class MessageWithIdAndFragments(messageId: String, text: String, fragments: List[ChatMessageFragment])
object MessageWithIdAndFragments {
  given Decoder[MessageWithIdAndFragments] = ConfiguredDecoder.derived
  given Encoder[MessageWithIdAndFragments] = ConfiguredEncoder.derived
}

case class Contribution(userId: String, userLogin: String, userName: String, `type`: String, total: Int)
object Contribution {
  given Decoder[Contribution] = ConfiguredDecoder.derived
  given Encoder[Contribution] = ConfiguredEncoder.derived
}

case class Entitlement(organizationId: String,
                       categoryId: String,
                       categoryName: String,
                       campaignId: String,
                       userId: String,
                       userLogin: String,
                       userName: String,
                       entitlementId: String,
                       benefitId: String,
                       createdAt: OffsetDateTime)
object Entitlement {
  given Decoder[Entitlement] = ConfiguredDecoder.derived
  given Encoder[Entitlement] = ConfiguredEncoder.derived
}

case class Whisper(text: String)
object Whisper {
  given Decoder[Whisper] = ConfiguredDecoder.derived
  given Encoder[Whisper] = ConfiguredEncoder.derived
}

case class ExtensionProduct(name: String, sku: String, bits: Int, inDevelopment: Boolean)
object ExtensionProduct {
  given Decoder[ExtensionProduct] = ConfiguredDecoder.derived
  given Encoder[ExtensionProduct] = ConfiguredEncoder.derived
}
