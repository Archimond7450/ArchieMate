package com.archimond7450.archiemate.actors.repositories.settings

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.chatbot.ChatbotsSupervisor
import com.archimond7450.archiemate.http.ChannelSettings.BuiltInCommandsSettings
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.apache.pekko.actor.typed.ActorRef
import org.scalatest.wordspec.AnyWordSpecLike

class BuiltInCommandsSettingsRepositorySpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike {
  import BuiltInCommandsSettingsRepositorySpec.*

  "A Built-in Commands Settings Repository" should {
    val ChatbotsSupervisorProbe =
      testKit.createTestProbe[ChatbotsSupervisor.Command](
        ChatbotsSupervisor.actorName
      )
    given chatbotsSupervisor: ActorRef[ChatbotsSupervisor.Command] =
      ChatbotsSupervisorProbe.ref
    val mediatorProbe = testKit.createTestProbe[ArchieMateMediator.Command](
      ArchieMateMediator.actorName
    )
    given mediator: ActorRef[ArchieMateMediator.Command] = mediatorProbe.ref

    "return default settings when GetSettings is received for any twitchRoomId if initially started" in {
      val builtInCommandsSettingsRepository = testKit.spawn(
        BuiltInCommandsSettingsRepository(),
        s"${BuiltInCommandsSettingsRepository.actorName}0"
      )
      val probe = testKit.createTestProbe[BuiltInCommandsSettings]()

      for (roomId <- Seq(firstRoomId, secondRoomId, thirdRoomId)) {
        builtInCommandsSettingsRepository ! BuiltInCommandsSettingsRepository
          .GetSettings(probe.ref, roomId)
        probe.expectMessage(defaultSettings)
      }

      mediatorProbe.expectNoMessage()
      ChatbotsSupervisorProbe.expectNoMessage()
    }

    "return acknowledge message when settings change and notify twitch chatbots supervisor of the change" in {
      val builtInCommandsSettingsRepository = testKit.spawn(
        BuiltInCommandsSettingsRepository(),
        s"${BuiltInCommandsSettingsRepository.actorName}1"
      )
      val probe = testKit
        .createTestProbe[BuiltInCommandsSettingsRepository.Acknowledged.type]()

      for (
        roomId -> newSettings <- Map(
          firstRoomId -> minimumSettings,
          secondRoomId -> fullSettings
        )
      ) {
        builtInCommandsSettingsRepository ! BuiltInCommandsSettingsRepository
          .ChangeSettings(
            probe.ref,
            roomId,
            newSettings
          )
        probe.expectMessage(BuiltInCommandsSettingsRepository.Acknowledged)

        val mediatorCmd = mediatorProbe.expectMessageType[
          ArchieMateMediator.SendChatbotsSupervisorCommand
        ]
        chatbotsSupervisor ! mediatorCmd.cmd
        ChatbotsSupervisorProbe.expectMessage(
          ChatbotsSupervisor.NewChannelSettingsEvent(
            roomId,
            ChatbotsSupervisor.BuiltInCommandsSettingsChanged(newSettings)
          )
        )
      }

      mediatorProbe.expectNoMessage()
      ChatbotsSupervisorProbe.expectNoMessage()
    }

    "return default settings when settings are not present for twitchRoomId, otherwise return the stored settings when GetSettings message is received" in {
      val builtInCommandsSettingsRepository = testKit.spawn(
        BuiltInCommandsSettingsRepository(),
        s"${BuiltInCommandsSettingsRepository.actorName}2"
      )
      val probe = testKit.createTestProbe[BuiltInCommandsSettings]()

      for (
        roomId -> expectedSettings <- Map(
          firstRoomId -> minimumSettings,
          secondRoomId -> fullSettings,
          thirdRoomId -> defaultSettings
        )
      ) {
        builtInCommandsSettingsRepository ! BuiltInCommandsSettingsRepository
          .GetSettings(probe.ref, roomId)
        probe.expectMessage(expectedSettings)
      }

      mediatorProbe.expectNoMessage()
      ChatbotsSupervisorProbe.expectNoMessage()
    }

    "return acknowledge message when settings reset and notify twitch chatbots supervisor of the change" in {
      val builtInCommandsSettingsRepository = testKit.spawn(
        BuiltInCommandsSettingsRepository(),
        s"${BuiltInCommandsSettingsRepository.actorName}3"
      )
      val probe = testKit
        .createTestProbe[BuiltInCommandsSettingsRepository.Acknowledged.type]()

      val roomId = firstRoomId
      builtInCommandsSettingsRepository ! BuiltInCommandsSettingsRepository
        .ResetSettings(probe.ref, roomId)
      probe.expectMessage(BuiltInCommandsSettingsRepository.Acknowledged)

      val mediatorCmd = mediatorProbe.expectMessageType[
        ArchieMateMediator.SendChatbotsSupervisorCommand
      ]
      chatbotsSupervisor ! mediatorCmd.cmd
      ChatbotsSupervisorProbe.expectMessage(
        ChatbotsSupervisor.NewChannelSettingsEvent(
          roomId,
          ChatbotsSupervisor.BuiltInCommandsSettingsChanged(
            defaultSettings
          )
        )
      )

      mediatorProbe.expectNoMessage()
      ChatbotsSupervisorProbe.expectNoMessage()
    }

    "return default settings when settings are not present for twitchRoomId, otherwise return the stored settings when GetSettings message is received #2" in {
      val builtInCommandsSettingsRepository = testKit.spawn(
        BuiltInCommandsSettingsRepository(),
        s"${BuiltInCommandsSettingsRepository.actorName}4"
      )
      val probe = testKit.createTestProbe[BuiltInCommandsSettings]()

      for (
        roomId -> expectedSettings <- Map(
          firstRoomId -> defaultSettings,
          secondRoomId -> fullSettings,
          thirdRoomId -> defaultSettings
        )
      ) {
        builtInCommandsSettingsRepository ! BuiltInCommandsSettingsRepository
          .GetSettings(probe.ref, roomId)
        probe.expectMessage(expectedSettings)
      }

      mediatorProbe.expectNoMessage()
      ChatbotsSupervisorProbe.expectNoMessage()
    }
  }
}

object BuiltInCommandsSettingsRepositorySpec {
  val firstRoomId = "1"
  val secondRoomId = "2"
  val thirdRoomId = "3"

  val defaultSettings = BuiltInCommandsSettings()
  val minimumSettings = BuiltInCommandsSettings(game = true, title = true)
  val fullSettings = BuiltInCommandsSettings(
    game = true,
    title = true,
    subs = true,
    uptime = true,
    followage = true,
    afk = true
  )
}
