package com.archimond7450.archiemate.actors.repositories

import com.archimond7450.archiemate.providers.RandomProvider
import com.archimond7450.archiemate.CirceConfiguration.frontendConfiguration
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.persistence.typed.PersistenceId
import org.apache.pekko.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}

import scala.util.Try

object OverlaySecretsRepository {
  val actorName = "OverlaySecretsRepository"

  private case class State(
      twitchUserIdToSecret: Map[String, String] = Map.empty
                          )

  sealed trait Command
  final case class GetSecret(replyTo: ActorRef[String], twitchUserId: String) extends Command
  final case class ResetSecret(replyTo: ActorRef[String], twitchUserId: String) extends Command

  private sealed trait Event
  private object Event {
    given Decoder[Event] = ConfiguredDecoder.derived
    given Encoder[Event] = ConfiguredEncoder.derived
  }

  private final case class SecretChanged(twitchUserId: String, secret: String) extends Event
  private object SecretChanged {
    given Decoder[SecretChanged] = ConfiguredDecoder.derived
    given Encoder[SecretChanged] = ConfiguredEncoder.derived
  }

  def apply()(using RandomProvider): Behavior[Command] = Behaviors.setup { ctx =>
    EventSourcedBehavior.withEnforcedReplies[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId(actorName),
      emptyState = State(),
      commandHandler = commandHandler,
      eventHandler = eventHandler
    )
  }

  private def commandHandler(using random: RandomProvider): (State, Command) => ReplyEffect[Event, State] = {
    (state, command) => command match {
      case GetSecret(replyTo, twitchUserId) if !state.twitchUserIdToSecret.contains(twitchUserId) =>
        Effect.persist(SecretChanged(twitchUserId, random.uuid().toString)).thenReply(replyTo)(_.twitchUserIdToSecret(twitchUserId))

      case GetSecret(replyTo, twitchUserId) =>
        Effect.none.thenReply(replyTo)(_.twitchUserIdToSecret(twitchUserId))

      case ResetSecret(replyTo, twitchUserId) =>
        Effect.persist(SecretChanged(twitchUserId, random.uuid().toString)).thenReply(replyTo)(_.twitchUserIdToSecret(twitchUserId))
    }
  }

  private val eventHandler: (State, Event) => State = { (state, event) =>
    event match {
      case SecretChanged(twitchUserId, secret) =>
        state.copy(twitchUserIdToSecret = state.twitchUserIdToSecret + (twitchUserId -> secret))
    }
  }
}
