package com.archimond7450.archiemate.actors.services

import com.archimond7450.archiemate.actors.services.JWTService.algorithm
import com.archimond7450.archiemate.extensions.Settings
import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import pdi.jwt.JwtCirce

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.{Random, Success}

class JWTServiceSpec extends ScalaTestWithActorTestKit(ConfigFactory.load("application-test.conf")) with AnyWordSpecLike {
  private val settings = Settings(testKit.system)

  "A JWT Service" should {
    "generate a valid JWT token with decodable claims" in {
      val jwtService = testKit.spawn(JWTService())
      val probe = testKit.createTestProbe[JWTService.GeneratedJWT]()
      val userId = Random.between(0, Long.MaxValue).toString
      val sessionId = UUID.randomUUID().toString

      jwtService ! JWTService.GenerateJWT(probe.ref, userId, sessionId)
      val response = probe.expectMessageType[JWTService.GeneratedJWT]

      JwtCirce.isValid(response.jwt, settings.jwtSecret, Seq(algorithm)) should be(true)
      val claims = JwtCirce.decode(response.jwt, settings.jwtSecret, Seq(algorithm)).get

      claims.jwtId shouldEqual Some(sessionId)
      claims.issuer shouldEqual Some(settings.jwtIssuer)
      claims.audience.nonEmpty shouldEqual true
      claims.audience.get should contain theSameElementsAs Seq(userId)
    }

    "return userId and sessionId out of a valid JWT token" in {
      val jwtService = testKit.spawn(JWTService())
      val probeGenerated = testKit.createTestProbe[JWTService.GeneratedJWT]()
      val probeDecoded = testKit.createTestProbe[JWTService.DecodeJWTResponse]()
      val userId = Random.between(0, Long.MaxValue).toString
      val sessionId = UUID.randomUUID().toString

      jwtService ! JWTService.GenerateJWT(probeGenerated.ref, userId, sessionId)
      val generatedResponse = probeGenerated.expectMessageType[JWTService.GeneratedJWT]

      jwtService ! JWTService.DecodeJWT(probeDecoded.ref, generatedResponse.jwt)
      val decodedResponse = probeDecoded.expectMessageType[JWTService.DecodedJWT]

      decodedResponse should be(JWTService.DecodedJWT(userId, sessionId))
    }

    "return invalid JWT when an invalid JWT token is passed" in {
      val jwtService = testKit.spawn(JWTService())
      val probe = testKit.createTestProbe[JWTService.DecodeJWTResponse]()

      jwtService ! JWTService.DecodeJWT(probe.ref, "")
      probe.expectMessage(JWTService.InvalidJWT)
    }

    "return invalid JWT when an expired JWT token is passed" in {
      given ExecutionContext = system.executionContext
      val jwtService = testKit.spawn(JWTService())
      val probeGenerated = testKit.createTestProbe[JWTService.GeneratedJWT]()
      val probeDecoded = testKit.createTestProbe[JWTService.DecodeJWTResponse]()
      val userId = Random.between(0, Long.MaxValue).toString
      val sessionId = UUID.randomUUID().toString

      jwtService ! JWTService.GenerateJWT(probeGenerated.ref, userId, sessionId)
      val generatedResponse = probeGenerated.expectMessageType[JWTService.GeneratedJWT]

      val decodeJwt: Runnable = () => jwtService ! JWTService.DecodeJWT(probeDecoded.ref, generatedResponse.jwt)

      system.scheduler.scheduleOnce(settings.jwtExpiration / 2, decodeJwt)
      system.scheduler.scheduleOnce(settings.jwtExpiration, decodeJwt)

      val decodedResponse = probeDecoded.expectMessageType[JWTService.DecodedJWT](settings.jwtExpiration / 2 + 1.second)
      decodedResponse should be(JWTService.DecodedJWT(userId, sessionId))

      probeDecoded.expectMessage(settings.jwtExpiration + 1.second, JWTService.InvalidJWT)
    }
  }
}
