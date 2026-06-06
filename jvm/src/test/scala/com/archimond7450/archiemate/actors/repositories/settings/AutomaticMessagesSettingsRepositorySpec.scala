package com.archimond7450.archiemate.actors.repositories.settings

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.chatbot.ChatbotsSupervisor
import com.archimond7450.archiemate.http.ChannelSettings.AutomaticMessagesSettings
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.apache.pekko.actor.typed.ActorRef
import org.scalatest.wordspec.AnyWordSpecLike

class AutomaticMessagesSettingsRepositorySpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike {
  import AutomaticMessagesSettingsRepositorySpec.*

  "An Automatic Messages Settings Repository" should {
    val chatbotsSupervisorProbe =
      testKit.createTestProbe[ChatbotsSupervisor.Command](
        ChatbotsSupervisor.actorName
      )
    given chatbotsSupervisor: ActorRef[ChatbotsSupervisor.Command] =
      chatbotsSupervisorProbe.ref
    val mediatorProbe = testKit.createTestProbe[ArchieMateMediator.Command](
      ArchieMateMediator.actorName
    )
    given mediator: ActorRef[ArchieMateMediator.Command] = mediatorProbe.ref

    "return default settings when GetSettings is received for any twitchRoomId if initially started" in {
      val automaticMessagesSettingsRepository = testKit.spawn(
        AutomaticMessagesSettingsRepository(),
        s"${AutomaticMessagesSettingsRepository.actorName}0"
      )
      val probe = testKit.createTestProbe[AutomaticMessagesSettings]()

      for (roomId <- Seq(firstRoomId, secondRoomId, thirdRoomId)) {
        automaticMessagesSettingsRepository ! AutomaticMessagesSettingsRepository
          .GetSettings(probe.ref, roomId)
        probe.expectMessage(defaultSettings)
      }

      mediatorProbe.expectNoMessage()
      chatbotsSupervisorProbe.expectNoMessage()
    }

    "return acknowledge message when settings change and notify twitch chatbots supervisor of the change" in {
      val automaticMessagesSettingsRepository = testKit.spawn(
        AutomaticMessagesSettingsRepository(),
        s"${AutomaticMessagesSettingsRepository.actorName}1"
      )
      val probe = testKit.createTestProbe[
        AutomaticMessagesSettingsRepository.Acknowledged.type
      ]()

      val roomId = firstRoomId

      automaticMessagesSettingsRepository ! AutomaticMessagesSettingsRepository
        .ChangeSettings(probe.ref, roomId, onlyStreamSettings)
      probe.expectMessage(AutomaticMessagesSettingsRepository.Acknowledged)

      val mediatorCmd = mediatorProbe.expectMessageType[
        ArchieMateMediator.SendChatbotsSupervisorCommand
      ]
      chatbotsSupervisor.ref ! mediatorCmd.cmd
      chatbotsSupervisorProbe.expectMessage(
        ChatbotsSupervisor.NewChannelSettingsEvent(
          roomId,
          ChatbotsSupervisor.AutomaticMessagesSettingsChanged(
            onlyStreamSettings
          )
        )
      )

      mediatorProbe.expectNoMessage()
      chatbotsSupervisorProbe.expectNoMessage()
    }

    "return default settings when settings are not present for twitchRoomId, otherwise return the stored settings when GetSettings message is received" in {
      val automaticMessagesSettingsRepository = testKit.spawn(
        AutomaticMessagesSettingsRepository(),
        s"${AutomaticMessagesSettingsRepository.actorName}2"
      )
      val probe = testKit.createTestProbe[AutomaticMessagesSettings]()

      for (
        roomId -> expectedSettings <- Map(
          firstRoomId -> onlyStreamSettings,
          secondRoomId -> defaultSettings,
          thirdRoomId -> defaultSettings
        )
      ) {
        automaticMessagesSettingsRepository ! AutomaticMessagesSettingsRepository
          .GetSettings(probe.ref, roomId)
        probe.expectMessage(expectedSettings)
      }

      mediatorProbe.expectNoMessage()
      chatbotsSupervisorProbe.expectNoMessage()
    }

    "return acknowledge message when settings reset and notify twitch chatbots supervisor of the change" in {
      val automaticMessagesSettingsRepository = testKit.spawn(
        AutomaticMessagesSettingsRepository(),
        s"${AutomaticMessagesSettingsRepository.actorName}3"
      )
      val probe = testKit.createTestProbe[
        AutomaticMessagesSettingsRepository.Acknowledged.type
      ]()

      val roomId = firstRoomId

      automaticMessagesSettingsRepository ! AutomaticMessagesSettingsRepository
        .ResetSettings(probe.ref, roomId)
      probe.expectMessage(AutomaticMessagesSettingsRepository.Acknowledged)

      val mediatorCmd = mediatorProbe.expectMessageType[
        ArchieMateMediator.SendChatbotsSupervisorCommand
      ]
      chatbotsSupervisor.ref ! mediatorCmd.cmd
      chatbotsSupervisorProbe.expectMessage(
        ChatbotsSupervisor.NewChannelSettingsEvent(
          roomId,
          ChatbotsSupervisor.AutomaticMessagesSettingsChanged(
            defaultSettings
          )
        )
      )

      mediatorProbe.expectNoMessage()
      chatbotsSupervisorProbe.expectNoMessage()
    }

    "return default settings when settings are not present for twitchRoomId, otherwise return the stored settings when GetSettings message is received #2" in {
      val automaticMessagesSettingsRepository = testKit.spawn(
        AutomaticMessagesSettingsRepository(),
        s"${AutomaticMessagesSettingsRepository.actorName}4"
      )
      val probe = testKit.createTestProbe[AutomaticMessagesSettings]()

      for (roomId <- Seq(firstRoomId, secondRoomId, thirdRoomId)) {
        automaticMessagesSettingsRepository ! AutomaticMessagesSettingsRepository
          .GetSettings(probe.ref, roomId)
        probe.expectMessage(defaultSettings)
      }

      mediatorProbe.expectNoMessage()
      chatbotsSupervisorProbe.expectNoMessage()
    }
  }
}

object AutomaticMessagesSettingsRepositorySpec {
  val firstRoomId = "1"
  val secondRoomId = "2"
  val thirdRoomId = "3"

  val defaultSettings = AutomaticMessagesSettings()
  val onlyStreamSettings = AutomaticMessagesSettings(
    streamStart = Some("The stream started! Welcome all!"),
    streamEnd = Some("The stream just ended. Thanks for watching!")
  )
}
