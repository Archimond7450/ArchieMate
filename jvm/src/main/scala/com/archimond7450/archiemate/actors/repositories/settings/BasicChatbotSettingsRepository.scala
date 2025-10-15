package com.archimond7450.archiemate.actors.repositories.settings

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.chatbot.TwitchChatbotsSupervisor
import com.archimond7450.archiemate.actors.repositories.GenericSerializer
import com.archimond7450.archiemate.http.ChannelSettings.BasicChatbotSettings
import com.archimond7450.archiemate.CirceConfiguration.frontendConfiguration
import com.archimond7450.archiemate.SerializerIDs
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import io.circe.{Decoder, Encoder}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.persistence.typed.PersistenceId
import org.apache.pekko.persistence.typed.scaladsl.{
  Effect,
  EventSourcedBehavior,
  ReplyEffect
}

object BasicChatbotSettingsRepository {
  val actorName = "BasicChatbotSettingsRepository"

  sealed trait Command

  final case class GetAllSettings(
      replyTo: ActorRef[Map[String, BasicChatbotSettings]]
  ) extends Command

  final case class GetSettings(
      replyTo: ActorRef[BasicChatbotSettings],
      twitchRoomId: String
  ) extends Command

  final case class ChangeSettings(
      replyTo: ActorRef[Acknowledged.type],
      twitchRoomId: String,
      newSettings: BasicChatbotSettings
  ) extends Command

  final case class ResetSettings(
      replyTo: ActorRef[Acknowledged.type],
      twitchRoomId: String
  ) extends Command

  case object Acknowledged

  private sealed trait Event
  private object Event {
    given Decoder[Event] = ConfiguredDecoder.derived
    given Encoder[Event] = ConfiguredEncoder.derived
  }

  private final case class SettingsChanged(
      twitchRoomId: String,
      newSettings: BasicChatbotSettings
  ) extends Event

  private object SettingsChanged {
    given Decoder[SettingsChanged] = ConfiguredDecoder.derived
    given Encoder[SettingsChanged] = ConfiguredEncoder.derived
  }

  private final case class SettingsReset(twitchRoomId: String) extends Event

  private object SettingsReset {
    given Decoder[SettingsReset] = ConfiguredDecoder.derived
    given Encoder[SettingsReset] = ConfiguredEncoder.derived
  }

  private class EventSerializer
      extends GenericSerializer[Event](
        actorName,
        SerializerIDs.basicChatbotSettingsRepositoryId
      ) {
    override val toEvent: PartialFunction[AnyRef, Event] = {
      case event: Event => event
    }
  }

  private final case class State(
      twitchRoomIdToSettings: Map[String, BasicChatbotSettings] = Map.empty
  )

  def apply()(using
      mediator: ActorRef[ArchieMateMediator.Command]
  ): Behavior[Command] =
    EventSourcedBehavior.withEnforcedReplies[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId(actorName),
      emptyState = State(),
      commandHandler = commandHandler,
      eventHandler = eventHandler
    )

  private def newStateSender(using
      mediator: ActorRef[ArchieMateMediator.Command]
  ): (String, BasicChatbotSettings) => State => Unit =
    (twitchRoomId, newSettings) =>
      state => {
        val event = TwitchChatbotsSupervisor
          .BasicChatbotSettingsChanged(newSettings)
        mediator ! ArchieMateMediator
          .SendTwitchChatbotsSupervisorCommand(
            TwitchChatbotsSupervisor
              .NewChannelSettingsEvent(twitchRoomId, event)
          )
      }

  private def commandHandler(using
      mediator: ActorRef[ArchieMateMediator.Command]
  ): (State, Command) => ReplyEffect[Event, State] = (state, command) =>
    command match {
      case GetAllSettings(replyTo) =>
        Effect.none.thenReply(replyTo)(_.twitchRoomIdToSettings)

      case GetSettings(replyTo, twitchRoomId) =>
        Effect.none.thenReply(replyTo)(
          _.twitchRoomIdToSettings.getOrElse(
            twitchRoomId,
            BasicChatbotSettings()
          )
        )

      case ChangeSettings(replyTo, twitchRoomId, newSettings) =>
        Effect
          .persist(SettingsChanged(twitchRoomId, newSettings))
          .thenRun(newStateSender(twitchRoomId, newSettings))
          .thenReply(replyTo)(_ => Acknowledged)

      case ResetSettings(replyTo, twitchRoomId) =>
        Effect
          .persist(SettingsReset(twitchRoomId))
          .thenRun(newStateSender(twitchRoomId, BasicChatbotSettings()))
          .thenReply(replyTo)(_ => Acknowledged)
    }

  private val eventHandler: (State, Event) => State =
    (state, event) =>
      event match {
        case SettingsChanged(twitchRoomId, newSettings) =>
          State(
            state.twitchRoomIdToSettings + (twitchRoomId -> newSettings)
          )

        case SettingsReset(twitchRoomId) =>
          State(state.twitchRoomIdToSettings - twitchRoomId)
      }
}
