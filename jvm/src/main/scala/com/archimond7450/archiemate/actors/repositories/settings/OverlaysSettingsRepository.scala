package com.archimond7450.archiemate.actors.repositories.settings

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.chatbot.TwitchChatbotsSupervisor
import com.archimond7450.archiemate.http.ChannelSettings.OverlaysSettings
import com.archimond7450.archiemate.CirceConfiguration.frontendConfiguration
import com.archimond7450.archiemate.SerializerIDs
import com.archimond7450.archiemate.actors.repositories.{
  GenericRepository,
  GenericSerializer
}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import io.circe.{Decoder, Encoder}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.persistence.typed.scaladsl.Effect

object OverlaysSettingsRepository {
  val actorName = "OverlaysSettingsRepository"

  sealed trait Command

  final case class GetSettings(
      replyTo: ActorRef[OverlaysSettings],
      twitchRoomId: String
  ) extends Command

  final case class ChangeSettings(
      replyTo: ActorRef[Acknowledged.type],
      twitchRoomId: String,
      newSettings: OverlaysSettings
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
      newSettings: OverlaysSettings
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
        SerializerIDs.overlaysSettingsRepositoryId
      ) {
    override val toEvent: PartialFunction[AnyRef, Event] = {
      case event: Event => event
    }
  }

  private final case class State(
      twitchRoomIdToSettings: Map[String, OverlaysSettings] = Map.empty
  )

  def apply()(using
      mediator: ActorRef[ArchieMateMediator.Command]
  ): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx

    new GenericRepository[Command, Event, State] {
      val newStateSender: (String, OverlaysSettings) => State => Unit =
        (twitchRoomId, newSettings) =>
          state => {
            val event = TwitchChatbotsSupervisor
              .OverlaysSettingsChanged(newSettings)
            mediator ! ArchieMateMediator
              .SendTwitchChatbotsSupervisorCommand(
                TwitchChatbotsSupervisor
                  .NewChannelSettingsEvent(twitchRoomId, event)
              )
          }

      override protected val actorName: String =
        OverlaysSettingsRepository.actorName

      override protected val emptyState: State = State()

      override protected val commandHandler
          : (State, Command) => Effect[Event, State] = (state, command) =>
        command match {
          case GetSettings(replyTo, twitchRoomId) =>
            Effect.none.thenReply(replyTo)(
              _.twitchRoomIdToSettings.getOrElse(
                twitchRoomId,
                OverlaysSettings()
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
              .thenRun(newStateSender(twitchRoomId, OverlaysSettings()))
              .thenReply(replyTo)(_ => Acknowledged)
        }

      override protected val eventHandler: (State, Event) => State =
        (state, event) =>
          event match {
            case SettingsChanged(twitchRoomId, newSettings) =>
              State(
                state.twitchRoomIdToSettings + (twitchRoomId -> newSettings)
              )

            case SettingsReset(twitchRoomId) =>
              State(state.twitchRoomIdToSettings - twitchRoomId)
          }
    }.eventSourcedBehavior()
  }
}
