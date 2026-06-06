package com.archimond7450.archiemate.actors.repositories.settings

import com.archimond7450.archiemate.CirceConfiguration.frontendConfiguration
import com.archimond7450.archiemate.SerializerIDs
import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.chatbot.ChatbotsSupervisor
import com.archimond7450.archiemate.actors.repositories.GenericSerializer
import com.archimond7450.archiemate.http.Predictions.{
  ChannelPredictions,
  Prediction
}
import com.archimond7450.archiemate.providers.RandomProvider
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import io.circe.{Decoder, Encoder}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import org.apache.pekko.persistence.typed.PersistenceId
import org.apache.pekko.persistence.typed.scaladsl.{
  Effect,
  EventSourcedBehavior,
  ReplyEffect
}

object PredictionsRepository {
  val actorName = "PredictionsRepository"

  sealed trait Command

  final case class GetPredictions(
      replyTo: ActorRef[ChannelPredictions],
      twitchRoomId: String
  ) extends Command

  final case class AddPrediction(
      replyTo: ActorRef[Acknowledged.type],
      twitchRoomId: String,
      prediction: Prediction
  ) extends Command

  final case class EditPrediction(
      replyTo: ActorRef[Acknowledged.type],
      twitchRoomId: String,
      predictionId: String,
      prediction: Prediction
  ) extends Command

  final case class DeletePrediction(
      replyTo: ActorRef[Acknowledged.type],
      twitchRoomId: String,
      predictionId: String
  ) extends Command

  case object Acknowledged

  private sealed trait Event
  private object Event {
    given Decoder[Event] = ConfiguredDecoder.derived
    given Encoder[Event] = ConfiguredEncoder.derived
  }

  private final case class PredictionAdded(
      twitchRoomId: String,
      prediction: Prediction
  ) extends Event

  private object PredictionAdded {
    given Decoder[PredictionAdded] = ConfiguredDecoder.derived
    given Encoder[PredictionAdded] = ConfiguredEncoder.derived
  }

  private final case class PredictionEdited(
      twitchRoomId: String,
      predictionId: String,
      prediction: Prediction
  ) extends Event

  private object PredictionEdited {
    given Decoder[PredictionEdited] = ConfiguredDecoder.derived
    given Encoder[PredictionEdited] = ConfiguredEncoder.derived
  }

  private final case class PredictionDeleted(
      twitchRoomId: String,
      predictionId: String
  ) extends Event

  private object PredictionDeleted {
    given Decoder[PredictionDeleted] = ConfiguredDecoder.derived
    given Encoder[PredictionDeleted] = ConfiguredEncoder.derived
  }

  private class EventSerializer
      extends GenericSerializer[Event](
        actorName,
        SerializerIDs.predictionsRepositoryId
      ) {
    override val toEvent: PartialFunction[AnyRef, Event] = {
      case event: Event => event
    }
  }

  private final case class State(
      twitchRoomIdToPredictions: Map[String, ChannelPredictions] = Map.empty
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
      val event = ChatbotsSupervisor.PredictionsChanged(
        state.twitchRoomIdToPredictions(twitchRoomId)
      )
      mediator ! ArchieMateMediator.SendChatbotsSupervisorCommand(
        ChatbotsSupervisor.NewChannelSettingsEvent(twitchRoomId, event)
      )
    }

  private def commandHandler(using
      mediator: ActorRef[ArchieMateMediator.Command]
  ): (State, Command) => ReplyEffect[Event, State] = (state, command) =>
    command match {
      case GetPredictions(replyTo, twitchRoomId) =>
        Effect.none.thenReply(replyTo)(
          _.twitchRoomIdToPredictions
            .getOrElse(twitchRoomId, ChannelPredictions())
        )

      case AddPrediction(replyTo, twitchRoomId, prediction) =>
        Effect
          .persist(PredictionAdded(twitchRoomId, prediction))
          .thenRun(newStateSender(twitchRoomId))
          .thenReply(replyTo)(_ => Acknowledged)

      case EditPrediction(replyTo, twitchRoomId, predictionId, prediction) =>
        Effect
          .persist(PredictionEdited(twitchRoomId, predictionId, prediction))
          .thenRun(newStateSender(twitchRoomId))
          .thenReply(replyTo)(_ => Acknowledged)

      case DeletePrediction(replyTo, twitchRoomId, predictionId) =>
        Effect
          .persist(PredictionDeleted(twitchRoomId, predictionId))
          .thenRun(newStateSender(twitchRoomId))
          .thenReply(replyTo)(_ => Acknowledged)
    }

  private def eventHandler(using
      randomProvider: RandomProvider
  ): (State, Event) => State = (state, event) =>
    val (twitchRoomId, newPredictions) = event match {
      case PredictionAdded(twitchRoomId, prediction) =>
        (
          twitchRoomId,
          getTwitchRoomPredictions(state, twitchRoomId).withPrediction(
            randomProvider.uuid().toString,
            prediction
          )
        )

      case PredictionEdited(twitchRoomId, predictionId, prediction) =>
        (
          twitchRoomId,
          getTwitchRoomPredictions(state, twitchRoomId).withEditedPrediction(
            predictionId,
            prediction
          )
        )

      case PredictionDeleted(twitchRoomId, predictionId) =>
        (
          twitchRoomId,
          getTwitchRoomPredictions(state, twitchRoomId).withDeletedPrediction(
            predictionId
          )
        )
    }
    State(state.twitchRoomIdToPredictions + (twitchRoomId -> newPredictions))

  private def getTwitchRoomPredictions(
      state: State,
      twitchRoomId: String
  ): ChannelPredictions =
    state.twitchRoomIdToPredictions.getOrElse(
      twitchRoomId,
      ChannelPredictions()
    )
}
