package com.archimond7450.archiemate.actors.repositories.settings

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.chatbot.TwitchChatbotsSupervisor
import com.archimond7450.archiemate.actors.repositories.GenericRepository
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.persistence.typed.scaladsl.Effect

object GenericSettingsRepository {
  sealed trait Command[Settings]

  case object Acknowledged

  final case class GetSettings[Settings](replyTo: ActorRef[Settings], twitchRoomId: String) extends Command[Settings]
  final case class ChangeSettings[Settings](replyTo: ActorRef[Acknowledged.type], twitchRoomId: String, newSettings: Settings) extends Command[Settings]
  final case class ResetSettings(replyTo: ActorRef[Acknowledged.type], twitchRoomId: String) extends Command[Nothing]

  sealed trait Event[+Settings]
  private final case class SettingsChanged[Settings](twitchRoomId: String, newSettings: Settings) extends Event[Settings]
  private final case class SettingsReset(twitchRoomId: String) extends Event[Nothing]

  case class State[Settings](twitchRoomIdToSettings: Map[String, Settings] = Map.empty)
}

abstract class GenericSettingsRepository[Settings](using ctx: ActorContext[GenericSettingsRepository.Command[Settings]], mediator: ActorRef[ArchieMateMediator.Command]) extends GenericRepository[GenericSettingsRepository.Command[Settings], GenericSettingsRepository.Event[Settings], GenericSettingsRepository.State[Settings]] {
  import GenericSettingsRepository.*

  protected val defaultSettings: Settings

  protected val twitchChatbotsSupervisorNotificationBuilder: Settings => TwitchChatbotsSupervisor.SettingsEvent

  protected override final val emptyState: State[Settings] = State()

  protected override final val commandHandler: (State[Settings], Command[Settings]) => Effect[Event[Settings], State[Settings]] = (state, command) => command match {
    case GetSettings(replyTo, twitchRoomId) =>
      Effect.none.thenReply(replyTo)(_.twitchRoomIdToSettings.getOrElse(twitchRoomId, defaultSettings))

    case ChangeSettings(replyTo, twitchRoomId, newSettings) =>
      Effect.persist(SettingsChanged(twitchRoomId, newSettings))
        .thenRun { (newState: State[Settings]) => // TODO: check why the hell the explicit typing is required here...
          val event = twitchChatbotsSupervisorNotificationBuilder(newSettings)
          mediator ! ArchieMateMediator.SendTwitchChatbotsSupervisorCommand(TwitchChatbotsSupervisor.NewChannelSettingsEvent(twitchRoomId, event))
        }
        .thenReply(replyTo)(_ => Acknowledged)

    case ResetSettings(replyTo, twitchRoomId) =>
      Effect.persist(SettingsReset(twitchRoomId)).thenReply(replyTo)(_ => Acknowledged)
  }

  protected override final val eventHandler: (State[Settings], Event[Settings]) => State[Settings] = (state, event) => event match {
    case SettingsChanged(twitchRoomId, newSettings) =>
      State(state.twitchRoomIdToSettings + (twitchRoomId -> newSettings))

    case SettingsReset(twitchRoomId) =>
      State(state.twitchRoomIdToSettings - twitchRoomId)
  }
}
