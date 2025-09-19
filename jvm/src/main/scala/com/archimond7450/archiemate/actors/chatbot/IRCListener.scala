package com.archimond7450.archiemate.actors.chatbot

import com.archimond7450.archiemate.actors.WebSocketClient
import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.twitch.irc.IncomingMessages.{EarlyNotice, NumericMessage, Ping}
import com.archimond7450.archiemate.twitch.irc.{IncomingMessage, IncomingMessageDecoder, OutgoingMessage, OutgoingMessageEncoder, OutgoingMessages}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer}
import org.apache.pekko.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import org.apache.pekko.util.ByteString

object IRCListener {
  val actorName = "IRCListener"

  sealed trait Command
  private case object Connected extends Command
  private final case class StreamTextMessage(msg: String) extends Command
  private final case class StreamBinaryMessage(msg: ByteString) extends Command
  private final case class StreamDecodedMessage(msg: IncomingMessage) extends Command
  private final case class StreamFailure(ex: Throwable) extends Command
  private case object Done extends Command
  final case class SendMessage(msg: String) extends Command
  final case class SendReplyMessage(msg: String, replyToMsgId: String) extends Command

  def apply(channelName: String)(using settings: Settings, decoder: IncomingMessageDecoder, encoder: OutgoingMessageEncoder): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx
    new IRCListener(channelName).initial
  }
}

private class IRCListener(channelName: String)(using ctx: ActorContext[IRCListener.Command], settings: Settings, decoder: IncomingMessageDecoder, encoder: OutgoingMessageEncoder) {
  import IRCListener.*

  private def initial: Behavior[Command] = {
    val webSocketClient = spawnWebSocketClient
    disconnected(webSocketClient)
  }

  private def disconnected(webSocketClient: ActorRef[WebSocketClient.Command]): Behavior[Command] = Behaviors.withStash(64) { buffer =>
    given StashBuffer[Command] = buffer

    Behaviors.receiveAndLogMessage {
      case Connected =>
        sendMessageWithDebug(webSocketClient, OutgoingMessages.Pass(settings.twitchIRCToken))
        sendMessageWithDebug(webSocketClient, OutgoingMessages.Nick(settings.twitchIRCUsername))
        sendMessageWithDebug(
          webSocketClient,
          OutgoingMessages.CapabilityRequest(
            List("twitch.tv/membership", "twitch.tv/tags", "twitch.tv/commands")
          )
        )
        sendMessageWithDebug(webSocketClient, OutgoingMessages.Join(List(channelName)))
        buffer.unstashAll(authenticating(webSocketClient))

      case msg: SendMessage =>
        buffer.stash(msg)
        Behaviors.same

      case msg: SendReplyMessage =>
        buffer.stash(msg)
        Behaviors.same

      case msg =>
        ctx.log.warn("Ignoring message {} while connecting", msg)
        Behaviors.same
    }
  }

  private def authenticating(webSocketClient: ActorRef[WebSocketClient.Command], remainingMessages: Seq[IncomingMessage] = authMessages)(using buffer: StashBuffer[Command]): Behavior[Command] = Behaviors.receiveAndLogMessage {
    case StreamTextMessage(msg) =>
      decodeAndProcess(msg)
      Behaviors.same

    case StreamDecodedMessage(msg: NumericMessage) if msg == remainingMessages.head =>
      if (remainingMessages.tail.nonEmpty) {
        authenticating(webSocketClient, remainingMessages.tail)
      } else {
        buffer.unstashAll(connected(webSocketClient))
      }

    case StreamDecodedMessage(notice: EarlyNotice) =>
      ctx.log.warn("Received early notice: {}", notice)
      Behaviors.same

    case StreamFailure(ex) =>
      ctx.log.error("Connection failed when authenticating. Are the environment variables for chatbot username and twitch token configured properly?", ex)
      Behaviors.same

    case Done =>
      ctx.log.error("Stopping {}", ctx.self)
      Behaviors.stopped

    case msg: SendMessage =>
      buffer.stash(msg)
      Behaviors.same

    case msg: SendReplyMessage =>
      buffer.stash(msg)
      Behaviors.same

    case msg =>
      ctx.log.warn("Ignoring message {} while authenticating", msg)
      Behaviors.same
  }

  private def connected(webSocketClient: ActorRef[WebSocketClient.Command]): Behavior[Command] = Behaviors.receiveAndLogMessage {
    case StreamTextMessage(msg) =>
      decodeAndProcess(msg)
      Behaviors.same

    case StreamDecodedMessage(Ping) =>
      webSocketClient ! WebSocketClient.SendText(encoder.encode(OutgoingMessages.Pong))
      Behaviors.same

    case StreamFailure(ex) =>
      ctx.log.error("Connection failed!", ex)
      Behaviors.same

    case Done =>
      ctx.log.warn("Restarting connection")
      disconnected(spawnWebSocketClient)

    case SendMessage(msg) =>
      webSocketClient ! WebSocketClient.SendText(encoder.encode(OutgoingMessages.PrivMsg(channelName, msg)))
      Behaviors.same

    case SendReplyMessage(msg, replyToMsgId) =>
      webSocketClient ! WebSocketClient.SendText(encoder.encode(OutgoingMessages.PrivMsg(channelName, msg, Some(replyToMsgId))))
      Behaviors.same

    case StreamDecodedMessage(msg) =>
      ctx.log.debug("Ignoring message {}", msg)
      Behaviors.same

    case msg =>
      ctx.log.warn("Ignoring unexpected message {}", msg) // e.g. no binary messages expected
      Behaviors.same
  }

  private def spawnWebSocketClient: ActorRef[WebSocketClient.Command] = {
    ctx.spawnAnonymous(
      WebSocketClient(settings.twitchIRCUri, ctx.self, connectedNotification, textMessageTransform, binaryMessageTransform, doneNotification, failNotification)
    )
  }

  private val connectedNotification: () => Command = () => Connected
  private val textMessageTransform: TextMessage.Strict => Command = msg => StreamTextMessage(msg.text)
  private val binaryMessageTransform: BinaryMessage.Strict => Command = msg => StreamBinaryMessage(msg.data)
  private val doneNotification: () => Command = () => Done
  private val failNotification: Throwable => Command = ex => StreamFailure(ex)

  private def decodeAndProcess(msg: String): Unit = {
    msg.split("\r?\n").foreach { oneMsg =>
      ctx.log.debug("I>{}", oneMsg)
      ctx.self ! StreamDecodedMessage(decoder.decode(oneMsg))
    }
  }

  private def sendMessageWithDebug(webSocketClient: ActorRef[WebSocketClient.Command], message: OutgoingMessage): Unit = {
    val msg = encoder.encode(message)
    message match {
      case OutgoingMessages.Pass(token) =>
        ctx.log.debug("I< PASS oauth:{}", "*".repeat(token.length))
      case _ =>
        ctx.log.debug("I< {}", msg)
    }
    webSocketClient ! WebSocketClient.SendText(msg)
  }

  private val controlChars = " \uDB40\uDC00"

  private def normalizeMessage(message: String): String = {
    message.trim.stripSuffix(controlChars)
  }

  private val authMessages = Seq(
    NumericMessage(1, settings.twitchIRCUsername, "Welcome, GLHF!"),
    NumericMessage(2, settings.twitchIRCUsername, "Your host is tmi.twitch.tv"),
    NumericMessage(3, settings.twitchIRCUsername, "This server is rather new"),
    NumericMessage(4, settings.twitchIRCUsername, "-"),
    NumericMessage(375, settings.twitchIRCUsername, "-"),
    NumericMessage(372, settings.twitchIRCUsername, "You are in a maze of twisty passages, all alike."),
    NumericMessage(376, settings.twitchIRCUsername, ">")
  )
}
