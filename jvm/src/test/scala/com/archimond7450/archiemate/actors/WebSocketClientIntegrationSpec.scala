package com.archimond7450.archiemate.actors

import org.apache.pekko
import pekko.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import pekko.http.scaladsl.Http
import pekko.http.scaladsl.model._
import pekko.http.scaladsl.model.ws._
import pekko.http.scaladsl.server.Directives._
import pekko.stream.scaladsl.{Flow, Sink, Source}
import pekko.util.ByteString
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class WebSocketClientIntegrationSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with Matchers {

  given classic: pekko.actor.ActorSystem = system.classicSystem
  import testKit.internalSystem.executionContext

  sealed trait ParentMsg
  case object Connected extends ParentMsg
  final case class GotText(text: String) extends ParentMsg
  final case class GotBinary(data: ByteString) extends ParentMsg
  case object Done extends ParentMsg
  final case class Failed(ex: Throwable) extends ParentMsg

  "WebSocketClient" should {

    "connect to an echo WebSocket server and handle text and binary messages" in {
      // --- Step 1: Start a local WebSocket echo server
      val echoFlow: Flow[Message, Message, Any] = Flow[Message]
        .takeWhile {
          case TextMessage.Strict("close") => false
          case _                           => true
        }
        .map {
          case TextMessage.Strict(text) =>
            TextMessage.Strict("echo: " + text)
          case BinaryMessage.Strict(bin) =>
            BinaryMessage.Strict(ByteString("echo: ") ++ bin)
          case streamed: TextMessage.Streamed =>
            TextMessage(streamed.textStream.map(txt => "echo: " + txt))
          case streamed: BinaryMessage.Streamed =>
            BinaryMessage(
              streamed.dataStream.map(bytes => ByteString("echo: ") ++ bytes)
            )
        }

      val route = path("ws-echo") {
        handleWebSocketMessages(echoFlow)
      }

      val binding =
        Await.result(Http().newServerAt("localhost", 0).bind(route), 5.seconds)
      val port = binding.localAddress.getPort
      val wsUri = Uri(s"ws://localhost:$port/ws-echo")

      // --- Step 2: Spawn the WebSocketClient actor
      val parent = TestProbe[ParentMsg]()

      val client = spawn(
        WebSocketClient(
          uri = wsUri,
          parent.ref,
          connectedNotification = () => Connected,
          textMessageTransform = msg => GotText(msg.text),
          binaryMessageTransform = msg => GotBinary(msg.data),
          doneNotification = () => Done,
          failNotification = ex => Failed(ex)
        )
      )

      // --- Step 3: Send and verify strict text/binary
      client ! WebSocketClient.SendText("hello")
      client ! WebSocketClient.SendBinary(ByteString("world"))

      parent.expectMessage(GotText("echo: hello"))
      parent.expectMessage(GotBinary(ByteString("echo: world")))

      // --- Step 4: Close server and expect Done
      client ! WebSocketClient.SendText("close")
      Await.result(binding.terminate(1.second), 5.seconds)
      parent.expectMessage(Done)
    }

    "notify parent on WebSocket connection failure" in {
      // --- Step 1: Use an invalid port to trigger failure
      val invalidUri = Uri("ws://localhost:65535/does-not-exist")

      val parent = TestProbe[ParentMsg]()

      val client = spawn(
        WebSocketClient(
          uri = invalidUri,
          parent.ref,
          connectedNotification = () => Connected,
          textMessageTransform = msg => GotText(msg.text),
          binaryMessageTransform = msg => GotBinary(msg.data),
          doneNotification = () => Done,
          failNotification = ex => Failed(ex)
        )
      )

      // --- Step 2: Expect failure
      val msg = parent.expectMessageType[Failed]
      msg.ex.getMessage.toLowerCase should (include("connection") or include(
        "refused"
      ))
      parent.expectMessage(Done)
    }
  }
}
