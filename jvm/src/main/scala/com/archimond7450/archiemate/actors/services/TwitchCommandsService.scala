package com.archimond7450.archiemate.actors.services

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.chatbot.{IRCListener, TwitchChatbot}
import com.archimond7450.archiemate.actors.repositories.settings.{
  CommandsSettingsRepository,
  VariablesSettingsRepository
}
import com.archimond7450.archiemate.actors.twitch.api
import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.twitch.eventsub
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.http.ChannelSettings
import com.archimond7450.archiemate.http.ChannelSettings.BuiltInCommandsSettings
import com.archimond7450.archiemate.providers.{RandomProvider, TimeProvider}
import com.archimond7450.archiemate.twitch.api.TwitchApiResponse
import com.archimond7450.archiemate.twitch.eventsub.{
  ChannelChatMessageEvent,
  ChatMessage
}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.util.Timeout
import org.slf4j.Logger

import java.time.{OffsetDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
import scala.util.matching.Regex

object TwitchCommandsService {
  val actorName = "twitchCommandsService"

  sealed trait Command
  final case class RespondToCommand(
      chatbotParams: TwitchChatbot.OperationalParameters,
      e: eventsub.ChannelChatMessageEvent
  ) extends Command
  private final case class ReturnCommandResponse(
      cmd: RespondToCommand,
      chatters: List[String],
      responseOption: Option[String]
  ) extends Command
  final case class RespondForTimer(
      chatbotParams: TwitchChatbot.OperationalParameters
  ) extends Command

  def apply()(using
      mediator: ActorRef[ArchieMateMediator.Command],
      chatbot: ActorRef[TwitchChatbot.Command],
      randomProvider: RandomProvider,
      timeProvider: TimeProvider
  ): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx
    (new TwitchCommandsService).operational()
  }
}

class TwitchCommandsService(using
    private val ctx: ActorContext[TwitchCommandsService.Command],
    private val mediator: ActorRef[ArchieMateMediator.Command],
    private val chatbot: ActorRef[TwitchChatbot.Command],
    private val randomProvider: RandomProvider,
    private val timeProvider: TimeProvider
) {
  private val settings = Settings(ctx.system)
  given Timeout = settings.askTimeout

  private val commandRegex: Regex =
    "^((@\\w+\\s*,??\\s*?)*?)!(\\w+)\\s*(\\s(.*)?)?$".r
  private val problemDiscord =
    "If the problem persists, report the problem on Archimond7450's Discord."

  private val log: Logger = ctx.log

  private object BuiltInVariables {
    val builtInVariablesRegex: Regex =
      "\\$\\{\\s*(\\w+)\\s*(:(\\s*(\\s*(((\\w+)\\s*=\\s*(\\S*?))|(\\w+)|((['\\\"]).*?\\11))\\s*[,;]?)+))?}".r // 11 groups, group 1 = var name, group 3 = arguments

    def expandBuiltInVariables(
        e: ChannelChatMessageEvent,
        chatters: List[String],
        text: String
    ): String = {
      val transformFunction: (String, Map[String, String]) => String =
        (varName, parameters) =>
          varName match {
            case Time.name =>
              val format = parameters.getOrElse("format", Time.defaultFormat)
              val zone = parameters.getOrElse("zone", Time.defaultZone)
              Time.getResponse(format, zone)

            case Chatters.name =>
              val separator =
                parameters.getOrElse("separator", Chatters.defaultSeparator)
              Chatters.getResponse(chatters, separator)

            case Sender.name =>
              Sender.getResponse(e.chatterUserName, parameters.get("notag"))

            case Random.name =>
              val from: Long = parameters
                .get("from")
                .flatMap(_.toLongOption)
                .getOrElse(Random.defaultFrom)
              val to: Long = parameters
                .get("to")
                .flatMap(_.toLongOption)
                .getOrElse(Random.defaultTo)
              Random.getResponse(from, to)
          }

      // params: " separator = ', ' , something"
      def splitParams(params: String): List[String] = {
        var state = 0
        var current = ""
        var result: List[String] = Nil
        for (char <- params) {
          state match {
            case 0 => // detecting parameter name
              char match {
                case space if space.isSpaceChar => current += char
                case ',' | ';' =>
                  result = result :+ current
                  current = ""
                case '=' =>
                  state = 1
                  current += char
                case letter => current += char
              }

            case 1 => // start of skipping value
              current += char
              char match {
                case '\'' => state = 2
                case '"'  => state = 3
                case _    => state = 4
              }

            case 2 => // skipping until '
              current += char
              if (char == '\'') state = 5

            case 3 => // skipping until "
              current += char
              if (char == '\"') state = 5

            case 4 => // skipping until space or separator
              char match {
                case ',' | ';' =>
                  state = 0
                  result = result :+ current
                  current = ""
                case space if space.isSpaceChar =>
                  current += char
                  state = 5
                case _ =>
                  current += char
              }

            case 5 => // skipping until separator
              if (char == ',' || char == ';') {
                state = 0
                result = result :+ current
                current = ""
              } else {
                current += char
              }
          }
        }

        if (current.nonEmpty) {
          result = result :+ current
        }

        result
      }

      val matches = builtInVariablesRegex.findAllMatchIn(text).toList
      val replacements = matches.map(m => {
        val varName: String = m.group(1)
        val params: String = if (m.group(3) == null) "" else m.group(3)
        val parameters: Map[String, String] =
          if (params == "") Map.empty
          else
            splitParams(params).map { param =>
              val nameValue = param.split("\\s*=\\s*").map(_.trim)
              if (nameValue.length == 1) {
                nameValue.last -> ""
              } else {
                nameValue(0) -> (nameValue(1) match {
                  case s"\"$value\"" => value
                  case s"'$value'"   => value
                  case value         => value
                })
              }
            }.toMap
        transformFunction(varName, parameters)
      })

      var result = text
      matches.zip(replacements).reverse.foreach {
        case (m, replacement) =>
          result =
            result.substring(0, m.start) + replacement + result.substring(m.end)
        case null =>
          result
      }

      result
    }

    object Time {
      val name = "time"
      val defaultFormat = "HH:mm:ss"
      val defaultZone = "Z"

      def getResponse(format: String, zone: String): String = {
        val now = timeProvider.now()
        Try(
          now
            .atZoneSameInstant(ZoneId.of(zone))
            .format(DateTimeFormatter.ofPattern(format))
        ) match {
          case Success(response) => response
          case Failure(ex) =>
            log.error(
              "Built-in variable time with parameters format={}, zone={} failed, returning with default parameters.",
              format,
              zone,
              ex
            )
            now
              .atZoneSameInstant(ZoneId.of(defaultZone))
              .format(DateTimeFormatter.ofPattern(defaultFormat))
        }
      }
    }

    object Chatters {
      val name = "chatters"
      val defaultSeparator = " "
      def getResponse(chatters: List[String], separator: String): String =
        chatters.mkString(separator)
    }

    object Sender {
      val name = "sender"
      def getResponse(
          senderDisplayName: String,
          notag: Option[String]
      ): String = {
        val tag = notag match {
          case Some(_) => ""
          case None    => "@"
        }
        s"$tag$senderDisplayName"
      }
    }

    object Random {
      val name = "random"

      val defaultFrom = 0
      val defaultTo = 100

      def getResponse(from: Long, to: Long): String =
        randomProvider.between(from, to).toString
    }
  }

  private object BuiltInCommands {
    trait BuiltInCommand {
      val name: String
      val getCommandResponse: (
          TwitchCommandsService.RespondToCommand,
          List[String],
          String,
          ActorRef[TwitchChatbot.Command]
      ) => Unit
    }

    object Command extends BuiltInCommand {
      override val name: String = "command"

      object Actions {
        val ADD = "add"
        val CREATE = "create"
        val RENAME = "rename"
        val EDIT = "edit"
        val UPDATE = "update"
        val CHANGE = "change"
        val DELETE = "delete"
        val REMOVE = "remove"

        val ALL_COMMAND_ACTIONS: List[String] = List(
          ADD,
          CREATE,
          RENAME,
          EDIT,
          UPDATE,
          CHANGE,
          DELETE,
          REMOVE
        )

        val ALL_ADDS: List[String] = List(ADD, CREATE)
        val ALL_RENAMES: List[String] = List(RENAME)
        val ALL_EDITS: List[String] = List(EDIT, UPDATE, CHANGE)
        val ALL_DELETES: List[String] = List(DELETE, REMOVE)
      }

      private val usage =
        s"$${sender} Usage: !command (${Actions.ALL_COMMAND_ACTIONS.mkString("|")}) ([!]newCommandName|newCommandResponse)"

      override val getCommandResponse: (
          TwitchCommandsService.RespondToCommand,
          List[String],
          String,
          ActorRef[TwitchChatbot.Command]
      ) => Unit = (cmd, chatters, strParameters, _) => {
        val broadcasterId = cmd.chatbotParams.broadcaster.id
        val channelName = cmd.chatbotParams.broadcaster.login
        val isBroadcaster = broadcasterId == cmd.e.chatterUserId
        val isModerator = cmd.chatbotParams.users.exists((userId, userState) =>
          userId == cmd.e.chatterUserId && userState.flags.contains(
            TwitchChatbot.UserFlag.Mod
          )
        )
        if (isBroadcaster || isModerator) {
          if (strParameters.nonEmpty) {
            val actionEnd = strParameters.indexOf(' ')
            val action = strParameters.substring(0, actionEnd).toLowerCase()
            if (Actions.ALL_COMMAND_ACTIONS.contains(action)) {
              val afterAction = strParameters.substring(actionEnd).trim
              if (afterAction.nonEmpty) {
                val commandNameEnd =
                  if (afterAction.contains(' ')) afterAction.indexOf(' ')
                  else afterAction.length
                val commandName = afterAction.substring(
                  if (afterAction.startsWith("!")) 1 else 0,
                  commandNameEnd
                )
                val afterCommandName =
                  afterAction.substring(commandNameEnd).trim
                action match {
                  case addAction if Actions.ALL_ADDS.contains(addAction) =>
                    val response = afterCommandName
                    ctx.ask[
                      ArchieMateMediator.Command,
                      CommandsSettingsRepository.AddCommandResponse
                    ](
                      mediator,
                      ref =>
                        ArchieMateMediator
                          .SendCommandsSettingsRepositoryCommand(
                            CommandsSettingsRepository.AddCommand(
                              ref,
                              broadcasterId,
                              commandName,
                              response
                            )
                          )
                    ) {
                      case Success(CommandsSettingsRepository.Acknowledged) =>
                        TwitchCommandsService.ReturnCommandResponse(
                          cmd,
                          chatters,
                          Some(
                            s"$${sender}, the command !$commandName was successfully created."
                          )
                        )

                      case Success(
                            CommandsSettingsRepository.CommandAlreadyExists
                          ) =>
                        TwitchCommandsService.ReturnCommandResponse(
                          cmd,
                          chatters,
                          Some(
                            s"$${sender}, the command !$commandName cannot be created because it already exists!"
                          )
                        )

                      case Failure(ex) =>
                        log.error(
                          "There was an exception when waiting for response from commands settings repository when asked to add command {} to channel {}",
                          commandName,
                          channelName,
                          ex
                        )
                        TwitchCommandsService.ReturnCommandResponse(
                          cmd,
                          chatters,
                          Some(
                            s"$${sender}, I cannot confirm the command !$commandName was successfully created... $problemDiscord"
                          )
                        )
                    }

                  case renameAction
                      if Actions.ALL_RENAMES.contains(renameAction) =>
                    val newCommandName =
                      if (afterCommandName.startsWith("!"))
                        afterCommandName.substring(1)
                      else afterCommandName
                    ctx.ask[
                      ArchieMateMediator.Command,
                      CommandsSettingsRepository.RenameCommandResponse
                    ](
                      mediator,
                      ref =>
                        ArchieMateMediator
                          .SendCommandsSettingsRepositoryCommand(
                            CommandsSettingsRepository.RenameCommand(
                              ref,
                              broadcasterId,
                              commandName,
                              newCommandName
                            )
                          )
                    ) {
                      case Success(CommandsSettingsRepository.Acknowledged) =>
                        TwitchCommandsService.ReturnCommandResponse(
                          cmd,
                          chatters,
                          Some(
                            s"$${sender}, the command !$commandName was successfully renamed."
                          )
                        )

                      case Success(CommandsSettingsRepository.NoSuchCommand) =>
                        TwitchCommandsService.ReturnCommandResponse(
                          cmd,
                          chatters,
                          Some(
                            s"$${sender}, the command !$commandName cannot be renamed because it doesn't exist!"
                          )
                        )

                      case Success(
                            CommandsSettingsRepository.CommandAlreadyExists
                          ) =>
                        TwitchCommandsService.ReturnCommandResponse(
                          cmd,
                          chatters,
                          Some(
                            s"$${sender}, the command !$commandName cannot be renamed because a command with the new name already exists!"
                          )
                        )

                      case Failure(ex) =>
                        log.error(
                          "There was an exception when waiting for response from commands settings repository when asked to rename command {} in channel {}",
                          commandName,
                          channelName,
                          ex
                        )
                        TwitchCommandsService.ReturnCommandResponse(
                          cmd,
                          chatters,
                          Some(
                            s"$${sender}, I cannot confirm the command !$commandName was successfully renamed... $problemDiscord"
                          )
                        )
                    }

                  case editAction if Actions.ALL_EDITS.contains(editAction) =>
                    val newResponse = afterCommandName
                    ctx.ask[
                      ArchieMateMediator.Command,
                      CommandsSettingsRepository.EditCommandResponse
                    ](
                      mediator,
                      ref =>
                        ArchieMateMediator
                          .SendCommandsSettingsRepositoryCommand(
                            CommandsSettingsRepository.EditCommand(
                              ref,
                              broadcasterId,
                              commandName,
                              newResponse
                            )
                          )
                    ) {
                      case Success(CommandsSettingsRepository.Acknowledged) =>
                        TwitchCommandsService.ReturnCommandResponse(
                          cmd,
                          chatters,
                          Some(
                            s"$${sender}, the response of command !$commandName was successfully edited."
                          )
                        )

                      case Success(CommandsSettingsRepository.NoSuchCommand) =>
                        TwitchCommandsService.ReturnCommandResponse(
                          cmd,
                          chatters,
                          Some(
                            s"$${sender}, the response of command !$commandName cannot be edited because the command doesn't exist!"
                          )
                        )

                      case Failure(ex) =>
                        log.error(
                          "There was an exception when waiting for response from commands settings repository when asked to edit the response of command {} in channel {}",
                          commandName,
                          channelName,
                          ex
                        )
                        TwitchCommandsService.ReturnCommandResponse(
                          cmd,
                          chatters,
                          Some(
                            s"$${sender}, I cannot confirm the response of command !$commandName was successfully edited... $problemDiscord"
                          )
                        )
                    }

                  case deleteAction
                      if Actions.ALL_DELETES.contains(deleteAction) =>
                    ctx.ask[
                      ArchieMateMediator.Command,
                      CommandsSettingsRepository.RemoveCommandResponse
                    ](
                      mediator,
                      ref =>
                        ArchieMateMediator
                          .SendCommandsSettingsRepositoryCommand(
                            CommandsSettingsRepository
                              .RemoveCommand(ref, broadcasterId, commandName)
                          )
                    ) {
                      case Success(CommandsSettingsRepository.Acknowledged) =>
                        TwitchCommandsService.ReturnCommandResponse(
                          cmd,
                          chatters,
                          Some(
                            s"$${sender}, the command !$commandName was successfully deleted."
                          )
                        )

                      case Success(CommandsSettingsRepository.NoSuchCommand) =>
                        TwitchCommandsService.ReturnCommandResponse(
                          cmd,
                          chatters,
                          Some(
                            s"$${sender}, the command !$commandName cannot be deleted because it doesn't exist!"
                          )
                        )

                      case Failure(ex) =>
                        log.error(
                          "There was an exception when waiting for response from commands settings repository when asked to delete command {} in channel {}",
                          commandName,
                          channelName,
                          ex
                        )
                        TwitchCommandsService.ReturnCommandResponse(
                          cmd,
                          chatters,
                          Some(
                            s"$${sender}, I cannot confirm the command !$commandName was successfully deleted... $problemDiscord"
                          )
                        )
                    }

                  case differentAction =>
                    log.error(
                      "Cannot handle !command action={} because there is no implementation for it!",
                      differentAction
                    )
                    ctx.self ! TwitchCommandsService.ReturnCommandResponse(
                      cmd,
                      chatters,
                      Some(usage)
                    )
                }
              } else {
                ctx.self ! TwitchCommandsService.ReturnCommandResponse(
                  cmd,
                  chatters,
                  Some(usage)
                )
              }
            } else {
              ctx.self ! TwitchCommandsService.ReturnCommandResponse(
                cmd,
                chatters,
                Some(usage)
              )
            }
          } else {
            ctx.self ! TwitchCommandsService.ReturnCommandResponse(
              cmd,
              chatters,
              Some(usage)
            )
          }
        }
      }
    }

    object Commands extends BuiltInCommand {
      override val name: String = "commands"
      override val getCommandResponse: (
          TwitchCommandsService.RespondToCommand,
          List[String],
          String,
          ActorRef[TwitchChatbot.Command]
      ) => Unit = (cmd, chatters, _, _) => {
        ctx.self ! TwitchCommandsService.ReturnCommandResponse(
          cmd,
          chatters,
          Some(
            s"$${sender}, you can find the commands for this channel on https://archiemate.com/t/commands/${cmd.chatbotParams.broadcaster.login}"
          )
        )
      }
    }

    object Set extends BuiltInCommand {
      override val name: String = "set"

      private val usage =
        s"$${sender} Usage: !set [variableName] [any value goes here]"

      override val getCommandResponse: (
          TwitchCommandsService.RespondToCommand,
          List[String],
          String,
          ActorRef[TwitchChatbot.Command]
      ) => Unit = (cmd, chatters, strParameters, _) => {
        val broadcasterId = cmd.chatbotParams.broadcaster.id
        val channelName = cmd.chatbotParams.broadcaster.login
        val variableNameEnd = strParameters.indexOf(' ')
        if (variableNameEnd > 0) {
          val variableName = strParameters.substring(0, variableNameEnd)
          val variableValue = strParameters.substring(variableNameEnd)
          ctx.ask[
            ArchieMateMediator.Command,
            VariablesSettingsRepository.Acknowledged.type
          ](
            mediator,
            ref =>
              ArchieMateMediator.SendVariablesSettingsRepositoryCommand(
                VariablesSettingsRepository
                  .SetVariable(ref, broadcasterId, variableName, variableValue)
              )
          ) {
            case Success(VariablesSettingsRepository.Acknowledged) =>
              TwitchCommandsService.ReturnCommandResponse(
                cmd,
                chatters,
                Some(
                  s"$${sender}, the variable $variableName has been successfully set."
                )
              )

            case Failure(ex) =>
              log.error(
                "Exception when waiting for change of {} variable in {} channel to value {}",
                variableName,
                channelName,
                variableValue,
                ex
              )
              TwitchCommandsService.ReturnCommandResponse(
                cmd,
                chatters,
                Some(
                  s"$${sender}, I cannot confirm the variable $variableName has been successfully set... $problemDiscord"
                )
              )
          }
        } else {
          ctx.self ! TwitchCommandsService.ReturnCommandResponse(
            cmd,
            chatters,
            Some(usage)
          )
        }
      }
    }

    object Unset extends BuiltInCommand {
      override val name: String = "unset"

      private val usage = "!unset [variableName1] [variableName2] ..."

      override val getCommandResponse: (
          TwitchCommandsService.RespondToCommand,
          List[String],
          String,
          ActorRef[TwitchChatbot.Command]
      ) => Unit = (cmd, chatters, strParameters, _) => {
        if (strParameters.trim.isEmpty) {
          ctx.self ! TwitchCommandsService.ReturnCommandResponse(
            cmd,
            chatters,
            Some(usage)
          )
        } else {
          val parameters = strParameters.split("\\s+").toList
          val broadcasterId = cmd.chatbotParams.broadcaster.id
          val channelName = cmd.chatbotParams.broadcaster.login
          ctx.ask[
            ArchieMateMediator.Command,
            VariablesSettingsRepository.Acknowledged.type
          ](
            mediator,
            ref =>
              ArchieMateMediator.SendVariablesSettingsRepositoryCommand(
                VariablesSettingsRepository
                  .UnsetVariables(ref, broadcasterId, parameters)
              )
          ) {
            case Success(VariablesSettingsRepository.Acknowledged) =>
              TwitchCommandsService.ReturnCommandResponse(
                cmd,
                chatters,
                Some(
                  s"$${sender}, the variables ${parameters.mkString} have been successfully unset."
                )
              )

            case Failure(ex) =>
              log.error(
                "Exception when waiting to unset variables {} in channel {}",
                parameters,
                channelName,
                ex
              )
              TwitchCommandsService.ReturnCommandResponse(
                cmd,
                chatters,
                Some(
                  s"$${sender}, I cannot confirm the variables ${parameters.mkString} have been successfully unset... $problemDiscord"
                )
              )
          }
        }
      }
    }

    object Title extends BuiltInCommand {
      override val name: String = "title"

      override val getCommandResponse: (
          TwitchCommandsService.RespondToCommand,
          List[String],
          String,
          ActorRef[TwitchChatbot.Command]
      ) => Unit = (cmd, chatters, strParameters, _) => {
        val chatterUserId = cmd.e.chatterUserId
        val broadcasterId = cmd.chatbotParams.broadcaster.id
        val tokenId = cmd.chatbotParams.tokenId

        if (strParameters.isEmpty) {
          ctx.askWithStatus[
            ArchieMateMediator.Command,
            TwitchApiResponse.ChannelInformation
          ](
            mediator,
            ref =>
              ArchieMateMediator.SendTwitchApiClientCommand(
                TwitchApiClient
                  .GetChannelInformation(ref, tokenId, broadcasterId)
              )
          ) {
            case Success(info: TwitchApiResponse.ChannelInformation) =>
              TwitchCommandsService.ReturnCommandResponse(
                cmd,
                chatters,
                Some(s"$${sender}, the channel title is \"${info.title}\".")
              )

            case Failure(ex) =>
              log.error(
                "Could not get response from Twitch API client about channel information",
                ex
              )
              TwitchCommandsService.ReturnCommandResponse(
                cmd,
                chatters,
                Some(
                  s"$${sender}, I'm sorry, I wasn't able to return the channel title. $problemDiscord"
                )
              )
          }
        } else if (
          chatterUserId == broadcasterId || cmd.chatbotParams.users.exists(
            (userId, userState) =>
              userId == chatterUserId && userState.flags
                .contains(TwitchChatbot.UserFlag.Mod)
          )
        ) {
          val newTitle = strParameters
          ctx.askWithStatus[
            ArchieMateMediator.Command,
            TwitchApiResponse.ModifyChannelInformation.type
          ](
            mediator,
            ref =>
              ArchieMateMediator.SendTwitchApiClientCommand(
                TwitchApiClient
                  .ChangeChannelTitle(ref, tokenId, broadcasterId, newTitle)
              )
          ) {
            case Success(TwitchApiResponse.ModifyChannelInformation) =>
              TwitchCommandsService.ReturnCommandResponse(
                cmd,
                chatters,
                Some(
                  s"$${sender}, the title has been successfully changed to \"$newTitle\"."
                )
              )

            case Failure(ex) =>
              log.error("Could not change channel title", ex)
              TwitchCommandsService.ReturnCommandResponse(
                cmd,
                chatters,
                Some(
                  s"$${sender}, I'm sorry, I wasn't able to change the channel title. $problemDiscord"
                )
              )
          }
        }
      }
    }

    object Game extends BuiltInCommand {
      override val name: String = "game"

      override val getCommandResponse: (
          TwitchCommandsService.RespondToCommand,
          List[String],
          String,
          ActorRef[TwitchChatbot.Command]
      ) => Unit = (cmd, chatters, strParameters, _) => {
        val chatterUserId = cmd.e.chatterUserId
        val broadcasterId = cmd.chatbotParams.broadcaster.id
        val tokenId = cmd.chatbotParams.tokenId

        if (strParameters.isEmpty) {
          ctx.askWithStatus[
            ArchieMateMediator.Command,
            TwitchApiResponse.ChannelInformation
          ](
            mediator,
            ref =>
              ArchieMateMediator.SendTwitchApiClientCommand(
                TwitchApiClient
                  .GetChannelInformation(ref, tokenId, broadcasterId)
              )
          ) {
            case Success(info: TwitchApiResponse.ChannelInformation) =>
              TwitchCommandsService.ReturnCommandResponse(
                cmd,
                chatters,
                Some(s"$${sender}, the channel game is \"${info.game_name}\".")
              )

            case Failure(ex) =>
              log.error(
                "Could not get response from Twitch API client about channel information",
                ex
              )
              TwitchCommandsService.ReturnCommandResponse(
                cmd,
                chatters,
                Some(
                  s"$${sender}, I'm sorry, I wasn't able to return the channel game. $problemDiscord"
                )
              )
          }
        } else if (
          chatterUserId == broadcasterId || cmd.chatbotParams.users.exists(
            (userId, userState) =>
              userId == chatterUserId && userState.flags
                .contains(TwitchChatbot.UserFlag.Mod)
          )
        ) {
          val gameName = strParameters
          ctx.askWithStatus[
            ArchieMateMediator.Command,
            TwitchApiResponse.ModifyChannelInformation.type
          ](
            mediator,
            ref =>
              ArchieMateMediator.SendTwitchApiClientCommand(
                TwitchApiClient.ChangeChannelToUnknownGame(
                  ref,
                  tokenId,
                  broadcasterId,
                  gameName
                )
              )
          ) {
            case Success(TwitchApiResponse.ModifyChannelInformation) =>
              TwitchCommandsService.ReturnCommandResponse(
                cmd,
                chatters,
                Some(
                  s"$${sender}, the channel game has been successfully changed to $gameName."
                )
              )

            case Failure(
                  TwitchApiResponse.GameNotFoundException(sentGameName)
                ) =>
              TwitchCommandsService.ReturnCommandResponse(
                cmd,
                chatters,
                Some(
                  s"$${sender}, I can't change the channel game to \"$sentGameName\" because Twitch doesn't know this game!"
                )
              )

            case Failure(ex) =>
              log.error("Could not change channel game", ex)
              TwitchCommandsService.ReturnCommandResponse(
                cmd,
                chatters,
                Some(
                  s"$${sender}, I'm sorry, I wasn't able to change the channel game. $problemDiscord"
                )
              )
          }
        }
      }
    }

    object Subs extends BuiltInCommand {
      override val name: String = "subs"

      override val getCommandResponse: (
          TwitchCommandsService.RespondToCommand,
          List[String],
          String,
          ActorRef[TwitchChatbot.Command]
      ) => Unit = (cmd, chatters, strParameters, _) => {
        val subCount = cmd.chatbotParams.users.count(
          _._2.flags.contains(TwitchChatbot.UserFlag.Sub)
        )
        val (isOrAre, subCountNum, subOrSubs) =
          if (subCount == 1) ("is", "one", "sub")
          else ("are", if (subCount == 0) "no" else subCount.toString, "subs")
        ctx.self ! TwitchCommandsService.ReturnCommandResponse(
          cmd,
          chatters,
          Some(s"$${sender}, currently there $isOrAre $subCount $subOrSubs.")
        )
      }
    }

    object Uptime extends BuiltInCommand {
      override val name: String = "uptime"

      override val getCommandResponse: (
          TwitchCommandsService.RespondToCommand,
          List[String],
          String,
          ActorRef[TwitchChatbot.Command]
      ) => Unit = (cmd, chatters, strParameters, _) => {
        val response = cmd.chatbotParams.stream match {
          case Some(stream) =>
            val nowSeconds = timeProvider
              .now()
              .atZoneSameInstant(ZoneId.of(BuiltInVariables.Time.defaultZone))
              .toEpochSecond
            val streamStartedSeconds = stream.startedAt.toEpochSecond
            val duration =
              Duration(nowSeconds - streamStartedSeconds, TimeUnit.SECONDS)
            val days = duration.toDays
            val hours = duration.toHours % 24
            val minutes = duration.toMinutes % 60
            val seconds = duration.toSeconds

            val daysHrsStr = (days, hours) match {
              case (0, 0) => ""
              case (0, _) => s"${hours}h "
              case (_, 0) => s"${days}d "
              case _      => s"${days}d ${hours}h "
            }

            val minStr = s"${minutes}m "
            val secStr = s"${seconds}s"

            s"$${sender}, the stream has been up for $daysHrsStr$minStr$secStr."
          case None => s"$${sender}, the stream is not up at the moment."
        }
        TwitchCommandsService.ReturnCommandResponse(
          cmd,
          chatters,
          Some(response)
        )
      }
    }

    object Followage extends BuiltInCommand {
      override val name: String = "followage"

      override val getCommandResponse: (
          TwitchCommandsService.RespondToCommand,
          List[String],
          String,
          ActorRef[TwitchChatbot.Command]
      ) => Unit = (cmd, chatters, strParameters, _) => {
        val chatterName = strParameters.split("\\s+").head
        val chatterUserIdOption = {
          if (strParameters.isEmpty) {
            Some(cmd.e.chatterUserId)
          } else {
            cmd.chatbotParams.users
              .find(
                _._2.user.user_login == chatterName.stripPrefix("@").toLowerCase
              )
              .map(_._2.user.user_id)
          }
        }

        chatterUserIdOption match {
          case Some(chatterUserId) =>
            ctx.askWithStatus[
              ArchieMateMediator.Command,
              TwitchApiResponse.CheckUserFollowage
            ](
              mediator,
              ref =>
                ArchieMateMediator.SendTwitchApiClientCommand(
                  TwitchApiClient.CheckUserFollowage(
                    ref,
                    cmd.chatbotParams.tokenId,
                    cmd.e.broadcasterUserId,
                    chatterUserId
                  )
                )
            ) {
              case Success(
                    TwitchApiResponse.CheckUserFollowage(Some(followage))
                  ) =>
                TwitchCommandsService.ReturnCommandResponse(
                  cmd,
                  chatters,
                  Some(if (chatterUserId == cmd.e.chatterUserId) {
                    s"$${sender}, you have been following since ${followage.followed_at}."
                  } else {
                    s"$${sender}, $chatterName has been following since ${followage.followed_at}."
                  })
                )

              case Success(TwitchApiResponse.CheckUserFollowage(None)) =>
                TwitchCommandsService.ReturnCommandResponse(
                  cmd,
                  chatters,
                  Some(if (chatterUserId == cmd.e.chatterUserId) {
                    s"$${sender}, you are NOT a follower of the channel."
                  } else {
                    s"$${sender}, $chatterName is NOT a follower of the channel."
                  })
                )

              case Failure(ex) =>
                TwitchCommandsService.ReturnCommandResponse(
                  cmd,
                  chatters,
                  Some(if (chatterUserId == cmd.e.chatterUserId) {
                    s"$${sender}, sorry, something went wrong and I can't check your followage... $problemDiscord"
                  } else {
                    s"$${sender}, sorry, something went wrong and I can't check $chatterName's followage... $problemDiscord"
                  })
                )

            }

          case None =>
            ctx.self ! TwitchCommandsService.ReturnCommandResponse(
              cmd,
              chatters,
              Some(
                s"$${sender}, I can't find user $chatterName and therefore can't check the followage."
              )
            )
        }
      }
    }

    object Afk extends BuiltInCommand {
      override val name: String = "afk"

      private val replies =
        Seq("see you soon!", "alright, then!", "I'll keep that in mind.")

      override val getCommandResponse: (
          TwitchCommandsService.RespondToCommand,
          List[String],
          String,
          ActorRef[TwitchChatbot.Command]
      ) => Unit = (cmd, chatters, strParameters, chatbot) => {
        ctx.self ! TwitchCommandsService.ReturnCommandResponse(
          cmd,
          chatters,
          Some(
            s"$${sender}, ${replies(randomProvider.between(0, replies.length))}"
          )
        )
        val userId = cmd.e.chatterUserId
        if (
          cmd.chatbotParams.users.exists((userId, userState) =>
            userId == userId && (userState.flags
              .contains(TwitchChatbot.UserFlag.Streamer) || userState.flags
              .contains(TwitchChatbot.UserFlag.Mod) || userState.flags.contains(
              TwitchChatbot.UserFlag.Vip
            ) || userState.flags.contains(TwitchChatbot.UserFlag.Sub))
          )
        ) {
          chatbot ! TwitchChatbot.Afk(userId)
        }
      }
    }

    val builtInCommands: Map[String, BuiltInCommand] = Map(
      Command.name -> Command,
      Commands.name -> Commands,
      Set.name -> Set,
      Unset.name -> Unset,
      Title.name -> Title,
      Game.name -> Game,
      Subs.name -> Subs,
      Uptime.name -> Uptime,
      Followage.name -> Followage,
      Afk.name -> Afk
    )

    val isBuilInCommandEnabled
        : Map[String, BuiltInCommandsSettings => Boolean] = Map(
      Command.name -> (_ => true),
      Commands.name -> (_ => true),
      Set.name -> (_ => true),
      Unset.name -> (_ => true),
      Title.name -> (_.title),
      Game.name -> (_.game),
      Subs.name -> (_.subs),
      Uptime.name -> (_.uptime),
      Followage.name -> (_.followage),
      Afk.name -> (_.afk)
    )
  }

  def operational(): Behavior[TwitchCommandsService.Command] =
    Behaviors.receiveAndLogMessage {
      case cmd @ TwitchCommandsService.RespondToCommand(chatbotParams, e) =>
        e.message.text.trim match {
          case commandRegex(strChatters, _, commandName, _, strParameters) =>
            val chatters: List[String] =
              if (strChatters == null) Nil else strChatters.split("\\s+").toList
            val parameters: String =
              if (strParameters == null) "" else strParameters.trim
            commandName.toLowerCase() match {
              case builtInCommandName
                  if BuiltInCommands.builtInCommands.contains(
                    builtInCommandName
                  ) && BuiltInCommands.isBuilInCommandEnabled(
                    builtInCommandName
                  )(
                    cmd.chatbotParams.channelSettings.builtInCommandsSettings
                  ) =>
                BuiltInCommands
                  .builtInCommands(builtInCommandName)
                  .getCommandResponse(cmd, chatters, parameters, chatbot)

              case channelCommandName =>
                ctx.self ! TwitchCommandsService.ReturnCommandResponse(
                  cmd,
                  chatters,
                  chatbotParams.channelSettings.commandsSettings.commands
                    .find(_.name == channelCommandName)
                    .map(_.response)
                )
            }
            Behaviors.same

          case _ =>
            Behaviors.same
        }

      case TwitchCommandsService.ReturnCommandResponse(
            cmd,
            chatters,
            responseOption
          ) =>
        responseOption.foreach { response =>
          cmd.chatbotParams.ircListener ! IRCListener.SendMessage(
            BuiltInVariables.expandBuiltInVariables(cmd.e, chatters, response)
          )
        }
        Behaviors.same

      case TwitchCommandsService.RespondForTimer(chatbotParams) =>
        val responseOption: Option[String] =
          chatbotParams.lastTimers.last match {
            case Left(commandName) =>
              chatbotParams.channelSettings.commandsSettings.commands
                .find(_.name == commandName)
                .map(_.response)
            case Right(manualTimerId) =>
              chatbotParams.channelSettings.timersSettings.manualTimers
                .find(_.id == manualTimerId)
                .map(_.response)
          }
        responseOption.foreach { response =>
          chatbotParams.ircListener ! IRCListener.SendMessage(
            BuiltInVariables.expandBuiltInVariables(
              ChannelChatMessageEvent(
                broadcasterUserId = chatbotParams.broadcaster.id,
                broadcasterUserLogin = chatbotParams.broadcaster.login,
                broadcasterUserName = chatbotParams.broadcaster.display_name,
                chatterUserId = "0",
                chatterUserLogin = "all",
                chatterUserName = "",
                messageId = "",
                message = ChatMessage(text = response, fragments = Nil),
                message_type = "",
                badges = Nil,
                cheer = None,
                color = "",
                reply = None,
                channel_points_custom_reward_id = None,
                channel_points_animation_id = None
              ),
              Nil,
              response
            )
          )
        }
        Behaviors.same
    }
}
