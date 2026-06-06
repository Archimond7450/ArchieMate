package com.archimond7450.archiemate.actors.services.controllerhelpers

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.kick.api.KickApiClient
import com.archimond7450.archiemate.kick.api.KickApiResponse
import org.apache.pekko.actor.typed.{
  ActorRef,
  Behavior,
  LogOptions,
  SupervisorStrategy
}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.util.Timeout

import java.security.{KeyFactory, PublicKey, Signature}
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

object WebhooksControllerKickHelperService {
  val actorName = "WebhooksControllerKickHelperService"

  sealed trait Command

  final case class VerifyWebhook(
      replyTo: ActorRef[Boolean],
      messageId: String,
      signature: String,
      timestamp: String,
      body: String
  ) extends Command

  private case object AskForNewPublicKey extends Command
  private case class NewPublicKey(
      tryResponse: Try[KickApiResponse.GetPublicKey]
  ) extends Command

  def apply()(using
      mediator: ActorRef[ArchieMateMediator.Command]
  )(using Timeout): Behavior[Command] = Behaviors
    .supervise[Command] {
      Behaviors.setup { ctx =>
        given ActorContext[Command] = ctx
        Behaviors.logMessages {
          Behaviors.withTimers { scheduler =>
            Behaviors.withStash(10) { buffer =>
              ctx.self ! AskForNewPublicKey
              Behaviors.receiveMessage {
                case AskForNewPublicKey =>
                  askForKey()
                  Behaviors.same

                case NewPublicKey(Success(response)) =>
                  scheduler.startTimerAtFixedRate(AskForNewPublicKey, 1.hour)
                  buffer.unstashAll(active(getPublicKey(response)))

                case NewPublicKey(Failure(ex)) =>
                  scheduler.startSingleTimer(AskForNewPublicKey, 1.minute)
                  Behaviors.same

                case other =>
                  buffer.stash(other)
                  Behaviors.same
              }
            }
          }
        }
      }
    }
    .onFailure[Throwable](SupervisorStrategy.resume)

  private def active(publicKey: PublicKey)(using
      ctx: ActorContext[Command],
      mediator: ActorRef[ArchieMateMediator.Command]
  )(using Timeout): Behavior[Command] = {
    Behaviors.receiveMessage {
      case VerifyWebhook(
            replyTo,
            messageId,
            signature,
            timestamp,
            body
          ) =>
        replyTo ! verify(publicKey, messageId, timestamp, body, signature)
        Behaviors.same

      case AskForNewPublicKey =>
        askForKey()
        Behaviors.same

      case NewPublicKey(Success(response)) =>
        active(getPublicKey(response))

      case NewPublicKey(Failure(ex)) =>
        ctx.log.error("Failed to get the new public key", ex)
        Behaviors.same
    }
  }

  private def askForKey()(using
      ctx: ActorContext[Command],
      mediator: ActorRef[ArchieMateMediator.Command]
  )(using Timeout): Unit = {
    ctx.askWithStatus[
      ArchieMateMediator.Command,
      KickApiResponse.GetPublicKey
    ](
      mediator,
      ref =>
        ArchieMateMediator.SendKickApiClientCommand(
          KickApiClient.GetPublicKey(ref)
        )
    )(NewPublicKey.apply)
  }

  private def getPublicKey(
      response: KickApiResponse.GetPublicKey
  ): PublicKey = {
    val cleaned = response.data.publicKey
      .replace("-----BEGIN PUBLIC KEY-----", "")
      .replace("-----END PUBLIC KEY-----", "")
      .replaceAll("\\s", "")
    val keyBytes = Base64.getDecoder.decode(cleaned)
    val keySpec = X509EncodedKeySpec(keyBytes)
    KeyFactory.getInstance("RSA").generatePublic(keySpec)
  }

  private def verify(
      publicKey: PublicKey,
      messageId: String,
      timestamp: String,
      body: String,
      signature: String
  ): Boolean = {
    Try {
      val signedPayload = s"$messageId.$timestamp.$body"
      val signatureBytes = Base64.getDecoder.decode(signature)
      val verifier = Signature.getInstance("SHA256withRSA")
      verifier.initVerify(publicKey)
      verifier.update(signedPayload.getBytes("UTF-8"))
      verifier.verify(signatureBytes)
    } match {
      case Success(true) => true
      case _             => false
    }
  }
}
