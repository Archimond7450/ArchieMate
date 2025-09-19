package com.archimond7450.archiemate.http

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.services.controllerhelpers.OAuthControllerHelperService
import com.archimond7450.archiemate.extensions.Settings
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.pekko.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Scheduler}
import org.apache.pekko.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import org.apache.pekko.http.scaladsl.model.Uri.Query
import org.apache.pekko.http.scaladsl.model.{HttpHeader, StatusCodes, Uri}
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import org.apache.pekko.util.Timeout
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class OAuthControllerSpec extends AnyWordSpecLike with Matchers with ScalatestRouteTest {
  override def testConfig: Config = ConfigFactory.load("application-test.conf")
  private given typedSystem: ActorSystem[Nothing] = system.toTyped
  private given settings: Settings = Settings(typedSystem)
  private given Scheduler = typedSystem.scheduler
  private given Timeout = settings.askTimeout

  private val mediatorProbe = TestProbe[ArchieMateMediator.Command](ArchieMateMediator.actorName)
  private val oauthControllerHelperServiceProbe = TestProbe[OAuthControllerHelperService.Command](OAuthControllerHelperService.actorName)

  private given mediator: ActorRef[ArchieMateMediator.Command] = mediatorProbe.ref
  private given oauthControllerHelperService: ActorRef[OAuthControllerHelperService.Command] = oauthControllerHelperServiceProbe.ref

  private val oauthController = new OAuthController

  "OAuthController" when {
    val oauthControllerRoutes = oauthController.getAllRoutes

    "GET /oauth/twitch request is received" should {
      "redirect to Twitch when helper service responds with Twitch Redirect URI" in {
        val test = Get("/oauth/twitch") ~> oauthControllerRoutes

        val mediatorCmd = mediatorProbe.expectMessageType[ArchieMateMediator.SendOAuthControllerHelperServiceCommand]
        oauthControllerHelperService ! mediatorCmd.cmd
        val cmd = oauthControllerHelperServiceProbe.expectMessageType[OAuthControllerHelperService.NewTwitchLoginRequest]

        val uri = Uri("https://id.twitch.tv/oauth2/authorize").withQuery(
          Query(
            "response_type" -> "code",
            "client_id" -> settings.twitchAppClientId,
            "redirect_uri" -> settings.twitchAppRedirectUri,
            "scope" -> "",
            "state" -> "0123456789abcdefghijklmnopqrstuvwxyz",
            "force_verify" -> "no"
          )
        )

        cmd.replyTo ! OAuthControllerHelperService.TwitchRedirect(uri)

        test ~> check {
          status shouldEqual StatusCodes.TemporaryRedirect
          header("Location").nonEmpty shouldEqual true
          header("Location").get.value shouldEqual uri.toString
        }
      }
    }

    "GET /oauth/twitch/connection request is received" should {
      "redirect to Twitch when helper service responds with Twitch Redirect URI" in {
        val test = Get("/oauth/twitch/connection") ~> oauthControllerRoutes

        val mediatorCmd = mediatorProbe.expectMessageType[ArchieMateMediator.SendOAuthControllerHelperServiceCommand]
        oauthControllerHelperService ! mediatorCmd.cmd
        val cmd = oauthControllerHelperServiceProbe.expectMessageType[OAuthControllerHelperService.NewTwitchConnectionRequest]

        val uri = Uri("https://id.twitch.tv/oauth2/authorize").withQuery(
          Query(
            "response_type" -> "code",
            "client_id" -> settings.twitchAppClientId,
            "redirect_uri" -> settings.twitchAppRedirectUri,
            "scope" -> "",
            "state" -> "zyxwvutsrqponmlkjihgfedcba9876543210",
            "force_verify" -> "yes"
          )
        )

        cmd.replyTo ! OAuthControllerHelperService.TwitchRedirect(uri)

        test ~> check {
          status shouldEqual StatusCodes.TemporaryRedirect
          header("Location").nonEmpty shouldEqual true
          header("Location").get.value shouldEqual uri.toString
        }
      }
    }
  }
}
