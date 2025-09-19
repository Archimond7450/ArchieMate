package com.archimond7450.archiemate.actors.repositories.settings

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.chatbot.TwitchChatbotsSupervisor
import com.archimond7450.archiemate.actors.repositories.settings
import com.archimond7450.archiemate.http.ChannelSettings.BasicChatbotSettings
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}

object BasicChatbotSettingsRepository {
  val actorName = "BasicChatbotSettingsRepository"

  type Command = GenericSettingsRepository.Command[BasicChatbotSettings]

  val GetSettings: (ActorRef[BasicChatbotSettings], String) => GenericSettingsRepository.GetSettings[BasicChatbotSettings] = GenericSettingsRepository.GetSettings.apply
  val ChangeSettings: (ActorRef[GenericSettingsRepository.Acknowledged.type], String, BasicChatbotSettings) => GenericSettingsRepository.ChangeSettings[BasicChatbotSettings] = GenericSettingsRepository.ChangeSettings.apply
  val ResetSettings: (ActorRef[GenericSettingsRepository.Acknowledged.type], String) => GenericSettingsRepository.ResetSettings = GenericSettingsRepository.ResetSettings.apply

  def apply()(using mediator: ActorRef[ArchieMateMediator.Command]): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx

    new GenericSettingsRepository[BasicChatbotSettings] {
      override protected val actorName: String = BasicChatbotSettingsRepository.actorName
      override protected val defaultSettings: BasicChatbotSettings = BasicChatbotSettings()
      override protected val twitchChatbotsSupervisorNotificationBuilder: BasicChatbotSettings => TwitchChatbotsSupervisor.SettingsEvent = TwitchChatbotsSupervisor.BasicChatbotSettingsChanged.apply
    }.eventSourcedBehavior()
  }
}
