package com.archimond7450.archiemate.actors.repositories.settings

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.chatbot.TwitchChatbotsSupervisor
import com.archimond7450.archiemate.actors.repositories.GenericRepository
import com.archimond7450.archiemate.http.ChannelSettings.{
  ChannelCommand,
  CommandsSettings
}
import com.archimond7450.archiemate.providers.RandomProvider
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.persistence.typed.scaladsl.Effect

object CommandsSettingsRepository {
  val actorName = "CommandsSettingsRepository"

  sealed trait Command

  final case class AddCommand(
      replyTo: ActorRef[AddCommandResponse],
      twitchRoomId: String,
      commandName: String,
      commandResponse: String
  ) extends Command

  final case class RenameCommand(
      replyTo: ActorRef[RenameCommandResponse],
      twitchRoomId: String,
      commandNameOld: String,
      commandNameNew: String
  ) extends Command

  final case class EditCommand(
      replyTo: ActorRef[EditCommandResponse],
      twitchRoomId: String,
      commandName: String,
      commandResponseNew: String
  ) extends Command

  final case class RemoveCommand(
      replyTo: ActorRef[RemoveCommandResponse],
      twitchRoomId: String,
      commandName: String
  ) extends Command

  final case class GetCommandsSettings(
      replyTo: ActorRef[CommandsSettings],
      twitchRoomId: String
  ) extends Command

  final case class SetCommandsSettings(
      replyTo: ActorRef[Acknowledged.type],
      twitchRoomId: String,
      commandsSettings: CommandsSettings
  ) extends Command

  final case class ResetCommandsSettings(
      replyTo: ActorRef[Acknowledged.type],
      twitchRoomId: String
  ) extends Command

  sealed trait AddCommandResponse
  sealed trait RenameCommandResponse
  sealed trait EditCommandResponse
  sealed trait RemoveCommandResponse

  case object CommandAlreadyExists
      extends AddCommandResponse
      with RenameCommandResponse
  case object NoSuchCommand
      extends RenameCommandResponse
      with EditCommandResponse
      with RemoveCommandResponse

  case object Acknowledged
      extends AddCommandResponse
      with RenameCommandResponse
      with EditCommandResponse
      with RemoveCommandResponse

  sealed trait Event
  final case class CommandAdded(
      twitchRoomId: String,
      commandName: String,
      commandResponse: String
  ) extends Event
  final case class CommandRenamed(
      twitchRoomId: String,
      commandNameOld: String,
      commandNameNew: String
  ) extends Event
  final case class CommandEdited(
      twitchRoomId: String,
      commandName: String,
      commandResponseNew: String
  ) extends Event
  final case class CommandRemoved(twitchRoomId: String, commandName: String)
      extends Event
  final case class CommandsSettingsSet(
      twitchRoomId: String,
      commandsSettings: CommandsSettings
  ) extends Event
  final case class CommandsSettingsReset(twitchRoomId: String) extends Event

  /** Repository state
    * @param twitchRoomIdToCommands
    *   Map of twitchRoomIds to the map of commandIds to the tuple of
    *   commandName and commandResponse
    */
  private case class State(
      twitchRoomIdToCommands: Map[String, Map[String, (String, String)]] =
        Map.empty
  )

  def apply()(using
      mediator: ActorRef[ArchieMateMediator.Command],
      randomProvider: RandomProvider
  ): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx

    new GenericRepository[Command, Event, State] {
      extension (state: State) {
        def getCommands(twitchRoomId: String): Map[String, (String, String)] = {
          state.twitchRoomIdToCommands.getOrElse(twitchRoomId, Map.empty)
        }

        def hasCommand(twitchRoomId: String, commandName: String): Boolean = {
          state.getCommands(twitchRoomId).values.exists(_._1 == commandName)
        }

        def getCommandsSettings(twitchRoomId: String): CommandsSettings =
          CommandsSettings(
            state
              .getCommands(twitchRoomId)
              .toList
              .map((commandId, commandNameAndResponse) =>
                ChannelCommand(
                  id = Some(commandId),
                  name = commandNameAndResponse._1,
                  response = commandNameAndResponse._2
                )
              )
          )

        def withAddedCommand(
            twitchRoomId: String,
            commandName: String,
            commandResponse: String
        ): State = {
          val oldCommands = state.getCommands(twitchRoomId)
          val newCommands = oldCommands + (randomProvider
            .uuid()
            .toString -> (commandName, commandResponse))
          state.withChangedCommands(twitchRoomId, newCommands)
        }

        def withRenamedCommand(
            twitchRoomId: String,
            commandNameOld: String,
            commandNameNew: String
        ): State = {
          val oldCommands = state.getCommands(twitchRoomId)
          val oldCommand = oldCommands.find(_._2._1 == commandNameOld).get
          val newCommands =
            oldCommands - oldCommand._1 + (oldCommand._1 -> (commandNameNew, oldCommand._2._2))
          state.withChangedCommands(twitchRoomId, newCommands)
        }

        def withEditedCommand(
            twitchRoomId: String,
            commandName: String,
            commandResponseNew: String
        ): State = {
          val oldCommands = state.getCommands(twitchRoomId)
          val oldCommand = oldCommands.find(_._2._1 == commandName).get
          val newCommands =
            oldCommands + (oldCommand._1 -> (commandName -> commandResponseNew))
          state.withChangedCommands(twitchRoomId, newCommands)
        }

        def withRemovedCommand(
            twitchRoomId: String,
            commandName: String
        ): State = {
          val oldCommands = state.getCommands(twitchRoomId)
          val oldCommand = oldCommands.find(_._2._1 == commandName).get
          val newCommands = oldCommands - oldCommand._1
          state.withChangedCommands(twitchRoomId, newCommands)
        }

        def withChangedCommands(
            twitchRoomId: String,
            newCommands: Map[String, (String, String)]
        ): State = {
          State(state.twitchRoomIdToCommands + (twitchRoomId -> newCommands))
        }

        def withChangedCommandsSettings(
            twitchRoomId: String,
            newSettings: CommandsSettings
        ): State = {
          val newCommands: Map[String, (String, String)] =
            newSettings.commands.map { cmd =>
              (
                cmd.id.getOrElse(randomProvider.uuid().toString),
                (cmd.name, cmd.response)
              )
            }.toMap
          State(state.twitchRoomIdToCommands + (twitchRoomId -> newCommands))
        }

        def withResetCommands(twitchRoomId: String): State = {
          state.withChangedCommands(twitchRoomId, Map.empty)
        }
      }

      override protected val actorName: String =
        CommandsSettingsRepository.actorName

      override protected val emptyState: State = State()

      private val notifyChatbotsSupervisor: String => State => Unit =
        twitchRoomId =>
          state =>
            mediator ! ArchieMateMediator.SendTwitchChatbotsSupervisorCommand(
              TwitchChatbotsSupervisor.NewChannelSettingsEvent(
                twitchRoomId,
                TwitchChatbotsSupervisor.CommandsSettingsChanged(
                  state.getCommandsSettings(twitchRoomId)
                )
              )
            )

      override protected val commandHandler
          : (State, Command) => Effect[Event, State] = (state, command) =>
        command match {
          case AddCommand(replyTo, twitchRoomId, commandName, response)
              if state.hasCommand(twitchRoomId, commandName) =>
            Effect.none.thenReply(replyTo)(_ => CommandAlreadyExists)

          case AddCommand(
                replyTo,
                twitchRoomId,
                commandName,
                commandResponse
              ) =>
            Effect
              .persist(CommandAdded(twitchRoomId, commandName, commandResponse))
              .thenRun(notifyChatbotsSupervisor(twitchRoomId))
              .thenReply(replyTo)(_ => Acknowledged)

          case RenameCommand(
                replyTo,
                twitchRoomId,
                commandNameOld,
                commandNameNew
              ) if !state.hasCommand(twitchRoomId, commandNameOld) =>
            Effect.none.thenReply(replyTo)(_ => NoSuchCommand)

          case RenameCommand(
                replyTo,
                twitchRoomId,
                commandNameOld,
                commandNameNew
              ) if state.hasCommand(twitchRoomId, commandNameNew) =>
            Effect.none.thenReply(replyTo)(_ => CommandAlreadyExists)

          case RenameCommand(
                replyTo,
                twitchRoomId,
                commandNameOld,
                commandNameNew
              ) =>
            Effect
              .persist(
                CommandRenamed(twitchRoomId, commandNameOld, commandNameNew)
              )
              .thenRun(notifyChatbotsSupervisor(twitchRoomId))
              .thenReply(replyTo)(_ => Acknowledged)

          case EditCommand(
                replyTo,
                twitchRoomId,
                commandName,
                commandResponseNew
              ) if !state.hasCommand(twitchRoomId, commandName) =>
            Effect.none.thenReply(replyTo)(_ => NoSuchCommand)

          case EditCommand(
                replyTo,
                twitchRoomId,
                commandName,
                commandResponseNew
              ) =>
            Effect
              .persist(
                CommandEdited(twitchRoomId, commandName, commandResponseNew)
              )
              .thenRun(notifyChatbotsSupervisor(twitchRoomId))
              .thenReply(replyTo)(_ => Acknowledged)

          case RemoveCommand(replyTo, twitchRoomId, commandName)
              if !state.hasCommand(twitchRoomId, commandName) =>
            Effect.none.thenReply(replyTo)(_ => NoSuchCommand)

          case RemoveCommand(replyTo, twitchRoomId, commandName) =>
            Effect
              .persist(CommandRemoved(twitchRoomId, commandName))
              .thenRun(notifyChatbotsSupervisor(twitchRoomId))
              .thenReply(replyTo)(_ => Acknowledged)

          case GetCommandsSettings(replyTo, twitchRoomId) =>
            Effect.none.thenReply(replyTo)(_.getCommandsSettings(twitchRoomId))

          case SetCommandsSettings(replyTo, twitchRoomId, commandsSettings) =>
            Effect
              .persist(CommandsSettingsSet(twitchRoomId, commandsSettings))
              .thenRun(notifyChatbotsSupervisor(twitchRoomId))
              .thenReply(replyTo)(_ => Acknowledged)

          case ResetCommandsSettings(replyTo, twitchRoomId) =>
            Effect
              .persist(CommandsSettingsReset(twitchRoomId))
              .thenRun(notifyChatbotsSupervisor(twitchRoomId))
              .thenReply(replyTo)(_ => Acknowledged)
        }

      override protected val eventHandler: (State, Event) => State =
        (state, event) =>
          event match {
            case CommandAdded(twitchRoomId, commandName, commandResponse) =>
              state.withAddedCommand(twitchRoomId, commandName, commandResponse)

            case CommandRenamed(twitchRoomId, commandNameOld, commandNameNew) =>
              state.withRenamedCommand(
                twitchRoomId,
                commandNameOld,
                commandNameNew
              )

            case CommandEdited(twitchRoomId, commandName, commandResponseNew) =>
              state.withEditedCommand(
                twitchRoomId,
                commandName,
                commandResponseNew
              )

            case CommandRemoved(twitchRoomId, commandName) =>
              state.withRemovedCommand(twitchRoomId, commandName)

            case CommandsSettingsSet(twitchRoomId, commandsSettings) =>
              state.withChangedCommandsSettings(twitchRoomId, commandsSettings)

            case CommandsSettingsReset(twitchRoomId) =>
              state.withResetCommands(twitchRoomId)
          }
    }.eventSourcedBehavior()
  }
}
