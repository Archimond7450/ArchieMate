package com.archimond7450.archiemate.actors.services

import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.providers.RandomProvider
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}

import java.time.OffsetDateTime
import java.util.UUID
import scala.concurrent.duration.DurationInt

object TwitchLoginValidatorService {
  val actorName = "TwitchLoginValidatorService"

  sealed trait Command
  final case class NewRequest(replyTo: ActorRef[NewRequestCreated]) extends Command
  final case class RequestSucceeded(replyTo: ActorRef[RequestSucceededResponse], uuid: UUID) extends Command
  private final case class RequestTimedOut(uuid: UUID) extends Command

  sealed trait Response
  final case class NewRequestCreated(uuid: UUID, when: OffsetDateTime) extends Response
  sealed trait RequestSucceededResponse extends Response
  final case class Acknowledged(cmd: Command) extends RequestSucceededResponse
  final case class InvalidRequest(uuid: UUID) extends RequestSucceededResponse

  def apply()(using randomProvider: RandomProvider, settings: Settings): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx
    Behaviors.withTimers { scheduler =>
      given TimerScheduler[Command] = scheduler
      ready()
    }
  }

  private def ready(requests: Map[UUID, OffsetDateTime] = Map.empty)(using ctx: ActorContext[Command], scheduler: TimerScheduler[Command], settings: Settings, randomProvider: RandomProvider): Behavior[Command] = Behaviors.receiveAndLogMessage {
    case NewRequest(replyTo) =>
      val uuid = randomProvider.uuid()
      val now = OffsetDateTime.now()
      scheduler.startSingleTimer(uuid, RequestTimedOut(uuid), settings.newLoginExpirationTime)
      ctx.log.debug("New login request with id '{}' to expire in {} seconds", uuid, settings.newLoginExpirationTime.toSeconds)
      replyTo ! NewRequestCreated(uuid, now.plusNanos(settings.newLoginExpirationTime.toNanos))
      ready(requests + (uuid -> now))

    case msg @ RequestSucceeded(replyTo, uuid) =>
      requests.get(uuid) match {
        case Some(_) =>
          ctx.log.debug("Login request with id '{}' succeeded.", uuid)
          scheduler.cancel(uuid)
          replyTo ! Acknowledged(msg)

        case None =>
          ctx.log.warn("Login request with id '{}' is no longer valid and therefore cannot succeed.", uuid)
          replyTo ! InvalidRequest(uuid)
      }

      ready(requests - uuid)

    case RequestTimedOut(uuid) =>
      ctx.log.debug("Login request with id '{}' timed out.", uuid)
      ready(requests - uuid)
  }
}
