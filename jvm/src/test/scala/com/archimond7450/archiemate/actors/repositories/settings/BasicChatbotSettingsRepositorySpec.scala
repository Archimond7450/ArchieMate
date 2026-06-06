package com.archimond7450.archiemate.actors.repositories.settings

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.chatbot.ChatbotsSupervisor
import com.archimond7450.archiemate.http.ChannelSettings.BasicChatbotSettings
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.apache.pekko.actor.typed.ActorRef
import org.scalatest.wordspec.AnyWordSpecLike

class BasicChatbotSettingsRepositorySpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike {
  import BasicChatbotSettingsRepositorySpec.*

  "A Basic Chatbot Settings Repository" should {
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
      val basicChatbotSettingsRepository = testKit.spawn(
        BasicChatbotSettingsRepository(),
        s"${BasicChatbotSettingsRepository.actorName}0"
      )
      val probe = testKit.createTestProbe[BasicChatbotSettings]()

      for (roomId <- Seq(firstRoomId, secondRoomId, thirdRoomId)) {
        basicChatbotSettingsRepository ! BasicChatbotSettingsRepository
          .GetSettings(
            probe.ref,
            roomId
          )
        probe.expectMessage(defaultSettings)
      }

      mediatorProbe.expectNoMessage()
      ChatbotsSupervisorProbe.expectNoMessage()
    }

    "return acknowledge message when settings change and notify twitch chatbots supervisor of the change" in {
      val basicChatbotSettingsRepository = testKit.spawn(
        BasicChatbotSettingsRepository(),
        s"${BasicChatbotSettingsRepository.actorName}1"
      )
      val probe = testKit
        .createTestProbe[BasicChatbotSettingsRepository.Acknowledged.type]()

      for (roomId <- Seq(firstRoomId, thirdRoomId)) {
        basicChatbotSettingsRepository ! BasicChatbotSettingsRepository
          .ChangeSettings(
            probe.ref,
            roomId,
            newSettings
          )
        probe.expectMessage(BasicChatbotSettingsRepository.Acknowledged)

        val mediatorCmd = mediatorProbe.expectMessageType[
          ArchieMateMediator.SendChatbotsSupervisorCommand
        ]
        chatbotsSupervisor ! mediatorCmd.cmd
        ChatbotsSupervisorProbe.expectMessage(
          ChatbotsSupervisor.NewChannelSettingsEvent(
            roomId,
            ChatbotsSupervisor.BasicChatbotSettingsChanged(newSettings)
          )
        )
      }

      mediatorProbe.expectNoMessage()
      ChatbotsSupervisorProbe.expectNoMessage()
    }

    "return default settings when settings are not present for twitchRoomId, otherwise return the stored settings when GetSettings message is received" in {
      val basicChatbotSettingsRepository = testKit.spawn(
        BasicChatbotSettingsRepository(),
        s"${BasicChatbotSettingsRepository.actorName}2"
      )

      val mediatorChatbotsSupervisorCommand1 = mediatorProbe.expectMessageType[
        ArchieMateMediator.SendChatbotsSupervisorCommand
      ]
      chatbotsSupervisor ! mediatorChatbotsSupervisorCommand1.cmd
      ChatbotsSupervisorProbe.expectMessage(
        ChatbotsSupervisor.Join(firstRoomId)
      )

      val mediatorChatbotsSupervisorCommand2 = mediatorProbe.expectMessageType[
        ArchieMateMediator.SendChatbotsSupervisorCommand
      ]
      chatbotsSupervisor ! mediatorChatbotsSupervisorCommand2.cmd
      ChatbotsSupervisorProbe.expectMessage(
        ChatbotsSupervisor.Join(thirdRoomId)
      )

      val probe = testKit.createTestProbe[BasicChatbotSettings]()

      for (roomId <- Seq(firstRoomId, secondRoomId, thirdRoomId)) {
        basicChatbotSettingsRepository ! BasicChatbotSettingsRepository
          .GetSettings(
            probe.ref,
            roomId
          )
        probe.expectMessage(
          if (roomId == secondRoomId) defaultSettings else newSettings
        )
      }

      mediatorProbe.expectNoMessage()
      ChatbotsSupervisorProbe.expectNoMessage()
    }

    "return acknowledge message when settings reset and notify twitch chatbots supervisor of the change" in {
      val basicChatbotSettingsRepository = testKit.spawn(
        BasicChatbotSettingsRepository(),
        s"${BasicChatbotSettingsRepository.actorName}3"
      )

      val mediatorChatbotsSupervisorCommand1 = mediatorProbe.expectMessageType[
        ArchieMateMediator.SendChatbotsSupervisorCommand
      ]
      chatbotsSupervisor ! mediatorChatbotsSupervisorCommand1.cmd
      ChatbotsSupervisorProbe.expectMessage(
        ChatbotsSupervisor.Join(firstRoomId)
      )

      val mediatorChatbotsSupervisorCommand2 = mediatorProbe.expectMessageType[
        ArchieMateMediator.SendChatbotsSupervisorCommand
      ]
      chatbotsSupervisor ! mediatorChatbotsSupervisorCommand2.cmd
      ChatbotsSupervisorProbe.expectMessage(
        ChatbotsSupervisor.Join(thirdRoomId)
      )

      val probe = testKit
        .createTestProbe[BasicChatbotSettingsRepository.Acknowledged.type]()

      val roomId = thirdRoomId

      basicChatbotSettingsRepository ! BasicChatbotSettingsRepository
        .ResetSettings(
          probe.ref,
          roomId
        )
      probe.expectMessage(BasicChatbotSettingsRepository.Acknowledged)

      val mediatorCmd = mediatorProbe.expectMessageType[
        ArchieMateMediator.SendChatbotsSupervisorCommand
      ]
      chatbotsSupervisor ! mediatorCmd.cmd
      ChatbotsSupervisorProbe.expectMessage(
        ChatbotsSupervisor.NewChannelSettingsEvent(
          roomId,
          ChatbotsSupervisor.BasicChatbotSettingsChanged(defaultSettings)
        )
      )

      mediatorProbe.expectNoMessage()
      ChatbotsSupervisorProbe.expectNoMessage()
    }

    "return default settings when settings are not present for twitchRoomId, otherwise return the stored settings when GetSettings message is received #2" in {
      val basicChatbotSettingsRepository = testKit.spawn(
        BasicChatbotSettingsRepository(),
        s"${BasicChatbotSettingsRepository.actorName}4"
      )

      val mediatorChatbotsSupervisorCommand = mediatorProbe.expectMessageType[
        ArchieMateMediator.SendChatbotsSupervisorCommand
      ]
      chatbotsSupervisor ! mediatorChatbotsSupervisorCommand.cmd
      ChatbotsSupervisorProbe.expectMessage(
        ChatbotsSupervisor.Join(firstRoomId)
      )

      val probe = testKit.createTestProbe[BasicChatbotSettings]()

      for (roomId <- Seq(firstRoomId, secondRoomId, thirdRoomId)) {
        basicChatbotSettingsRepository ! BasicChatbotSettingsRepository
          .GetSettings(
            probe.ref,
            roomId
          )
        probe.expectMessage(
          if (roomId == firstRoomId) newSettings else defaultSettings
        )
      }

      mediatorProbe.expectNoMessage()
      ChatbotsSupervisorProbe.expectNoMessage()
    }
  }
}

object BasicChatbotSettingsRepositorySpec {
  val firstRoomId = "1"
  val secondRoomId = "2"
  val thirdRoomId = "3"

  val defaultSettings = BasicChatbotSettings()
  val newSettings = BasicChatbotSettings(join = true)
}
