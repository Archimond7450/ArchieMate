package com.archimond7450.archiemate.actors.repositories.settings

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.chatbot.TwitchChatbotsSupervisor
import com.archimond7450.archiemate.http.ChannelSettings.OverlaysSettings
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}

object OverlaysSettingsRepository {
  val actorName = "OverlaysSettingsRepository"

  type Command = GenericSettingsRepository.Command[OverlaysSettings]

  val GetSettings: (ActorRef[OverlaysSettings], String) => GenericSettingsRepository.GetSettings[OverlaysSettings] = GenericSettingsRepository.GetSettings.apply
  val ChangeSettings: (ActorRef[Acknowledged.type], String, OverlaysSettings) => GenericSettingsRepository.ChangeSettings[OverlaysSettings] = GenericSettingsRepository.ChangeSettings.apply
  val ResetSettings: (ActorRef[Acknowledged.type], String) => GenericSettingsRepository.ResetSettings = GenericSettingsRepository.ResetSettings.apply

  val Acknowledged: GenericSettingsRepository.Acknowledged.type = GenericSettingsRepository.Acknowledged

  def apply()(using mediator: ActorRef[ArchieMateMediator.Command]): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx

    new GenericSettingsRepository[OverlaysSettings] {
      override protected val actorName: String = OverlaysSettingsRepository.actorName
      override protected val defaultSettings: OverlaysSettings = OverlaysSettings()
      override protected val twitchChatbotsSupervisorNotificationBuilder: OverlaysSettings => TwitchChatbotsSupervisor.SettingsEvent = TwitchChatbotsSupervisor.OverlaysSettingsChanged.apply
    }.eventSourcedBehavior()
  }
}
