package com.archimond7450.archiemate.actors.repositories.settings

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.chatbot.TwitchChatbotsSupervisor
import com.archimond7450.archiemate.actors.repositories.{
  GenericRepository,
  GenericSerializer
}
import com.archimond7450.archiemate.http.ChannelSettings.{
  ChannelVariable,
  VariablesSettings
}
import com.archimond7450.archiemate.providers.RandomProvider
import com.archimond7450.archiemate.CirceConfiguration.frontendConfiguration
import com.archimond7450.archiemate.SerializerIDs
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.persistence.typed.scaladsl.Effect

object VariablesSettingsRepository {
  val actorName = "VariablesSettingsRepository"

  sealed trait Command

  final case class SetVariable(
      replyTo: ActorRef[Acknowledged.type],
      twitchRoomId: String,
      variableName: String,
      variableValueNew: String
  ) extends Command

  final case class UnsetVariables(
      replyTo: ActorRef[Acknowledged.type],
      twitchRoomId: String,
      variableNames: List[String]
  ) extends Command

  final case class GetVariablesSettings(
      replyTo: ActorRef[VariablesSettings],
      twitchRoomId: String
  ) extends Command

  final case class SetVariablesSettings(
      replyTo: ActorRef[Acknowledged.type],
      twitchRoomId: String,
      variablesSettings: VariablesSettings
  ) extends Command

  final case class ResetVariablesSettings(
      replyTo: ActorRef[Acknowledged.type],
      twitchRoomId: String
  ) extends Command

  sealed trait SetVariableResponse
  sealed trait UnsetVariableResponse

  case object Acknowledged

  private sealed trait Event
  private object Event {
    given Decoder[Event] = ConfiguredDecoder.derived
    given Encoder[Event] = ConfiguredEncoder.derived
  }

  private final case class VariableSet(
      twitchRoomId: String,
      variableName: String,
      variableValueNew: String
  ) extends Event
  private object VariableSet {
    given Decoder[VariableSet] = ConfiguredDecoder.derived
    given Encoder[VariableSet] = ConfiguredEncoder.derived
  }

  private final case class VariablesUnset(
      twitchRoomId: String,
      variableNames: List[String]
  ) extends Event
  private object VariablesUnset {
    given Decoder[VariablesUnset] = ConfiguredDecoder.derived
    given Encoder[VariablesUnset] = ConfiguredEncoder.derived
  }

  private final case class VariablesSettingsSet(
      twitchRoomId: String,
      variablesSettings: VariablesSettings
  ) extends Event
  private object VariablesSettingsSet {
    given Decoder[VariablesSettingsSet] = ConfiguredDecoder.derived
    given Encoder[VariablesSettingsSet] = ConfiguredEncoder.derived
  }

  private final case class VariablesSettingsReset(twitchRoomId: String)
      extends Event
  private object VariablesSettingsReset {
    given Decoder[VariablesSettingsReset] = ConfiguredDecoder.derived
    given Encoder[VariablesSettingsReset] = ConfiguredEncoder.derived
  }

  private class EventSerializer
      extends GenericSerializer[Event](
        actorName,
        SerializerIDs.variablesSettingsRepositoryId
      ) {
    override val toEvent: PartialFunction[AnyRef, Event] = {
      case event: Event => event
    }
  }

  /** Repository state
    * @param twitchRoomIdToVariables
    *   Map of twitchRoomIds to the map of variableIds to the tuple of
    *   variableName and variableResponse
    */
  private case class State(
      twitchRoomIdToVariables: Map[String, Map[String, (String, String)]] =
        Map.empty
  )

  def apply()(using
      mediator: ActorRef[ArchieMateMediator.Command],
      randomProvider: RandomProvider
  ): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx

    new GenericRepository[Command, Event, State] {
      extension (state: State) {
        def getVariables(
            twitchRoomId: String
        ): Map[String, (String, String)] = {
          state.twitchRoomIdToVariables.getOrElse(twitchRoomId, Map.empty)
        }

        def getVariablesSettings(twitchRoomId: String): VariablesSettings =
          VariablesSettings(
            state
              .getVariables(twitchRoomId)
              .toList
              .map((variableId, variableNameAndResponse) =>
                ChannelVariable(
                  id = Some(variableId),
                  name = variableNameAndResponse._1,
                  value = variableNameAndResponse._2
                )
              )
          )

        def withSetVariable(
            twitchRoomId: String,
            variableName: String,
            variableValueNew: String
        ): State = {
          val oldVariables = state.getVariables(twitchRoomId)
          val oldVariable = oldVariables.find(_._2._1 == variableName).get
          val newVariables =
            oldVariables + (oldVariable._1 -> (variableName, variableValueNew))
          state.withChangedVariables(twitchRoomId, newVariables)
        }

        def withUnsetVariables(
            twitchRoomId: String,
            variableNames: List[String]
        ): State = {
          val oldVariables = state.getVariables(twitchRoomId)
          val unsetVariableIds = oldVariables
            .filter((variableId, variableNameAndValue) =>
              variableNames.contains(variableNameAndValue._1)
            )
            .keys
          val newVariables = oldVariables -- unsetVariableIds
          state.withChangedVariables(twitchRoomId, newVariables)
        }

        def withChangedVariables(
            twitchRoomId: String,
            newSettings: Map[String, (String, String)]
        ): State = {
          State(state.twitchRoomIdToVariables + (twitchRoomId -> newSettings))
        }

        def withChangedVariablesSettings(
            twitchRoomId: String,
            newSettings: VariablesSettings
        ): State = {
          val newVariables: Map[String, (String, String)] =
            newSettings.variables.map { variable =>
              (
                variable.id.getOrElse(randomProvider.uuid().toString),
                (variable.name, variable.value)
              )
            }.toMap
          State(state.twitchRoomIdToVariables + (twitchRoomId -> newVariables))
        }

        def withResetVariables(twitchRoomId: String): State = {
          state.withChangedVariables(twitchRoomId, Map.empty)
        }
      }

      override protected val actorName: String =
        VariablesSettingsRepository.actorName

      override protected val emptyState: State = State()

      private val notifyChatbotsSupervisor: String => State => Unit =
        twitchRoomId =>
          state =>
            mediator ! ArchieMateMediator.SendTwitchChatbotsSupervisorCommand(
              TwitchChatbotsSupervisor.NewChannelSettingsEvent(
                twitchRoomId,
                TwitchChatbotsSupervisor.VariablesSettingsChanged(
                  state.getVariablesSettings(twitchRoomId)
                )
              )
            )

      override protected val commandHandler
          : (State, Command) => Effect[Event, State] = (state, command) =>
        command match {
          case SetVariable(
                replyTo,
                twitchRoomId,
                variableName,
                variableValueNew
              ) =>
            Effect
              .persist(
                VariableSet(twitchRoomId, variableName, variableValueNew)
              )
              .thenRun(notifyChatbotsSupervisor(twitchRoomId))
              .thenReply(replyTo)(_ => Acknowledged)

          case UnsetVariables(replyTo, twitchRoomId, variableNames) =>
            Effect
              .persist(VariablesUnset(twitchRoomId, variableNames))
              .thenRun(notifyChatbotsSupervisor(twitchRoomId))
              .thenReply(replyTo)(_ => Acknowledged)

          case GetVariablesSettings(replyTo, twitchRoomId) =>
            Effect.none.thenReply(replyTo)(_.getVariablesSettings(twitchRoomId))

          case SetVariablesSettings(replyTo, twitchRoomId, variablesSettings) =>
            Effect
              .persist(VariablesSettingsSet(twitchRoomId, variablesSettings))
              .thenRun(notifyChatbotsSupervisor(twitchRoomId))
              .thenReply(replyTo)(_ => Acknowledged)

          case ResetVariablesSettings(replyTo, twitchRoomId) =>
            Effect
              .persist(VariablesSettingsReset(twitchRoomId))
              .thenRun(notifyChatbotsSupervisor(twitchRoomId))
              .thenReply(replyTo)(_ => Acknowledged)
        }

      override protected val eventHandler: (State, Event) => State =
        (state, event) =>
          event match {
            case VariableSet(twitchRoomId, variableName, variableValueNew) =>
              state.withSetVariable(
                twitchRoomId,
                variableName,
                variableValueNew
              )

            case VariablesUnset(twitchRoomId, variableNames) =>
              state.withUnsetVariables(twitchRoomId, variableNames)

            case VariablesSettingsSet(twitchRoomId, variablesSettings) =>
              state.withChangedVariablesSettings(
                twitchRoomId,
                variablesSettings
              )

            case VariablesSettingsReset(twitchRoomId) =>
              state.withResetVariables(twitchRoomId)
          }
    }.eventSourcedBehavior()
  }
}
