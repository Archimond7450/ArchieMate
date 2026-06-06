package com.archimond7450.archiemate.http

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.chatbot.ChatbotsSupervisor
import com.archimond7450.archiemate.actors.services.controllerhelpers.WebhooksControllerKickHelperService
import com.archimond7450.archiemate.helpers.JsonHelper.decodeToTry
import com.archimond7450.archiemate.kick.webhooks.KickWebhooks
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout

import scala.util.{Failure, Success}

class WebhooksController(using mediator: ActorRef[ArchieMateMediator.Command])(
    using
    Scheduler,
    Timeout
) extends IController("webhooks") {
  override def routes: Route = kick

  private def kick: Route = {
    (post & extractLog & path("kick") & headerValueByName(
      "Kick-Event-Message-Id"
    ) & headerValueByName("Kick-Event-Subscription-Id") & headerValueByName(
      "Kick-Event-Signature"
    ) & headerValueByName(
      "Kick-Event-Message-Timestamp"
    ) & headerValueByName("Kick-Event-Type") & headerValueByName(
      "Kick-Event-Version"
    ) & entity(
      as[String]
    )) {
      (
          log,
          messageId,
          subscriptionId,
          signature,
          timestamp,
          eventType,
          eventVersion,
          body
      ) =>
        onComplete(
          mediator.ask[Boolean](ref =>
            ArchieMateMediator.SendWebhooksControllerKickHelperServiceCommand(
              WebhooksControllerKickHelperService
                .VerifyWebhook(ref, messageId, signature, timestamp, body)
            )
          )
        ) {
          case Success(true) =>
            decodeToTry[KickWebhooks.KickWebhook](body) match {
              case Success(webhook) =>
                val kickBroadcasterId = webhook match {
                  case KickWebhooks.ChannelFollowedV1(broadcaster, follower) =>
                    broadcaster.userId
                  case KickWebhooks.ChatMessageSentV1(messageId, repliesTo, broadcaster, sender, content, emotes, createdAt) =>
                    broadcaster.userId
                }
                mediator ! ArchieMateMediator.SendChatbotsSupervisorCommand(
                  ChatbotsSupervisor.KickWebhookReceived(kickBroadcasterId, webhook)
                )
                complete(StatusCodes.NoContent)
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError)
            }
          case Success(false) =>
            log.warning("The received webhook was not from Kick. Ignoring.")
            complete(StatusCodes.Forbidden)
          case Failure(ex) =>
            log.error(
              ex,
              "Something happened when waiting to check if the webhook came from Kick."
            )
            complete(StatusCodes.InternalServerError)
        }
    }
  }
}
