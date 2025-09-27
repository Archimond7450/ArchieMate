package com.archimond7450.archiemate.actors.chatbot

import com.archimond7450.archiemate.actors.{ArchieMateMediator, WebSocketClient}
import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.twitch.api.TwitchApiResponse
import com.archimond7450.archiemate.twitch.eventsub.*
import io.circe.jawn.decode
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.http.scaladsl.model.Uri
import org.apache.pekko.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import org.apache.pekko.util.ByteString

import scala.concurrent.duration.DurationInt

object EventSubListener {
  val actorName = "EventSubListener"

  sealed trait Command

  private case object Connected extends Command
  private case object RequestSubscription extends Command
  private final case class StreamTextMessage(msg: String) extends Command
  private final case class StreamBinaryMessage(bin: ByteString) extends Command
  private final case class StreamDecodedMessage(msg: IncomingMessage) extends Command
  private case object Done extends Command
  private final case class StreamFailure(ex: Throwable) extends Command

  def apply(tokenId: String, broadcaster: TwitchApiResponse.GetTokenUser)(using twitchChatbot: ActorRef[TwitchChatbot.Command], mediator: ActorRef[ArchieMateMediator.Command], settings: Settings): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx
    Behaviors.withTimers { scheduler =>
      given TimerScheduler[Command] = scheduler
      Behaviors.withStash(64) { buffer =>
        given StashBuffer[Command] = buffer
        new EventSubListener(tokenId, broadcaster).initial
      }
    }
  }
}

private class EventSubListener(
  private val tokenId: String,
  private val broadcaster: TwitchApiResponse.GetTokenUser
)(using
  private val ctx: ActorContext[EventSubListener.Command],
  private val scheduler: TimerScheduler[EventSubListener.Command],
  private val buffer: StashBuffer[EventSubListener.Command],
  private val twitchChatbot: ActorRef[TwitchChatbot.Command],
  private val mediator: ActorRef[ArchieMateMediator.Command],
  private val settings: Settings
) {
  import EventSubListener.*

  private case class RequestSubscriptionParameters(sessionId: String, subscriptionType: String, subscriptionVersion: String, condition: Condition)

  private object RequestSubscriptionParameters {
    val all: Seq[RequestSubscriptionParameters] = Seq(

    )
  }

  private val query = Uri.Query("keepalive_timeout_seconds" -> settings.twitchEventSubKeepalive.toSeconds.toString)

  private def initial: Behavior[Command] = {
    val webSocketClient = spawnWebSocketClient(settings.twitchEventSubUri.withQuery(query))
    disconnected(webSocketClient)
  }

  private def disconnected(webSocketClient: ActorRef[WebSocketClient.Command]): Behavior[Command] = Behaviors.receiveAndLogMessage {
    case Connected =>
      given ActorRef[WebSocketClient.Command] = webSocketClient
      connected()

    case msg =>
      logIgnore(msg, "disconnected")
      Behaviors.same
  }

  private def connected(remainingSubscriptions: Seq[RequestSubscriptionParameters] = Seq.empty)(using webSocketClient: ActorRef[WebSocketClient.Command]): Behavior[Command] = Behaviors.receiveAndLogMessage {
    case StreamTextMessage(msg) =>
      decodeAndProcess(msg)
      Behaviors.same

    case StreamBinaryMessage(data) =>
      logIgnoreBinary(data, "connected")
      Behaviors.same

    case StreamDecodedMessage(IncomingMessage(metadata, Payload(Some(session), _, _))) if metadata.messageType == "session_welcome" =>
      val sessionId = session.id
      val userId = Some(broadcaster.id)
      val condition = Condition(broadcasterUserId = userId)
      val conditionWithMod = condition.copy(moderatorUserId = userId)
      val conditionWithUser = condition.copy(userId = userId)

      scheduler.startTimerAtFixedRate(RequestSubscription, RequestSubscription, 100.millis, 100.millis)

      connected(
        Seq(
          RequestSubscriptionParameters(sessionId, "channel.update", "2", condition),
          RequestSubscriptionParameters(sessionId, "channel.follow", "2", conditionWithMod),
          RequestSubscriptionParameters(sessionId, "channel.ad_break.begin", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.chat.clear", "1", conditionWithUser),
          RequestSubscriptionParameters(sessionId, "channel.chat.clear_user_messages", "1", conditionWithUser),
          RequestSubscriptionParameters(sessionId, "channel.chat.message", "1", conditionWithUser),
          RequestSubscriptionParameters(sessionId, "channel.chat.message.delete", "1", conditionWithUser),
          RequestSubscriptionParameters(sessionId, "channel.chat.notification", "1", conditionWithUser),
          RequestSubscriptionParameters(sessionId, "channel.subscribe", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.subscription.end", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.subscription.gift", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.subscription.message", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.cheer", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.raid", "1", Condition(toBroadcasterUserId = userId)),
          RequestSubscriptionParameters(sessionId, "channel.raid", "1", Condition(fromBroadcasterUserId = userId)),
          RequestSubscriptionParameters(sessionId, "channel.ban", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.unban", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.moderator.add", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.moderator.remove", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.channel_points_automatic_reward_redemption.add", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.channel_points_custom_reward.add", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.channel_points_custom_reward.update", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.channel_points_custom_reward.remove", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.channel_points_custom_reward_redemption.add", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.channel_points_custom_reward_redemption.update", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.poll.begin", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.poll.progress", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.poll.end", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.prediction.begin", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.prediction.progress", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.prediction.lock", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.prediction.end", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.vip.add", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.vip.remove", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.goal.begin", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.goal.progress", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.goal.end", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.hype_train.begin", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.hype_train.progress", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.hype_train.end", "1", condition),
          RequestSubscriptionParameters(sessionId, "channel.shoutout.receive", "1", conditionWithMod),
          RequestSubscriptionParameters(sessionId, "stream.online", "1", condition),
          RequestSubscriptionParameters(sessionId, "stream.offline", "1", condition),
          RequestSubscriptionParameters(sessionId, "user.update", "1", Condition(userId = userId))
        )
      )

    case RequestSubscription =>
      requestSubscription(remainingSubscriptions.head)
      if (remainingSubscriptions.tail.nonEmpty) {
        connected(remainingSubscriptions.tail)
      } else {
        scheduler.cancelAll()
        buffer.unstashAll(operational)
      }

    case msg =>
      buffer.stash(msg)
      Behaviors.same
  }

  private def operational(using webSocketClient: ActorRef[WebSocketClient.Command]): Behavior[Command] = Behaviors.receiveAndLogMessage {
    case StreamTextMessage(msg) =>
      decodeAndProcess(msg)
      Behaviors.same

    case StreamBinaryMessage(data) =>
      logIgnoreBinary(data, "operational")
      Behaviors.same

    case StreamDecodedMessage(IncomingMessage(metadata, Payload(_, _, Some(event)))) if metadata.messageType == "notification" =>
      twitchChatbot ! TwitchChatbot.EventSubEvent(event)
      Behaviors.same

    case StreamDecodedMessage(IncomingMessage(metadata, Payload(Some(Session(_, _, _, Some(reconnectUrl), _)), _, _))) if metadata.messageType == "session_reconnect" =>
      reconnecting(webSocketClient, spawnWebSocketClient(Uri(reconnectUrl)))

    // TODO: add revocation handling

    case cmd: StreamDecodedMessage =>
      logIgnore(cmd, "operational")
      Behaviors.same

    case Done =>
      ctx.stop(webSocketClient)
      initial

    case StreamFailure(ex) =>
      ctx.log.error("WebSocket connection failed", ex)
      Behaviors.same // reconnect when the Done message is received

    case Connected =>
      ctx.log.warn("Ignoring unexpected Connected message")
      Behaviors.same

    case RequestSubscription =>
      ctx.log.warn("Ignoring unexpected RequestSubscription message")
      Behaviors.same
  }

  private def reconnecting(webSocketClientOld: ActorRef[WebSocketClient.Command], webSocketClientNew: ActorRef[WebSocketClient.Command]): Behavior[Command] = Behaviors.receiveAndLogMessage {
    case StreamTextMessage(msg) =>
      decodeAndProcess(msg)
      Behaviors.same

    case StreamBinaryMessage(data) =>
      logIgnoreBinary(data, "reconnecting")
      Behaviors.same
    
    case StreamDecodedMessage(IncomingMessage(metadata, Payload(Some(_), _, _))) if metadata.messageType == "session_welcome" =>
      ctx.stop(webSocketClientOld)
      given ActorRef[WebSocketClient.Command] = webSocketClientNew
      buffer.unstashAll(reconnected)

    case msg =>
      buffer.stash(msg)
      Behaviors.same
  }


  private def reconnected(using webSocketClient: ActorRef[WebSocketClient.Command]): Behavior[Command] = Behaviors.withStash(64) { buffer =>
    Behaviors.receiveAndLogMessage {
      case StreamTextMessage(msg) =>
        decodeAndProcess(msg)
        Behaviors.same

      case StreamBinaryMessage(data) =>
        logIgnoreBinary(data, "reconnected")
        Behaviors.same
      
      case Done =>
        ctx.log.debug("Old WebSocket connection closed, returning to operational state")
        buffer.unstashAll(operational)

      case StreamFailure(_) =>
        Behaviors.same

      case msg =>
        buffer.stash(msg)
        Behaviors.same
    }
  }

  private def spawnWebSocketClient(uri: Uri): ActorRef[WebSocketClient.Command] = {
    ctx.spawnAnonymous(
      WebSocketClient(uri, ctx.self, connectedNotification, textMessageTransform, binaryMessageTransform, doneNotification, failNotification)
    )
  }

  private val connectedNotification: () => Command = () => Connected
  private val textMessageTransform: TextMessage.Strict => Command = msg => StreamTextMessage(msg.text)
  private val binaryMessageTransform: BinaryMessage.Strict => Command = msg => StreamBinaryMessage(msg.data)
  private val doneNotification: () => Command = () => Done
  private val failNotification: Throwable => Command = ex => StreamFailure(ex)

  private def decodeAndProcess(text: String): Unit = {
    ctx.log.debug("E>{}", text)
    decode[IncomingMessage](text) match {
      case Left(ex) => ctx.log.error("Failed to decode EventSub message", ex)
      case Right(msg) => ctx.self ! StreamDecodedMessage(msg)
    }
  }

  private def requestSubscription(parameters: RequestSubscriptionParameters): Unit = {
    mediator ! ArchieMateMediator.SendTwitchApiClientCommand(TwitchApiClient.CreateEventSubWebsocketSubscription(ctx.system.ignoreRef, tokenId, parameters.sessionId, parameters.subscriptionType, parameters.subscriptionVersion, parameters.condition))
  }

  private def logIgnoreBinary(data: ByteString, state: String): Unit = ctx.log.info("Ignoring the binary message {} in the {} state", data, state)
  private def logIgnore(msg: Command, state: String): Unit = ctx.log.warn("Ignoring the command {} in the {} state", msg, state)

}
