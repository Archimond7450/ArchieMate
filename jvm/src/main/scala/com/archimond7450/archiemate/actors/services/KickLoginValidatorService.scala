package com.archimond7450.archiemate.actors.services

import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.helpers.PkceHelpers
import com.archimond7450.archiemate.providers.RandomProvider
import org.apache.pekko.actor.typed.scaladsl.{
  ActorContext,
  Behaviors,
  TimerScheduler
}
import org.apache.pekko.actor.typed.{ActorRef, Behavior, SupervisorStrategy}

import java.time.OffsetDateTime
import java.util.UUID

object KickLoginValidatorService {
  val actorName = "KickLoginValidatorService"

  sealed trait Command
  final case class NewRequest(replyTo: ActorRef[NewRequestCreated], twitchUserId: String)
      extends Command
  final case class RequestSucceeded(
      replyTo: ActorRef[RequestSucceededResponse],
      uuid: UUID
  ) extends Command
  private final case class RequestTimedOut(uuid: UUID) extends Command

  sealed trait Response
  final case class NewRequestCreated(
      uuid: UUID,
      codeChallenge: String,
      when: OffsetDateTime
  ) extends Response
  sealed trait RequestSucceededResponse extends Response
  final case class Acknowledged(cmd: Command, twitchUserId: String, codeVerifier: String)
      extends RequestSucceededResponse
  final case class InvalidRequest(uuid: UUID) extends RequestSucceededResponse

  private final case class RequestData(
      created: OffsetDateTime,
      twitchUserId: String,
      codeChallenge: String,
      codeVerifier: String
  )

  def apply()(using
      randomProvider: RandomProvider,
      pkceHelpers: PkceHelpers,
      settings: Settings
  ): Behavior[Command] = Behaviors
    .supervise[Command] {
      Behaviors.setup { ctx =>
        given ActorContext[Command] = ctx

        Behaviors.withTimers { scheduler =>
          given TimerScheduler[Command] = scheduler

          ready()
        }
      }
    }
    .onFailure[Throwable](SupervisorStrategy.resume)

  private def ready(requests: Map[UUID, RequestData] = Map.empty)(using
      ctx: ActorContext[Command],
      scheduler: TimerScheduler[Command],
      settings: Settings,
      randomProvider: RandomProvider,
      pkceHelpers: PkceHelpers
  ): Behavior[Command] = Behaviors.receiveAndLogMessage {
    case NewRequest(replyTo, twitchUserId) =>
      val uuid = randomProvider.uuid()
      val codeVerifier = pkceHelpers.generateCodeVerifier()
      val codeChallenge = pkceHelpers.codeChallengeS256(codeVerifier)
      val now = OffsetDateTime.now()
      scheduler.startSingleTimer(
        uuid,
        RequestTimedOut(uuid),
        settings.newLoginExpirationTime
      )
      ctx.log.debug(
        "New login request with id '{}' to expire in {} seconds",
        uuid,
        settings.newLoginExpirationTime.toSeconds
      )
      replyTo ! NewRequestCreated(
        uuid,
        codeChallenge,
        now.plusNanos(settings.newLoginExpirationTime.toNanos)
      )
      ready(requests + (uuid -> RequestData(now, twitchUserId, codeChallenge, codeVerifier)))

    case msg @ RequestSucceeded(replyTo, uuid) =>
      requests.get(uuid) match {
        case Some(requestData) =>
          ctx.log.debug("Login request with id '{}' succeeded.", uuid)
          scheduler.cancel(uuid)
          replyTo ! Acknowledged(msg, requestData.twitchUserId, requestData.codeVerifier)

        case None =>
          ctx.log.warn(
            "Login request with id '{}' is no longer valid and therefore cannot succeed.",
            uuid
          )
          replyTo ! InvalidRequest(uuid)
      }

      ready(requests - uuid)

    case RequestTimedOut(uuid) =>
      ctx.log.debug("Login request with id '{}' timed out.", uuid)
      ready(requests - uuid)
  }
}
