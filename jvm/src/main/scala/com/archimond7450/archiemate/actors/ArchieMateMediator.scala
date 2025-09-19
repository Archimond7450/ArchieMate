package com.archimond7450.archiemate.actors

import com.archimond7450.archiemate.actors.chatbot.TwitchChatbotsSupervisor
import com.archimond7450.archiemate.actors.repositories.sessions.{TwitchUserSessionsRepository, YouTubeChannelSessionsRepository}
import com.archimond7450.archiemate.actors.repositories.settings.{AutomaticMessagesSettingsRepository, BasicChatbotSettingsRepository, BuiltInCommandsSettingsRepository, CommandsSettingsRepository, OverlaysSettingsRepository, TimersSettingsRepository, VariablesSettingsRepository}
import com.archimond7450.archiemate.actors.services.{JWTService, TwitchLoginValidatorService}
import com.archimond7450.archiemate.actors.services.caches.TwitchTokenUserCacheService
import com.archimond7450.archiemate.actors.services.controllerhelpers.{CommandsControllerHelperService, OAuthControllerHelperService, SettingsControllerHelperService, UserControllerHelperService}
import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.actors.youtube.api.YouTubeApiClient
import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.providers.{RandomProvider, TimeProvider}
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.util.Timeout

object ArchieMateMediator {
  val actorName = "ArchieMateMediator"

  sealed trait Command

  final case class SendHttpClientRequest(cmd: HttpClient.Request) extends Command
  final case class SendTwitchChatbotsSupervisorCommand(cmd: TwitchChatbotsSupervisor.Command) extends Command
  final case class SendTwitchUserSessionsRepositoryCommand(cmd: TwitchUserSessionsRepository.Command) extends Command
  final case class SendYouTubeChannelSessionsRepositoryCommand(cmd: YouTubeChannelSessionsRepository.Command) extends Command
  final case class SendAutomaticMessagesSettingsRepositoryCommand(cmd: AutomaticMessagesSettingsRepository.Command) extends Command
  final case class SendBasicChatbotSettingsRepositoryCommand(cmd: BasicChatbotSettingsRepository.Command) extends Command
  final case class SendBuiltInCommandsSettingsRepositoryCommand(cmd: BuiltInCommandsSettingsRepository.Command) extends Command
  final case class SendCommandsSettingsRepositoryCommand(cmd: CommandsSettingsRepository.Command) extends Command
  final case class SendOverlaysSettingsRepositoryCommand(cmd: OverlaysSettingsRepository.Command) extends Command
  final case class SendTimersSettingsRepositoryCommand(cmd: TimersSettingsRepository.Command) extends Command
  final case class SendVariablesSettingsRepositoryCommand(cmd: VariablesSettingsRepository.Command) extends Command
  final case class SendTwitchTokenUserCacheServiceCommand(cmd: TwitchTokenUserCacheService.Command) extends Command
  final case class SendCommandsControllerHelperServiceCommand(Cmd: CommandsControllerHelperService.Command) extends Command
  final case class SendOAuthControllerHelperServiceCommand(cmd: OAuthControllerHelperService.Command) extends Command
  final case class SendSettingsControllerHelperServiceCommand(cmd: SettingsControllerHelperService.Command) extends Command
  final case class SendUserControllerHelperServiceCommand(cmd: UserControllerHelperService.Command) extends Command
  final case class SendJWTServiceCommand(cmd: JWTService.Command) extends Command
  final case class SendTwitchLoginValidatorServiceCommand(cmd: TwitchLoginValidatorService.Command) extends Command
  final case class SendTwitchApiClientCommand(cmd: TwitchApiClient.Command) extends Command
  final case class SendYouTubeApiClientCommand(cmd: YouTubeApiClient.Command) extends Command

  def apply()(using randomProvider: RandomProvider, timeProvider: TimeProvider, settings: Settings): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx
    new ArchieMateMediator().operational()
  }
}

final class ArchieMateMediator(using
                               ctx: ActorContext[ArchieMateMediator.Command],
                               randomProvider: RandomProvider,
                               timeProvider: TimeProvider,
                               settings: Settings
                              ) {
  import ArchieMateMediator.*

  given ActorSystem[Nothing] = ctx.system
  given ActorRef[Command] = ctx.self
  given Timeout = settings.askTimeout

  case class State(
      httpClient: ActorRef[HttpClient.Request],
      twitchChatbotsSupervisor: ActorRef[TwitchChatbotsSupervisor.Command],
      twitchUserSessionsRepository: ActorRef[TwitchUserSessionsRepository.Command],
      youTubeChannelSessionsRepository: ActorRef[YouTubeChannelSessionsRepository.Command],
      automaticMessagesSettingsRepository: ActorRef[AutomaticMessagesSettingsRepository.Command],
      basicChatbotSettingsRepository: ActorRef[BasicChatbotSettingsRepository.Command],
      builtInCommandsSettingsRepository: ActorRef[BuiltInCommandsSettingsRepository.Command],
      commandsSettingsRepository: ActorRef[CommandsSettingsRepository.Command],
      overlaysSettingsRepository: ActorRef[OverlaysSettingsRepository.Command],
      timersSettingsRepository: ActorRef[TimersSettingsRepository.Command],
      variablesSettingsRepository: ActorRef[VariablesSettingsRepository.Command],
      twitchTokenUserCacheService: ActorRef[TwitchTokenUserCacheService.Command],
      commandsControllerHelperService: ActorRef[CommandsControllerHelperService.Command],
      oauthControllerHelperService: ActorRef[OAuthControllerHelperService.Command],
      settingsControllerHelperService: ActorRef[SettingsControllerHelperService.Command],
      userControllerHelperService: ActorRef[UserControllerHelperService.Command],
      jwtService: ActorRef[JWTService.Command],
      twitchLoginValidatorServiceCommand: ActorRef[TwitchLoginValidatorService.Command],
      twitchApiClient: ActorRef[TwitchApiClient.Command],
      youTubeApiClient: ActorRef[YouTubeApiClient.Command]
                  )

  object State {
    val initial = State(
      httpClient = ctx.spawn(HttpClient(HttpClient.PekkoHttpClientAdapter(Http())), HttpClient.actorName),
      twitchChatbotsSupervisor = ctx.spawn(TwitchChatbotsSupervisor(), TwitchChatbotsSupervisor.actorName),
      twitchUserSessionsRepository = ctx.spawn(TwitchUserSessionsRepository(), TwitchUserSessionsRepository.actorName),
      youTubeChannelSessionsRepository = ctx.spawn(YouTubeChannelSessionsRepository(), YouTubeChannelSessionsRepository.actorName),
      automaticMessagesSettingsRepository = ctx.spawn(AutomaticMessagesSettingsRepository(), AutomaticMessagesSettingsRepository.actorName),
      basicChatbotSettingsRepository = ctx.spawn(BasicChatbotSettingsRepository(), BasicChatbotSettingsRepository.actorName),
      builtInCommandsSettingsRepository = ctx.spawn(BuiltInCommandsSettingsRepository(), BuiltInCommandsSettingsRepository.actorName),
      commandsSettingsRepository = ctx.spawn(CommandsSettingsRepository(), CommandsSettingsRepository.actorName),
      overlaysSettingsRepository = ctx.spawn(OverlaysSettingsRepository(), OverlaysSettingsRepository.actorName),
      timersSettingsRepository = ctx.spawn(TimersSettingsRepository(), TimersSettingsRepository.actorName),
      variablesSettingsRepository = ctx.spawn(VariablesSettingsRepository(), VariablesSettingsRepository.actorName),
      twitchTokenUserCacheService = ctx.spawn(TwitchTokenUserCacheService(), TwitchTokenUserCacheService.actorName),
      commandsControllerHelperService = ctx.spawn(CommandsControllerHelperService(), CommandsControllerHelperService.actorName),
      oauthControllerHelperService = ctx.spawn(OAuthControllerHelperService(), OAuthControllerHelperService.actorName),
      settingsControllerHelperService = ctx.spawn(SettingsControllerHelperService(), SettingsControllerHelperService.actorName),
      userControllerHelperService = ctx.spawn(UserControllerHelperService(), UserControllerHelperService.actorName),
      jwtService = ctx.spawn(JWTService(), JWTService.actorName),
      twitchLoginValidatorServiceCommand = ctx.spawn(TwitchLoginValidatorService(), TwitchLoginValidatorService.actorName),
      twitchApiClient = ctx.spawn(TwitchApiClient(), TwitchApiClient.actorName),
      youTubeApiClient = ctx.spawn(YouTubeApiClient(), YouTubeApiClient.actorName)
    )
  }

  def operational(
      state: State = State.initial
  ):
  Behavior[Command] = Behaviors.receiveAndLogMessage {
    case SendHttpClientRequest(cmd) =>
      state.httpClient ! cmd
      Behaviors.same

    case SendTwitchChatbotsSupervisorCommand(cmd) =>
      state.twitchChatbotsSupervisor ! cmd
      Behaviors.same

    case SendTwitchUserSessionsRepositoryCommand(cmd) =>
      state.twitchUserSessionsRepository ! cmd
      Behaviors.same

    case SendYouTubeChannelSessionsRepositoryCommand(cmd) =>
      state.youTubeChannelSessionsRepository ! cmd
      Behaviors.same

    case SendAutomaticMessagesSettingsRepositoryCommand(cmd) =>
      state.automaticMessagesSettingsRepository ! cmd
      Behaviors.same

    case SendBasicChatbotSettingsRepositoryCommand(cmd) =>
      state.basicChatbotSettingsRepository ! cmd
      Behaviors.same

    case SendBuiltInCommandsSettingsRepositoryCommand(cmd) =>
      state.builtInCommandsSettingsRepository ! cmd
      Behaviors.same

    case SendCommandsSettingsRepositoryCommand(cmd) =>
      state.commandsSettingsRepository ! cmd
      Behaviors.same

    case SendOverlaysSettingsRepositoryCommand(cmd) =>
      state.overlaysSettingsRepository ! cmd
      Behaviors.same

    case SendTimersSettingsRepositoryCommand(cmd) =>
      state.timersSettingsRepository ! cmd
      Behaviors.same

    case SendVariablesSettingsRepositoryCommand(cmd) =>
      state.variablesSettingsRepository ! cmd
      Behaviors.same

    case SendTwitchTokenUserCacheServiceCommand(cmd) =>
      state.twitchTokenUserCacheService ! cmd
      Behaviors.same

    case SendCommandsControllerHelperServiceCommand(cmd) =>
      state.commandsControllerHelperService ! cmd
      Behaviors.same

    case SendOAuthControllerHelperServiceCommand(cmd) =>
      state.oauthControllerHelperService ! cmd
      Behaviors.same

    case SendSettingsControllerHelperServiceCommand(cmd) =>
      state.settingsControllerHelperService ! cmd
      Behaviors.same

    case SendUserControllerHelperServiceCommand(cmd) =>
      state.userControllerHelperService ! cmd
      Behaviors.same

    case SendJWTServiceCommand(cmd) =>
      state.jwtService ! cmd
      Behaviors.same

    case SendTwitchLoginValidatorServiceCommand(cmd) =>
      state.twitchLoginValidatorServiceCommand ! cmd
      Behaviors.same

    case SendTwitchApiClientCommand(cmd) =>
      state.twitchApiClient ! cmd
      Behaviors.same

    case SendYouTubeApiClientCommand(cmd) =>
      state.youTubeApiClient ! cmd
      Behaviors.same
  }
}
