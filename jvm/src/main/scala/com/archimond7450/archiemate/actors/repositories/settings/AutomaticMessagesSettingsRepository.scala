package com.archimond7450.archiemate.actors.repositories.settings

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.chatbot.TwitchChatbotsSupervisor
import com.archimond7450.archiemate.http.ChannelSettings.AutomaticMessagesSettings
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}

object AutomaticMessagesSettingsRepository {
  val actorName = "AutomaticMessagesSettingsRepository"

  type Command = GenericSettingsRepository.Command[AutomaticMessagesSettings]

  val GetSettings: (ActorRef[AutomaticMessagesSettings], String) => GenericSettingsRepository.GetSettings[AutomaticMessagesSettings] = GenericSettingsRepository.GetSettings.apply
  val ChangeSettings: (ActorRef[Acknowledged.type], String, AutomaticMessagesSettings) => GenericSettingsRepository.ChangeSettings[AutomaticMessagesSettings] = GenericSettingsRepository.ChangeSettings.apply
  val ResetSettings: (ActorRef[Acknowledged.type], String) => GenericSettingsRepository.ResetSettings = GenericSettingsRepository.ResetSettings.apply

  val Acknowledged: GenericSettingsRepository.Acknowledged.type = GenericSettingsRepository.Acknowledged

  def apply()(using mediator: ActorRef[ArchieMateMediator.Command]): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx

    new GenericSettingsRepository[AutomaticMessagesSettings] {
      override protected val actorName: String = AutomaticMessagesSettingsRepository.actorName
      override protected val defaultSettings: AutomaticMessagesSettings = AutomaticMessagesSettings()
      override protected val twitchChatbotsSupervisorNotificationBuilder: AutomaticMessagesSettings => TwitchChatbotsSupervisor.SettingsEvent = TwitchChatbotsSupervisor.AutomaticMessagesSettingsChanged.apply
    }.eventSourcedBehavior()
  }
}
