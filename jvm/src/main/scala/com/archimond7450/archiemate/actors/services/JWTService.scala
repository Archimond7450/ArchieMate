package com.archimond7450.archiemate.actors.services

import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.extensions.Settings
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

import java.time.{Clock, Instant, OffsetDateTime, ZoneId}
import scala.util.{Failure, Success}

object JWTService {
  val actorName = "JWTService"

  sealed trait Command

  final case class GenerateJWT(replyTo: ActorRef[GeneratedJWT], userId: String, sessionId: String) extends Command
  final case class GeneratedJWT(jwt: String)

  final case class DecodeJWT(replyTo: ActorRef[DecodeJWTResponse], jwt: String) extends Command
  sealed trait DecodeJWTResponse
  final case class DecodedJWT(userId: String, sessionId: String) extends DecodeJWTResponse
  case object InvalidJWT extends DecodeJWTResponse

  val algorithm: JwtHmacAlgorithm = JwtAlgorithm.HS256

  def apply(): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx
    val settings = Settings(ctx.system)

    Behaviors.receiveAndLogMessage {
      case GenerateJWT(replyTo, userId, sessionId) =>
        val nowSeconds = OffsetDateTime.now(ZoneId.of("Z")).toEpochSecond
        val claim = JwtClaim()
          .withId(sessionId)
          .issuedAt(nowSeconds)
          .by(settings.jwtIssuer)
          .to(userId)
          .startsAt(nowSeconds)
          .expiresAt(nowSeconds + settings.jwtExpiration.toSeconds)
        val jwt = JwtCirce.encode(claim, settings.jwtSecret, algorithm)
        replyTo ! GeneratedJWT(jwt)
        Behaviors.same

      case DecodeJWT(replyTo, jwt) =>
        val response: DecodeJWTResponse = {
          if (JwtCirce.isValid(jwt, settings.jwtSecret, Seq(algorithm))) {
            given Clock = Clock.fixed(Instant.ofEpochSecond(OffsetDateTime.now(ZoneId.of("Z")).toEpochSecond), ZoneId.of("Z"))
            JwtCirce.decode(jwt, settings.jwtSecret, Seq(algorithm)) match {
              case Success(claims) if claims.isValid && claims.issuer.contains(settings.jwtIssuer) && claims.audience.nonEmpty && claims.jwtId.nonEmpty =>
                DecodedJWT(claims.audience.get.head, claims.jwtId.get)

              case Success(claims) =>
                ctx.log.error("Invalid JWT claims: {}", claims)
                InvalidJWT

              case Failure(ex) =>
                ctx.log.error("Failed to decode JWT '{}'!", jwt, ex)
                InvalidJWT
            }
          } else {
            ctx.log.error(s"The JWT '$jwt' is not valid!")
            InvalidJWT
          }
        }

        replyTo ! response
        Behaviors.same
    }
  }
}
