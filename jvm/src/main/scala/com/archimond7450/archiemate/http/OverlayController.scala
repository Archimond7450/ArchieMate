package com.archimond7450.archiemate.http

import com.archimond7450.archiemate.actors.{ArchieMateMediator, UserGuardian}
import com.archimond7450.archiemate.actors.chatbot.TwitchChatbotsSupervisor
import com.archimond7450.archiemate.twitch.eventsub
import com.archimond7450.archiemate.actors.repositories.OverlaySecretsRepository
import com.archimond7450.archiemate.actors.services.JWTService
import com.archimond7450.archiemate.actors.services.caches.TwitchEventsCacheService
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.helpers.HttpControllerHelpers.failWithoutSessionCookie
import com.archimond7450.archiemate.providers.RandomProvider
import io.circe.syntax.EncoderOps
import org.apache.pekko.{Done, NotUsed}
import org.apache.pekko.actor.ClassicActorSystemProvider
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.model.ws.{Message, TextMessage}
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout
import org.apache.pekko.http.scaladsl.server.Directives.{*, given}
import org.apache.pekko.stream.OverflowStrategy
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

class OverlayController(using
    ctx: ActorContext[UserGuardian.Command],
    mediator: ActorRef[ArchieMateMediator.Command],
    random: RandomProvider
)(using Settings, Scheduler, Timeout)
    extends IController("overlay") {

  given ClassicActorSystemProvider = ctx.system.classicSystem
  import ctx.executionContext

  override def routes: Route = secret ~ ws

  private def secret: Route = {
    failWithoutSessionCookie { jwt =>
      onComplete(mediator.ask[JWTService.DecodeJWTResponse](ref => ArchieMateMediator.SendJWTServiceCommand(JWTService.DecodeJWT(ref, jwt.value)))) {
        case Success(JWTService.DecodedJWT(userId, sessionId)) =>
          (get & path("secret")) {
            onComplete(
              mediator.ask[String](ref =>
                ArchieMateMediator.SendOverlaySecretsRepositoryCommand(
                  OverlaySecretsRepository.GetSecret(ref, userId)
                )
              )
            ) {
              case Success(secret) =>
                complete(StatusCodes.OK, secret)

              case Failure(ex) =>
                complete(StatusCodes.InternalServerError)
            }
          } ~ (post & path("secret")) {
            onComplete(
              mediator.ask[String](ref =>
                ArchieMateMediator.SendOverlaySecretsRepositoryCommand(
                  OverlaySecretsRepository.ResetSecret(ref, userId)
                )
              )
            ) {
              case Success(secret) =>
                complete(StatusCodes.OK, secret)

              case Failure(ex) =>
                complete(StatusCodes.InternalServerError)
            }
          }

        case Success(JWTService.InvalidJWT) =>
          complete(StatusCodes.Forbidden)

        case Failure(ex) =>
          complete(StatusCodes.InternalServerError)

      }
    }
  }

  private def ws: Route = {
    (get & extractLog & path("ws" / Segment / Segment)) {
      (log, twitchRoomId, overlaySecret) =>
        onComplete(
          mediator.ask[String](ref =>
            ArchieMateMediator.SendOverlaySecretsRepositoryCommand(
              OverlaySecretsRepository.GetSecret(ref, twitchRoomId)
            )
          )
        ) {
          case Success(correctSecret) if correctSecret == overlaySecret =>
            handleWebSocketMessages(webSocketFlow(twitchRoomId))

          case Success(secret) =>
            complete(StatusCodes.Forbidden)

          case Failure(ex) =>
            complete(StatusCodes.InternalServerError)
        }
    } ~ (get & extractLog & path("ws" / Segment / Segment / Segment)) {
      (log, twitchRoomId, overlaySecret, fromEventId) =>
        onComplete(
          mediator.ask[String](ref =>
            ArchieMateMediator.SendOverlaySecretsRepositoryCommand(
              OverlaySecretsRepository.GetSecret(ref, twitchRoomId)
            )
          )
        ) {
          case Success(correctSecret) if correctSecret == overlaySecret =>
            onComplete(
              mediator.ask[List[eventsub.Event]](ref =>
                ArchieMateMediator.SendTwitchEventsCacheServiceCommand(
                  TwitchEventsCacheService
                    .GetEventsFromId(ref, twitchRoomId, fromEventId)
                )
              )
            ) {
              case Success(initialTwitchEvents) =>
                handleWebSocketMessages(
                  webSocketFlow(twitchRoomId, initialTwitchEvents)
                )

              case Failure(ex) =>
                handleWebSocketMessages(webSocketFlow(twitchRoomId))
            }

          case Success(secret) =>
            complete(StatusCodes.Forbidden)

          case Failure(ex) =>
            complete(StatusCodes.InternalServerError)
        }
    }
  }

  private def webSocketFlow(
      twitchRoomId: String,
      initialTwitchEvents: List[eventsub.Event] = Nil
  ): Flow[Message, Message, Any] = {
    val (queue, source) = Source
      .queue[OverlayMessages.OverlayMessage](
        bufferSize = 64,
        OverflowStrategy.backpressure
      )
      .preMaterialize()

    mediator ! ArchieMateMediator.SendTwitchEventsCacheServiceCommand(
      TwitchEventsCacheService.OverlayConnected(twitchRoomId, queue)
    )

    val outbound: Source[Message, NotUsed] = source
      .map(msg => TextMessage.Strict(msg.asJson.noSpaces))

    val inbound: Sink[Message, Future[Done]] = Sink.ignore

    val keepAliveFlow = Flow[Message]
      .keepAlive(
        30.seconds,
        () =>
          TextMessage.Strict(
            OverlayMessages
              .OverlayMessage(random.uuid().toString, OverlayMessages.OverlayMessageType.Ping)
              .asJson
              .noSpaces
          )
      )

    val completionHandler = Flow[Message]
      .watchTermination() { (_, done) =>
        done.onComplete { _ =>
          mediator ! ArchieMateMediator.SendTwitchEventsCacheServiceCommand(
            TwitchEventsCacheService.OverlayDisconnected(twitchRoomId)
          )
        }
      }

    initialTwitchEvents.foreach { event =>
      queue.offer(
        OverlayMessages.OverlayMessage(
          random.uuid().toString,
          OverlayMessages.OverlayMessageType.TwitchEvent,
          twitchEvent = Some(event)
        )
      )
    }

    Flow
      .fromSinkAndSourceCoupled(inbound, outbound)
      .via(keepAliveFlow)
      .via(completionHandler)
  }
}
