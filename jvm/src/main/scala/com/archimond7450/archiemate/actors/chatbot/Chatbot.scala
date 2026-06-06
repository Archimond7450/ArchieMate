package com.archimond7450.archiemate.actors.chatbot

import com.archimond7450.archiemate.actors
import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.chatbot.Chatbot.UserFlag
import com.archimond7450.archiemate.actors.kick.api.KickApiClient
import com.archimond7450.archiemate.actors.repositories.sessions.{
  KickUserSessionsRepository,
  TwitchUserSessionsRepository
}
import com.archimond7450.archiemate.actors.repositories.settings.{
  AutomaticMessagesSettingsRepository,
  BasicChatbotSettingsRepository,
  BuiltInCommandsSettingsRepository,
  CommandsSettingsRepository,
  OverlaysSettingsRepository,
  PollsRepository,
  PredictionsRepository,
  TimersSettingsRepository,
  VariablesSettingsRepository
}
import com.archimond7450.archiemate.actors.services.{
  TwitchApiPaginationHandlerService,
  TwitchCommandsService
}
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
import com.archimond7450.archiemate.http.Polls.ChannelPolls
import com.archimond7450.archiemate.http.Predictions.ChannelPredictions
import com.archimond7450.archiemate.kick.api.{KickApiRequest, KickApiResponse}
import com.archimond7450.archiemate.kick.webhooks.KickWebhooks
import com.archimond7450.archiemate.providers.{RandomProvider, TimeProvider}
import com.archimond7450.archiemate.twitch.api.{TwitchApi, TwitchApiResponse}
import com.archimond7450.archiemate.twitch.api.TwitchApiResponse.{
  GetChannelFollowers,
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
import org.apache.pekko.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import org.apache.pekko.util.Timeout

import java.time.temporal.ChronoUnit
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

object Chatbot {
  sealed trait Command
  case object Leave extends Command
  final case class NewTwitchTokenId(tokenId: String) extends Command
  final case class NewKickTokenId(tokenIdOption: Option[String]) extends Command
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
  final case class NewPolls(polls: ChannelPolls) extends Command
  final case class NewPredictions(predictions: ChannelPredictions)
      extends Command
  final case class Broadcaster(broadcaster: GetTokenUser) extends Command
  private final case class FailedToGetSettings(ex: Throwable) extends Command
  final case class Mods(mods: Option[GetModerators]) extends Command
  final case class VIPs(vips: Option[GetVIPs]) extends Command
  final case class Subs(subs: Option[GetSubs]) extends Command
  final case class Followers(followers: Option[GetChannelFollowers])
      extends Command
  final case class Chatters(chatters: Option[GetChatters]) extends Command
  final case class Stream(stream: Option[Option[TwitchApi.Stream]])
      extends Command // TODO: Change outer Option to Try
  final case class PollHistory(polls: Option[List[TwitchApi.Poll]])
      extends Command
  final case class PredictionHistory(
      predictions: Option[List[TwitchApi.Prediction]]
  ) extends Command
  final case class EventSubEvent(event: eventsub.Event) extends Command
  final case class NewKickWebhook(webhook: KickWebhooks.KickWebhook)
      extends Command
  final case class Afk(chatterUserId: String) extends Command
  private case object ChattersTimer extends Command
  private case object MessageTimer extends Command
  private case object AskForKickWebhooks extends Command
  private final case class KickWebhooksSubscriptions(
      trySubscriptions: Try[List[KickApiResponse.GetEventsSubscriptionsData]]
  ) extends Command

  sealed trait UserFlag
  object UserFlag {
    object Streamer extends UserFlag
    object Mod extends UserFlag
    object Vip extends UserFlag
    object Sub extends UserFlag
    object Online extends UserFlag
    object DontGreet extends UserFlag
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
      twitchTokenId: String,
      kickTokenIdOption: Option[String],
      channelSettings: ChannelSettings,
      polls: ChannelPolls,
      currentPoll: Option[TwitchApi.Poll],
      predictions: ChannelPredictions,
      currentPrediction: Option[TwitchApi.Prediction],
      broadcaster: GetTokenUser,
      eventSubListener: ActorRef[EventSubListener.Command],
      ircListener: ActorRef[IRCListener.Command],
      twitchCommandsService: ActorRef[TwitchCommandsService.Command],
      users: Map[String, UserState],
      followers: Map[String, TwitchApi.UserFollowage],
      stream: Option[TwitchApi.Stream],
      messagesAfterLastTimer: Int = 0,
      lastTimers: Seq[Either[String, String]] = Seq.empty
  )

  def apply(twitchRoomId: String)(using
      supervisor: ActorRef[ChatbotsSupervisor.Command],
      mediator: ActorRef[ArchieMateMediator.Command],
      decoder: IncomingMessageDecoder,
      encoder: OutgoingMessageEncoder,
      randomProvider: RandomProvider,
      timeProvider: TimeProvider,
      settings: Settings
  ): Behavior[Command] = Behaviors
    .supervise[Command] {
      Behaviors.withStash(100) { buffer =>
        given StashBuffer[Command] = buffer
        Behaviors.setup { ctx =>
          given ActorContext[Command] = ctx
          Behaviors.withTimers { timers =>
            given TimerScheduler[Command] = timers
            new Chatbot(twitchRoomId).initial
          }
        }
      }
    }
    .onFailure(SupervisorStrategy.resume)
}

class Chatbot(twitchRoomId: String)(using
    private val ctx: ActorContext[Chatbot.Command],
    private val buffer: StashBuffer[Chatbot.Command],
    private val timers: TimerScheduler[Chatbot.Command],
    private val supervisor: ActorRef[ChatbotsSupervisor.Command],
    private val mediator: ActorRef[ArchieMateMediator.Command],
    private val decoder: IncomingMessageDecoder,
    private val encoder: OutgoingMessageEncoder,
    private val randomProvider: RandomProvider,
    private val timeProvider: TimeProvider,
    private val settings: Settings
) {
  given Timeout = settings.askTimeout
  given ActorRef[Chatbot.Command] = ctx.self

  given Ordering[Int] = Ordering.fromLessThan(_ < _)

  final case class InitialParameters(
      twitchRoomId: String,
      tokenIdOption: Option[String] = None,
      kickTokenIdOptionOption: Option[Option[String]] = None,
      basicChatbotSettingsOption: Option[BasicChatbotSettings] = None,
      builtInCommandsSettingsOption: Option[BuiltInCommandsSettings] = None,
      commandsSettingsOption: Option[CommandsSettings] = None,
      variablesSettingsOption: Option[VariablesSettings] = None,
      timersSettingsOption: Option[TimersSettings] = None,
      overlaysSettingsOption: Option[OverlaysSettings] = None,
      automaticMessagesSettingsOption: Option[AutomaticMessagesSettings] = None,
      polls: Option[ChannelPolls] = None,
      predictions: Option[ChannelPredictions] = None,
      broadcasterOption: Option[GetTokenUser] = None
  )

  def initial: Behavior[Chatbot.Command] = initial1(
    InitialParameters(twitchRoomId)
  )

  private def initial1(
      params: InitialParameters
  ): Behavior[Chatbot.Command] = {
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
        Chatbot.NewTwitchTokenId(tokenId)

      case Success(
            TwitchUserSessionsRepository.ReturnedTokenIdForUserId(None)
          ) =>
        Chatbot.NoToken(None)

      case Failure(ex) =>
        Chatbot.NoToken(Some(ex))
    }

    Behaviors.receiveAndLogMessage {
      case Chatbot.Leave =>
        Behaviors.stopped

      case Chatbot.NewTwitchTokenId(tokenId) =>
        ctx.ask[ArchieMateMediator.Command, BasicChatbotSettings](
          mediator,
          ref =>
            ArchieMateMediator.SendBasicChatbotSettingsRepositoryCommand(
              BasicChatbotSettingsRepository
                .GetSettings(ref, params.twitchRoomId)
            )
        ) {
          case Success(settings) =>
            Chatbot.NewBasicChatbotSettings(settings)
          case Failure(ex) => Chatbot.FailedToGetSettings(ex)
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
            Chatbot.NewBuiltInCommandsSettings(settings)
          case Failure(ex) => Chatbot.FailedToGetSettings(ex)
        }

        ctx.ask[ArchieMateMediator.Command, CommandsSettings](
          mediator,
          ref =>
            ArchieMateMediator.SendCommandsSettingsRepositoryCommand(
              CommandsSettingsRepository
                .GetCommandsSettings(ref, params.twitchRoomId)
            )
        ) {
          case Success(settings) => Chatbot.NewCommandsSettings(settings)
          case Failure(ex)       => Chatbot.FailedToGetSettings(ex)
        }

        ctx.ask[ArchieMateMediator.Command, VariablesSettings](
          mediator,
          ref =>
            ArchieMateMediator.SendVariablesSettingsRepositoryCommand(
              VariablesSettingsRepository
                .GetVariablesSettings(ref, params.twitchRoomId)
            )
        ) {
          case Success(settings) => Chatbot.NewVariablesSettings(settings)
          case Failure(ex)       => Chatbot.FailedToGetSettings(ex)
        }

        ctx.ask[ArchieMateMediator.Command, TimersSettings](
          mediator,
          ref =>
            ArchieMateMediator.SendTimersSettingsRepositoryCommand(
              TimersSettingsRepository.GetSettings(ref, params.twitchRoomId)
            )
        ) {
          case Success(settings) => Chatbot.NewTimersSettings(settings)
          case Failure(ex)       => Chatbot.FailedToGetSettings(ex)
        }

        ctx.ask[ArchieMateMediator.Command, OverlaysSettings](
          mediator,
          ref =>
            ArchieMateMediator.SendOverlaysSettingsRepositoryCommand(
              OverlaysSettingsRepository.GetSettings(ref, params.twitchRoomId)
            )
        ) {
          case Success(settings) => Chatbot.NewOverlaysSettings(settings)
          case Failure(ex)       => Chatbot.FailedToGetSettings(ex)
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
            Chatbot.NewAutomaticMessagesSettings(settings)
          case Failure(ex) => Chatbot.FailedToGetSettings(ex)
        }

        ctx.ask[ArchieMateMediator.Command, ChannelPolls](
          mediator,
          ref =>
            ArchieMateMediator.SendPollsRepositoryCommand(
              PollsRepository.GetPolls(ref, params.twitchRoomId)
            )
        ) {
          case Success(polls) =>
            Chatbot.NewPolls(polls)
          case Failure(ex) =>
            Chatbot.FailedToGetSettings(ex)
        }

        ctx.ask[ArchieMateMediator.Command, ChannelPredictions](
          mediator,
          ref =>
            ArchieMateMediator.SendPredictionsRepositoryCommand(
              PredictionsRepository.GetPredictions(ref, params.twitchRoomId)
            )
        ) {
          case Success(predictions) =>
            Chatbot.NewPredictions(predictions)
          case Failure(ex) =>
            Chatbot.FailedToGetSettings(ex)
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
          case Success(broadcaster) => Chatbot.Broadcaster(broadcaster)
          case Failure(ex)          => Chatbot.FailedToGetSettings(ex)
        }

        initial2(params.copy(tokenIdOption = Some(tokenId)))

      case Chatbot.NoToken(None) =>
        ctx.log.error("No token available!")
        supervisor ! ChatbotsSupervisor.AuthorizationNeeded(
          params.twitchRoomId
        )
        Behaviors.stopped

      case Chatbot.NoToken(Some(ex)) =>
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
  ): Behavior[Chatbot.Command] = (
    params.tokenIdOption,
    params.kickTokenIdOptionOption,
    params.basicChatbotSettingsOption,
    params.builtInCommandsSettingsOption,
    params.commandsSettingsOption,
    params.variablesSettingsOption,
    params.timersSettingsOption,
    params.overlaysSettingsOption,
    params.automaticMessagesSettingsOption,
    params.polls,
    params.predictions,
    params.broadcasterOption
  ) match {
    case (
          Some(tokenId),
          Some(kickTokenIdOption),
          Some(basicChatbotSettings),
          Some(builtInCommandsSettings),
          Some(commandsSettings),
          Some(variablesSettings),
          Some(timersSettings),
          Some(overlaysSettings),
          Some(automaticMessagesSettings),
          Some(polls),
          Some(predictions),
          Some(broadcaster)
        ) =>
      withSettings(
        tokenId,
        kickTokenIdOption,
        ChannelSettings(
          basicChatbotSettings,
          builtInCommandsSettings,
          commandsSettings,
          variablesSettings,
          timersSettings,
          overlaysSettings,
          automaticMessagesSettings
        ),
        polls,
        predictions,
        broadcaster
      )

    case _ =>
      Behaviors.receiveAndLogMessage {
        case Chatbot.NewBasicChatbotSettings(settings) =>
          initial2(params.copy(basicChatbotSettingsOption = Some(settings)))

        case Chatbot.NewBuiltInCommandsSettings(settings) =>
          initial2(params.copy(builtInCommandsSettingsOption = Some(settings)))

        case Chatbot.NewCommandsSettings(settings) =>
          initial2(params.copy(commandsSettingsOption = Some(settings)))

        case Chatbot.NewVariablesSettings(settings) =>
          initial2(params.copy(variablesSettingsOption = Some(settings)))

        case Chatbot.NewTimersSettings(settings) =>
          initial2(params.copy(timersSettingsOption = Some(settings)))

        case Chatbot.NewOverlaysSettings(settings) =>
          initial2(params.copy(overlaysSettingsOption = Some(settings)))

        case Chatbot.NewAutomaticMessagesSettings(settings) =>
          initial2(
            params.copy(automaticMessagesSettingsOption = Some(settings))
          )

        case Chatbot.NewPolls(polls) =>
          initial2(params.copy(polls = Some(polls)))

        case Chatbot.NewPredictions(predictions) =>
          initial2(params.copy(predictions = Some(predictions)))

        case Chatbot.Broadcaster(broadcaster) =>
          ctx.ask[
            ArchieMateMediator.Command,
            KickUserSessionsRepository.ReturnedTokenIdForUserId
          ](
            mediator,
            ref =>
              ArchieMateMediator.SendKickUserSessionsRepositoryCommand(
                KickUserSessionsRepository
                  .GetTokenIdForTwitchUserId(ref, broadcaster.id)
              )
          ) {
            case Success(
                  KickUserSessionsRepository.ReturnedTokenIdForUserId(
                    maybeKickTokenId
                  )
                ) =>
              Chatbot.NewKickTokenId(maybeKickTokenId)
            case Failure(ex) =>
              ctx.log.error(
                "There was an exception while waiting for possible kick token id.",
                ex
              )
              Chatbot.NewKickTokenId(None)
          }
          initial2(params.copy(broadcasterOption = Some(broadcaster)))

        case Chatbot.NewKickTokenId(tokenIdOption) =>
          initial2(params.copy(kickTokenIdOptionOption = Some(tokenIdOption)))

        case Chatbot.FailedToGetSettings(ex) =>
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
      twitchTokenId: String,
      kickTokenIdOption: Option[String],
      channelSettings: ChannelSettings,
      polls: ChannelPolls,
      predictions: ChannelPredictions,
      broadcaster: GetTokenUser
  ): Behavior[Chatbot.Command] = {
    val eventSubListener = ctx.spawn(
      EventSubListener(
        twitchTokenId,
        broadcaster
      ),
      EventSubListener.actorName
    )

    val ircListener = ctx.spawn(
      IRCListener(broadcaster.login),
      IRCListener.actorName
    )

    askForMods(twitchTokenId, broadcaster)
    askForVIPs(twitchTokenId, broadcaster)
    askForSubs(twitchTokenId, broadcaster)
    askForFollowers(twitchTokenId, broadcaster)
    askForChatters(twitchTokenId, broadcaster)
    askForStream(twitchTokenId, broadcaster)
    askForPollsHistory(twitchTokenId, broadcaster)
    askForPredictionsHistory(twitchTokenId, broadcaster)
    timers.startTimerAtFixedRate(
      Chatbot.AskForKickWebhooks,
      0.seconds,
      10.minutes
    )

    initializing(
      InitializingParameters(
        twitchTokenId,
        kickTokenIdOption,
        channelSettings,
        polls,
        predictions,
        broadcaster,
        eventSubListener,
        ircListener
      )
    )
  }

  final case class InitializingParameters(
      twitchTokenId: String,
      kickTokenIdOption: Option[String],
      channelSettings: ChannelSettings,
      polls: ChannelPolls,
      predictions: ChannelPredictions,
      broadcaster: GetTokenUser,
      eventSubListener: ActorRef[EventSubListener.Command],
      ircListener: ActorRef[IRCListener.Command],
      mods: Option[GetModerators] = None,
      vips: Option[GetVIPs] = None,
      subs: Option[GetSubs] = None,
      followers: Option[GetChannelFollowers] = None,
      chatters: Option[GetChatters] = None,
      stream: Option[Option[TwitchApi.Stream]] = None,
      pollHistory: Option[List[TwitchApi.Poll]] = None,
      predictionHistory: Option[List[TwitchApi.Prediction]] = None
  )

  private def initializing(
      params: InitializingParameters
  ): Behavior[Chatbot.Command] = {
    if (
      params.mods.nonEmpty && params.vips.nonEmpty && params.subs.nonEmpty && params.followers.nonEmpty && params.chatters.nonEmpty && params.stream.nonEmpty && params.pollHistory.nonEmpty && params.predictionHistory.nonEmpty
    ) {
      supervisor ! ChatbotsSupervisor.JoinOK(params.broadcaster.id)

      val twitchCommandsService = ctx.spawn(
        TwitchCommandsService(),
        TwitchCommandsService.actorName
      )

      timers.startTimerAtFixedRate(
        Chatbot.ChattersTimer,
        Chatbot.ChattersTimer,
        1 minute
      )
      if (params.channelSettings.timersSettings.enabled) {
        timers.startTimerAtFixedRate(
          Chatbot.MessageTimer,
          Chatbot.MessageTimer,
          params.channelSettings.timersSettings.intervalMinutes minutes
        )
      }
      val mods = params.mods.get.data.toMapWithKey(_.user_id)
      val vips = params.vips.get.data.toMapWithKey(_.user_id)
      val subs = params.subs.get.data.toMapWithKey(_.user_id)
      val subsAsUser = subs.map((userId, sub) =>
        userId -> TwitchApi.User(sub.user_id, sub.user_login, sub.user_name)
      )
      val followers = params.followers.get.data.toMapWithKey(_.user_id)
      val chatters = params.chatters.get.data.toMapWithKey(_.user_id)
      val usersState: Map[String, Chatbot.UserState] = {
        (mods ++ vips ++ subsAsUser ++ chatters).map { (userId, user) =>
          val flags = Seq(
            mods.get(userId).map(_ => Chatbot.UserFlag.Mod),
            vips.get(userId).map(_ => Chatbot.UserFlag.Vip),
            subs.get(userId).map(_ => Chatbot.UserFlag.Sub),
            chatters.get(userId).map(_ => Chatbot.UserFlag.Online),
            if (params.broadcaster.id == userId)
              Some(Chatbot.UserFlag.Streamer)
            else None,
            if (settings.twitchIRCUsername == user.user_login)
              Some(Chatbot.UserFlag.Ignore)
            else None
          ).filter(_.nonEmpty).map(_.get).toSet
          val subInfo = subs.get(userId)
          userId -> Chatbot.UserState(user, flags, subInfo)
        }
      }
      buffer.unstashAll(
        operational(
          Chatbot.OperationalParameters(
            params.twitchTokenId,
            params.kickTokenIdOption,
            params.channelSettings,
            params.polls,
            params.pollHistory.get.find(_.status == "ACTIVE"),
            params.predictions,
            params.predictionHistory.get.find(prediction =>
              prediction.status.toUpperCase() == "ACTIVE" || prediction.status
                .toUpperCase() == "LOCKED"
            ),
            params.broadcaster,
            params.eventSubListener,
            params.ircListener,
            twitchCommandsService,
            usersState,
            followers,
            params.stream.get
          )
        )
      )
    } else {
      Behaviors.receiveAndLogMessage {
        case Chatbot.Mods(Some(mods)) =>
          initializing(params.copy(mods = Some(mods)))

        case Chatbot.VIPs(Some(vips)) =>
          initializing(params.copy(vips = Some(vips)))

        case Chatbot.Subs(Some(subs)) =>
          initializing(params.copy(subs = Some(subs)))

        case Chatbot.Followers(Some(followers)) =>
          initializing(params.copy(followers = Some(followers)))

        case Chatbot.Chatters(Some(chatters)) =>
          initializing(params.copy(chatters = Some(chatters)))

        case Chatbot.Stream(Some(stream)) =>
          initializing(params.copy(stream = Some(stream)))

        case Chatbot.PollHistory(Some(polls)) =>
          initializing(params.copy(pollHistory = Some(polls)))

        case Chatbot.PredictionHistory(Some(predictions)) =>
          initializing(params.copy(predictionHistory = Some(predictions)))

        case Chatbot.Mods(None) | Chatbot.VIPs(None) | Chatbot.Subs(None) |
            Chatbot.Followers(None) | Chatbot.Chatters(None) |
            Chatbot.Stream(None) | Chatbot.PollHistory(None) |
            Chatbot.PredictionHistory(None) =>
          supervisor ! ChatbotsSupervisor.AuthorizationNeeded(
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
      params: Chatbot.OperationalParameters
  ): Behavior[Chatbot.Command] = Behaviors.receiveAndLogMessage {
    case Chatbot.ChattersTimer =>
      askForChatters(params.twitchTokenId, params.broadcaster)
      Behaviors.same

    case Chatbot.MessageTimer =>
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

    case Chatbot.Chatters(Some(chattersResponse)) =>
      val currentChatters = chattersResponse.data

      val newUsers = {
        currentChatters.toMapWithKey(_.user_id).map { (userId, user) =>
          val existingUserOption = params.users.get(userId)
          userId -> Chatbot.UserState(
            user,
            existingUserOption
              .map(_.flags)
              .getOrElse(Set.empty),
            existingUserOption.flatMap(_.subInfo),
            existingUserOption.map(_.afkConversations).getOrElse(Set.empty)
          )
        }
      }

      params.twitchCommandsService ! TwitchCommandsService.GreetUsers(
        params,
        newUsers
      )

      operational(
        params.copy(
          users = params.users ++ newUsers.map((userId, userState) =>
            userId -> userState.copy(flags =
              userState.flags + UserFlag.Online + UserFlag.DontGreet
            )
          )
        )
      )

    case Chatbot.Chatters(None) =>
      supervisor ! ChatbotsSupervisor.AuthorizationNeeded(
        params.broadcaster.id
      )
      Behaviors.stopped

    case Chatbot.EventSubEvent(e: eventsub.ChannelUpdateEvent) =>
      params.stream match {
        case Some(_) => askForStream(params.twitchTokenId, params.broadcaster)
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

    case Chatbot.Stream(Some(stream)) =>
      operational(params.copy(stream = stream))

    case Chatbot.Stream(None) =>
      supervisor ! ChatbotsSupervisor.AuthorizationNeeded(
        params.broadcaster.id
      )
      Behaviors.stopped

    case Chatbot.EventSubEvent(e: eventsub.ChannelFollowEvent) =>
      params.channelSettings.automaticMessagesSettings.follow.foreach { msg =>
        val msgWithUser = msg.replaceAll("user".asVariableRegex, e.userName)
        params.ircListener ! IRCListener.SendMessage(msgWithUser)
      }
      operational(
        params.copy(followers =
          params.followers + (e.userId -> TwitchApi
            .UserFollowage(e.userId, e.userLogin, e.userName, e.followedAt))
        )
      )

    case Chatbot.EventSubEvent(_: eventsub.ChannelAdBreakBeginEvent) =>
      Behaviors.same

    case Chatbot.EventSubEvent(_: eventsub.ChannelChatClearEvent) =>
      Behaviors.same

    case Chatbot.EventSubEvent(
          _: eventsub.ChannelChatClearUserMessagesEvent
        ) =>
      Behaviors.same

    case Chatbot.EventSubEvent(e: eventsub.ChannelChatMessageEvent)
        if params.users.exists((userId, userState) =>
          userId == e.chatterUserId && userState.flags
            .contains(Chatbot.UserFlag.Ignore)
        ) =>
      Behaviors.same

    case Chatbot.EventSubEvent(e: eventsub.ChannelChatMessageEvent) =>
      val userState = params.users.getOrElse(
        e.chatterUserId,
        Chatbot.UserState(
          TwitchApi.User(e.chatterUserId, e.chatterUserLogin, e.chatterUserName)
        )
      )

      params.twitchCommandsService ! TwitchCommandsService.GreetUsers(
        params,
        Map(userState.user.user_id -> userState)
      )

      val mentionedAfks = params.users
        .filter((userId, userState) =>
          userState.flags.contains(
            Chatbot.UserFlag.Afk
          ) && e.message.text.toLowerCase.contains(userState.user.user_login)
        )
        .map((userId, userState) =>
          userId -> userState
            .copy(afkConversations = userState.afkConversations + e.messageId)
        )

      mentionedAfks.size match {
        case never if never <= 0 =>
        case 1 =>
          IRCListener.SendReplyMessage(
            s"@${e.chatterUserName}, ${mentionedAfks(mentionedAfks.keySet.head).user.user_name} is AFK. When they get back I'll notify them.",
            e.messageId
          )
        case _ =>
          IRCListener.SendReplyMessage(
            s"@${e.chatterUserName}, the following users are AFK: ${mentionedAfks.map(_._2.user.user_name).mkString} - when they get back I'll notify them.",
            e.messageId
          )
      }

      val formerAfk: Map[String, Chatbot.UserState] =
        if (
          params.users.exists((userId, userState) =>
            userId == e.chatterUserId && userState.flags
              .contains(Chatbot.UserFlag.Afk)
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
              flags = userState.flags - Chatbot.UserFlag.Afk,
              afkConversations = Set.empty
            )
          )
        } else {
          Map.empty
        }

      val newUsers = params.users ++ mentionedAfks ++ formerAfk

      val newParams = params.copy(
        users = newUsers.map((userId, userState) =>
          userId -> userState.copy(flags =
            userState.flags ++ (if (userId == e.chatterUserId)
                                  Set(UserFlag.Online, UserFlag.DontGreet)
                                else Set.empty)
          )
        )
      )

      newParams.twitchCommandsService ! TwitchCommandsService.RespondToCommand(
        newParams,
        e
      )

      operational(newParams)

    case Chatbot.NewBasicChatbotSettings(settings) =>
      operational(
        params.copy(channelSettings =
          params.channelSettings.copy(basicChatbotSettings = settings)
        )
      )

    case Chatbot.NewBuiltInCommandsSettings(settings) =>
      operational(
        params.copy(channelSettings =
          params.channelSettings.copy(builtInCommandsSettings = settings)
        )
      )

    case Chatbot.NewCommandsSettings(settings) =>
      operational(
        params.copy(channelSettings =
          params.channelSettings.copy(commandsSettings = settings)
        )
      )

    case Chatbot.NewVariablesSettings(settings) =>
      operational(
        params.copy(channelSettings =
          params.channelSettings.copy(variablesSettings = settings)
        )
      )

    case Chatbot.NewTimersSettings(settings) =>
      if (settings.enabled) {
        timers.startTimerAtFixedRate(
          Chatbot.MessageTimer,
          Chatbot.MessageTimer,
          params.channelSettings.timersSettings.intervalMinutes minutes
        )
      } else {
        timers.cancel(Chatbot.MessageTimer)
      }
      operational(
        params.copy(channelSettings =
          params.channelSettings.copy(timersSettings = settings)
        )
      )

    case Chatbot.NewOverlaysSettings(settings) =>
      operational(
        params.copy(channelSettings =
          params.channelSettings.copy(overlaysSettings = settings)
        )
      )

    case Chatbot.NewAutomaticMessagesSettings(settings) =>
      operational(
        params.copy(channelSettings =
          params.channelSettings.copy(automaticMessagesSettings = settings)
        )
      )

    case Chatbot.NewPolls(polls) =>
      operational(
        params.copy(polls = polls)
      )

    case Chatbot.NewPredictions(predictions) =>
      operational(
        params.copy(predictions = predictions)
      )

    case Chatbot.Broadcaster(broadcaster) =>
      operational(params.copy(broadcaster = broadcaster))

    case Chatbot.EventSubEvent(
          _: eventsub.ChannelChatMessageDeleteEvent
        ) =>
      Behaviors.same

    case Chatbot.EventSubEvent(
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
            Chatbot.UserState(
              TwitchApi
                .User(e.chatterUserId, e.chatterUserLogin, e.chatterUserName),
              Set(Chatbot.UserFlag.Online)
            )
          )
          val subbedUser = e.chatterUserId -> userState.copy(
            flags = userState.flags + Chatbot.UserFlag.Sub,
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
            Chatbot.UserState(
              TwitchApi
                .User(e.chatterUserId, e.chatterUserLogin, e.chatterUserName),
              Set(Chatbot.UserFlag.Online)
            )
          )
          val subbedUser = e.chatterUserId -> userState.copy(
            flags = userState.flags + Chatbot.UserFlag.Sub,
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
            Chatbot.UserState(
              TwitchApi
                .User(
                  subGift.recipientUserId,
                  subGift.recipientUserLogin,
                  subGift.recipientUserName
                ),
              Set(Chatbot.UserFlag.Online)
            )
          )

          val subbedUser = subGift.recipientUserId -> userState.copy(
            flags = userState.flags + Chatbot.UserFlag.Sub,
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

    case Chatbot.EventSubEvent(e: eventsub.ChannelSubscribeEvent) =>
      ctx.log.debug(
        "User {} added as a new subscriber after ChannelSubscribeEvent",
        e.userName
      )
      val userState = params.users.getOrElse(
        e.userId,
        Chatbot.UserState(
          TwitchApi
            .User(e.userId, e.userLogin, e.userName),
          Set(Chatbot.UserFlag.Online)
        )
      )
      val subbedUser = e.userId -> userState.copy(
        flags = userState.flags + Chatbot.UserFlag.Sub,
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

    case Chatbot.EventSubEvent(e: eventsub.ChannelSubscriptionEndEvent) =>
      ctx.log.debug("User {} is no longer a subscriber", e.userName)
      val userState = params.users.getOrElse(
        e.userId,
        Chatbot.UserState(
          TwitchApi
            .User(e.userId, e.userLogin, e.userName),
          Set(Chatbot.UserFlag.Online)
        )
      )
      val unsubbedUser = e.userId -> userState.copy(
        flags = userState.flags - Chatbot.UserFlag.Sub,
        subInfo = None
      )
      operational(
        params.copy(
          users = params.users + unsubbedUser
        )
      )

    case Chatbot.EventSubEvent(
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

    case Chatbot.EventSubEvent(
          e: eventsub.ChannelSubscriptionMessageEvent
        ) =>
      Behaviors.same

    case Chatbot.EventSubEvent(e: eventsub.ChannelCheerEvent) =>
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

    case Chatbot.EventSubEvent(e: eventsub.ChannelRaidEvent) =>
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
          msg
            .replaceAll("broadcaster".asVariableRegex, otherBroadcaster)
            .replaceAll("viewerCount".asVariableRegex, e.viewers.toString)
        params.ircListener ! IRCListener.SendMessage(message)
      }
      Behaviors.same

    case Chatbot.EventSubEvent(e: eventsub.ChannelBanEvent) =>
      Behaviors.same

    case Chatbot.EventSubEvent(e: eventsub.ChannelUnbanEvent) =>
      Behaviors.same

    case Chatbot.EventSubEvent(e: eventsub.ChannelModeratorAddEvent) =>
      ctx.log.debug("User {} added as a new moderator", e.userName)
      val userState = params.users.getOrElse(
        e.userId,
        Chatbot.UserState(
          TwitchApi.User(e.userId, e.userLogin, e.userName)
        )
      )
      val newMod = e.userId -> userState.copy(
        user = TwitchApi.User(e.userId, e.userLogin, e.userName),
        flags = userState.flags + Chatbot.UserFlag.Mod
      )

      operational(
        params.copy(
          users = params.users + newMod
        )
      )

    case Chatbot.EventSubEvent(e: eventsub.ChannelModeratorRemoveEvent) =>
      ctx.log.debug("User {} is no longer a moderator", e.userName)
      val userState = params.users(e.userId)
      val formerMod = e.userId -> userState.copy(flags =
        userState.flags - Chatbot.UserFlag.Mod
      )
      operational(
        params.copy(
          users = params.users + formerMod
        )
      )

    case Chatbot.EventSubEvent(
          _: eventsub.ChannelPointsAutomaticRewardRedemptionAddEvent
        ) =>
      Behaviors.same

    case Chatbot.EventSubEvent(
          _: eventsub.ChannelPointsCustomRewardAddEvent
        ) =>
      Behaviors.same

    case Chatbot.EventSubEvent(
          _: eventsub.ChannelPointsCustomRewardUpdateEvent
        ) =>
      Behaviors.same

    case Chatbot.EventSubEvent(
          _: eventsub.ChannelPointsCustomRewardRemoveEvent
        ) =>
      Behaviors.same

    case Chatbot.EventSubEvent(
          _: eventsub.ChannelPointsCustomRewardRedemptionAddEvent
        ) =>
      Behaviors.same

    case Chatbot.EventSubEvent(
          _: eventsub.ChannelPointsCustomRewardRedemptionUpdateEvent
        ) =>
      Behaviors.same

    case Chatbot.EventSubEvent(e: eventsub.ChannelPollBeginEvent) =>
      operational(params =
        params.copy(currentPoll =
          Some(
            TwitchApi.Poll(
              id = e.id,
              broadcasterId = e.broadcasterUserId,
              broadcasterName = e.broadcasterUserName,
              broadcasterLogin = e.broadcasterUserLogin,
              title = e.title,
              choices = e.choices
                .map(choice =>
                  TwitchApi.PollChoice(
                    id = choice.id,
                    title = choice.title,
                    votes = 0,
                    channelPointsVotes = 0,
                    bitsVotes = None
                  )
                ),
              bitsVotingEnabled = Some(e.bitsVoting.isEnabled),
              bitsPerVote = Some(e.bitsVoting.amountPerVote),
              channelPointsVotingEnabled = e.channelPointsVoting.isEnabled,
              channelPointsPerVote = e.channelPointsVoting.amountPerVote,
              status = "ACTIVE",
              duration =
                ChronoUnit.SECONDS.between(e.endsAt, e.startedAt).toInt,
              startedAt = e.startedAt
            )
          )
        )
      )

    case Chatbot.EventSubEvent(e: eventsub.ChannelPollProgressEvent) =>
      operational(params = params.copy(currentPoll = params.currentPoll.map {
        poll =>
          poll.copy(
            choices = e.choices.map(choice =>
              TwitchApi.PollChoice(
                id = choice.id,
                title = choice.title,
                votes = choice.votes,
                channelPointsVotes = choice.channelPointsVotes,
                bitsVotes = Some(choice.bitsVotes)
              )
            )
          )
      }))

    case Chatbot.EventSubEvent(e: eventsub.ChannelPollEndEvent) =>
      given Ordering[eventsub.StartedPollChoice] = Ordering.fromLessThan((x, y) =>
        x.votes + x.bitsVotes + x.channelPointsVotes < y.votes + y.bitsVotes + y.channelPointsVotes
      )
      val sortedChoices = e.choices.sorted
      val maxChoice = e.choices.max
      val maxVote =
        maxChoice.votes + maxChoice.bitsVotes + maxChoice.channelPointsVotes
      val winningChoices = e.choices.filter(choice =>
        choice.votes + choice.bitsVotes + choice.channelPointsVotes == maxVote
      )
      params.ircListener ! IRCListener.SendMessage(
        s"Poll ended with the following winner(s) - $maxVote vote(s): ${winningChoices.map(_.title).mkString(", ")}"
      )
      operational(params = params.copy(currentPoll = None))

    case Chatbot.EventSubEvent(e: eventsub.ChannelPredictionBeginEvent) =>
      operational(params =
        params.copy(currentPrediction =
          Some(
            TwitchApi.Prediction(
              id = e.id,
              broadcasterId = e.broadcasterUserId,
              broadcasterName = e.broadcasterUserName,
              broadcasterLogin = e.broadcasterUserLogin,
              title = e.title,
              winningOutcomeId = None,
              outcomes = e.outcomes.map(outcome =>
                TwitchApi.PredictionOutcome(
                  id = outcome.id,
                  title = outcome.title,
                  users = 0,
                  channelPoints = 0,
                  topPredictors = None,
                  color = outcome.color
                )
              ),
              predictionWindow =
                ChronoUnit.SECONDS.between(e.locksAt, e.startedAt).toInt,
              status = "ACTIVE",
              createdAt = e.startedAt,
              endedAt = None,
              lockedAt = None
            )
          )
        )
      )

    case Chatbot.EventSubEvent(
          e: eventsub.ChannelPredictionProgressEvent
        ) =>
      val prediction = params.currentPrediction.get
      operational(params =
        params.copy(currentPrediction =
          Some(
            prediction.copy(outcomes =
              e.outcomes.map(outcome =>
                TwitchApi.PredictionOutcome(
                  id = outcome.id,
                  title = outcome.title,
                  users = outcome.users,
                  channelPoints = outcome.channelPoints,
                  topPredictors = Some(
                    outcome.topPredictors.map(prediction =>
                      TwitchApi.UserPrediction(
                        userId = prediction.userId,
                        userName = prediction.userName,
                        userLogin = prediction.userLogin,
                        channelPointsUsed = prediction.channelPointsUsed,
                        channelPointsWon = prediction.channelPointsWon
                      )
                    )
                  ),
                  color = outcome.color
                )
              )
            )
          )
        )
      )

    case Chatbot.EventSubEvent(e: eventsub.ChannelPredictionLockEvent) =>
      val prediction = params.currentPrediction.get
      operational(params =
        params.copy(currentPrediction =
          Some(
            prediction.copy(
              outcomes = e.outcomes.map(outcome =>
                TwitchApi.PredictionOutcome(
                  id = outcome.id,
                  title = outcome.title,
                  users = outcome.users,
                  channelPoints = outcome.channelPoints,
                  topPredictors = Some(
                    outcome.topPredictors.map(prediction =>
                      TwitchApi.UserPrediction(
                        userId = prediction.userId,
                        userName = prediction.userName,
                        userLogin = prediction.userLogin,
                        channelPointsUsed = prediction.channelPointsUsed,
                        channelPointsWon = prediction.channelPointsWon
                      )
                    )
                  ),
                  color = outcome.color
                )
              ),
              status = "LOCKED",
              lockedAt = Some(e.lockedAt)
            )
          )
        )
      )

    case Chatbot.EventSubEvent(e: eventsub.ChannelPredictionEndEvent) =>
      if (e.status.toUpperCase() != "CANCELED") {
        e.outcomes.find(_.id == e.winningOutcomeId) match {
          case None =>
            ctx.log.error(
              "Invalid prediction end event {}: Winning outcome id does not match any outcomes.",
              e
            )
          case Some(outcome) =>
            IRCListener.SendMessage(
              s"Prediction ended! Winning outcome was \"${outcome.title}\"."
            )
        }
      }
      operational(params = params.copy(currentPrediction = None))

    case Chatbot.EventSubEvent(e: eventsub.ChannelVIPAddEvent) =>
      ctx.log.debug("User {} added as a new VIP", e.userName)
      val userState = params.users.getOrElse(
        e.userId,
        Chatbot.UserState(
          TwitchApi.User(e.userId, e.userLogin, e.userName)
        )
      )
      val newVip = e.userId -> userState.copy(
        user = TwitchApi.User(e.userId, e.userLogin, e.userName),
        flags = userState.flags + Chatbot.UserFlag.Vip
      )

      operational(
        params.copy(
          users = params.users + newVip
        )
      )

    case Chatbot.EventSubEvent(e: eventsub.ChannelVIPRemoveEvent) =>
      ctx.log.debug("User {} is no longer a VIP", e.userName)
      val userState = params.users(e.userId)
      val formerVip = e.userId -> userState.copy(flags =
        userState.flags - Chatbot.UserFlag.Vip
      )
      operational(
        params.copy(
          users = params.users + formerVip
        )
      )

    case Chatbot.EventSubEvent(_: eventsub.ChannelGoalBeginEvent) =>
      Behaviors.same

    case Chatbot.EventSubEvent(_: eventsub.ChannelGoalProgressEvent) =>
      Behaviors.same

    case Chatbot.EventSubEvent(e: eventsub.ChannelGoalEndEvent) =>
      Behaviors.same

    case Chatbot.EventSubEvent(e: eventsub.ChannelHypeTrainBeginEvent) =>
      params.channelSettings.automaticMessagesSettings.hypeTrainBegin
        .foreach(params.ircListener ! IRCListener.SendMessage(_))
      Behaviors.same

    case Chatbot.EventSubEvent(
          _: eventsub.ChannelHypeTrainProgressEvent
        ) =>
      params.channelSettings.automaticMessagesSettings.hypeTrainLevelUp
        .foreach { thresholds =>
          val maxThreshold = thresholds.max(Ordering.by(_._1))
          val message =
            thresholds.getOrElse(maxThreshold._1, thresholds.last._2)
          params.ircListener ! IRCListener.SendMessage(message)
        }
      Behaviors.same

    case Chatbot.EventSubEvent(_: eventsub.ChannelHypeTrainEndEvent) =>
      params.channelSettings.automaticMessagesSettings.hypeTrainEnd
        .foreach(params.ircListener ! IRCListener.SendMessage(_))
      Behaviors.same

    case Chatbot.EventSubEvent(_: eventsub.ChannelShoutoutReceiveEvent) =>
      Behaviors.same

    case Chatbot.EventSubEvent(e: eventsub.StreamOnlineEvent) =>
      ctx
        .askWithStatus[ArchieMateMediator.Command, TwitchApiResponse.GetStream](
          mediator,
          ref =>
            ArchieMateMediator.SendTwitchApiClientCommand(
              TwitchApiClient
                .GetStream(ref, params.twitchTokenId, params.broadcaster.id)
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
            Chatbot.Stream(Some(optionStream))

          case Failure(ex) =>
            ctx.log.error(
              "Could not retrieve whether the channel {} is live",
              params.broadcaster.login,
              ex
            )
            Chatbot.Stream(None)
        }

      val newUsers = params.users.map((userId, userState) =>
        userId -> userState.copy(flags =
          userState.flags - UserFlag.Online - UserFlag.DontGreet
        )
      )

      operational(
        params.copy(
          stream = Some(
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
          ),
          users = newUsers
        )
      )

    case Chatbot.EventSubEvent(e: eventsub.StreamOfflineEvent) =>
      params.channelSettings.automaticMessagesSettings.streamEnd.foreach {
        msg =>
          params.ircListener ! IRCListener.SendMessage(msg)
      }
      operational(params.copy(stream = None))

    case Chatbot.EventSubEvent(e: eventsub.UserUpdateEvent) =>
      operational(
        params.copy(
          broadcaster = params.broadcaster.copy(
            login = e.userLogin,
            displayName = e.userName,
            description = e.description
          )
        )
      )

    case Chatbot.AskForKickWebhooks =>
      params.kickTokenIdOption match {
        case Some(kickTokenId) =>
          ctx.askWithStatus[
            ArchieMateMediator.Command,
            KickApiResponse.GetEventsSubscriptions
          ](
            mediator,
            ref =>
              ArchieMateMediator.SendKickApiClientCommand(
                KickApiClient.GetEventsSubscriptions(ref, kickTokenId)
              )
          ) { (resp: Try[KickApiResponse.GetEventsSubscriptions]) =>
            Chatbot.KickWebhooksSubscriptions(resp.map(_.data))
          }
        case None =>
          Chatbot.KickWebhooksSubscriptions(Success(Nil))
      }
      Behaviors.same

    case Chatbot.KickWebhooksSubscriptions(
          Success(
            subscriptions: List[KickApiResponse.GetEventsSubscriptionsData]
          )
        ) if params.kickTokenIdOption.nonEmpty =>
      val expectedSubscriptions: Set[(String, Int)] =
        Set("chat.message.sent" -> 1, "channel.followed" -> 1)
      val actualSubscriptions: Set[(String, Int)] =
        subscriptions.map(sub => (sub.event, sub.version)).toSet
      expectedSubscriptions.diff(actualSubscriptions) match {
        case nothing if nothing.isEmpty =>
        case subsToAsk =>
          val subs: List[KickApiRequest.KickEvent] =
            subsToAsk.map(KickApiRequest.KickEvent.apply).toList
          mediator ! ArchieMateMediator.SendKickApiClientCommand(
            KickApiClient.SubscribeToEvents(
              ctx.system.ignoreRef,
              params.kickTokenIdOption.get,
              subs
            )
          )
      }
      Behaviors.same

    case _: Chatbot.KickWebhooksSubscriptions =>
      Behaviors.same

    case Chatbot.NewKickWebhook(e: KickWebhooks.ChatMessageSentV1) =>
      e.content.toLowerCase().match {
        case "!commands" =>
          KickApiClient.PostChatMessage(
            ctx.system.ignoreRef,
            params.kickTokenIdOption.get,
            s"${settings.archiemateRedirectUriPrefix}/t/commands/${params.broadcaster.login}",
            None
          )
        case _ =>
      }
      Behaviors.same

    case Chatbot.NewKickWebhook(e: KickWebhooks.ChannelFollowedV1) =>
      params.channelSettings.automaticMessagesSettings.follow.foreach { msg =>
        val msgWithUser =
          msg.replaceAll("user".asVariableRegex, e.follower.username)
        KickApiClient.PostChatMessage(
          ctx.system.ignoreRef,
          params.kickTokenIdOption.get,
          msgWithUser,
          None
        )
      }
      Behaviors.same

    case Chatbot.Afk(chatterUserId) =>
      val userState = params.users(chatterUserId)
      val afkUser = chatterUserId -> userState.copy(flags =
        userState.flags + Chatbot.UserFlag.Afk
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
        ArchieMateMediator.SendTwitchApiPaginationHandlerServiceCommand(
          TwitchApiPaginationHandlerService.GetModerators(
            TwitchApiClient.GetModerators(ref, tokenId, broadcaster.id)
          )
        )
    ) {
      case Success(mods: GetModerators) =>
        Chatbot.Mods(Some(mods))

      case Failure(ex) =>
        ctx.log.error(
          "Could not retrieve moderators of channel {}",
          broadcaster.login,
          ex
        )
        Chatbot.Mods(None)
    }

  private def askForVIPs(tokenId: String, broadcaster: GetTokenUser): Unit =
    ctx.askWithStatus[ArchieMateMediator.Command, TwitchApiResponse.GetVIPs](
      mediator,
      ref =>
        ArchieMateMediator.SendTwitchApiPaginationHandlerServiceCommand(
          TwitchApiPaginationHandlerService.GetVIPs(
            TwitchApiClient.GetVIPs(ref, tokenId, broadcaster.id)
          )
        )
    ) {
      case Success(vips: GetVIPs) =>
        Chatbot.VIPs(Some(vips))

      case Failure(ex) =>
        ctx.log.error(
          "Could not retrieve VIPs of channel {}",
          broadcaster.login,
          ex
        )
        Chatbot.VIPs(None)
    }

  private def askForSubs(tokenId: String, broadcaster: GetTokenUser): Unit =
    ctx.askWithStatus[ArchieMateMediator.Command, TwitchApiResponse.GetSubs](
      mediator,
      ref =>
        ArchieMateMediator.SendTwitchApiPaginationHandlerServiceCommand(
          TwitchApiPaginationHandlerService.GetSubs(
            TwitchApiClient.GetSubs(ref, tokenId, broadcaster.id)
          )
        )
    ) {
      case Success(subs: GetSubs) =>
        Chatbot.Subs(Some(subs))

      case Failure(ex) =>
        ctx.log.error(
          "Could not retrieve subs of channel {}",
          broadcaster.login,
          ex
        )
        Chatbot.Subs(None)
    }

  private def askForFollowers(
      tokenId: String,
      broadcaster: GetTokenUser
  ): Unit =
    ctx.askWithStatus[
      ArchieMateMediator.Command,
      TwitchApiResponse.GetChannelFollowers
    ](
      mediator,
      ref =>
        ArchieMateMediator.SendTwitchApiPaginationHandlerServiceCommand(
          TwitchApiPaginationHandlerService.GetChannelFollowers(
            TwitchApiClient.GetChannelFollowers(ref, tokenId, broadcaster.id)
          )
        )
    ) {
      case Success(followers: GetChannelFollowers) =>
        Chatbot.Followers(Some(followers))

      case Failure(ex) =>
        ctx.log.error(
          "Could not retrieve followers of channel {}",
          broadcaster.login,
          ex
        )
        Chatbot.Followers(None)
    }

  private def askForChatters(tokenId: String, broadcaster: GetTokenUser): Unit =
    ctx
      .askWithStatus[
        ArchieMateMediator.Command,
        TwitchApiResponse.GetChatters
      ](
        mediator,
        ref =>
          ArchieMateMediator.SendTwitchApiPaginationHandlerServiceCommand(
            TwitchApiPaginationHandlerService.GetChatters(
              TwitchApiClient
                .GetChatters(ref, tokenId, broadcaster.id, broadcaster.id)
            )
          )
      ) {
        case Success(chatters: GetChatters) =>
          Chatbot.Chatters(Some(chatters))

        case Failure(ex) =>
          ctx.log.error(
            "Could not retrieve chatters of channel {}",
            broadcaster.login,
            ex
          )
          Chatbot.Chatters(None)
      }

  private def askForStream(tokenId: String, broadcaster: GetTokenUser): Unit = {
    ctx.askWithStatus[ArchieMateMediator.Command, TwitchApiResponse.GetStream](
      mediator,
      ref =>
        ArchieMateMediator.SendTwitchApiPaginationHandlerServiceCommand(
          TwitchApiPaginationHandlerService.GetStream(
            TwitchApiClient.GetStream(ref, tokenId, broadcaster.id)
          )
        )
    ) {
      case Success(GetStream(List(stream), _)) =>
        Chatbot.Stream(Some(Some(stream)))

      case Success(GetStream(Nil, _)) =>
        Chatbot.Stream(Some(None))

      case Success(GetStream(streams, _)) =>
        ctx.log.warn(
          "Received stream response for channel {} with unexpected amount of streams when expected one stream or none: {}",
          broadcaster.login,
          streams
        )
        Chatbot.Stream(Some(streams.lastOption))

      case Failure(ex) =>
        ctx.log.error(
          "Could not retrieve whether channel {} is live",
          broadcaster.login,
          ex
        )
        Chatbot.Stream(Some(None))
    }
  }

  private def askForPollsHistory(
      tokenId: String,
      broadcaster: GetTokenUser
  ): Unit = {
    ctx.askWithStatus[ArchieMateMediator.Command, TwitchApiResponse.GetPolls](
      mediator,
      ref =>
        ArchieMateMediator.SendTwitchApiPaginationHandlerServiceCommand(
          TwitchApiPaginationHandlerService.GetPolls(
            TwitchApiClient.GetPolls(ref, tokenId, broadcaster.id)
          )
        )
    ) {
      case Success(TwitchApiResponse.GetPolls(polls, _)) =>
        Chatbot.PollHistory(Some(polls))

      case Failure(ex) =>
        ctx.log.error(
          "Could not retrieve channel {} poll history",
          broadcaster.login,
          ex
        )
        Chatbot.PollHistory(None)
    }
  }

  private def askForPredictionsHistory(
      tokenId: String,
      broadcaster: GetTokenUser
  ): Unit = {
    ctx.askWithStatus[
      ArchieMateMediator.Command,
      TwitchApiResponse.GetPredictions
    ](
      mediator,
      ref =>
        ArchieMateMediator.SendTwitchApiPaginationHandlerServiceCommand(
          TwitchApiPaginationHandlerService.GetPredictions(
            TwitchApiClient.GetPredictions(ref, tokenId, broadcaster.id)
          )
        )
    ) {
      case Success(TwitchApiResponse.GetPredictions(predictions, _)) =>
        Chatbot.PredictionHistory(Some(predictions))

      case Failure(ex) =>
        ctx.log.error(
          "Could not retrieve channel {} prediction history",
          broadcaster.login,
          ex
        )
        Chatbot.PredictionHistory(None)
    }
  }
}
