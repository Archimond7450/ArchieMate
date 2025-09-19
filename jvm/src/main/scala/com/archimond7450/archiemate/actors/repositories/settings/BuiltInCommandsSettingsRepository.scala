package com.archimond7450.archiemate.actors.repositories.settings

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.chatbot.TwitchChatbotsSupervisor
import com.archimond7450.archiemate.actors.repositories.settings
import com.archimond7450.archiemate.http.ChannelSettings.BuiltInCommandsSettings
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

object BuiltInCommandsSettingsRepository {
  val actorName = "BuiltInCommandsSettingsRepository"

  type Command = GenericSettingsRepository.Command[BuiltInCommandsSettings]

  val GetSettings: (ActorRef[BuiltInCommandsSettings], String) => GenericSettingsRepository.GetSettings[BuiltInCommandsSettings] = GenericSettingsRepository.GetSettings.apply
  val ChangeSettings: (ActorRef[GenericSettingsRepository.Acknowledged.type], String, BuiltInCommandsSettings) => GenericSettingsRepository.ChangeSettings[BuiltInCommandsSettings] = GenericSettingsRepository.ChangeSettings.apply
  val ResetSettings: (ActorRef[GenericSettingsRepository.Acknowledged.type], String) => GenericSettingsRepository.ResetSettings = GenericSettingsRepository.ResetSettings.apply

  def apply()(using mediator: ActorRef[ArchieMateMediator.Command]): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx

    new GenericSettingsRepository[BuiltInCommandsSettings] {
      override protected val actorName: String = BuiltInCommandsSettingsRepository.actorName
      override protected val defaultSettings: BuiltInCommandsSettings = BuiltInCommandsSettings()
      override protected val twitchChatbotsSupervisorNotificationBuilder: BuiltInCommandsSettings => TwitchChatbotsSupervisor.SettingsEvent = TwitchChatbotsSupervisor.BuiltInCommandsSettingsChanged.apply
    }.eventSourcedBehavior()
  }
}
