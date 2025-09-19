package com.archimond7450.archiemate.actors.repositories.settings

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.chatbot.TwitchChatbotsSupervisor
import com.archimond7450.archiemate.http.ChannelSettings.TimersSettings
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}

object TimersSettingsRepository {
  val actorName = "TimersSettingsRepository"

  type Command = GenericSettingsRepository.Command[TimersSettings]

  val GetSettings: (ActorRef[TimersSettings], String) => GenericSettingsRepository.GetSettings[TimersSettings] = GenericSettingsRepository.GetSettings.apply
  val ChangeSettings: (ActorRef[Acknowledged.type], String, TimersSettings) => GenericSettingsRepository.ChangeSettings[TimersSettings] = GenericSettingsRepository.ChangeSettings.apply
  val ResetSettings: (ActorRef[Acknowledged.type], String) => GenericSettingsRepository.ResetSettings = GenericSettingsRepository.ResetSettings.apply

  val Acknowledged: GenericSettingsRepository.Acknowledged.type = GenericSettingsRepository.Acknowledged

  def apply()(using mediator: ActorRef[ArchieMateMediator.Command]): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx

    new GenericSettingsRepository[TimersSettings] {
      override protected val actorName: String = TimersSettingsRepository.actorName
      override protected val defaultSettings: TimersSettings = TimersSettings()
      override protected val twitchChatbotsSupervisorNotificationBuilder: TimersSettings => TwitchChatbotsSupervisor.SettingsEvent = TwitchChatbotsSupervisor.TimersSettingsChanged.apply
    }.eventSourcedBehavior()
  }
}
