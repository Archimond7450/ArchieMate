package com.archimond7450.archiemate.actors.repositories.sessions

import com.archimond7450.archiemate.actors.ArchieMateMediator
import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.providers.{RandomProvider, TimeProvider}
import com.archimond7450.archiemate.twitch.api.TwitchApiResponse
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.apache.pekko.actor.typed.ActorRef
import org.scalatest.wordspec.AnyWordSpecLike

class TwitchUserSessionsRepositorySpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike {
  import TwitchUserSessionsRepositorySpec.*

  private given RandomProvider = new RandomProvider
  private given TimeProvider = new TimeProvider
  private given Settings = Settings(system)

  "A Twitch user sessions repository" should {
    val twitchApiClientProbe = testKit.createTestProbe[TwitchApiClient.Command](
      TwitchApiClient.actorName
    )
    given twitchApiClient: ActorRef[TwitchApiClient.Command] =
      twitchApiClientProbe.ref
    val mediatorProbe = testKit.createTestProbe[ArchieMateMediator.Command](
      ArchieMateMediator.actorName
    )
    given mediator: ActorRef[ArchieMateMediator.Command] = mediatorProbe.ref

    "return no token from token id when initially started and send nothing to twitchApiClient" in {
      val twitchUserSessionsRepository = testKit.spawn(
        TwitchUserSessionsRepository(),
        s"${TwitchUserSessionsRepository.actorName}0"
      )
      val probe = testKit
        .createTestProbe[TwitchUserSessionsRepository.ReturnedTokenFromId]()

      for (tokenId <- Seq(tokenId1, tokenId2, tokenId3, wrongTokenId)) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository
          .GetTokenFromId(probe.ref, tokenId)
        probe.expectMessage(
          TwitchUserSessionsRepository.ReturnedTokenFromId(None)
        )
      }

      mediatorProbe.expectNoMessage()
      twitchApiClientProbe.expectNoMessage()
    }

    "return the first token from the correct id when it is the only one added and return nothing for other tokens" in {
      val twitchUserSessionsRepository = testKit.spawn(
        TwitchUserSessionsRepository(),
        s"${TwitchUserSessionsRepository.actorName}1"
      )
      val probe = testKit
        .createTestProbe[TwitchUserSessionsRepository.ReturnedTokenFromId]()

      twitchUserSessionsRepository ! TwitchUserSessionsRepository.SetToken(
        tokenId1,
        userId1,
        sessionId1Token
      )
      twitchUserSessionsRepository ! TwitchUserSessionsRepository
        .GetTokenFromId(probe.ref, tokenId1)
      probe.expectMessage(
        TwitchUserSessionsRepository.ReturnedTokenFromId(Some(sessionId1Token))
      )

      for (tokenId <- Seq(tokenId2, tokenId3, wrongTokenId)) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository
          .GetTokenFromId(probe.ref, tokenId)
        probe.expectMessage(
          TwitchUserSessionsRepository.ReturnedTokenFromId(None)
        )
      }

      mediatorProbe.expectNoMessage()
      twitchApiClientProbe
        .expectNoMessage()
    }

    "return the correct token from the correct id when there is second session added, nothing for others" in {
      val twitchUserSessionsRepository = testKit.spawn(
        TwitchUserSessionsRepository(),
        s"${TwitchUserSessionsRepository.actorName}2"
      )

      val mediatorTwitchApiClientMessage = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchApiClientCommand]
      twitchApiClient ! mediatorTwitchApiClientMessage.cmd
      twitchApiClientProbe.expectMessage(TwitchApiClient.GetTokenUserFromTokenId(system.ignoreRef, tokenId1))

      val probe = testKit
        .createTestProbe[TwitchUserSessionsRepository.ReturnedTokenFromId]()

      twitchUserSessionsRepository ! TwitchUserSessionsRepository.SetToken(
        tokenId2,
        userId2,
        sessionId2Token
      )

      val tokenSession = Map(
        tokenId1 -> Some(sessionId1Token),
        tokenId2 -> Some(sessionId2Token),
        tokenId3 -> None,
        wrongTokenId -> None
      )

      for (tokenId <- tokenSession.keys) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository
          .GetTokenFromId(probe.ref, tokenId)
        probe.expectMessage(
          TwitchUserSessionsRepository.ReturnedTokenFromId(
            tokenSession(tokenId)
          )
        )
      }

      mediatorProbe.expectNoMessage()
      twitchApiClientProbe
        .expectNoMessage()
    }

    "return the correct token from the correct id when there is third session added, nothing for others" in {
      val twitchUserSessionsRepository = testKit.spawn(
        TwitchUserSessionsRepository(),
        s"${TwitchUserSessionsRepository.actorName}3"
      )

      val mediatorTwitchApiClientMessage1 = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchApiClientCommand]
      twitchApiClient ! mediatorTwitchApiClientMessage1.cmd
      twitchApiClientProbe.expectMessage(TwitchApiClient.GetTokenUserFromTokenId(system.ignoreRef, tokenId1))

      val mediatorTwitchApiClientMessage2 = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchApiClientCommand]
      twitchApiClient ! mediatorTwitchApiClientMessage2.cmd
      twitchApiClientProbe.expectMessage(TwitchApiClient.GetTokenUserFromTokenId(system.ignoreRef, tokenId2))

      val probe = testKit
        .createTestProbe[TwitchUserSessionsRepository.ReturnedTokenFromId]()

      twitchUserSessionsRepository ! TwitchUserSessionsRepository.SetToken(
        tokenId3,
        userId3,
        sessionId3Token
      )

      val tokenSession = Map(
        tokenId1 -> Some(sessionId1Token),
        tokenId2 -> Some(sessionId2Token),
        tokenId3 -> Some(sessionId3Token),
        wrongTokenId -> None
      )

      for (tokenId <- tokenSession.keys) {
        twitchUserSessionsRepository ! TwitchUserSessionsRepository
          .GetTokenFromId(probe.ref, tokenId)
        probe.expectMessage(
          TwitchUserSessionsRepository.ReturnedTokenFromId(
            tokenSession(tokenId)
          )
        )
      }

      mediatorProbe.expectNoMessage()
      twitchApiClientProbe
        .expectNoMessage()
    }

    "" in {
      val twitchUserSessionsRepository = testKit.spawn(
        TwitchUserSessionsRepository(),
        s"${TwitchUserSessionsRepository.actorName}4"
      )

      val mediatorTwitchApiClientMessage1 = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchApiClientCommand]
      twitchApiClient ! mediatorTwitchApiClientMessage1.cmd
      twitchApiClientProbe.expectMessage(TwitchApiClient.GetTokenUserFromTokenId(system.ignoreRef, tokenId1))

      val mediatorTwitchApiClientMessage2 = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchApiClientCommand]
      twitchApiClient ! mediatorTwitchApiClientMessage2.cmd
      twitchApiClientProbe.expectMessage(TwitchApiClient.GetTokenUserFromTokenId(system.ignoreRef, tokenId2))

      val mediatorTwitchApiClientMessage3 = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchApiClientCommand]
      twitchApiClient ! mediatorTwitchApiClientMessage3.cmd
      twitchApiClientProbe.expectMessage(TwitchApiClient.GetTokenUserFromTokenId(system.ignoreRef, tokenId3))

      mediatorProbe.expectNoMessage()
      twitchApiClientProbe
        .expectNoMessage()
    }
  }
}

object TwitchUserSessionsRepositorySpec {
  val tokenId1 = "t1"
  val userId1 = "u1"
  val tokenId2 = "t2"
  val userId2 = "u2"
  val tokenId3 = "t3"
  val userId3 = "u3"
  val wrongTokenId = "0"
  val wrongUserId = "0"
  val noScopes: Option[List[String]] = None
  val scopes: Option[List[String]] = Some(
    List("channel:moderate", "chat:edit", "chat:read")
  )
  val tokenType = "bearer"
  val sessionId1Token: TwitchApiResponse.GetToken = TwitchApiResponse.GetToken(
    access_token = "123",
    expires_in = 14141,
    refresh_token = "1a2b3c",
    scope = scopes,
    token_type = tokenType
  )
  val sessionId2Token: TwitchApiResponse.GetToken = TwitchApiResponse.GetToken(
    access_token = "456",
    expires_in = 14253,
    refresh_token = "4d5e6f",
    scope = noScopes,
    token_type = tokenType
  )
  val sessionId3Token: TwitchApiResponse.GetToken = TwitchApiResponse.GetToken(
    access_token = "111",
    expires_in = 11111,
    refresh_token = "1a1a1a",
    scope = scopes,
    token_type = tokenType
  )
}
