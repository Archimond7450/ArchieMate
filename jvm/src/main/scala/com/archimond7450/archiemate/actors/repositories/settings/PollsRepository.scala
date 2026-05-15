package com.archimond7450.archiemate.actors.repositories.settings

import com.archimond7450.archiemate.CirceConfiguration.frontendConfiguration
import com.archimond7450.archiemate.SerializerIDs
import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.chatbot.TwitchChatbotsSupervisor
import com.archimond7450.archiemate.actors.repositories.GenericSerializer
import com.archimond7450.archiemate.http.Polls.{ChannelPolls, Poll}
import com.archimond7450.archiemate.providers.RandomProvider
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import org.apache.pekko.persistence.typed.PersistenceId
import org.apache.pekko.persistence.typed.scaladsl.{
  Effect,
  EventSourcedBehavior,
  ReplyEffect
}

object PollsRepository {
  val actorName = "PollsRepository"

  sealed trait Command

  final case class GetPolls(
      replyTo: ActorRef[ChannelPolls],
      twitchRoomId: String
  ) extends Command

  final case class AddPoll(
      replyTo: ActorRef[Acknowledged.type],
      twitchRoomId: String,
      poll: Poll
  ) extends Command

  final case class EditPoll(
      replyTo: ActorRef[Acknowledged.type],
      twitchRoomId: String,
      pollId: String,
      poll: Poll
  ) extends Command

  final case class DeletePoll(
      replyTo: ActorRef[Acknowledged.type],
      twitchRoomId: String,
      pollId: String
  ) extends Command

  case object Acknowledged

  private sealed trait Event
  private object Event {
    given Decoder[Event] = ConfiguredDecoder.derived
    given Encoder[Event] = ConfiguredEncoder.derived
  }

  private final case class PollAdded(
      twitchRoomId: String,
      poll: Poll
  ) extends Event

  private object PollAdded {
    given Decoder[PollAdded] = ConfiguredDecoder.derived
    given Encoder[PollAdded] = ConfiguredEncoder.derived
  }

  private final case class PollEdited(
      twitchRoomId: String,
      pollId: String,
      poll: Poll
  ) extends Event

  private object PollEdited {
    given Decoder[PollEdited] = ConfiguredDecoder.derived
    given Encoder[PollEdited] = ConfiguredEncoder.derived
  }

  private final case class PollDeleted(
      twitchRoomId: String,
      pollId: String
  ) extends Event

  private object PollDeleted {
    given Decoder[PollDeleted] = ConfiguredDecoder.derived
    given Encoder[PollDeleted] = ConfiguredEncoder.derived
  }

  private class EventSerializer
      extends GenericSerializer[Event](
        actorName,
        SerializerIDs.pollsRepositoryId
      ) {
    override val toEvent: PartialFunction[AnyRef, Event] = {
      case event: Event => event
    }
  }

  private final case class State(
      twitchRoomIdToPolls: Map[String, ChannelPolls] = Map.empty
  )

  def apply()(using
      mediator: ActorRef[ArchieMateMediator.Command],
      randomProvider: RandomProvider
  ): Behavior[Command] =
    Behaviors
      .supervise[Command] {
        EventSourcedBehavior.withEnforcedReplies[Command, Event, State](
          persistenceId = PersistenceId.ofUniqueId(actorName),
          emptyState = State(),
          commandHandler = commandHandler,
          eventHandler = eventHandler
        )
      }
      .onFailure[Throwable](SupervisorStrategy.restart)

  private def newStateSender(using
      mediator: ActorRef[ArchieMateMediator.Command]
  ): String => State => Unit = twitchRoomId =>
    state => {
      val event = TwitchChatbotsSupervisor.PollsChanged(
        state.twitchRoomIdToPolls(twitchRoomId)
      )
      mediator ! ArchieMateMediator.SendTwitchChatbotsSupervisorCommand(
        TwitchChatbotsSupervisor.NewChannelSettingsEvent(twitchRoomId, event)
      )
    }

  private def commandHandler(using
      mediator: ActorRef[ArchieMateMediator.Command]
  ): (State, Command) => ReplyEffect[Event, State] = (state, command) =>
    command match {
      case GetPolls(replyTo, twitchRoomId) =>
        Effect.none.thenReply(replyTo)(
          _.twitchRoomIdToPolls.getOrElse(twitchRoomId, ChannelPolls())
        )

      case AddPoll(replyTo, twitchRoomId, poll) =>
        Effect
          .persist(PollAdded(twitchRoomId, poll))
          .thenRun(newStateSender(twitchRoomId))
          .thenReply(replyTo)(_ => Acknowledged)

      case EditPoll(replyTo, twitchRoomId, pollId, poll) =>
        Effect
          .persist(PollEdited(twitchRoomId, pollId, poll))
          .thenRun(newStateSender(twitchRoomId))
          .thenReply(replyTo)(_ => Acknowledged)

      case DeletePoll(replyTo, twitchRoomId, pollId) =>
        Effect
          .persist(PollDeleted(twitchRoomId, pollId))
          .thenRun(newStateSender(twitchRoomId))
          .thenReply(replyTo)(_ => Acknowledged)
    }

  private def eventHandler(using
      randomProvider: RandomProvider
  ): (State, Event) => State = (state, event) =>
    val (twitchRoomId, newPolls) = event match {
      case PollAdded(twitchRoomId, poll) =>
        (
          twitchRoomId,
          getTwitchRoomPolls(state, twitchRoomId).withPoll(
            randomProvider.uuid().toString,
            poll
          )
        )

      case PollEdited(twitchRoomId, pollId, poll) =>
        (
          twitchRoomId,
          getTwitchRoomPolls(state, twitchRoomId).withEditedPoll(pollId, poll)
        )

      case PollDeleted(twitchRoomId, pollId) =>
        (
          twitchRoomId,
          getTwitchRoomPolls(state, twitchRoomId).withDeletedPoll(pollId)
        )
    }
    State(state.twitchRoomIdToPolls + (twitchRoomId -> newPolls))

  private def getTwitchRoomPolls(
      state: State,
      twitchRoomId: String
  ): ChannelPolls =
    state.twitchRoomIdToPolls.getOrElse(twitchRoomId, ChannelPolls())
}
