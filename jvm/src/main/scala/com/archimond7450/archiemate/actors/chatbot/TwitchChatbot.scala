package com.archimond7450.archiemate.actors.chatbot

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.repositories.sessions.TwitchUserSessionsRepository
import com.archimond7450.archiemate.actors.repositories.settings.{
  AutomaticMessagesSettingsRepository,
  BasicChatbotSettingsRepository,
  BuiltInCommandsSettingsRepository,
  CommandsSettingsRepository,
  OverlaysSettingsRepository,
  TimersSettingsRepository,
  VariablesSettingsRepository
}
import com.archimond7450.archiemate.actors.services.TwitchCommandsService
import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.extensions.ListExtension.{
  randomOrDefault,
  toMapWithKey
}
import com.archimond7450.archiemate.extensions.StringExtensions.{
  asTwitchSubTier,
  asVariableRegex
}
import com.archimond7450.archiemate.http.ChannelSettings.{
  AutomaticMessagesSettings,
  BasicChatbotSettings,
  BuiltInCommandsSettings,
  CommandsSettings,
  KnownGreetsMode,
  KnownGreetsSettings,
  ManualTimer,
  OverlaysSettings,
  TimersSettings,
  VariablesSettings,
  Settings as ChannelSettings
}
import com.archimond7450.archiemate.providers.{RandomProvider, TimeProvider}
import com.archimond7450.archiemate.twitch.api.{TwitchApi, TwitchApiResponse}
import com.archimond7450.archiemate.twitch.api.TwitchApiResponse.{
  GetChatters,
  GetModerators,
  GetStream,
  GetSubs,
  GetTokenUser,
  GetVIPs
}
import com.archimond7450.archiemate.twitch.eventsub
import com.archimond7450.archiemate.twitch.irc.{
  IncomingMessageDecoder,
  OutgoingMessageEncoder
}
import org.apache.pekko.actor.typed.scaladsl.{
  ActorContext,
  Behaviors,
  StashBuffer,
  TimerScheduler
}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}

object TwitchChatbot {
  sealed trait Command
  case object Leave extends Command
  final case class NewTokenId(tokenId: String) extends Command
  private final case class NoToken(exceptionOption: Option[Throwable])
      extends Command
  final case class NewBasicChatbotSettings(settings: BasicChatbotSettings)
      extends Command
  final case class NewBuiltInCommandsSettings(settings: BuiltInCommandsSettings)
      extends Command
  final case class NewCommandsSettings(settings: CommandsSettings)
      extends Command
  final case class NewVariablesSettings(settings: VariablesSettings)
      extends Command
  final case class NewTimersSettings(settings: TimersSettings) extends Command
  final case class NewOverlaysSettings(settings: OverlaysSettings)
      extends Command
  final case class NewAutomaticMessagesSettings(
      settings: AutomaticMessagesSettings
  ) extends Command
  final case class Broadcaster(broadcaster: GetTokenUser) extends Command
  private final case class FailedToGetSettings(ex: Throwable) extends Command
  final case class Mods(mods: Option[GetModerators]) extends Command
  final case class VIPs(vips: Option[GetVIPs]) extends Command
  final case class Subs(subs: Option[GetSubs]) extends Command
  final case class Chatters(chatters: Option[GetChatters]) extends Command
  final case class Stream(stream: Option[Option[TwitchApi.Stream]])
      extends Command // TODO: Change outer Option to Try
  final case class EventSubEvent(event: eventsub.Event) extends Command
  final case class Afk(chatterUserId: String) extends Command
  private case object ChattersTimer extends Command
  private case object MessageTimer extends Command

  sealed trait UserFlag
  object UserFlag {
    object Streamer extends UserFlag
    object Mod extends UserFlag
    object Vip extends UserFlag
    object Sub extends UserFlag
    object Online extends UserFlag
    object Spoke extends UserFlag
    object Afk extends UserFlag
    object Ignore extends UserFlag
  }

  final case class UserState(
      user: TwitchApi.User,
      flags: Set[UserFlag] = Set.empty,
      subInfo: Option[TwitchApi.SubbedUser] = None,
      afkConversations: Set[String] = Set.empty
  )

  final case class OperationalParameters(
      tokenId: String,
      channelSettings: ChannelSettings,
      broadcaster: GetTokenUser,
      eventSubListener: ActorRef[EventSubListener.Command],
      ircListener: ActorRef[IRCListener.Command],
      twitchCommandsService: ActorRef[TwitchCommandsService.Command],
      users: Map[String, UserState],
      stream: Option[TwitchApi.Stream],
      messagesAfterLastTimer: Int = 0,
      lastTimers: Seq[Either[String, String]] = Seq.empty
  )

  def apply(twitchRoomId: String)(using
      supervisor: ActorRef[TwitchChatbotsSupervisor.Command],
      mediator: ActorRef[ArchieMateMediator.Command],
      decoder: IncomingMessageDecoder,
      encoder: OutgoingMessageEncoder,
      randomProvider: RandomProvider,
      timeProvider: TimeProvider,
      settings: Settings
  ): Behavior[Command] = {
    Behaviors.withStash(100) { buffer =>
      given StashBuffer[Command] = buffer
      Behaviors.setup { ctx =>
        given ActorContext[Command] = ctx
        Behaviors.withTimers { timers =>
          given TimerScheduler[Command] = timers
          (new TwitchChatbot).initial(twitchRoomId)
        }
      }
    }
  }
}

class TwitchChatbot(using
    private val ctx: ActorContext[TwitchChatbot.Command],
    private val buffer: StashBuffer[TwitchChatbot.Command],
    private val timers: TimerScheduler[TwitchChatbot.Command],
    private val supervisor: ActorRef[TwitchChatbotsSupervisor.Command],
    private val mediator: ActorRef[ArchieMateMediator.Command],
    private val decoder: IncomingMessageDecoder,
    private val encoder: OutgoingMessageEncoder,
    private val randomProvider: RandomProvider,
    private val timeProvider: TimeProvider,
    private val settings: Settings
) {
  given Timeout = settings.askTimeout
  given ActorRef[TwitchChatbot.Command] = ctx.self

  given Ordering[Int] = Ordering.fromLessThan(_ < _)

  final case class InitialParameters(
      twitchRoomId: String,
      tokenIdOption: Option[String] = None,
      basicChatbotSettingsOption: Option[BasicChatbotSettings] = None,
      builtInCommandsSettingsOption: Option[BuiltInCommandsSettings] = None,
      commandsSettingsOption: Option[CommandsSettings] = None,
      variablesSettingsOption: Option[VariablesSettings] = None,
      timersSettingsOption: Option[TimersSettings] = None,
      overlaysSettingsOption: Option[OverlaysSettings] = None,
      automaticMessagesSettingsOption: Option[AutomaticMessagesSettings] = None,
      broadcasterOption: Option[GetTokenUser] = None
  )

  def initial(twitchRoomId: String): Behavior[TwitchChatbot.Command] = initial1(
    InitialParameters(twitchRoomId)
  )

  private def initial1(
      params: InitialParameters
  ): Behavior[TwitchChatbot.Command] = {
    ctx.ask[
      ArchieMateMediator.Command,
      TwitchUserSessionsRepository.ReturnedTokenIdForUserId
    ](
      mediator,
      ref =>
        ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand(
          TwitchUserSessionsRepository
            .GetTokenIdForUserId(ref, params.twitchRoomId)
        )
    ) {
      case Success(
            TwitchUserSessionsRepository.ReturnedTokenIdForUserId(Some(tokenId))
          ) =>
        TwitchChatbot.NewTokenId(tokenId)

      case Success(
            TwitchUserSessionsRepository.ReturnedTokenIdForUserId(None)
          ) =>
        TwitchChatbot.NoToken(None)

      case Failure(ex) =>
        TwitchChatbot.NoToken(Some(ex))
    }

    Behaviors.receiveAndLogMessage {
      case TwitchChatbot.Leave =>
        Behaviors.stopped

      case TwitchChatbot.NewTokenId(tokenId) =>
        ctx.ask[ArchieMateMediator.Command, BasicChatbotSettings](
          mediator,
          ref =>
            ArchieMateMediator.SendBasicChatbotSettingsRepositoryCommand(
              BasicChatbotSettingsRepository
                .GetSettings(ref, params.twitchRoomId)
            )
        ) {
          case Success(settings) =>
            TwitchChatbot.NewBasicChatbotSettings(settings)
          case Failure(ex) => TwitchChatbot.FailedToGetSettings(ex)
        }

        ctx.ask[ArchieMateMediator.Command, BuiltInCommandsSettings](
          mediator,
          ref =>
            ArchieMateMediator.SendBuiltInCommandsSettingsRepositoryCommand(
              BuiltInCommandsSettingsRepository
                .GetSettings(ref, params.twitchRoomId)
            )
        ) {
          case Success(settings) =>
            TwitchChatbot.NewBuiltInCommandsSettings(settings)
          case Failure(ex) => TwitchChatbot.FailedToGetSettings(ex)
        }

        ctx.ask[ArchieMateMediator.Command, CommandsSettings](
          mediator,
          ref =>
            ArchieMateMediator.SendCommandsSettingsRepositoryCommand(
              CommandsSettingsRepository
                .GetCommandsSettings(ref, params.twitchRoomId)
            )
        ) {
          case Success(settings) => TwitchChatbot.NewCommandsSettings(settings)
          case Failure(ex)       => TwitchChatbot.FailedToGetSettings(ex)
        }

        ctx.ask[ArchieMateMediator.Command, VariablesSettings](
          mediator,
          ref =>
            ArchieMateMediator.SendVariablesSettingsRepositoryCommand(
              VariablesSettingsRepository
                .GetVariablesSettings(ref, params.twitchRoomId)
            )
        ) {
          case Success(settings) => TwitchChatbot.NewVariablesSettings(settings)
          case Failure(ex)       => TwitchChatbot.FailedToGetSettings(ex)
        }

        ctx.ask[ArchieMateMediator.Command, TimersSettings](
          mediator,
          ref =>
            ArchieMateMediator.SendTimersSettingsRepositoryCommand(
              TimersSettingsRepository.GetSettings(ref, params.twitchRoomId)
            )
        ) {
          case Success(settings) => TwitchChatbot.NewTimersSettings(settings)
          case Failure(ex)       => TwitchChatbot.FailedToGetSettings(ex)
        }

        ctx.ask[ArchieMateMediator.Command, OverlaysSettings](
          mediator,
          ref =>
            ArchieMateMediator.SendOverlaysSettingsRepositoryCommand(
              OverlaysSettingsRepository.GetSettings(ref, params.twitchRoomId)
            )
        ) {
          case Success(settings) => TwitchChatbot.NewOverlaysSettings(settings)
          case Failure(ex)       => TwitchChatbot.FailedToGetSettings(ex)
        }

        ctx.ask[ArchieMateMediator.Command, AutomaticMessagesSettings](
          mediator,
          ref =>
            ArchieMateMediator.SendAutomaticMessagesSettingsRepositoryCommand(
              AutomaticMessagesSettingsRepository
                .GetSettings(ref, params.twitchRoomId)
            )
        ) {
          case Success(settings) =>
            TwitchChatbot.NewAutomaticMessagesSettings(settings)
          case Failure(ex) => TwitchChatbot.FailedToGetSettings(ex)
        }

        ctx.askWithStatus[
          ArchieMateMediator.Command,
          TwitchApiResponse.GetTokenUser
        ](
          mediator,
          ref =>
            ArchieMateMediator.SendTwitchApiClientCommand(
              TwitchApiClient.GetTokenUserFromTokenId(ref, tokenId)
            )
        ) {
          case Success(broadcaster) => TwitchChatbot.Broadcaster(broadcaster)
          case Failure(ex)          => TwitchChatbot.FailedToGetSettings(ex)
        }

        initial2(params.copy(tokenIdOption = Some(tokenId)))

      case TwitchChatbot.NoToken(None) =>
        ctx.log.error("No token available!")
        supervisor ! TwitchChatbotsSupervisor.AuthorizationNeeded(
          params.twitchRoomId
        )
        Behaviors.stopped

      case TwitchChatbot.NoToken(Some(ex)) =>
        ctx.log.error("Cannot retrieve token!", ex)
        Behaviors.stopped // TODO: Make the actor restart in this case as this can be a temporary problem

      case other =>
        ctx.log.warn(
          "Received unexpected message {} in initial1 {}",
          other,
          params
        )
        Behaviors.same
    }
  }

  private def initial2(
      params: InitialParameters
  ): Behavior[TwitchChatbot.Command] = (
    params.tokenIdOption,
    params.basicChatbotSettingsOption,
    params.builtInCommandsSettingsOption,
    params.commandsSettingsOption,
    params.variablesSettingsOption,
    params.timersSettingsOption,
    params.overlaysSettingsOption,
    params.automaticMessagesSettingsOption,
    params.broadcasterOption
  ) match {
    case (
          Some(tokenId),
          Some(basicChatbotSettings),
          Some(builtInCommandsSettings),
          Some(commandsSettings),
          Some(variablesSettings),
          Some(timersSettings),
          Some(overlaysSettings),
          Some(automaticMessagesSettings),
          Some(broadcaster)
        ) =>
      withSettings(
        tokenId,
        ChannelSettings(
          basicChatbotSettings,
          builtInCommandsSettings,
          commandsSettings,
          variablesSettings,
          timersSettings,
          overlaysSettings,
          automaticMessagesSettings
        ),
        broadcaster
      )

    case _ =>
      Behaviors.receiveAndLogMessage {
        case TwitchChatbot.NewBasicChatbotSettings(settings) =>
          initial2(params.copy(basicChatbotSettingsOption = Some(settings)))

        case TwitchChatbot.NewBuiltInCommandsSettings(settings) =>
          initial2(params.copy(builtInCommandsSettingsOption = Some(settings)))

        case TwitchChatbot.NewCommandsSettings(settings) =>
          initial2(params.copy(commandsSettingsOption = Some(settings)))

        case TwitchChatbot.NewVariablesSettings(settings) =>
          initial2(params.copy(variablesSettingsOption = Some(settings)))

        case TwitchChatbot.NewTimersSettings(settings) =>
          initial2(params.copy(timersSettingsOption = Some(settings)))

        case TwitchChatbot.NewOverlaysSettings(settings) =>
          initial2(params.copy(overlaysSettingsOption = Some(settings)))

        case TwitchChatbot.NewAutomaticMessagesSettings(settings) =>
          initial2(
            params.copy(automaticMessagesSettingsOption = Some(settings))
          )

        case TwitchChatbot.Broadcaster(broadcaster) =>
          initial2(params.copy(broadcasterOption = Some(broadcaster)))

        case TwitchChatbot.FailedToGetSettings(ex) =>
          Behaviors.stopped // TODO: Restart capability

        case other =>
          ctx.log.warn(
            "Received unexpected message {} in initial2 {}",
            other,
            params
          )
          Behaviors.same
      }
  }

  def withSettings(
      tokenId: String,
      channelSettings: ChannelSettings,
      broadcaster: GetTokenUser
  ): Behavior[TwitchChatbot.Command] = {
    val eventSubListener = ctx.spawn(
      EventSubListener(
        tokenId,
        broadcaster
      ),
      EventSubListener.actorName
    )

    val ircListener = ctx.spawn(
      IRCListener(broadcaster.login),
      IRCListener.actorName
    )

    askForMods(tokenId, broadcaster)
    askForVIPs(tokenId, broadcaster)
    askForSubs(tokenId, broadcaster)
    askForChatters(tokenId, broadcaster)
    askForStream(tokenId, broadcaster)

    initializing(
      InitializingParameters(
        tokenId,
        channelSettings,
        broadcaster,
        eventSubListener,
        ircListener
      )
    )
  }

  final case class InitializingParameters(
      tokenId: String,
      channelSettings: ChannelSettings,
      broadcaster: GetTokenUser,
      eventSubListener: ActorRef[EventSubListener.Command],
      ircListener: ActorRef[IRCListener.Command],
      mods: Option[GetModerators] = None,
      vips: Option[GetVIPs] = None,
      subs: Option[GetSubs] = None,
      chatters: Option[GetChatters] = None,
      stream: Option[Option[TwitchApi.Stream]] = None
  )

  private def initializing(
      params: InitializingParameters
  ): Behavior[TwitchChatbot.Command] = {
    if (
      params.mods.nonEmpty && params.vips.nonEmpty && params.subs.nonEmpty && params.chatters.nonEmpty && params.stream.nonEmpty
    ) {
      supervisor ! TwitchChatbotsSupervisor.JoinOK(params.broadcaster.id)

      val twitchCommandsService = ctx.spawn(
        TwitchCommandsService(),
        TwitchCommandsService.actorName
      )

      timers.startTimerAtFixedRate(
        TwitchChatbot.ChattersTimer,
        TwitchChatbot.ChattersTimer,
        1 minute
      )
      if (params.channelSettings.timersSettings.enabled) {
        timers.startTimerAtFixedRate(
          TwitchChatbot.MessageTimer,
          TwitchChatbot.MessageTimer,
          params.channelSettings.timersSettings.intervalMinutes minutes
        )
      }
      val mods = params.mods.get.data.toMapWithKey(_.user_id)
      val vips = params.vips.get.data.toMapWithKey(_.user_id)
      val subs = params.subs.get.data.toMapWithKey(_.user_id)
      val subsAsUser = subs.map((userId, sub) =>
        userId -> TwitchApi.User(sub.user_id, sub.user_login, sub.user_name)
      )
      val chatters = params.chatters.get.data.toMapWithKey(_.user_id)
      val usersState: Map[String, TwitchChatbot.UserState] =
        (mods ++ vips ++ subsAsUser ++ chatters).map { (userId, user) =>
          val flags = Seq(
            mods.get(userId).map(_ => TwitchChatbot.UserFlag.Mod),
            vips.get(userId).map(_ => TwitchChatbot.UserFlag.Vip),
            subs.get(userId).map(_ => TwitchChatbot.UserFlag.Sub),
            chatters.get(userId).map(_ => TwitchChatbot.UserFlag.Online),
            if (params.broadcaster.id == userId)
              Some(TwitchChatbot.UserFlag.Streamer)
            else None,
            if (settings.twitchIRCUsername == user.user_login)
              Some(TwitchChatbot.UserFlag.Ignore)
            else None
          ).filter(_.nonEmpty).map(_.get).toSet
          val subInfo = subs.get(userId)
          userId -> TwitchChatbot.UserState(user, flags, subInfo)
        }
      buffer.unstashAll(
        operational(
          TwitchChatbot.OperationalParameters(
            params.tokenId,
            params.channelSettings,
            params.broadcaster,
            params.eventSubListener,
            params.ircListener,
            twitchCommandsService,
            usersState,
            params.stream.get
          )
        )
      )
    } else {
      Behaviors.receiveAndLogMessage {
        case TwitchChatbot.Mods(Some(mods)) =>
          initializing(params.copy(mods = Some(mods)))

        case TwitchChatbot.VIPs(Some(vips)) =>
          initializing(params.copy(vips = Some(vips)))

        case TwitchChatbot.Subs(Some(subs)) =>
          initializing(params.copy(subs = Some(subs)))

        case TwitchChatbot.Chatters(Some(chatters)) =>
          initializing(params.copy(chatters = Some(chatters)))

        case TwitchChatbot.Stream(Some(stream)) =>
          initializing(params.copy(stream = Some(stream)))

        case TwitchChatbot.Mods(None) | TwitchChatbot.VIPs(None) |
            TwitchChatbot.Subs(None) | TwitchChatbot.Chatters(None) |
            TwitchChatbot.Stream(None) =>
          supervisor ! TwitchChatbotsSupervisor.AuthorizationNeeded(
            params.broadcaster.id
          )
          Behaviors.stopped

        case other =>
          buffer.stash(other)
          Behaviors.same
      }
    }
  }

  private def operational(
      params: TwitchChatbot.OperationalParameters
  ): Behavior[TwitchChatbot.Command] = Behaviors.receiveAndLogMessage {
    case TwitchChatbot.ChattersTimer =>
      askForChatters(params.tokenId, params.broadcaster)
      Behaviors.same

    case TwitchChatbot.MessageTimer =>
      val timersSettings = params.channelSettings.timersSettings
      if (params.messagesAfterLastTimer >= timersSettings.minimumMessages) {
        val tempLastTimers =
          if (
            params.lastTimers.length >= timersSettings.timersUntilNextAllowedRepeat
          ) {
            params.lastTimers.tail
          } else {
            params.lastTimers
          }
        val availableCommands: List[String] = timersSettings.commands.filter {
          commandName =>
            tempLastTimers.forall {
              case Left(unavailableCommandName) =>
                unavailableCommandName != commandName
              case Right(_) => true
            }
        }.toList
        val availableManualTimers: List[ManualTimer] =
          timersSettings.manualTimers.filter { manualTimer =>
            tempLastTimers.forall {
              case Left(_) => true
              case Right(unavailableManualTimerId) =>
                unavailableManualTimerId != manualTimer.id
            }
          }
        val nextTimerId = randomProvider.between(
          0,
          availableCommands.length + availableManualTimers.length
        )
        val nextTimer: Either[String, String] = {
          if (nextTimerId < availableCommands.length) {
            Left(availableCommands(nextTimerId))
          } else {
            Right(
              availableManualTimers(nextTimerId - availableCommands.length).id
            )
          }
        }
        val newLastTimers = tempLastTimers :+ nextTimer
        val nextParams =
          params.copy(messagesAfterLastTimer = 0, lastTimers = newLastTimers)
        nextParams.twitchCommandsService ! TwitchCommandsService
          .RespondForTimer(nextParams)
      }

      Behaviors.same

    case TwitchChatbot.Chatters(Some(chattersResponse)) =>
      val currentChatters = chattersResponse.data

      val newUsers =
        currentChatters.toMapWithKey(_.user_id).map { (userId, user) =>
          val existingUserOption = params.users.get(userId)
          userId -> TwitchChatbot.UserState(
            user,
            existingUserOption
              .map(_.flags)
              .getOrElse(Set.empty) + TwitchChatbot.UserFlag.Online,
            existingUserOption.flatMap(_.subInfo),
            existingUserOption.map(_.afkConversations).getOrElse(Set.empty)
          )
        }

      params.channelSettings.automaticMessagesSettings.knownGreets match {
        case Some(knownGreetsSettings) =>
          newUsers
            .filter((_, userState) =>
              shouldGreet(
                userState,
                knownGreetsSettings
              )
            )
            .keySet
            .diff(params.users.keySet)
            .foreach { userId =>
              val (greet, name) =
                getGreetAndName(params.users(userId), knownGreetsSettings)
              val finalGreet = greet.replaceAll("name".asVariableRegex, name)
              params.ircListener ! IRCListener.SendMessage(finalGreet)
            }

        case None =>
      }

      operational(
        params.copy(
          users = params.users ++ newUsers
        )
      )

    case TwitchChatbot.Chatters(None) =>
      supervisor ! TwitchChatbotsSupervisor.AuthorizationNeeded(
        params.broadcaster.id
      )
      Behaviors.stopped

    case TwitchChatbot.EventSubEvent(e: eventsub.ChannelUpdateEvent) =>
      params.stream match {
        case Some(_) => askForStream(params.tokenId, params.broadcaster)
        case None    =>
      }

      operational(
        params.copy(stream =
          params.stream.map(
            _.copy(
              userId = e.broadcasterUserId,
              userLogin = e.broadcasterUserLogin,
              userName = e.broadcasterUserName,
              gameId = e.categoryId,
              gameName = e.categoryName,
              title = e.title,
              language = e.language
            )
          )
        )
      )

    case TwitchChatbot.Stream(Some(stream)) =>
      operational(params.copy(stream = stream))

    case TwitchChatbot.Stream(None) =>
      supervisor ! TwitchChatbotsSupervisor.AuthorizationNeeded(
        params.broadcaster.id
      )
      Behaviors.stopped

    case TwitchChatbot.EventSubEvent(e: eventsub.ChannelFollowEvent) =>
      params.channelSettings.automaticMessagesSettings.follow.foreach { msg =>
        val msgWithUser = msg.replaceAll("user".asVariableRegex, e.userName)
        params.ircListener ! IRCListener.SendMessage(msgWithUser)
      }
      Behaviors.same

    case TwitchChatbot.EventSubEvent(_: eventsub.ChannelAdBreakBeginEvent) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(_: eventsub.ChannelChatClearEvent) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(
          _: eventsub.ChannelChatClearUserMessagesEvent
        ) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(e: eventsub.ChannelChatMessageEvent)
        if params.users.exists((userId, userState) =>
          userId == e.chatterUserId && userState.flags
            .contains(TwitchChatbot.UserFlag.Ignore)
        ) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(e: eventsub.ChannelChatMessageEvent) =>
      val userState = params.users.getOrElse(
        e.chatterUserId,
        TwitchChatbot.UserState(
          TwitchApi.User(e.chatterUserId, e.chatterUserLogin, e.chatterUserName)
        )
      )
      params.channelSettings.automaticMessagesSettings.knownGreets match {
        case Some(knownGreetsSettings)
            if shouldGreet(
              userState,
              knownGreetsSettings
            ) =>
          val (greet, name) =
            getGreetAndName(userState, knownGreetsSettings)
          val finalGreet = greet.replaceAll("name".asVariableRegex, name)
          params.ircListener ! IRCListener.SendMessage(finalGreet)

        case _ =>
      }

      val mentionedAfks = params.users
        .filter((userId, userState) =>
          userState.flags.contains(
            TwitchChatbot.UserFlag.Afk
          ) && e.message.text.toLowerCase.contains(userState.user.user_login)
        )
        .map((userId, userState) =>
          userId -> userState
            .copy(afkConversations = userState.afkConversations + e.messageId)
        )

      mentionedAfks.size match {
        case 1 =>
          IRCListener.SendReplyMessage(
            s"@${e.chatterUserName}, ${mentionedAfks(mentionedAfks.keySet.head).user.user_name} is AFK. When they get back I'll notify them.",
            e.messageId
          )
        case 2 =>
          IRCListener.SendReplyMessage(
            s"@${e.chatterUserName}, the following users are AFK: ${mentionedAfks.map(_._2.user.user_name).mkString} - when they get back I'll notify them.",
            e.messageId
          )
      }

      val formerAfk: Map[String, TwitchChatbot.UserState] =
        if (
          params.users.exists((userId, userState) =>
            userId == e.chatterUserId && userState.flags
              .contains(TwitchChatbot.UserFlag.Afk)
          )
        ) {
          val userState = params.users(e.chatterUserId)
          userState.afkConversations.foreach { messageId =>
            params.ircListener ! IRCListener.SendReplyMessage(
              s"@${userState.user.user_name}",
              messageId
            )
          }
          Map(
            e.chatterUserId -> userState.copy(
              flags = userState.flags - TwitchChatbot.UserFlag.Afk,
              afkConversations = Set.empty
            )
          )
        } else {
          Map.empty
        }

      val newParams = params.copy(
        users = params.users ++ mentionedAfks ++ formerAfk
      )

      newParams.twitchCommandsService ! TwitchCommandsService.RespondToCommand(
        newParams,
        e
      )

      operational(newParams)

    case TwitchChatbot.NewBasicChatbotSettings(settings) =>
      operational(
        params.copy(channelSettings =
          params.channelSettings.copy(basicChatbotSettings = settings)
        )
      )

    case TwitchChatbot.NewBuiltInCommandsSettings(settings) =>
      operational(
        params.copy(channelSettings =
          params.channelSettings.copy(builtInCommandsSettings = settings)
        )
      )

    case TwitchChatbot.NewCommandsSettings(settings) =>
      operational(
        params.copy(channelSettings =
          params.channelSettings.copy(commandsSettings = settings)
        )
      )

    case TwitchChatbot.NewVariablesSettings(settings) =>
      operational(
        params.copy(channelSettings =
          params.channelSettings.copy(variablesSettings = settings)
        )
      )

    case TwitchChatbot.NewTimersSettings(settings) =>
      if (settings.enabled) {
        timers.startTimerAtFixedRate(
          TwitchChatbot.MessageTimer,
          TwitchChatbot.MessageTimer,
          params.channelSettings.timersSettings.intervalMinutes minutes
        )
      } else {
        timers.cancel(TwitchChatbot.MessageTimer)
      }
      operational(
        params.copy(channelSettings =
          params.channelSettings.copy(timersSettings = settings)
        )
      )

    case TwitchChatbot.NewOverlaysSettings(settings) =>
      operational(
        params.copy(channelSettings =
          params.channelSettings.copy(overlaysSettings = settings)
        )
      )

    case TwitchChatbot.NewAutomaticMessagesSettings(settings) =>
      operational(
        params.copy(channelSettings =
          params.channelSettings.copy(automaticMessagesSettings = settings)
        )
      )

    case TwitchChatbot.Broadcaster(broadcaster) =>
      operational(params.copy(broadcaster = broadcaster))

    case TwitchChatbot.EventSubEvent(
          _: eventsub.ChannelChatMessageDeleteEvent
        ) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(
          e: eventsub.ChannelChatNotificationEvent
        ) =>
      e.noticeType match {
        case "sub" =>
          val sub = e.sub.get
          val tier = sub.subTier.asTwitchSubTier(sub.isPrime)
          val months = sub.durationMonths

          // TODO: send message
          val userState = params.users.getOrElse(
            e.chatterUserId,
            TwitchChatbot.UserState(
              TwitchApi
                .User(e.chatterUserId, e.chatterUserLogin, e.chatterUserName),
              Set(TwitchChatbot.UserFlag.Online)
            )
          )
          val subbedUser = e.chatterUserId -> userState.copy(
            flags = userState.flags + TwitchChatbot.UserFlag.Sub,
            subInfo = Some(
              TwitchApi.SubbedUser(
                broadcaster_id = e.broadcasterUserId,
                broadcaster_login = e.broadcasterUserLogin,
                broadcaster_name = e.broadcasterUserName,
                gifter_id = "",
                gifter_login = "",
                gifter_name = "",
                is_gift = false,
                tier = sub.subTier,
                plan_name = "",
                user_id = e.chatterUserId,
                user_login = e.chatterUserLogin,
                user_name = e.chatterUserName
              )
            )
          )

          operational(
            params.copy(users = params.users + subbedUser)
          )

        case "resub" =>
          val resub = e.resub.get
          val tier = resub.subTier.asTwitchSubTier(resub.isPrime)
          val months = resub.cumulativeMonths

          // TODO: send message
          val userState = params.users.getOrElse(
            e.chatterUserId,
            TwitchChatbot.UserState(
              TwitchApi
                .User(e.chatterUserId, e.chatterUserLogin, e.chatterUserName),
              Set(TwitchChatbot.UserFlag.Online)
            )
          )
          val subbedUser = e.chatterUserId -> userState.copy(
            flags = userState.flags + TwitchChatbot.UserFlag.Sub,
            subInfo = Some(
              TwitchApi.SubbedUser(
                broadcaster_id = e.broadcasterUserId,
                broadcaster_login = e.broadcasterUserLogin,
                broadcaster_name = e.broadcasterUserName,
                gifter_id = resub.gifterUserId.getOrElse(""),
                gifter_login = resub.gifterUserLogin.getOrElse(""),
                gifter_name = resub.gifterUserName.getOrElse(""),
                is_gift = resub.isGift,
                tier = resub.subTier,
                plan_name = "",
                user_id = e.chatterUserId,
                user_login = e.chatterUserLogin,
                user_name = e.chatterUserName
              )
            )
          )

          operational(
            params.copy(
              users = params.users + subbedUser
            )
          )

        case "sub_gift" =>
          val subGift = e.subGift.get
          val tier = subGift.subTier.asTwitchSubTier(isPrime = false)
          val totalGifts = subGift.cumulativeTotal

          // TODO: send message only if not part of community sub gift
          val userState = params.users.getOrElse(
            subGift.recipientUserId,
            TwitchChatbot.UserState(
              TwitchApi
                .User(
                  subGift.recipientUserId,
                  subGift.recipientUserLogin,
                  subGift.recipientUserName
                ),
              Set(TwitchChatbot.UserFlag.Online)
            )
          )

          val subbedUser = subGift.recipientUserId -> userState.copy(
            flags = userState.flags + TwitchChatbot.UserFlag.Sub,
            subInfo = Some(
              TwitchApi.SubbedUser(
                broadcaster_id = e.broadcasterUserId,
                broadcaster_login = e.broadcasterUserLogin,
                broadcaster_name = e.broadcasterUserName,
                gifter_id = e.chatterUserId,
                gifter_login = e.chatterUserLogin,
                gifter_name = e.chatterUserName,
                is_gift = true,
                tier = subGift.subTier,
                plan_name = "",
                user_id = subGift.recipientUserId,
                user_login = subGift.recipientUserLogin,
                user_name = subGift.recipientUserName
              )
            )
          )

          operational(
            params.copy(
              users = params.users + subbedUser
            )
          )

        case "community_sub_gift" =>
          val communitySubGift = e.communitySubGift.get
          val tier = communitySubGift.subTier.asTwitchSubTier(isPrime = false)
          val totalGifts = communitySubGift.cumulativeTotal

          // TODO: send message

          Behaviors.same

        case "gift_paid_upgrade" =>
          val giftPaidUpgrade = e.giftPaidUpgrade.get

          // TODO: send message after adding automatic message setting

          Behaviors.same

        case "prime_paid_upgrade" =>
          val primePaidUpgrade = e.primePaidUpgrade.get

          // TODO: send message after adding automatic message setting

          Behaviors.same

        case "pay_it_forward" =>
          val payItForward = e.payItForward.get

          // TODO: send message after adding automatic message setting

          Behaviors.same

        case "raid" =>
          val raid = e.raid.get

          // TODO: send message after adding automatic message setting

          Behaviors.same

        case "unraid" =>
          val unraid = e.unraid.get

          // TODO: send message after adding automatic message setting

          Behaviors.same

        case "announcement" =>
          val announcement = e.announcement.get

          // TODO: send message after adding automatic message setting

          Behaviors.same

        case "bits_badge_tier" =>
          val bitsBadgeTier = e.bitsBadgeTier.get

          // TODO: send message after adding automatic message setting

          Behaviors.same

        case "charity_donation" =>
          val charityDonation = e.charityDonation.get

          // TODO: send message after adding automatic message setting

          Behaviors.same

        case other =>
          ctx.log.error("Unexpected notice type {}", other)
          Behaviors.same
      }

    case TwitchChatbot.EventSubEvent(e: eventsub.ChannelSubscribeEvent) =>
      ctx.log.debug(
        "User {} added as a new subscriber after ChannelSubscribeEvent",
        e.userName
      )
      val userState = params.users.getOrElse(
        e.userId,
        TwitchChatbot.UserState(
          TwitchApi
            .User(e.userId, e.userLogin, e.userName),
          Set(TwitchChatbot.UserFlag.Online)
        )
      )
      val subbedUser = e.userId -> userState.copy(
        flags = userState.flags + TwitchChatbot.UserFlag.Sub,
        subInfo = Some(
          TwitchApi.SubbedUser(
            broadcaster_id = e.broadcasterUserId,
            broadcaster_login = e.broadcasterUserLogin,
            broadcaster_name = e.broadcasterUserName,
            gifter_id = "",
            gifter_login = "",
            gifter_name = "",
            is_gift = e.isGift,
            tier = e.tier,
            plan_name = "",
            user_id = e.userId,
            user_login = e.userLogin,
            user_name = e.userName
          )
        )
      )
      operational(
        params.copy(
          users = params.users + subbedUser
        )
      )

    case TwitchChatbot.EventSubEvent(e: eventsub.ChannelSubscriptionEndEvent) =>
      ctx.log.debug("User {} is no longer a subscriber", e.userName)
      val userState = params.users.getOrElse(
        e.userId,
        TwitchChatbot.UserState(
          TwitchApi
            .User(e.userId, e.userLogin, e.userName),
          Set(TwitchChatbot.UserFlag.Online)
        )
      )
      val unsubbedUser = e.userId -> userState.copy(
        flags = userState.flags - TwitchChatbot.UserFlag.Sub,
        subInfo = None
      )
      operational(
        params.copy(
          users = params.users + unsubbedUser
        )
      )

    case TwitchChatbot.EventSubEvent(
          e: eventsub.ChannelSubscriptionGiftEvent
        ) =>
      ctx.log.debug("User {} gifted subscription(s)", e.userName)

      val gift =
        if (e.isAnonymous)
          params.channelSettings.automaticMessagesSettings.anonymousSubscriptionGift
        else
          params.channelSettings.automaticMessagesSettings.subscriptionGift

      gift.foreach { g =>
        val key = g.keys.filter(_ <= e.total).max
        val message =
          g(key).replaceAll("gifter".asVariableRegex, e.userName.getOrElse(""))
        params.ircListener ! IRCListener.SendMessage(message)
      }

      Behaviors.same

    case TwitchChatbot.EventSubEvent(
          e: eventsub.ChannelSubscriptionMessageEvent
        ) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(e: eventsub.ChannelCheerEvent) =>
      val userInMessage =
        if (e.isAnonymous) "An anonymous user" else s"User ${e.userName}"
      ctx.log.debug("{} cheered for {} bit(s)", userInMessage, e.bits)
      val cheer = if (e.isAnonymous) {
        params.channelSettings.automaticMessagesSettings.anonymousCheer
      } else {
        params.channelSettings.automaticMessagesSettings.cheer
      }
      cheer.foreach { c =>
        val key = c.keys.filter(_ <= e.bits).max
        val message =
          c(key).replaceAll("user".asVariableRegex, e.userName.getOrElse(""))
        params.ircListener ! IRCListener.SendMessage(message)
      }
      Behaviors.same

    case TwitchChatbot.EventSubEvent(e: eventsub.ChannelRaidEvent) =>
      val (otherBroadcaster, msgOption) =
        (e.fromBroadcasterUserId, e.toBroadcasterUserId) match {
          case (params.broadcaster.id, _) =>
            (
              e.toBroadcasterUserName,
              params.channelSettings.automaticMessagesSettings.outgoingRaid
            )
          case (_, params.broadcaster.id) =>
            (
              e.fromBroadcasterUserName,
              params.channelSettings.automaticMessagesSettings.incomingRaid
            )
          case _ => ("", None)
        }
      msgOption.foreach { msg =>
        val message =
          msg.replaceAll("broadcaster".asVariableRegex, otherBroadcaster)
        params.ircListener ! IRCListener.SendMessage(message)
      }
      Behaviors.same

    case TwitchChatbot.EventSubEvent(e: eventsub.ChannelBanEvent) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(e: eventsub.ChannelUnbanEvent) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(e: eventsub.ChannelModeratorAddEvent) =>
      ctx.log.debug("User {} added as a new moderator", e.userName)
      val userState = params.users.getOrElse(
        e.userId,
        TwitchChatbot.UserState(
          TwitchApi.User(e.userId, e.userLogin, e.userName)
        )
      )
      val newMod = e.userId -> userState.copy(
        user = TwitchApi.User(e.userId, e.userLogin, e.userName),
        flags = userState.flags + TwitchChatbot.UserFlag.Mod
      )

      operational(
        params.copy(
          users = params.users + newMod
        )
      )

    case TwitchChatbot.EventSubEvent(e: eventsub.ChannelModeratorRemoveEvent) =>
      ctx.log.debug("User {} is no longer a moderator", e.userName)
      val userState = params.users(e.userId)
      val formerMod = e.userId -> userState.copy(flags =
        userState.flags - TwitchChatbot.UserFlag.Mod
      )
      operational(
        params.copy(
          users = params.users + formerMod
        )
      )

    case TwitchChatbot.EventSubEvent(
          _: eventsub.ChannelPointsAutomaticRewardRedemptionAddEvent
        ) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(
          _: eventsub.ChannelPointsCustomRewardAddEvent
        ) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(
          _: eventsub.ChannelPointsCustomRewardUpdateEvent
        ) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(
          _: eventsub.ChannelPointsCustomRewardRemoveEvent
        ) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(
          _: eventsub.ChannelPointsCustomRewardRedemptionAddEvent
        ) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(
          _: eventsub.ChannelPointsCustomRewardRedemptionUpdateEvent
        ) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(_: eventsub.ChannelPollBeginEvent) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(_: eventsub.ChannelPollProgressEvent) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(_: eventsub.ChannelPollEndEvent) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(_: eventsub.ChannelPredictionBeginEvent) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(
          _: eventsub.ChannelPredictionProgressEvent
        ) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(_: eventsub.ChannelPredictionLockEvent) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(_: eventsub.ChannelPredictionEndEvent) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(e: eventsub.ChannelVIPAddEvent) =>
      ctx.log.debug("User {} added as a new VIP", e.userName)
      val userState = params.users.getOrElse(
        e.userId,
        TwitchChatbot.UserState(
          TwitchApi.User(e.userId, e.userLogin, e.userName)
        )
      )
      val newVip = e.userId -> userState.copy(
        user = TwitchApi.User(e.userId, e.userLogin, e.userName),
        flags = userState.flags + TwitchChatbot.UserFlag.Vip
      )

      operational(
        params.copy(
          users = params.users + newVip
        )
      )

    case TwitchChatbot.EventSubEvent(e: eventsub.ChannelVIPRemoveEvent) =>
      ctx.log.debug("User {} is no longer a VIP", e.userName)
      val userState = params.users(e.userId)
      val formerVip = e.userId -> userState.copy(flags =
        userState.flags - TwitchChatbot.UserFlag.Vip
      )
      operational(
        params.copy(
          users = params.users + formerVip
        )
      )

    case TwitchChatbot.EventSubEvent(_: eventsub.ChannelGoalBeginEvent) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(_: eventsub.ChannelGoalProgressEvent) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(_: eventsub.ChannelGoalEndEvent) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(_: eventsub.ChannelHypeTrainBeginEvent) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(
          _: eventsub.ChannelHypeTrainProgressEvent
        ) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(_: eventsub.ChannelHypeTrainEndEvent) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(_: eventsub.ChannelShoutoutReceiveEvent) =>
      Behaviors.same

    case TwitchChatbot.EventSubEvent(e: eventsub.StreamOnlineEvent) =>
      ctx
        .askWithStatus[ArchieMateMediator.Command, TwitchApiResponse.GetStream](
          mediator,
          ref =>
            ArchieMateMediator.SendTwitchApiClientCommand(
              TwitchApiClient
                .GetStream(ref, params.tokenId, params.broadcaster.id)
            )
        ) {
          case Success(GetStream(streams, _)) =>
            val optionStream = streams.lastOption
            optionStream.foreach { stream =>
              params.channelSettings.automaticMessagesSettings.streamStart
                .foreach { msg =>
                  val message = msg
                    .replaceAll("game".asVariableRegex, stream.gameName)
                    .replaceAll("title".asVariableRegex, stream.title)
                  params.ircListener ! IRCListener.SendMessage(message)
                }
            }
            TwitchChatbot.Stream(Some(optionStream))

          case Failure(ex) =>
            ctx.log.error(
              "Could not retrieve whether the channel {} is live",
              params.broadcaster.login,
              ex
            )
            TwitchChatbot.Stream(None)
        }
      operational(
        params.copy(stream =
          Some(
            TwitchApi.Stream(
              id = e.id,
              userId = e.broadcasterUserId,
              userLogin = e.broadcasterUserLogin,
              userName = e.broadcasterUserName,
              gameId = "",
              gameName = "",
              `type` = e.`type`,
              title = "",
              tags = Nil,
              viewerCount = 0,
              startedAt = e.startedAt,
              language = "",
              thumbnailUrl = "",
              tagIds = Nil,
              isMature = false
            )
          )
        )
      )

    case TwitchChatbot.EventSubEvent(e: eventsub.StreamOfflineEvent) =>
      params.channelSettings.automaticMessagesSettings.streamEnd.foreach {
        msg =>
          params.ircListener ! IRCListener.SendMessage(msg)
      }
      operational(params.copy(stream = None))

    case TwitchChatbot.EventSubEvent(e: eventsub.UserUpdateEvent) =>
      operational(
        params.copy(
          broadcaster = params.broadcaster.copy(
            login = e.userLogin,
            display_name = e.userName,
            description = e.description
          )
        )
      )

    case TwitchChatbot.Afk(chatterUserId) =>
      val userState = params.users(chatterUserId)
      val afkUser = chatterUserId -> userState.copy(flags =
        userState.flags + TwitchChatbot.UserFlag.Afk
      )
      operational(
        params.copy(
          users = params.users + afkUser
        )
      )

    case msg =>
      ctx.log.warn("Ignoring message {}", msg)
      Behaviors.same
  }

  private def askForMods(tokenId: String, broadcaster: GetTokenUser): Unit = ctx
    .askWithStatus[ArchieMateMediator.Command, TwitchApiResponse.GetModerators](
      mediator,
      ref =>
        ArchieMateMediator.SendTwitchApiClientCommand(
          TwitchApiClient.GetModerators(ref, tokenId, broadcaster.id)
        )
    ) {
      case Success(mods: GetModerators) =>
        TwitchChatbot.Mods(Some(mods))

      case Failure(ex) =>
        ctx.log.error(
          "Could not retrieve moderators of channel {}",
          broadcaster.login,
          ex
        )
        TwitchChatbot.Mods(None)
    }

  private def askForVIPs(tokenId: String, broadcaster: GetTokenUser): Unit =
    ctx.askWithStatus[ArchieMateMediator.Command, TwitchApiResponse.GetVIPs](
      mediator,
      ref =>
        ArchieMateMediator.SendTwitchApiClientCommand(
          TwitchApiClient.GetVIPs(ref, tokenId, broadcaster.id)
        )
    ) {
      case Success(vips: GetVIPs) =>
        TwitchChatbot.VIPs(Some(vips))

      case Failure(ex) =>
        ctx.log.error(
          "Could not retrieve VIPs of channel {}",
          broadcaster.login,
          ex
        )
        TwitchChatbot.VIPs(None)
    }

  private def askForSubs(tokenId: String, broadcaster: GetTokenUser): Unit =
    ctx.askWithStatus[ArchieMateMediator.Command, TwitchApiResponse.GetSubs](
      mediator,
      ref =>
        ArchieMateMediator.SendTwitchApiClientCommand(
          TwitchApiClient.GetSubs(ref, tokenId, broadcaster.id)
        )
    ) {
      case Success(subs: GetSubs) =>
        TwitchChatbot.Subs(Some(subs))

      case Failure(ex) =>
        ctx.log.error(
          "Could not retrieve subs of channel {}",
          broadcaster.login,
          ex
        )
        TwitchChatbot.Subs(None)
    }

  private def askForChatters(tokenId: String, broadcaster: GetTokenUser): Unit =
    ctx
      .askWithStatus[ArchieMateMediator.Command, TwitchApiResponse.GetChatters](
        mediator,
        ref =>
          ArchieMateMediator.SendTwitchApiClientCommand(
            TwitchApiClient
              .GetChatters(ref, tokenId, broadcaster.id, broadcaster.id)
          )
      ) {
        case Success(chatters: GetChatters) =>
          TwitchChatbot.Chatters(Some(chatters))

        case Failure(ex) =>
          ctx.log.error(
            "Could not retrieve chatters of channel {}",
            broadcaster.login,
            ex
          )
          TwitchChatbot.Chatters(None)
      }

  private def askForStream(tokenId: String, broadcaster: GetTokenUser): Unit =
    ctx.askWithStatus[ArchieMateMediator.Command, TwitchApiResponse.GetStream](
      mediator,
      ref =>
        ArchieMateMediator.SendTwitchApiClientCommand(
          TwitchApiClient.GetStream(ref, tokenId, broadcaster.id)
        )
    ) {
      case Success(GetStream(List(stream), _)) =>
        TwitchChatbot.Stream(Some(Some(stream)))

      case Success(GetStream(Nil, _)) =>
        TwitchChatbot.Stream(Some(None))

      case Success(GetStream(streams, _)) =>
        ctx.log.warn(
          "Received stream response for channel {} with unexpected amount of streams when expected one stream or none: {}",
          broadcaster.login,
          streams
        )
        TwitchChatbot.Stream(Some(streams.lastOption))

      case Failure(ex) =>
        ctx.log.error(
          "Could not retrieve whether channel {} is live",
          broadcaster.login,
          ex
        )
        TwitchChatbot.Stream(Some(None))
    }

  private def shouldGreet(
      userState: TwitchChatbot.UserState,
      settings: KnownGreetsSettings
  ): Boolean = settings.mode match {
    case KnownGreetsMode.All      => true
    case KnownGreetsMode.Mods     => isMod(userState)
    case KnownGreetsMode.ModsVips => isMod(userState) || isVip(userState)
    case KnownGreetsMode.ModsVipsSubs =>
      isMod(userState) || isVip(userState) || isSub(userState)
  }

  private def isMod(
      userState: TwitchChatbot.UserState
  ): Boolean =
    userState.flags.contains(TwitchChatbot.UserFlag.Mod)
  private def isVip(
      userState: TwitchChatbot.UserState
  ): Boolean =
    userState.flags.contains(TwitchChatbot.UserFlag.Vip)
  private def isSub(
      userState: TwitchChatbot.UserState
  ): Boolean =
    userState.flags.contains(TwitchChatbot.UserFlag.Sub)

  private def getGreetAndName(
      userState: TwitchChatbot.UserState,
      settings: KnownGreetsSettings
  ): (String, String) = {
    val greet = settings.specificGreets.getOrElse(
      userState.user.user_id,
      settings.standardGreets.randomOrDefault("Hey")
    )
    val name =
      settings.specificNames.getOrElse(
        userState.user.user_id,
        s"@${userState.user.user_name}"
      )
    (greet, name)
  }
}
