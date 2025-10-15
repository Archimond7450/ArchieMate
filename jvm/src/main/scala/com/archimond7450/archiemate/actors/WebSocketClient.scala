package com.archimond7450.archiemate.actors

import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.extensions.Settings
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.ws.{
  BinaryMessage,
  Message,
  PeerClosedConnectionException,
  TextMessage,
  WebSocketRequest,
  WebSocketUpgradeResponse
}
import org.apache.pekko.http.scaladsl.model.{StatusCodes, Uri}
import org.apache.pekko.stream.OverflowStrategy
import org.apache.pekko.stream.scaladsl.{
  Flow,
  Keep,
  Sink,
  Source,
  SourceQueueWithComplete
}
import org.apache.pekko.util.ByteString

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object WebSocketClient {
  val actorName = "WebSocketClient"

  sealed trait Command

  final case class SendText(text: String) extends Command
  final case class SendBinary(data: ByteString) extends Command
  private case object InternalUpgradeSucceeded extends Command
  private final case class InternalConnected(
      queue: SourceQueueWithComplete[Message]
  ) extends Command
  private final case class InternalFailed(ex: Throwable) extends Command
  private case object InternalDone extends Command
  private final case class StreamToTextFailed(ex: Throwable) extends Command
  private final case class StreamToBinaryFailed(ex: Throwable) extends Command
  private final case class IncomingText(text: TextMessage.Strict)
      extends Command
  private final case class IncomingBinary(bin: BinaryMessage.Strict)
      extends Command

  def apply[T](
      uri: Uri,
      parent: ActorRef[T],
      connectedNotification: () => T,
      textMessageTransform: TextMessage.Strict => T,
      binaryMessageTransform: BinaryMessage.Strict => T,
      doneNotification: () => T,
      failNotification: Throwable => T
  ): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx
    given ActorSystem[Nothing] = ctx.system
    given ExecutionContext = ctx.executionContext

    val settings = Settings(ctx.system)

    val self = ctx.self

    val outgoing = Source
      .queue[Message](bufferSize = 64, OverflowStrategy.dropHead)
      .mapMaterializedValue { queue =>
        self ! InternalConnected(queue)
        queue
      }

    val incoming: Sink[Message, Future[Done]] = Sink.foreach {
      case tm: TextMessage.Strict   => self ! IncomingText(tm)
      case bm: BinaryMessage.Strict => self ! IncomingBinary(bm)
      case tm: TextMessage.Streamed =>
        ctx.pipeToSelf(tm.toStrict(settings.askTimeout.duration)) {
          case Success(text) => IncomingText(text)
          case Failure(ex)   => StreamToTextFailed(ex)
        }
      case bm: BinaryMessage.Streamed =>
        bm.toStrict(settings.askTimeout.duration).onComplete {
          case Success(bin) => self ! IncomingBinary(bin)
          case Failure(ex)  => StreamToBinaryFailed(ex)
        }
    }

    val flow: Flow[Message, Message, Future[Done]] =
      Flow.fromSinkAndSourceMat(incoming, outgoing)(Keep.left)

    val (
      upgradeResponse: Future[WebSocketUpgradeResponse],
      closed: Future[Done]
    ) = Http().singleWebSocketRequest(WebSocketRequest(uri), flow)

    ctx.pipeToSelf(upgradeResponse) {
      case Success(resp: WebSocketUpgradeResponse)
          if resp.response.status == StatusCodes.SwitchingProtocols =>
        InternalUpgradeSucceeded
      case Success(resp) =>
        InternalFailed(new RuntimeException(s"WebSocket upgrade failed: $resp"))
      case Failure(ex) =>
        InternalFailed(ex)
    }

    ctx.pipeToSelf(closed) {
      case Success(_)  => InternalDone
      case Failure(ex) => InternalFailed(ex)
    }

    waitingForQueue(
      parent,
      connectedNotification,
      textMessageTransform,
      binaryMessageTransform,
      doneNotification,
      failNotification
    )
  }

  private def waitingForQueue[T](
      parent: ActorRef[T],
      connectedNotification: () => T,
      textMessageTransform: TextMessage.Strict => T,
      binaryMessageTransform: BinaryMessage.Strict => T,
      doneNotification: () => T,
      failNotification: Throwable => T
  )(using ctx: ActorContext[Command]): Behavior[Command] =
    Behaviors.withStash(64) { buffer =>
      given ExecutionContext = ctx.executionContext

      Behaviors.receiveAndLogMessage {
        case InternalUpgradeSucceeded =>
          ctx.log.debug("WebSocket upgrade succeeded")
          parent ! connectedNotification()
          Behaviors.same

        case InternalConnected(queue) =>
          ctx.pipeToSelf(queue.watchCompletion()) {
            case Success(_) =>
              InternalDone
            case Failure(ex: PeerClosedConnectionException) =>
              InternalFailed(ex)
            case Failure(ex) =>
              InternalFailed(ex)
          }
          buffer.unstashAll(
            active(
              parent,
              textMessageTransform,
              binaryMessageTransform,
              doneNotification,
              failNotification,
              queue
            )
          )

        case InternalFailed(ex) =>
          ctx.log.error("Could not connect to the websocket", ex)
          parent ! failNotification(ex)
          parent ! doneNotification()
          Behaviors.stopped

        case InternalDone =>
          parent ! doneNotification()
          Behaviors.stopped

        case cmd: Command =>
          buffer.stash(cmd)
          Behaviors.same
      }
    }

  private def active[T](
      parent: ActorRef[T],
      textMessageTransform: TextMessage.Strict => T,
      binaryMessageTransform: BinaryMessage.Strict => T,
      doneNotification: () => T,
      failNotification: Throwable => T,
      queue: SourceQueueWithComplete[Message]
  )(using ctx: ActorContext[Command]): Behavior[Command] =
    Behaviors.receiveAndLogMessage {
      case SendText(text) =>
        queue.offer(TextMessage(text))
        Behaviors.same

      case SendBinary(bytes) =>
        queue.offer(BinaryMessage(ByteString(bytes)))
        Behaviors.same

      case IncomingText(msg) =>
        msg.text.split("\r?\n").foreach { oneMsg =>
          parent ! textMessageTransform(TextMessage.Strict(oneMsg))
        }
        Behaviors.same

      case IncomingBinary(msg) =>
        parent ! binaryMessageTransform(msg)
        Behaviors.same

      case InternalUpgradeSucceeded =>
        ctx.log.debug("WebSocket upgrade succeeded")
        parent ! doneNotification()
        Behaviors.same

      case StreamToTextFailed(ex) =>
        ctx.log.error("Failed to convert streamed text message to strict!", ex)
        Behaviors.same

      case StreamToBinaryFailed(ex) =>
        ctx.log.error(
          "Failed to convert streamed binary message to strict!",
          ex
        )
        Behaviors.same

      case InternalDone =>
        parent ! doneNotification()
        Behaviors.stopped

      case InternalFailed(ex: RuntimeException)
          if ex.getMessage.contains("WebSocket upgrade failed") =>
        ctx.log.error("Websocket upgrade failed")
        parent ! failNotification(ex)
        parent ! doneNotification()
        Behaviors.stopped

      case InternalFailed(ex: PeerClosedConnectionException) =>
        ctx.log.error(
          "Peer closed the WebSocket connection with code {} and reason {}",
          ex.closeCode,
          ex.closeReason
        )
        parent ! failNotification(ex)
        parent ! doneNotification()
        Behaviors.stopped

      case InternalFailed(ex) =>
        ctx.log.error("WebSocket failed", ex)
        parent ! failNotification(ex)
        parent ! doneNotification()
        Behaviors.stopped

      case other: Command =>
        // Shouldn't happen
        ctx.log.warn("Ignoring the command {}", other)
        Behaviors.same
    }
}
