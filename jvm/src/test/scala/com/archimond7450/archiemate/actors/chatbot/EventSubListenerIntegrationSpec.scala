package com.archimond7450.archiemate.actors.chatbot

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class EventSubListenerIntegrationSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers {
  given ActorSystem = system.classicSystem
  import testKit.internalSystem.executionContext

  "EventSubListener" should {

    "connect to Twitch EventSub, subscribe to events and handle them correctly" in {

    }
  }
}
