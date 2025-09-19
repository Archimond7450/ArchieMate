package com.archimond7450.archiemate.actors

import org.apache.pekko.actor.ClassicActorSystemProvider
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.http.scaladsl.HttpExt
import org.apache.pekko.http.scaladsl.model.{HttpEntity, HttpHeader, HttpMethod, HttpRequest, HttpResponse, RequestEntity, Uri}
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal
import org.apache.pekko.pattern.StatusReply
import org.slf4j.Logger

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

object HttpClient {
  val actorName = "HttpClient"

  final case class Request(replyTo: ActorRef[StatusReply[Response]],
                           method: HttpMethod, uri: Uri,
                           headers: Seq[HttpHeader] = Seq.empty,
                           entity: RequestEntity = HttpEntity.Empty)

  final case class Response(response: HttpResponse,
                            entityString: String)

  trait HttpClientAdapter {
    def singleRequest(request: HttpRequest): Future[HttpResponse]
  }

  class PekkoHttpClientAdapter(http: HttpExt) extends HttpClientAdapter {
    override def singleRequest(request: HttpRequest): Future[HttpResponse] =
      http.singleRequest(request)
  }

  def apply(http: HttpClientAdapter): Behavior[Request] = Behaviors.receive { (ctx, msg) =>
    val log = ctx.log
    given ClassicActorSystemProvider = ctx.system
    given ExecutionContextExecutor = ctx.executionContext

    msg match {
      case Request(replyTo, method, uri, headers, entity) =>
        val request = HttpRequest(method = method, uri = uri, headers = headers, entity = entity)
        val responseFuture = http.singleRequest(request).flatMap { response =>
          Unmarshal(response.entity).to[String].map(entity => Response(response, entity))
        }
        responseFuture.onComplete(logAndReply(log, request, replyTo))
    }

    Behaviors.same
  }

  private def logAndReply(log: Logger, request: HttpRequest, replyTo: ActorRef[StatusReply[Response]]): PartialFunction[Try[Response], Unit] = {
    case Success(resp @ Response(response, entityString)) =>
      log.debug("{} ({}) -> {} ({})", request, request.entity, response, entityString)
      replyTo ! StatusReply.success(resp)
    case Failure(ex) =>
      log.error("{} ({}) -> FAIL", request, request.entity, ex)
      replyTo ! StatusReply.error(ex)
  }
}
