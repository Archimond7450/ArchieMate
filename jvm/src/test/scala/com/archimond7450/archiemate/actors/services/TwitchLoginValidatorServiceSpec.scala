package com.archimond7450.archiemate.actors.services

import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.providers.RandomProvider
import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.testkit.typed.scaladsl.{ManualTime, ScalaTestWithActorTestKit}
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.duration.DurationInt

class TwitchLoginValidatorServiceSpec extends ScalaTestWithActorTestKit(ManualTime.config.withFallback(ConfigFactory.load("application-test.conf"))) with AnyWordSpecLike {
  given settings: Settings = Settings(testKit.system)

  private given randomProvider: RandomProvider = new RandomProvider

  "A Twitch Login Validator Service" when {
    "request uuid is not present" should {
      "respond with InvalidRequest repeatedly if such request was never created" in {
        val twitchLoginValidatorService = testKit.spawn(TwitchLoginValidatorService())
        val probe = testKit.createTestProbe[TwitchLoginValidatorService.Response]()

        val uuid = randomProvider.uuid()

        twitchLoginValidatorService ! TwitchLoginValidatorService.RequestSucceeded(probe.ref, uuid)
        probe.expectMessage(TwitchLoginValidatorService.InvalidRequest(uuid))

        val manualTime = ManualTime()
        for (_ <- 1 to 10) {
          manualTime.timePasses(5.seconds)

          twitchLoginValidatorService ! TwitchLoginValidatorService.RequestSucceeded(probe.ref, uuid)
          probe.expectMessage(TwitchLoginValidatorService.InvalidRequest(uuid))
        }
      }

      "respond with InvalidRequest if such request was created but expired" in {
        val twitchLoginValidatorService = testKit.spawn(TwitchLoginValidatorService())
        val probe = testKit.createTestProbe[TwitchLoginValidatorService.Response]()

        twitchLoginValidatorService ! TwitchLoginValidatorService.NewRequest(probe.ref)
        val newRequest = probe.expectMessageType[TwitchLoginValidatorService.NewRequestCreated]

        val manualTime = ManualTime()
        manualTime.timePasses(settings.newLoginExpirationTime + 1.second)

        twitchLoginValidatorService ! TwitchLoginValidatorService.RequestSucceeded(probe.ref, newRequest.uuid)
        probe.expectMessage(TwitchLoginValidatorService.InvalidRequest(newRequest.uuid))
      }

      "respond with InvalidRequest repeatedly if two requests were created but expired" in {
        val twitchLoginValidatorService = testKit.spawn(TwitchLoginValidatorService())
        val probe = testKit.createTestProbe[TwitchLoginValidatorService.Response]()

        twitchLoginValidatorService ! TwitchLoginValidatorService.NewRequest(probe.ref)
        val firstRequest = probe.expectMessageType[TwitchLoginValidatorService.NewRequestCreated]

        val manualTime = ManualTime()
        manualTime.timePasses(settings.newLoginExpirationTime - 1.second)

        twitchLoginValidatorService ! TwitchLoginValidatorService.NewRequest(probe.ref)
        val secondRequest = probe.expectMessageType[TwitchLoginValidatorService.NewRequestCreated]

        manualTime.timePasses(2.seconds)

        twitchLoginValidatorService ! TwitchLoginValidatorService.RequestSucceeded(probe.ref, firstRequest.uuid)
        probe.expectMessage(TwitchLoginValidatorService.InvalidRequest(firstRequest.uuid))

        manualTime.timePasses(settings.newLoginExpirationTime)
        twitchLoginValidatorService ! TwitchLoginValidatorService.RequestSucceeded(probe.ref, firstRequest.uuid)
        twitchLoginValidatorService ! TwitchLoginValidatorService.RequestSucceeded(probe.ref, secondRequest.uuid)
        probe.expectMessage(TwitchLoginValidatorService.InvalidRequest(firstRequest.uuid))
        probe.expectMessage(TwitchLoginValidatorService.InvalidRequest(secondRequest.uuid))
      }
    }

    "request uuid is present" should {
      "respond with Acknowledged if RequestSucceeded is received immediately and exactly once for a valid request" in {
        val twitchLoginValidatorService = testKit.spawn(TwitchLoginValidatorService())
        val probe = testKit.createTestProbe[TwitchLoginValidatorService.Response]()

        twitchLoginValidatorService ! TwitchLoginValidatorService.NewRequest(probe.ref)
        val request = probe.expectMessageType[TwitchLoginValidatorService.NewRequestCreated]

        val command = TwitchLoginValidatorService.RequestSucceeded(probe.ref, request.uuid)
        twitchLoginValidatorService ! command
        probe.expectMessage(TwitchLoginValidatorService.Acknowledged(command))
      }

      "respond with Acknowledged if RequestSucceeded is received just in time and exactly once for a valid request" in {
        val twitchLoginValidatorService = testKit.spawn(TwitchLoginValidatorService())
        val probe = testKit.createTestProbe[TwitchLoginValidatorService.Response]()

        twitchLoginValidatorService ! TwitchLoginValidatorService.NewRequest(probe.ref)
        val request = probe.expectMessageType[TwitchLoginValidatorService.NewRequestCreated]

        val manualTime = ManualTime()
        manualTime.timePasses(settings.newLoginExpirationTime - 1.second)

        val command = TwitchLoginValidatorService.RequestSucceeded(probe.ref, request.uuid)
        twitchLoginValidatorService ! command
        probe.expectMessage(TwitchLoginValidatorService.Acknowledged(command))
      }

      "respond with InvalidRequest if more than one RequestSucceeded is received for a valid request" in {
        val twitchLoginValidatorService = testKit.spawn(TwitchLoginValidatorService())
        val probe = testKit.createTestProbe[TwitchLoginValidatorService.Response]()

        twitchLoginValidatorService ! TwitchLoginValidatorService.NewRequest(probe.ref)
        val newRequest = probe.expectMessageType[TwitchLoginValidatorService.NewRequestCreated]

        val cmd = TwitchLoginValidatorService.RequestSucceeded(probe.ref, newRequest.uuid)
        twitchLoginValidatorService ! cmd
        probe.expectMessage(TwitchLoginValidatorService.Acknowledged(cmd))

        val expectedResponse = TwitchLoginValidatorService.InvalidRequest(newRequest.uuid)
        for (_ <- 1 to 10) {
          twitchLoginValidatorService ! cmd
          probe.expectMessage(expectedResponse)
        }
      }
    }
  }
}
