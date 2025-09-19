package com.archimond7450.archiemate.actors

import org.apache.pekko
import org.apache.pekko.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.http.scaladsl.model.*
import org.apache.pekko.http.scaladsl.HttpExt
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal
import org.apache.pekko.pattern.StatusReply
import org.apache.pekko.stream.Materializer
import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class HttpClientSpec
  extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with MockFactory {

  "HttpClient actor" should {

    "send a request and return a successful response" in {
      val mockHttp = mock[HttpClient.HttpClientAdapter]

      val expectedBody = "Hello world"
      val httpResponse = HttpResponse(
        status = StatusCodes.OK,
        entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, expectedBody)
      )

      val method = HttpMethods.GET
      val uri = Uri("http://test.com")
      val expectedReq: HttpRequest => Boolean = req => req.method == method && req.uri.toString() == uri.toString()

      (mockHttp.singleRequest _)
        .expects(where(expectedReq))
        .returning(Future.successful(httpResponse))

      val probe = TestProbe[StatusReply[HttpClient.Response]]()
      val actor = spawn(HttpClient(mockHttp))

      actor ! HttpClient.Request(
        replyTo = probe.ref,
        method = method,
        uri = uri
      )

      val result = probe.receiveMessage()
      result match {
        case StatusReply.Success(HttpClient.Response(resp, entity)) =>
          resp.status shouldEqual StatusCodes.OK
          entity shouldEqual expectedBody
        case StatusReply.Error(ex) =>
          fail(s"Expected success, got failure: $ex")
      }
    }

    "handle a failed request" in {
      val mockHttp = mock[HttpClient.HttpClientAdapter]

      val method = HttpMethods.POST
      val uri = Uri("http://fail.com")
      val expectedReq: HttpRequest => Boolean = req => req.method == method && req.uri.toString() == uri.toString()

      val exception = new RuntimeException("Boom!")
      (mockHttp.singleRequest _)
        .expects(where(expectedReq))
        .returning(Future.failed(exception))

      val probe = TestProbe[StatusReply[HttpClient.Response]]()
      val actor: ActorRef[HttpClient.Request] = spawn(HttpClient(mockHttp))

      actor ! HttpClient.Request(
        replyTo = probe.ref,
        method = method,
        uri = uri
      )

      val result = probe.receiveMessage()
      result shouldEqual StatusReply.Error(exception)
      /*result match {
        case Success(_) => fail("Expected failure, got success")
        case Failure(ex) => ex shouldEqual exception
      }*/
    }
  }
}
