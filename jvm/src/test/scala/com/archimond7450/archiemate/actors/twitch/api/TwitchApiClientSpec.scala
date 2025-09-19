package com.archimond7450.archiemate.actors.twitch.api

import org.apache.pekko.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import org.apache.pekko.actor.typed.scaladsl.adapter.*
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.pattern.StatusReply
import org.apache.pekko.http.scaladsl.model.*
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import com.archimond7450.archiemate.actors.{ArchieMateMediator, HttpClient}
import com.archimond7450.archiemate.actors.repositories.sessions.TwitchUserSessionsRepository
import com.archimond7450.archiemate.actors.services.caches.TwitchTokenUserCacheService
import com.archimond7450.archiemate.twitch.api.{TwitchApi, TwitchApiResponse}
import com.archimond7450.archiemate.twitch.eventsub.{Condition, Subscription, Transport}
import io.circe.syntax.EncoderOps
import org.apache.pekko.http.scaladsl.model.Uri.Query

import java.time.OffsetDateTime
import scala.util.Success

class TwitchApiClientSpec
  extends AnyWordSpecLike
    with Matchers {
  import TwitchApiClientSpec.*

  private val testKit = ActorTestKit()

  val repoProbe: TestProbe[TwitchUserSessionsRepository.Command] = testKit.createTestProbe(TwitchUserSessionsRepository.actorName)
  val twitchTokenUserCacheServiceProbe: TestProbe[TwitchTokenUserCacheService.Command] = testKit.createTestProbe(TwitchTokenUserCacheService.actorName)
  val mediatorProbe: TestProbe[ArchieMateMediator.Command] = testKit.createTestProbe(ArchieMateMediator.actorName)

  given twitchUserSessionsRepository: ActorRef[TwitchUserSessionsRepository.Command] = repoProbe.ref
  given twitchTokenUserCacheService: ActorRef[TwitchTokenUserCacheService.Command] = twitchTokenUserCacheServiceProbe.ref
  given mediator: ActorRef[ArchieMateMediator.Command] = mediatorProbe.ref

  val client: ActorRef[TwitchApiClient.Command] = testKit.spawn(TwitchApiClient(), TwitchApiClient.actorName)

  "TwitchApiClient" when {
    "Twitch API returns successful response" should {
      "reply with a token when GetToken message is sent" in {
        val replyProbe = testKit.createTestProbe[StatusReply[TwitchApiResponse.GetToken]]()

        client ! TwitchApiClient.GetToken(
          replyTo = replyProbe.ref,
          code = "auth123code"
        )

        val mediatorHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val httpRequest = mediatorHttpRequest.cmd

        httpRequest.method shouldEqual HttpMethods.POST
        httpRequest.uri shouldEqual Uri("https://id.twitch.tv/oauth2/token")

        val fakeBody = TwitchApiResponse.GetToken(
          access_token = "accesstoken",
          expires_in = 54321,
          refresh_token = "refreshtoken",
          scope = Some(Nil),
          token_type = "bearer"
        )

        val fakeResponse = HttpClient.Response(
          HttpResponse(
            status = StatusCodes.OK
          ),
          entityString = fakeBody.asJson.noSpaces
        )

        httpRequest.replyTo ! StatusReply.success(fakeResponse)

        val reply = replyProbe.receiveMessage()
        reply.isSuccess shouldEqual true
        reply.getValue shouldEqual fakeBody

        repoProbe.expectNoMessage()
        mediatorProbe.expectNoMessage()
      }

      "reply with a user when GetTokenUserFromTokenId message is sent and also send copy of the response to the caching service" in {
        val replyProbe = testKit.createTestProbe[StatusReply[TwitchApiResponse.GetTokenUser]]()

        val tokenId = "myTokenId"

        client ! TwitchApiClient.GetTokenUserFromTokenId(
          replyTo = replyProbe.ref,
          tokenId = tokenId
        )

        val mediatorTokenRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand]
        twitchUserSessionsRepository ! mediatorTokenRequest.cmd
        val tokenRequest = repoProbe.expectMessageType[TwitchUserSessionsRepository.GetTokenFromId]
        tokenRequest.tokenId shouldEqual tokenId

        val token = exampleToken

        tokenRequest.replyTo ! TwitchUserSessionsRepository.ReturnedTokenFromId(
          token = Some(token)
        )

        val mediatorHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val httpRequest = mediatorHttpRequest.cmd

        httpRequest.method shouldEqual HttpMethods.GET
        httpRequest.uri shouldEqual Uri("https://api.twitch.tv/helix/users")
        httpRequest.headers.exists(_.name == "Authorization") shouldEqual true

        val fakeBody = TwitchApiResponse.GetUsers(
          data = List(
            exampleTokenUser
          )
        )
        val fakeResponse = HttpClient.Response(
          HttpResponse(
            status = StatusCodes.OK
          ),
          entityString = fakeBody.asJson.noSpaces
        )

        httpRequest.replyTo ! StatusReply.success(fakeResponse)

        val reply = replyProbe.receiveMessage()
        reply.isSuccess shouldEqual true
        reply.getValue shouldEqual fakeBody.data.last

        val mediatorCacheCommand = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchTokenUserCacheServiceCommand]
        twitchTokenUserCacheService ! mediatorCacheCommand.cmd
        twitchTokenUserCacheServiceProbe.expectMessage(TwitchTokenUserCacheService.CacheTokenUser(reply.getValue))

        mediatorProbe.expectNoMessage()
      }

      "reply with a user when GetTokenUserFromAccessToken message is sent" in {
        val replyProbe = testKit.createTestProbe[StatusReply[TwitchApiResponse.GetTokenUser]]()

        client ! TwitchApiClient.GetTokenUserFromAccessToken(
          replyTo = replyProbe.ref,
          accessToken = "abc123"
        )

        val mediatorHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val httpRequest = mediatorHttpRequest.cmd

        httpRequest.method shouldEqual HttpMethods.GET
        httpRequest.uri shouldEqual Uri("https://api.twitch.tv/helix/users")
        httpRequest.headers.exists(_.name == "Authorization") shouldEqual true

        val fakeBody = TwitchApiResponse.GetUsers(
          data = List(
            exampleTokenUser
          )
        )
        val fakeResponse = HttpClient.Response(
          HttpResponse(
            status = StatusCodes.OK
          ),
          entityString = fakeBody.asJson.noSpaces
        )

        httpRequest.replyTo ! StatusReply.success(fakeResponse)

        val reply = replyProbe.receiveMessage()
        reply.isSuccess shouldEqual true
        reply.getValue shouldEqual fakeBody.data.last

        mediatorProbe.expectNoMessage()
      }

      "reply with game when GetGameByName message is sent" in {
        val replyProbe = testKit.createTestProbe[StatusReply[TwitchApiResponse.GameResponse]]()

        val tokenId = "asdf654"
        val gameName = warcraft3

        client ! TwitchApiClient.GetGameByName(
          replyTo = replyProbe.ref,
          tokenId = tokenId,
          gameName = gameName
        )

        val mediatorTokenRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand]
        twitchUserSessionsRepository ! mediatorTokenRequest.cmd
        val tokenRequest = repoProbe.expectMessageType[TwitchUserSessionsRepository.GetTokenFromId]
        tokenRequest.tokenId shouldEqual tokenId

        val token = exampleToken

        tokenRequest.replyTo ! TwitchUserSessionsRepository.ReturnedTokenFromId(
          token = Some(token)
        )

        val mediatorHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val httpRequest = mediatorHttpRequest.cmd

        httpRequest.method shouldEqual HttpMethods.GET
        httpRequest.uri shouldEqual Uri("https://api.twitch.tv/helix/games").withQuery(Query("name" -> gameName))
        httpRequest.headers.exists(_.name == "Authorization") shouldEqual true

        val fakeBody = TwitchApiResponse.GetGames(
          data = List(
            warcraft3Game
          )
        )
        val fakeResponse = StatusReply.success(HttpClient.Response(
          response = HttpResponse(
            status = StatusCodes.OK
          ),
          entityString = fakeBody.asJson.noSpaces
        ))

        httpRequest.replyTo ! fakeResponse

        val reply = replyProbe.receiveMessage()
        reply.isSuccess shouldEqual true
        reply.getValue shouldEqual TwitchApiResponse.GameResponse(fakeBody.data.lastOption)

        mediatorProbe.expectNoMessage()
      }

      "reply with ModifyChannelInformation when ChangeChannelGame message is sent" in {
        val replyProbe = testKit.createTestProbe[StatusReply[TwitchApiResponse.ModifyChannelInformation.type]]()

        val tokenId = "qwert789"
        val roomId = archimond7450Id
        val gameId = warcraft3Id

        client ! TwitchApiClient.ChangeChannelGame(
          replyTo = replyProbe.ref,
          tokenId = tokenId,
          roomId = roomId,
          gameId = gameId
        )

        val mediatorTokenRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand]
        twitchUserSessionsRepository ! mediatorTokenRequest.cmd
        val tokenRequest = repoProbe.expectMessageType[TwitchUserSessionsRepository.GetTokenFromId]
        tokenRequest.tokenId shouldEqual tokenId

        val token = exampleToken

        tokenRequest.replyTo ! TwitchUserSessionsRepository.ReturnedTokenFromId(
          token = Some(token)
        )

        val mediatorHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val httpRequest = mediatorHttpRequest.cmd

        httpRequest.method shouldEqual HttpMethods.PATCH
        httpRequest.uri shouldEqual Uri("https://api.twitch.tv/helix/channels").withQuery(Query("broadcaster_id" -> roomId))
        httpRequest.headers.exists(_.name == "Authorization") shouldEqual true

        val fakeResponse = StatusReply.success(HttpClient.Response(
          response = HttpResponse(
            status = StatusCodes.NoContent
          ),
          entityString = ""
        ))

        httpRequest.replyTo ! fakeResponse

        val reply = replyProbe.receiveMessage()
        reply.isSuccess shouldEqual true
        reply.getValue shouldEqual TwitchApiResponse.ModifyChannelInformation

        mediatorProbe.expectNoMessage()
      }

      "reply with ModifyChannelInformation when ChangeChannelToUnknownGame message is sent" in {
        val replyProbe = testKit.createTestProbe[StatusReply[TwitchApiResponse.ModifyChannelInformation.type]]()

        val tokenId = "uiop258"
        val roomId = archimond7450Id
        val gameName = warcraft3

        client ! TwitchApiClient.ChangeChannelToUnknownGame(
          replyTo = replyProbe.ref,
          tokenId = tokenId,
          roomId = roomId,
          gameName = gameName
        )

        val mediatorTokenRequest1 = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand]
        twitchUserSessionsRepository ! mediatorTokenRequest1.cmd
        val tokenRequest1 = repoProbe.expectMessageType[TwitchUserSessionsRepository.GetTokenFromId]
        tokenRequest1.tokenId shouldEqual tokenId

        val token = exampleToken

        tokenRequest1.replyTo ! TwitchUserSessionsRepository.ReturnedTokenFromId(
          token = Some(token)
        )

        val mediatorHttpRequestGetGame = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val httpRequestGetGame = mediatorHttpRequestGetGame.cmd

        httpRequestGetGame.method shouldEqual HttpMethods.GET
        httpRequestGetGame.uri shouldEqual Uri("https://api.twitch.tv/helix/games").withQuery(Query("name" -> gameName))
        httpRequestGetGame.headers.exists(_.name == "Authorization") shouldEqual true

        val fakeGetGameBody = TwitchApiResponse.GetGames(
          data = List(
            warcraft3Game
          )
        )

        val fakeGetGameResponse = StatusReply.success(HttpClient.Response(
          response = HttpResponse(
            status = StatusCodes.OK
          ),
          entityString = fakeGetGameBody.asJson.noSpaces
        ))

        httpRequestGetGame.replyTo ! fakeGetGameResponse

        val mediatorTokenRequest2 = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand]
        twitchUserSessionsRepository ! mediatorTokenRequest2.cmd
        val tokenRequest2 = repoProbe.expectMessageType[TwitchUserSessionsRepository.GetTokenFromId]
        tokenRequest2.tokenId shouldEqual tokenId

        tokenRequest2.replyTo ! TwitchUserSessionsRepository.ReturnedTokenFromId(
          token = Some(token)
        )

        val mediatorHttpRequestChange = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val httpRequestChange = mediatorHttpRequestChange.cmd

        httpRequestChange.method shouldEqual HttpMethods.PATCH
        httpRequestChange.uri shouldEqual Uri("https://api.twitch.tv/helix/channels").withQuery(Query("broadcaster_id" -> roomId))
        httpRequestChange.headers.exists(_.name == "Authorization") shouldEqual true

        val fakeChangeResponse = StatusReply.success(HttpClient.Response(
          response = HttpResponse(
            status = StatusCodes.NoContent
          ),
          entityString = ""
        ))

        httpRequestChange.replyTo ! fakeChangeResponse

        val reply = replyProbe.receiveMessage()
        reply.isSuccess shouldEqual true
        reply.getValue shouldEqual TwitchApiResponse.ModifyChannelInformation

        mediatorProbe.expectNoMessage()
      }

      "reply with ModifyChannelInformation when ChangeChannelTitle message is sent" in {
        val replyProbe = testKit.createTestProbe[StatusReply[TwitchApiResponse.ModifyChannelInformation.type]]()

        val tokenId = "qwert789"
        val roomId = archimond7450Id
        val gameId = warcraft3Id
        val title = "New Archimond7450's channel title"

        client ! TwitchApiClient.ChangeChannelTitle(
          replyTo = replyProbe.ref,
          tokenId = tokenId,
          roomId = archimond7450Id,
          title = title
        )

        val mediatorTokenRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand]
        twitchUserSessionsRepository ! mediatorTokenRequest.cmd
        val tokenRequest = repoProbe.expectMessageType[TwitchUserSessionsRepository.GetTokenFromId]
        tokenRequest.tokenId shouldEqual tokenId

        val token = exampleToken

        tokenRequest.replyTo ! TwitchUserSessionsRepository.ReturnedTokenFromId(
          token = Some(token)
        )

        val mediatorHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val httpRequest = mediatorHttpRequest.cmd

        httpRequest.method shouldEqual HttpMethods.PATCH
        httpRequest.uri shouldEqual Uri("https://api.twitch.tv/helix/channels").withQuery(Query("broadcaster_id" -> roomId))
        httpRequest.headers.exists(_.name == "Authorization") shouldEqual true

        val fakeResponse = StatusReply.success(HttpClient.Response(
          response = HttpResponse(
            status = StatusCodes.NoContent
          ),
          entityString = ""
        ))

        httpRequest.replyTo ! fakeResponse

        val reply = replyProbe.receiveMessage()
        reply.isSuccess shouldEqual true
        reply.getValue shouldEqual TwitchApiResponse.ModifyChannelInformation

        mediatorProbe.expectNoMessage()
      }

      "reply with ChannelInformation when GetChannelInformation message is sent" in {
        val replyProbe = testKit.createTestProbe[StatusReply[TwitchApiResponse.ChannelInformation]]()

        val tokenId = "cvbnm369"
        val roomId = archimond7450Id

        client ! TwitchApiClient.GetChannelInformation(
          replyTo = replyProbe.ref,
          tokenId = tokenId,
          roomId = roomId
        )

        val mediatorTokenRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand]
        twitchUserSessionsRepository ! mediatorTokenRequest.cmd
        val tokenRequest = repoProbe.expectMessageType[TwitchUserSessionsRepository.GetTokenFromId]
        tokenRequest.tokenId shouldEqual tokenId

        val token = exampleToken

        tokenRequest.replyTo ! TwitchUserSessionsRepository.ReturnedTokenFromId(
          token = Some(token)
        )

        val mediatorHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val httpRequest = mediatorHttpRequest.cmd

        httpRequest.method shouldEqual HttpMethods.GET
        httpRequest.uri shouldEqual Uri("https://api.twitch.tv/helix/channels").withQuery(Query("broadcaster_id" -> roomId))
        httpRequest.headers.exists(_.name == "Authorization") shouldEqual true

        val fakeBody = TwitchApiResponse.GetChannelInformation(
          data = List(
            TwitchApiResponse.ChannelInformation(
              broadcaster_id = archimond7450Id,
              broadcaster_login = archimond7450.toLowerCase,
              broadcaster_name = archimond7450,
              broadcaster_language = "en",
              game_name = warcraft3,
              game_id = warcraft3Id,
              title = "Archimond7450's channel title",
              delay = 0,
              tags = Nil,
              content_classification_labels = Nil,
              is_branded_content = false
            )
          )
        )

        val fakeResponse = StatusReply.success(HttpClient.Response(
          response = HttpResponse(
            status = StatusCodes.OK
          ),
          entityString = fakeBody.asJson.noSpaces
        ))

        httpRequest.replyTo ! fakeResponse

        val reply = replyProbe.receiveMessage()
        reply.isSuccess shouldEqual true
        reply.getValue shouldEqual fakeBody.data.last

        mediatorProbe.expectNoMessage()
      }

      "reply with GetChatters when GetChatters message is sent" in {
        val replyProbe = testKit.createTestProbe[StatusReply[TwitchApiResponse.GetChatters]]()

        val tokenId = "ghjkl147"
        val roomId = archimond7450Id

        client ! TwitchApiClient.GetChatters(
          replyTo = replyProbe.ref,
          tokenId = tokenId,
          roomId = roomId,
          moderatorId = roomId
        )

        val mediatorTokenRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand]
        twitchUserSessionsRepository ! mediatorTokenRequest.cmd
        val tokenRequest = repoProbe.expectMessageType[TwitchUserSessionsRepository.GetTokenFromId]
        tokenRequest.tokenId shouldEqual tokenId

        val token = exampleToken

        tokenRequest.replyTo ! TwitchUserSessionsRepository.ReturnedTokenFromId(
          token = Some(token)
        )

        val mediatorHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val httpRequest = mediatorHttpRequest.cmd

        httpRequest.method shouldEqual HttpMethods.GET
        httpRequest.uri shouldEqual Uri("https://api.twitch.tv/helix/chat/chatters").withQuery(
          Query(
            "broadcaster_id" -> roomId,
            "moderator_id" -> roomId,
            "first" -> "1000"
          )
        )
        httpRequest.headers.exists(_.name == "Authorization") shouldEqual true

        val fakeBody = TwitchApiResponse.GetChatters(
          data = List(
            TwitchApi.User(user_id = archimond7450Id, user_login = archimond7450.toLowerCase, user_name = archimond7450)
          ),
          pagination = TwitchApi.Pagination(
            cursor = None
          ),
          total = 1
        )

        val fakeResponse = StatusReply.success(HttpClient.Response(
          response = HttpResponse(
            status = StatusCodes.OK
          ),
          entityString = fakeBody.asJson.noSpaces
        ))

        httpRequest.replyTo ! fakeResponse

        val reply = replyProbe.receiveMessage()
        reply.isSuccess shouldEqual true
        reply.getValue shouldEqual fakeBody

        mediatorProbe.expectNoMessage()
      }

      "reply with GetModerators when GetModerators message is sent" in {
        val replyProbe = testKit.createTestProbe[StatusReply[TwitchApiResponse.GetModerators]]()

        val tokenId = "wsxed753"
        val roomId = archimond7450Id

        client ! TwitchApiClient.GetModerators(
          replyTo = replyProbe.ref,
          tokenId = tokenId,
          roomId = roomId
        )

        val mediatorTokenRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand]
        twitchUserSessionsRepository ! mediatorTokenRequest.cmd
        val tokenRequest = repoProbe.expectMessageType[TwitchUserSessionsRepository.GetTokenFromId]
        tokenRequest.tokenId shouldEqual tokenId

        val token = exampleToken

        tokenRequest.replyTo ! TwitchUserSessionsRepository.ReturnedTokenFromId(
          token = Some(token)
        )

        val mediatorHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val httpRequest = mediatorHttpRequest.cmd

        httpRequest.method shouldEqual HttpMethods.GET
        httpRequest.uri shouldEqual Uri("https://api.twitch.tv/helix/moderation/moderators").withQuery(
          Query(
            "broadcaster_id" -> roomId,
            "first" -> "100"
          )
        )
        httpRequest.headers.exists(_.name == "Authorization") shouldEqual true

        val fakeBody = TwitchApiResponse.GetModerators(
          data = List(
            TwitchApi.User(user_id = archimond7450Id, user_login = archimond7450.toLowerCase, user_name = archimond7450)
          ),
          pagination = TwitchApi.Pagination(
            cursor = None
          )
        )

        val fakeResponse = StatusReply.success(HttpClient.Response(
          response = HttpResponse(
            status = StatusCodes.OK
          ),
          entityString = fakeBody.asJson.noSpaces
        ))

        httpRequest.replyTo ! fakeResponse

        val reply = replyProbe.receiveMessage()
        reply.isSuccess shouldEqual true
        reply.getValue shouldEqual fakeBody

        mediatorProbe.expectNoMessage()
      }

      "reply with GetVIPs when GetVIPs message is sent" in {
        val replyProbe = testKit.createTestProbe[StatusReply[TwitchApiResponse.GetVIPs]]()

        val tokenId = "rfvtgb159"
        val roomId = archimond7450Id

        client ! TwitchApiClient.GetVIPs(
          replyTo = replyProbe.ref,
          tokenId = tokenId,
          roomId = roomId
        )

        val mediatorTokenRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand]
        twitchUserSessionsRepository ! mediatorTokenRequest.cmd
        val tokenRequest = repoProbe.expectMessageType[TwitchUserSessionsRepository.GetTokenFromId]
        tokenRequest.tokenId shouldEqual tokenId

        val token = exampleToken

        tokenRequest.replyTo ! TwitchUserSessionsRepository.ReturnedTokenFromId(
          token = Some(token)
        )

        val mediatorHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val httpRequest = mediatorHttpRequest.cmd

        httpRequest.method shouldEqual HttpMethods.GET
        httpRequest.uri shouldEqual Uri("https://api.twitch.tv/helix/channels/vips").withQuery(
          Query(
            "broadcaster_id" -> roomId,
            "first" -> "100"
          )
        )
        httpRequest.headers.exists(_.name == "Authorization") shouldEqual true

        val fakeBody = TwitchApiResponse.GetVIPs(
          data = List(
            TwitchApi.User(user_id = archimond7450Id, user_login = archimond7450.toLowerCase, user_name = archimond7450)
          ),
          pagination = TwitchApi.Pagination(
            cursor = None
          )
        )

        val fakeResponse = StatusReply.success(HttpClient.Response(
          response = HttpResponse(
            status = StatusCodes.OK
          ),
          entityString = fakeBody.asJson.noSpaces
        ))

        httpRequest.replyTo ! fakeResponse

        val reply = replyProbe.receiveMessage()
        reply.isSuccess shouldEqual true
        reply.getValue shouldEqual fakeBody

        mediatorProbe.expectNoMessage()
      }

      "reply with GetSubs when GetSubs message is sent" in {
        val replyProbe = testKit.createTestProbe[StatusReply[TwitchApiResponse.GetSubs]]()

        val tokenId = "ujmikolp5"
        val roomId = archimond7450Id

        client ! TwitchApiClient.GetSubs(
          replyTo = replyProbe.ref,
          tokenId = tokenId,
          roomId = roomId
        )

        val mediatorTokenRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand]
        twitchUserSessionsRepository ! mediatorTokenRequest.cmd
        val tokenRequest = repoProbe.expectMessageType[TwitchUserSessionsRepository.GetTokenFromId]
        tokenRequest.tokenId shouldEqual tokenId

        val token = exampleToken

        tokenRequest.replyTo ! TwitchUserSessionsRepository.ReturnedTokenFromId(
          token = Some(token)
        )

        val mediatorHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val httpRequest = mediatorHttpRequest.cmd

        httpRequest.method shouldEqual HttpMethods.GET
        httpRequest.uri shouldEqual Uri("https://api.twitch.tv/helix/subscriptions").withQuery(
          Query(
            "broadcaster_id" -> roomId,
            "first" -> "100"
          )
        )
        httpRequest.headers.exists(_.name == "Authorization") shouldEqual true

        val fakeBody = TwitchApiResponse.GetSubs(
          data = List(
            TwitchApi.SubbedUser(
              broadcaster_id = archimond7450Id,
              broadcaster_login = archimond7450.toLowerCase,
              broadcaster_name = archimond7450,
              gifter_id = "",
              gifter_login = "",
              gifter_name = "",
              is_gift = false,
              tier = "3000",
              plan_name = "Channel subscription (archimond7450)",
              user_id = archimond7450Id,
              user_login = archimond7450.toLowerCase,
              user_name = archimond7450
            )
          ),
          pagination = TwitchApi.Pagination(
            cursor = None
          ),
          total = 1,
          points = 1
        )

        val fakeResponse = StatusReply.success(HttpClient.Response(
          response = HttpResponse(
            status = StatusCodes.OK
          ),
          entityString = fakeBody.asJson.noSpaces
        ))

        httpRequest.replyTo ! fakeResponse

        val reply = replyProbe.receiveMessage()
        reply.isSuccess shouldEqual true
        reply.getValue shouldEqual fakeBody

        mediatorProbe.expectNoMessage()
      }

      "reply with SendShoutout when SendShoutout message is sent" in {
        val replyProbe = testKit.createTestProbe[StatusReply[TwitchApiResponse.SendShoutout.type]]()

        val tokenId = "uiopjklm"
        val fromBroadcaserId = archimond7450Id
        val toBroadcasterId = wtiiId

        client ! TwitchApiClient.SendShoutout(
          replyTo = replyProbe.ref,
          tokenId = tokenId,
          roomId = archimond7450Id,
          toBroadcasterId = wtiiId,
          moderatorId = archimond7450Id
        )

        val mediatorTokenRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand]
        twitchUserSessionsRepository ! mediatorTokenRequest.cmd
        val tokenRequest = repoProbe.expectMessageType[TwitchUserSessionsRepository.GetTokenFromId]
        tokenRequest.tokenId shouldEqual tokenId

        val token = exampleToken

        tokenRequest.replyTo ! TwitchUserSessionsRepository.ReturnedTokenFromId(
          token = Some(token)
        )

        val mediatorHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val httpRequest = mediatorHttpRequest.cmd

        httpRequest.method shouldEqual HttpMethods.POST
        httpRequest.uri shouldEqual Uri("https://api.twitch.tv/helix/chat/shoutouts").withQuery(
          Query(
            "from_broadcaster_id" -> fromBroadcaserId,
            "to_broadcaster_id" -> toBroadcasterId,
            "moderator_id" -> fromBroadcaserId
          )
        )
        httpRequest.headers.exists(_.name == "Authorization") shouldEqual true

        val fakeResponse = StatusReply.success(HttpClient.Response(
          response = HttpResponse(
            status = StatusCodes.NoContent
          ),
          entityString = ""
        ))

        httpRequest.replyTo ! fakeResponse

        val reply = replyProbe.receiveMessage()
        reply.isSuccess shouldEqual true
        reply.getValue shouldEqual TwitchApiResponse.SendShoutout

        mediatorProbe.expectNoMessage()
      }

      "reply with GetChannelFollowers when GetChannelFollowers messaeg is sent" in {
        val replyProbe = testKit.createTestProbe[StatusReply[TwitchApiResponse.GetChannelFollowers]]()

        val tokenId = "wasdesdf"
        val roomId = archimond7450Id

        client ! TwitchApiClient.GetChannelFollowers(
          replyTo = replyProbe.ref,
          tokenId = tokenId,
          roomId = roomId
        )

        val mediatorTokenRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand]
        twitchUserSessionsRepository ! mediatorTokenRequest.cmd
        val tokenRequest = repoProbe.expectMessageType[TwitchUserSessionsRepository.GetTokenFromId]
        tokenRequest.tokenId shouldEqual tokenId

        val token = exampleToken

        tokenRequest.replyTo ! TwitchUserSessionsRepository.ReturnedTokenFromId(
          token = Some(token)
        )

        val mediatorHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val httpRequest = mediatorHttpRequest.cmd

        httpRequest.method shouldEqual HttpMethods.GET
        httpRequest.uri shouldEqual Uri("https://api.twitch.tv/helix/channels/followers").withQuery(
          Query(
            "broadcaster_id" -> roomId,
            "first" -> "100"
          )
        )
        httpRequest.headers.exists(_.name == "Authorization") shouldEqual true

        val fakeBody = TwitchApiResponse.GetChannelFollowers(
          total = 1,
          data = List(
            TwitchApi.UserFollowage(user_id = archimond7450Id, user_login = archimond7450.toLowerCase, user_name = archimond7450, followed_at = OffsetDateTime.now)
          ),
          pagination = TwitchApi.Pagination(
            cursor = None
          )
        )

        val fakeResponse = StatusReply.success(HttpClient.Response(
          response = HttpResponse(
            status = StatusCodes.OK
          ),
          entityString = fakeBody.asJson.noSpaces
        ))

        httpRequest.replyTo ! fakeResponse

        val reply = replyProbe.receiveMessage()
        reply.isSuccess shouldEqual true
        reply.getValue shouldEqual fakeBody

        mediatorProbe.expectNoMessage()
      }

      "reply with CheckUserFollowage when CheckUserFollowage message is sent" in {
        val replyProbe = testKit.createTestProbe[StatusReply[TwitchApiResponse.CheckUserFollowage]]()

        val tokenId = "yxcvbnm"
        val roomId = wtiiId
        val userId = archimond7450Id

        client ! TwitchApiClient.CheckUserFollowage(
          replyTo = replyProbe.ref,
          tokenId = tokenId,
          roomId = roomId,
          userId = userId
        )

        val mediatorTokenRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand]
        twitchUserSessionsRepository ! mediatorTokenRequest.cmd
        val tokenRequest = repoProbe.expectMessageType[TwitchUserSessionsRepository.GetTokenFromId]
        tokenRequest.tokenId shouldEqual tokenId

        val token = exampleToken

        tokenRequest.replyTo ! TwitchUserSessionsRepository.ReturnedTokenFromId(
          token = Some(token)
        )

        val mediatorHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val httpRequest = mediatorHttpRequest.cmd

        httpRequest.method shouldEqual HttpMethods.GET
        httpRequest.uri shouldEqual Uri("https://api.twitch.tv/helix/channels/followers").withQuery(
          Query(
            "broadcaster_id" -> roomId,
            "first" -> "100",
            "user_id" -> userId
          )
        )
        httpRequest.headers.exists(_.name == "Authorization") shouldEqual true

        val fakeBody = TwitchApiResponse.GetChannelFollowers(
          total = 1,
          data = List(
            TwitchApi.UserFollowage(user_id = archimond7450Id, user_login = archimond7450.toLowerCase, user_name = archimond7450, followed_at = OffsetDateTime.now)
          ),
          pagination = TwitchApi.Pagination(
            cursor = None
          )
        )

        val fakeResponse = StatusReply.success(HttpClient.Response(
          response = HttpResponse(
            status = StatusCodes.OK
          ),
          entityString = fakeBody.asJson.noSpaces
        ))

        httpRequest.replyTo ! fakeResponse

        val reply = replyProbe.receiveMessage()
        reply.isSuccess shouldEqual true
        reply.getValue shouldEqual TwitchApiResponse.CheckUserFollowage(fakeBody.data.lastOption)

        mediatorProbe.expectNoMessage()
      }

      "reply with CreateEventSubWebsocketSubscriptionResponse when CreateEventSubWebsocketSubscription message is sent" in {
        val replyProbe = testKit.createTestProbe[StatusReply[TwitchApiResponse.CreateEventSubWebsocketSubscriptionResponse]]()

        val tokenId = "qwertz"
        val websocketSessionId = "wsxujmrfvtgb"
        val subscriptionType = "stream.online"
        val subscriptionVersion = "1"
        val condition = Condition(broadcasterUserId = Some(archimond7450Id))

        client ! TwitchApiClient.CreateEventSubWebsocketSubscription(
          replyTo = replyProbe.ref,
          tokenId = tokenId,
          websocketSessionId = websocketSessionId,
          subscriptionType = subscriptionType,
          subscriptionVersion = subscriptionVersion,
          condition = condition
        )

        val mediatorTokenRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand]
        twitchUserSessionsRepository ! mediatorTokenRequest.cmd
        val tokenRequest = repoProbe.expectMessageType[TwitchUserSessionsRepository.GetTokenFromId]
        tokenRequest.tokenId shouldEqual tokenId

        val token = exampleToken

        tokenRequest.replyTo ! TwitchUserSessionsRepository.ReturnedTokenFromId(
          token = Some(token)
        )

        val mediatorHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val httpRequest = mediatorHttpRequest.cmd

        httpRequest.method shouldEqual HttpMethods.POST
        httpRequest.uri shouldEqual Uri("https://api.twitch.tv/helix/eventsub/subscriptions")
        httpRequest.headers.exists(_.name == "Authorization") shouldEqual true

        val fakeBody = TwitchApiResponse.CreateEventSubWebsocketSubscriptionResponse(
          data = List(
            Subscription(
              id = "123456789",
              status = "enabled",
              `type` = subscriptionType,
              version = subscriptionVersion,
              cost = 0,
              condition = condition,
              transport = Transport(method = "websocket", session = Some(websocketSessionId), connectedAt = Some(OffsetDateTime.now)),
              createdAt = OffsetDateTime.now
            )
          ),
          total = 1,
          totalCost = 0,
          maxTotalCost = 10
        )

        val fakeResponse = StatusReply.success(HttpClient.Response(
          response = HttpResponse(
            status = StatusCodes.Accepted
          ),
          entityString = fakeBody.asJson.noSpaces
        ))

        httpRequest.replyTo ! fakeResponse

        val reply = replyProbe.receiveMessage()
        reply.isSuccess shouldEqual true
        reply.getValue shouldEqual fakeBody

        mediatorProbe.expectNoMessage()
      }

      "reply with GetStream when GetStream message is sent" in {
        val replyProbe = testKit.createTestProbe[StatusReply[TwitchApiResponse.GetStream]]()

        val tokenId = "rfvbgt"
        val roomId = archimond7450Id

        client ! TwitchApiClient.GetStream(
          replyTo = replyProbe.ref,
          tokenId = tokenId,
          roomId = roomId
        )

        val mediatorTokenRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand]
        twitchUserSessionsRepository ! mediatorTokenRequest.cmd
        val tokenRequest = repoProbe.expectMessageType[TwitchUserSessionsRepository.GetTokenFromId]
        tokenRequest.tokenId shouldEqual tokenId

        val token = exampleToken

        tokenRequest.replyTo ! TwitchUserSessionsRepository.ReturnedTokenFromId(
          token = Some(token)
        )

        val mediatorHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val httpRequest = mediatorHttpRequest.cmd

        httpRequest.method shouldEqual HttpMethods.GET
        httpRequest.uri shouldEqual Uri("https://api.twitch.tv/helix/streams").withQuery(Query("user_id" -> roomId))
        httpRequest.headers.exists(_.name == "Authorization") shouldEqual true

        val fakeBody = TwitchApiResponse.GetStream(
          data = List(
            TwitchApi.Stream(
              id = "987654321",
              userId = archimond7450Id,
              userLogin = archimond7450.toLowerCase,
              userName = archimond7450,
              gameId = warcraft3Id,
              gameName = warcraft3,
              `type` = "live",
              title = "Long live Warcraft III",
              tags = List("English"),
              viewerCount = 1,
              startedAt = OffsetDateTime.now,
              language = "en",
              thumbnailUrl = "https://static-cdn.jtvnw.net/previews-ttv/live_user_auronplay-{width}x{height}.jpg",
              tagIds = Nil,
              isMature = false
            )
          ),
          pagination = TwitchApi.Pagination(cursor = None)
        )

        val fakeResponse = StatusReply.success(HttpClient.Response(
          response = HttpResponse(
            status = StatusCodes.Accepted
          ),
          entityString = fakeBody.asJson.noSpaces
        ))

        httpRequest.replyTo ! fakeResponse

        val reply = replyProbe.receiveMessage()
        reply.isSuccess shouldEqual true
        reply.getValue shouldEqual fakeBody

        mediatorProbe.expectNoMessage()
      }

      "reply with GetEmoteSets when GetEmoteSets message is sent" in {
        val replyProbe = testKit.createTestProbe[StatusReply[TwitchApiResponse.GetEmoteSets]]()

        val tokenId = "bhunji"

        client ! TwitchApiClient.GetEmoteSets(
          replyTo = replyProbe.ref,
          tokenId = tokenId,
          emoteSets = List("1", "2")
        )

        val mediatorTokenRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand]
        twitchUserSessionsRepository ! mediatorTokenRequest.cmd
        val tokenRequest = repoProbe.expectMessageType[TwitchUserSessionsRepository.GetTokenFromId]
        tokenRequest.tokenId shouldEqual tokenId

        val token = exampleToken

        tokenRequest.replyTo ! TwitchUserSessionsRepository.ReturnedTokenFromId(
          token = Some(token)
        )

        val mediatorHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val httpRequest = mediatorHttpRequest.cmd

        httpRequest.method shouldEqual HttpMethods.GET
        httpRequest.uri shouldEqual Uri("https://api.twitch.tv/helix/chat/emotes/set").withQuery(Query("emote_set_id" -> "1", "emote_set_id" -> "2"))
        httpRequest.headers.exists(_.name == "Authorization") shouldEqual true

        val fakeBody = TwitchApiResponse.GetEmoteSets(
          data = List(
            TwitchApi.EmoteWithSet(
              id = "1",
              name = "someOne",
              images = TwitchApi.EmoteImages(
                url_1x = "https://static-cdn.jtvnw.net/emoticons/v2/1/static/light/1.0",
                url_2x = "https://static-cdn.jtvnw.net/emoticons/v2/1/static/light/2.0",
                url_4x = "https://static-cdn.jtvnw.net/emoticons/v2/1/static/light/3.0"
              ),
              emoteType = "subscriptions",
              emoteSetId = "1",
              ownerId = "1",
              format = List("static"),
              scale = List("1.0", "2.0", "3.0"),
              themeMode = List("light", "dark")
            )
          ),
          template = "https://static-cdn.jtvnw.net/emoticons/v2/{{id}}/{{format}}/{{theme_mode}}/{{scale}}"
        )

        val fakeResponse = StatusReply.success(HttpClient.Response(
          response = HttpResponse(
            status = StatusCodes.Accepted
          ),
          entityString = fakeBody.asJson.noSpaces
        ))

        httpRequest.replyTo ! fakeResponse

        val reply = replyProbe.receiveMessage()
        reply.isSuccess shouldEqual true
        reply.getValue shouldEqual fakeBody

        mediatorProbe.expectNoMessage()
      }
    }

    "Twitch API returns Unauthorized response" should {
      "reply with an exception when GetToken message is sent" in {
        val replyProbe = testKit.createTestProbe[StatusReply[TwitchApiResponse.GetToken]]()

        client ! TwitchApiClient.GetToken(
          replyTo = replyProbe.ref,
          code = "the123wrong456code"
        )

        val mediatorHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val httpRequest = mediatorHttpRequest.cmd

        httpRequest.method shouldEqual HttpMethods.POST
        httpRequest.uri shouldEqual Uri("https://id.twitch.tv/oauth2/token")

        httpRequest.replyTo ! httpUnauthorizedResponse

        val reply = replyProbe.receiveMessage()
        reply.isSuccess shouldEqual false
        reply.getError.getMessage shouldEqual twitchResponseException.getMessage

        mediatorProbe.expectNoMessage()
      }

      "refresh the token and reply with GetTokenUser when GetTokenUserFromTokenId message is sent and the refresh succeeds" in {
        val replyProbe = testKit.createTestProbe[StatusReply[TwitchApiResponse.GetTokenUser]]()

        val tokenId = "someTokenId"

        client ! TwitchApiClient.GetTokenUserFromTokenId(
          replyTo = replyProbe.ref,
          tokenId = tokenId
        )

        val mediatorTokenRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand]
        twitchUserSessionsRepository ! mediatorTokenRequest.cmd
        val tokenRequest = repoProbe.expectMessageType[TwitchUserSessionsRepository.GetTokenFromId]
        tokenRequest.tokenId shouldEqual tokenId

        val token = oldToken

        tokenRequest.replyTo ! TwitchUserSessionsRepository.ReturnedTokenFromId(
          token = Some(oldToken)
        )

        val mediatorFirstHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val firstHttpRequest = mediatorFirstHttpRequest.cmd

        firstHttpRequest.method shouldEqual HttpMethods.GET
        firstHttpRequest.uri shouldEqual Uri("https://api.twitch.tv/helix/users")
        firstHttpRequest.headers.exists(_.name == "Authorization") shouldEqual true

        firstHttpRequest.replyTo ! httpUnauthorizedResponse

        val mediatorRefreshTokenHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val refreshTokenHttpRequest = mediatorRefreshTokenHttpRequest.cmd

        refreshTokenHttpRequest.method shouldEqual HttpMethods.POST
        refreshTokenHttpRequest.uri shouldEqual Uri("https://id.twitch.tv/oauth2/token")

        refreshTokenHttpRequest.replyTo ! StatusReply.success(HttpClient.Response(
          response = HttpResponse(
            status = StatusCodes.OK
          ),
          entityString = exampleToken.asJson.noSpaces
        ))

        val mediatorRefreshTokenCommand = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand]
        twitchUserSessionsRepository ! mediatorRefreshTokenCommand.cmd
        repoProbe.expectMessageType[TwitchUserSessionsRepository.RefreshToken] shouldEqual TwitchUserSessionsRepository.RefreshToken(tokenId = tokenId, token = exampleToken)

        val mediatorSecondTokenRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand]
        twitchUserSessionsRepository ! mediatorSecondTokenRequest.cmd
        val secondTokenRequest = repoProbe.expectMessageType[TwitchUserSessionsRepository.GetTokenFromId]
        secondTokenRequest.tokenId shouldEqual tokenId

        secondTokenRequest.replyTo ! TwitchUserSessionsRepository.ReturnedTokenFromId(
          token = Some(exampleToken)
        )

        val mediatorSecondHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val secondHttpRequest = mediatorSecondHttpRequest.cmd

        secondHttpRequest.method shouldEqual HttpMethods.GET
        secondHttpRequest.uri shouldEqual Uri("https://api.twitch.tv/helix/users")
        secondHttpRequest.headers.exists(_.name == "Authorization") shouldEqual true

        val fakeBody = TwitchApiResponse.GetUsers(
          data = List(
            exampleTokenUser
          )
        )
        val fakeResponse = HttpClient.Response(
          HttpResponse(
            status = StatusCodes.OK
          ),
          entityString = fakeBody.asJson.noSpaces
        )

        secondHttpRequest.replyTo ! StatusReply.success(fakeResponse)

        val reply = replyProbe.receiveMessage()
        reply.isSuccess shouldEqual true
        reply.getValue shouldEqual fakeBody.data.last

        val mediatorCacheCommand = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchTokenUserCacheServiceCommand]
        twitchTokenUserCacheService ! mediatorCacheCommand.cmd
        twitchTokenUserCacheServiceProbe.expectMessage(TwitchTokenUserCacheService.CacheTokenUser(reply.getValue))

        mediatorProbe.expectNoMessage()
      }

      "refresh the token and reply with an exception when GetTokenUserFromTokenId message is sent and the refresh fails" in {
        val replyProbe = testKit.createTestProbe[StatusReply[TwitchApiResponse.GetTokenUser]]()

        val tokenId = "someTokenId"

        client ! TwitchApiClient.GetTokenUserFromTokenId(
          replyTo = replyProbe.ref,
          tokenId = tokenId
        )

        val mediatorTokenRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand]
        twitchUserSessionsRepository ! mediatorTokenRequest.cmd
        val tokenRequest = repoProbe.expectMessageType[TwitchUserSessionsRepository.GetTokenFromId]
        tokenRequest.tokenId shouldEqual tokenId

        val token = oldToken

        tokenRequest.replyTo ! TwitchUserSessionsRepository.ReturnedTokenFromId(
          token = Some(oldToken)
        )

        val mediatorHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val httpRequest = mediatorHttpRequest.cmd

        httpRequest.method shouldEqual HttpMethods.GET
        httpRequest.uri shouldEqual Uri("https://api.twitch.tv/helix/users")
        httpRequest.headers.exists(_.name == "Authorization") shouldEqual true

        httpRequest.replyTo ! httpUnauthorizedResponse

        val mediatorRefreshTokenHttpRequest = mediatorProbe.expectMessageType[ArchieMateMediator.SendHttpClientRequest]
        val refreshTokenHttpRequest = mediatorRefreshTokenHttpRequest.cmd

        refreshTokenHttpRequest.method shouldEqual HttpMethods.POST
        refreshTokenHttpRequest.uri shouldEqual Uri("https://id.twitch.tv/oauth2/token")

        refreshTokenHttpRequest.replyTo ! httpUnauthorizedResponse

        val reply = replyProbe.receiveMessage()
        reply.isSuccess shouldEqual false
        reply.getError.getMessage shouldEqual twitchResponseException.getMessage

        mediatorProbe.expectNoMessage()
      }
    }
  }
}

object TwitchApiClientSpec {
  val exampleToken = TwitchApiResponse.GetToken(
    access_token = "accesstoken",
    expires_in = 54321,
    refresh_token = "refreshtoken",
    scope = Some(Nil),
    token_type = "bearer"
  )
  val exampleTokenUser = TwitchApiResponse.GetTokenUser(
    id = "1",
    login = "testuser",
    display_name = "TestUser",
    `type` = "",
    broadcaster_type = "",
    description = "Description",
    profile_image_url = "",
    offline_image_url = "",
    view_count = 0,
    email = None,
    created_at = OffsetDateTime.now
  )
  val oldToken = TwitchApiResponse.GetToken(
    access_token = "oldaccesstoken",
    expires_in = 54321,
    refresh_token = "oldrefreshtoken",
    scope = Some(Nil),
    token_type = "bearer"
  )
  val archimond7450Id = "147113965"
  val archimond7450 = "Archimond7450"
  val wtiiId = "23693840"
  val wtii = "WTii"
  val warcraft3 = "Warcraft III"
  val warcraft3Id = "12924"
  val warcraft3BoxArtUrl = s"https://static-cdn.jtvnw.net/ttv-boxart/${warcraft3Id}-{width}x{height}.jpg"
  val warcraft3Game = TwitchApi.Game(
    id = warcraft3Id,
    name = warcraft3,
    box_art_url = warcraft3BoxArtUrl,
    igdb_id = ""
  )
  val twitchResponseException = RuntimeException("Received error response from Twitch")
  val httpUnauthorizedResponse = StatusReply.success(HttpClient.Response(
    HttpResponse(
      status = StatusCodes.Unauthorized
    ),
    entityString = "Unauthorized"
  ))
}
