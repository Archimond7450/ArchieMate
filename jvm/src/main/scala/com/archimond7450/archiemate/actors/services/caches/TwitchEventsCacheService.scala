package com.archimond7450.archiemate.actors.services.caches

import com.archimond7450.archiemate.http.OverlayMessages
import com.archimond7450.archiemate.twitch.eventsub
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.stream.scaladsl.SourceQueueWithComplete

import scala.concurrent.duration.DurationInt

object TwitchEventsCacheService {
  val actorName = "TwitchEventsCacheService"

  sealed trait Command
  final case class CacheEvent(
      twitchRoomId: String,
      eventId: String,
      event: eventsub.Event
  ) extends Command
  final case class GetEventsFromId(
      replyTo: ActorRef[List[eventsub.Event]],
      twitchRoomId: String,
      eventId: String
  ) extends Command
  final case class OverlayConnected(
      twitchRoomId: String,
      queue: SourceQueueWithComplete[OverlayMessages.OverlayMessage]
  ) extends Command
  final case class OverlayDisconnected(twitchRoomId: String) extends Command
  private case object Cleanup extends Command

  def apply(): Behavior[Command] = Behaviors.setup { ctx =>
    Behaviors.withTimers { scheduler =>
      scheduler.startTimerAtFixedRate(Cleanup, 60.seconds)
      active()
    }
  }

  private def active(
      twitchRoomIdToEvents: Map[String, List[(String, eventsub.Event)]] =
        Map.empty,
      connectedOverlays: Map[String, SourceQueueWithComplete[
        OverlayMessages.OverlayMessage
      ]] = Map.empty
  ): Behavior[Command] = Behaviors.receiveMessage {
    case CacheEvent(twitchRoomId, eventId, event) =>
      connectedOverlays.get(twitchRoomId).foreach { queue =>
        queue.offer(OverlayMessages.OverlayMessage(eventId, OverlayMessages.OverlayMessageType.TwitchEvent, twitchEvent = Some(event)))
      }
      val newEvents =
        twitchRoomIdToEvents.getOrElse(twitchRoomId, Nil) :+ (eventId, event)
      active(
        twitchRoomIdToEvents + (twitchRoomId -> newEvents),
        connectedOverlays
      )

    case GetEventsFromId(replyTo, twitchRoomId, eventId) =>
      val events = twitchRoomIdToEvents.getOrElse(twitchRoomId, Nil)
      val eventsFromId = events.dropWhile((id, _) => id != eventId)
      replyTo ! eventsFromId.map(_._2)
      Behaviors.same

    case OverlayConnected(twitchRoomId, queue) =>
      active(twitchRoomIdToEvents, connectedOverlays + (twitchRoomId -> queue))

    case OverlayDisconnected(twitchRoomId) =>
      active(twitchRoomIdToEvents, connectedOverlays - twitchRoomId)

    case Cleanup =>
      active(
        twitchRoomIdToEvents.map((twitchRoomId, events) => {
          val maxEventCapacity = 64
          val newEvents =
            if (events.length > maxEventCapacity) {
              events.drop(events.length - maxEventCapacity)
            } else {
              events
            }
          (twitchRoomId, newEvents)
        }),
        connectedOverlays
      )
  }
}
