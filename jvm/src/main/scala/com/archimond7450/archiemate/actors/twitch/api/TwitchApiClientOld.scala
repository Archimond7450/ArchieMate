/*package com.archimond7450.archiemate.actors.twitch.api

import com.archimond7450.archiemate.actors.HttpClient
import com.archimond7450.archiemate.actors.repositories.TwitchUserSessionsRepository
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.helpers.JsonHelper.decodeOrThrow
import com.archimond7450.archiemate.twitch.api.*
import com.archimond7450.archiemate.twitch.api.TwitchApiResponse
import com.archimond7450.archiemate.twitch.eventsub.{Condition, Transport}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.http.scaladsl.HttpExt
import org.apache.pekko.http.scaladsl.marshalling.Marshal
import org.apache.pekko.http.scaladsl.model.Uri.Query
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.model.{ContentTypes, FormData, HttpEntity, HttpHeader, HttpMethod, HttpMethods, RequestEntity, StatusCode, StatusCodes, Uri}
import org.apache.pekko.pattern.StatusReply
import org.apache.pekko.util.Timeout
import org.slf4j.Logger
import io.circe.syntax.EncoderOps

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success, Try}

object TwitchApiClientOld {
  val actorName = "TwitchApiClient"

  sealed trait Command

  case object Nothing extends Command

  private final case class Request[T <: TwitchApiResponse](
                                                            replyTo: ActorRef[StatusReply[T]],
                                                            tokenId: Option[String] = None,
                                                            token: Option[TwitchApiResponse.GetToken] = None,
                                                            shouldRefresh: Boolean = false,
                                                            method: HttpMethod,
                                                            uri: Uri,
                                                            headers: Seq[HttpHeader] = Seq.empty,
                                                            entity: RequestEntity = HttpEntity.Empty,
                                                            onHttpResponse: HttpClient.Response => Unit
                                                          ) extends Command

  private[api] final case class RefreshToken[T <: TwitchApiResponse](
                                                                      originalRequest: Request[T]
                                                                    ) extends Command

  private[api] final case class RefreshTokenMarshalled[T <: TwitchApiResponse](
                                                                                originalRequest: Request[T],
                                                                                entity: RequestEntity
                                                                              ) extends Command

  final case class GetToken(
                             replyTo: ActorRef[StatusReply[TwitchApiResponse.GetToken]],
                             code: String
                           ) extends Command

  private[api] final case class GetTokenMarshalled(
                                                    replyTo: ActorRef[StatusReply[TwitchApiResponse.GetToken]],
                                                    entity: RequestEntity
                                                  ) extends Command

  case class GetTokenUserFromTokenId(
                                      replyTo: ActorRef[StatusReply[TwitchApiResponse.GetTokenUser]],
                                      tokenId: String
                                    ) extends Command

  case class GetTokenUserFromAccessToken(
                                          replyTo: ActorRef[StatusReply[TwitchApiResponse.GetTokenUser]],
                                          accessToken: String
                                        ) extends Command

  case class GetGameByName(
                            replyTo: ActorRef[StatusReply[TwitchApiResponse.GameResponse]],
                            tokenId: String,
                            gameName: String
                          ) extends Command

  case class ChangeChannelGame(
                                replyTo: ActorRef[StatusReply[TwitchApiResponse.ModifyChannelInformation.type]],
                                tokenId: String,
                                roomId: String,
                                gameId: String
                              ) extends Command

  case class ChangeChannelToUnknownGame(
                                         replyTo: ActorRef[StatusReply[TwitchApiResponse.ModifyChannelInformation.type]],
                                         tokenId: String,
                                         roomId: String,
                                         gameName: String
                                       ) extends Command

  case class ChangeChannelTitle(
                                 replyTo: ActorRef[StatusReply[TwitchApiResponse.ModifyChannelInformation.type]],
                                 tokenId: String,
                                 roomId: String,
                                 title: String
                               ) extends Command

  case class GetChannelInformation(
                                    replyTo: ActorRef[StatusReply[TwitchApiResponse.ChannelInformation]],
                                    tokenId: String,
                                    roomId: String
                                  ) extends Command

  case class GetChatters(
                          replyTo: ActorRef[StatusReply[TwitchApiResponse.GetChatters]],
                          tokenId: String,
                          roomId: String,
                          moderatorId: String
                        ) extends Command

  case class GetModerators(
                            replyTo: ActorRef[StatusReply[TwitchApiResponse.GetModerators]],
                            tokenId: String,
                            roomId: String
                          ) extends Command

  case class GetVIPs(
                      replyTo: ActorRef[StatusReply[TwitchApiResponse.GetVIPs]],
                      tokenId: String,
                      roomId: String
                    ) extends Command

  case class GetSubs(
                      replyTo: ActorRef[StatusReply[TwitchApiResponse.GetSubs]],
                      tokenId: String,
                      roomId: String
                    ) extends Command

  case class SendShoutout(
                           replyTo: ActorRef[StatusReply[TwitchApiResponse.SendShoutout.type]],
                           tokenId: String,
                           roomId: String,
                           toBroadcasterId: String,
                           moderatorId: String
                         ) extends Command

  case class GetChannelFollowers(
                                  replyTo: ActorRef[StatusReply[TwitchApiResponse.GetChannelFollowers]],
                                  tokenId: String,
                                  roomId: String
                                ) extends Command

  case class CheckUserFollowage(
                                 replyTo: ActorRef[StatusReply[TwitchApiResponse.CheckUserFollowage]],
                                 tokenId: String,
                                 roomId: String,
                                 userId: String
                               ) extends Command

  case class CreateEventSubWebsocketSubscription(
                                                  replyTo: ActorRef[
                                                    StatusReply[TwitchApiResponse.CreateEventSubWebsocketSubscriptionResponse]
                                                  ],
                                                  tokenId: String,
                                                  websocketSessionId: String,
                                                  subscriptionType: String,
                                                  subscriptionVersion: String,
                                                  condition: Condition
                                                ) extends Command

  case class GetStream(
                        replyTo: ActorRef[StatusReply[TwitchApiResponse.GetStream]],
                        tokenId: String
                      ) extends Command

  case class GetEmoteSets(
                           replyTo: ActorRef[StatusReply[TwitchApiResponse.GetEmoteSets]],
                           tokenId: String,
                           emoteSets: List[String]
                         ) extends Command

  def apply(
             httpClient: ActorRef[HttpClient.Request],
             twitchUserSessionsRepository: ActorRef[
               TwitchUserSessionsRepository.Command
             ]
           ): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx
    given Logger = ctx.log
    given ExecutionContextExecutor = ctx.executionContext
    given ActorRef[HttpClient.Request] = httpClient
    given ActorRef[TwitchUserSessionsRepository.Command] = twitchUserSessionsRepository
    given self: ActorRef[Command] = ctx.self
    given settings: Settings = Settings(ctx.system)
    given Timeout = settings.askTimeout

    Behaviors.receiveAndLogMessage {
      case Nothing =>
        Behaviors.same

      case cmd: Request[TwitchApiResponse] @unchecked =>
        processRequest(cmd)
        Behaviors.same

      case cmd: RefreshToken[TwitchApiResponse] @unchecked =>
        processRefreshToken(cmd)
        Behaviors.same

      case cmd: RefreshTokenMarshalled[TwitchApiResponse] @unchecked =>
        given RefreshTokenMarshalled[TwitchApiResponse] = cmd
        processRefreshTokenMarshalled()
        Behaviors.same

      case cmd: GetToken =>
        processGetToken(cmd)
        Behaviors.same

      case cmd: GetTokenMarshalled =>
        given GetTokenMarshalled = cmd
        processGetTokenMarshalled()
        Behaviors.same

      case cmd: GetTokenUserFromTokenId =>
        given GetTokenUserFromTokenId = cmd
        processGetTokenUserFromTokenId()
        Behaviors.same

      case cmd: GetTokenUserFromAccessToken =>
        processGetTokenUserFromAccessToken(cmd)
        Behaviors.same

      case cmd: GetGameByName =>
        given GetGameByName = cmd
        processGetGameByName()
        Behaviors.same

      case cmd: ChangeChannelToUnknownGame =>
        processChangeChannelToUnknownGame(cmd)
        Behaviors.same

      case cmd: ChangeChannelGame =>
        given ChangeChannelGame = cmd
        processChangeChannelGame()
        Behaviors.same

      case cmd: ChangeChannelTitle =>
        given ChangeChannelTitle = cmd
        processChangeChannelTitle()
        Behaviors.same

      case cmd: GetChannelInformation =>
        given GetChannelInformation = cmd
        processGetChannelInformation()
        Behaviors.same

      case cmd: GetChatters =>
        given GetChatters = cmd
        processGetChatters()
        Behaviors.same

      case cmd: GetModerators =>
        given GetModerators = cmd
        processGetModerators()
        Behaviors.same

      case cmd: GetVIPs =>
        given GetVIPs = cmd
        processGetVIPs()
        Behaviors.same

      case cmd: GetSubs =>
        given GetSubs = cmd
        processGetSubs()
        Behaviors.same

      case cmd: SendShoutout =>
        given SendShoutout = cmd
        processSendShoutout()
        Behaviors.same

      case cmd: GetChannelFollowers =>
        given GetChannelFollowers = cmd
        processGetChannelFollowers()
        Behaviors.same

      case cmd: CheckUserFollowage =>
        given CheckUserFollowage = cmd
        processCheckUserFollowage()
        Behaviors.same

      case cmd: CreateEventSubWebsocketSubscription =>
        given CreateEventSubWebsocketSubscription = cmd
        processCreateEventSubWebsocketSubscription()
        Behaviors.same

      case cmd: GetStream =>
        given GetStream = cmd
        processGetStream()
        Behaviors.same

      case cmd: GetEmoteSets =>
        given GetEmoteSets = cmd
        processGetEmoteSets()
        Behaviors.same
    }
  }

  private def processRequest(request: Request[TwitchApiResponse])(using ctx: ActorContext[Command], log: Logger, self: ActorRef[Command], timeout: Timeout, httpClient: ActorRef[HttpClient.Request], settings: Settings, twitchUserSessionsRepository: ActorRef[TwitchUserSessionsRepository.Command]): Unit = {
    log.debug("processRequest")
    (request.tokenId, request.token) match {
      case (Some(_), None) =>
        retrieveToken(request)
      case _ =>
        sendRequest(request)
    }
  }

  private def sendRequest(request: Request[TwitchApiResponse])(using log: Logger, self: ActorRef[Command], timeout: Timeout, httpClient: ActorRef[HttpClient.Request], settings: Settings, twitchUserSessionsRepository: ActorRef[TwitchUserSessionsRepository.Command]): Unit = {
    log.debug("sendRequest")
    given Request[TwitchApiResponse] = request

    val authHeaders = request.token match {
      case Some(actualToken) =>
        List(
          RawHeader("Authorization", s"Bearer ${actualToken.access_token}"),
          RawHeader("Client-Id", settings.twitchAppClientId)
        )

      case None => Nil
    }

    val headers = request.headers ++ authHeaders

    httpClient ! HttpClient.Request(
      method = request.method,
      uri = request.uri,
      headers = headers,
      entity = request.entity,
      replyTo = responseCallback
    )
  }

  private def retrieveToken(request: Request[TwitchApiResponse])(using ctx: ActorContext[Command], log: Logger, self: ActorRef[Command], timeout: Timeout, httpClient: ActorRef[HttpClient.Request], settings: Settings, twitchUserSessionsRepository: ActorRef[TwitchUserSessionsRepository.Command]): Unit = {
    ctx.ask[TwitchUserSessionsRepository.Command, TwitchUserSessionsRepository.ReturnedTokenFromId](twitchUserSessionsRepository, ref => TwitchUserSessionsRepository.GetTokenFromId(ref, request.tokenId.get)) {
      case Success(TwitchUserSessionsRepository.ReturnedTokenFromId(id, token)) =>
        request.copy(token = token)

      case Failure(ex) =>
        request.replyTo ! StatusReply.error(ex)
        Nothing
    }
  }

  private def onGetTokenResponse(token: TwitchUserSessionsRepository.ReturnedTokenFromId)(using request: Request[TwitchApiResponse], self: ActorRef[Command]): Unit = {
    self ! request.copy(token = token.token)
  }

  private def responseCallback(response: Try[HttpClient.Response])(using request: Request[TwitchApiResponse], log: Logger, self: ActorRef[Command], httpClient: ActorRef[HttpClient.Request]): PartialFunction[Try[HttpClient.Response], Unit] = {
    case Success(r: HttpClient.Response) =>
      log.debug("responseCallback SUCCESS")
      if (r.response.status.isSuccess()) {
        request.onHttpResponse(r)
      } else if (request.token.nonEmpty && r.response.status == StatusCodes.Unauthorized && request.shouldRefresh) {
        RefreshToken(request)
      } else {
        request.replyTo ! StatusReply.error(TwitchApiResponse.NOK(r.response.status))
      }

    case Failure(ex) =>
      log.debug("responseCallbak FAILURE")
      request.replyTo ! StatusReply.error(ex)
  }

  private def processRefreshToken(cmd: RefreshToken[TwitchApiResponse])(using ctx: ActorContext[Command], settings: Settings, ec: ExecutionContextExecutor): Unit = {
    val formDataFuture = Marshal(FormData(Map(
      "client_id" -> settings.twitchAppClientId,
      "client_secret" -> settings.twitchAppClientSecret,
      "grant_type" -> "refresh_token",
      "refresh_token" -> cmd.originalRequest.token.get.refresh_token
    ))).to[RequestEntity]

    ctx.pipeToSelf(formDataFuture) {
      case Success(formData) =>
        RefreshTokenMarshalled(cmd.originalRequest, formData)

      case Failure(ex) =>
        cmd.originalRequest.replyTo ! StatusReply.error(ex)
        Nothing
    }
  }

  private def processRefreshTokenMarshalled()(using cmd: RefreshTokenMarshalled[TwitchApiResponse], ctx: ActorContext[Command], settings: Settings, ec: ExecutionContextExecutor, httpClient: ActorRef[HttpClient.Request], twitchUserSessionsRepository: ActorRef[TwitchUserSessionsRepository.Command]): Unit = {
    httpClient ! HttpClient.Request(
      method = HttpMethods.POST,
      uri = "https://id.twitch.tv/oauth2/token",
      entity = cmd.entity,
      replyTo = refreshTokenResponseCallback
    )
  }

  private def refreshTokenResponseCallback(using cmd: RefreshTokenMarshalled[TwitchApiResponse], ctx: ActorContext[Command], settings: Settings, ec: ExecutionContextExecutor, httpClient: ActorRef[HttpClient.Request], twitchUserSessionsRepository: ActorRef[TwitchUserSessionsRepository.Command]): PartialFunction[Try[HttpClient.Response], Unit] = {
    case Success(HttpClient.Response(response, body)) =>
      if (response.status.isSuccess()) {
        val token = decodeOrThrow[TwitchApiResponse.GetToken](body)
        twitchUserSessionsRepository ! TwitchUserSessionsRepository.RefreshToken(cmd.originalRequest.tokenId.get, token)
        cmd.originalRequest.copy(token = Some(token))
      } else {
        cmd.originalRequest.replyTo ! StatusReply.error("Cannot refresh old token")
      }

    case Failure(ex) =>
      cmd.originalRequest.replyTo ! StatusReply.error(ex)
  }

  private def processGetToken(cmd: GetToken)(using ctx: ActorContext[Command], settings: Settings, ec: ExecutionContextExecutor): Unit = {
    val formDataFuture = Marshal(FormData(Map(
      "client_id" -> settings.twitchAppClientId,
      "client_secret" -> settings.twitchAppClientSecret,
      "code" -> cmd.code,
      "grant_type" -> "authorization_code",
      "redirect_uri" -> settings.twitchAppRedirectUri
    ))).to[RequestEntity]

    ctx.pipeToSelf(formDataFuture) {
      case Success(formData) =>
        GetTokenMarshalled(cmd.replyTo, formData)

      case Failure(ex) =>
        cmd.replyTo ! StatusReply.error(ex)
        Nothing
    }
  }

  private def processGetTokenMarshalled()(using cmd: GetTokenMarshalled, ctx: ActorContext[Command], self: ActorRef[Command], settings: Settings, ec: ExecutionContextExecutor, httpClient: ActorRef[HttpClient.Request], twitchUserSessionsRepository: ActorRef[TwitchUserSessionsRepository.Command]): Unit = {
    self ! Request(
      cmd.replyTo,
      method = HttpMethods.POST,
      uri = "https://id.twitch.tv/oauth2/token",
      entity = cmd.entity,
      onHttpResponse = getTokenResponseCallback
    )
  }

  private def getTokenResponseCallback(r: HttpClient.Response)(using cmd: GetTokenMarshalled): Unit = {
    cmd.replyTo ! StatusReply.success(decodeOrThrow[TwitchApiResponse.GetToken](r.entityString))
  }

  private def processGetTokenUserFromTokenId()(using cmd: GetTokenUserFromTokenId, ctx: ActorContext[Command], self: ActorRef[Command], settings: Settings, ec: ExecutionContextExecutor, httpClient: ActorRef[HttpClient.Request], twitchUserSessionsRepository: ActorRef[TwitchUserSessionsRepository.Command], log: Logger): Unit = {
    given replyTo: ActorRef[StatusReply[TwitchApiResponse.GetTokenUser]] = cmd.replyTo

    self ! Request(
      replyTo,
      shouldRefresh = true,
      method = HttpMethods.GET,
      uri = "https://api.twitch.tv/helix/users",
      tokenId = Some(cmd.tokenId),
      onHttpResponse = getTokenUserResponseCallback
    )
  }

  private def processGetTokenUserFromAccessToken(cmd: GetTokenUserFromAccessToken)(using ctx: ActorContext[Command], self: ActorRef[Command], settings: Settings, ec: ExecutionContextExecutor, httpClient: ActorRef[HttpClient.Request], twitchUserSessionsRepository: ActorRef[TwitchUserSessionsRepository.Command], log: Logger): Unit = {
    log.debug("processGetTokenUserFromAccessToken")

    given replyTo: ActorRef[StatusReply[TwitchApiResponse.GetTokenUser]] = cmd.replyTo

    self ! Request(
      replyTo,
      method = HttpMethods.GET,
      uri = "https://api.twitch.tv/helix/users",
      token = Some(TwitchApiResponse.GetToken(cmd.accessToken, 0, "", None, "")),
      onHttpResponse = getTokenUserResponseCallback
    )
  }

  private def getTokenUserResponseCallback(r: HttpClient.Response)(using replyTo: ActorRef[StatusReply[TwitchApiResponse.GetTokenUser]], log: Logger): Unit = {
    log.debug("getTokenUserResponseCallback")
    replyTo ! StatusReply.success(decodeOrThrow[TwitchApiResponse.GetUsers](r.entityString).data.last)
  }

  private def processGetGameByName()(using cmd: GetGameByName, self: ActorRef[Command]): Unit = {
    self ! Request(
      cmd.replyTo,
      method = HttpMethods.GET,
      uri = Uri(s"https://api.twitch.tv/helix/games").withQuery(Query("name" -> cmd.gameName)),
      tokenId = Some(cmd.tokenId),
      onHttpResponse = getGameByNameResponseCallback
    )
  }

  private def getGameByNameResponseCallback(r: HttpClient.Response)(using cmd: GetGameByName): Unit = {
    cmd.replyTo ! StatusReply.success(TwitchApiResponse.GameResponse(decodeOrThrow[TwitchApiResponse.GetGames](r.entityString).data.lastOption))
  }

  private def processChangeChannelToUnknownGame(cmd: ChangeChannelToUnknownGame)(using ctx: ActorContext[Command], timeout: Timeout): Unit = {
    ctx.askWithStatus[TwitchApiClientOld.Command, TwitchApiResponse.GameResponse](ctx.self, ref => TwitchApiClientOld.GetGameByName(ref, cmd.tokenId, cmd.gameName)) {
      case Success(TwitchApiResponse.GameResponse(Some(game))) =>
        TwitchApiClientOld.ChangeChannelGame(cmd.replyTo, cmd.tokenId, cmd.roomId, game.id)

      case Success(TwitchApiResponse.GameResponse(None)) =>
        cmd.replyTo ! StatusReply.error(TwitchApiResponse.GameNotFoundException(cmd.gameName))
        TwitchApiClientOld.Nothing

      case Failure(ex) =>
        cmd.replyTo ! StatusReply.error(ex)
        TwitchApiClientOld.Nothing
    }
  }

  private def processChangeChannelGame()(using cmd: ChangeChannelGame, self: ActorRef[Command]): Unit = {
    self ! Request(
      cmd.replyTo,
      shouldRefresh = true,
      method = HttpMethods.GET,
      uri = Uri("https://api.twitch.tv/helix/channels").withQuery(Query(Map("broadcaster_id" -> cmd.roomId))),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        TwitchApiRequest.ModifyChannelInformationRequestData(Some(cmd.gameId), None, None).asJson.noSpaces
      ),
      tokenId = Some(cmd.tokenId),
      onHttpResponse = changeChannelGameResponseCallback
    )
  }

  private def changeChannelGameResponseCallback(r: HttpClient.Response)(using cmd: ChangeChannelGame): Unit = {
    cmd.replyTo ! StatusReply.success(TwitchApiResponse.ModifyChannelInformation)
  }

  private def processChangeChannelTitle()(using cmd: ChangeChannelTitle, self: ActorRef[Command]): Unit = {
    self ! Request(
      cmd.replyTo,
      shouldRefresh = true,
      method = HttpMethods.GET,
      uri = Uri("https://api.twitch.tv/helix/channels").withQuery(Query(Map("broadcaster_id" -> cmd.roomId))),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        TwitchApiRequest.ModifyChannelInformationRequestData(None, Some(cmd.title), None).asJson.noSpaces
      ),
      tokenId = Some(cmd.tokenId),
      onHttpResponse = changeChannelTitleResponseCallback
    )
  }

  private def changeChannelTitleResponseCallback(r: HttpClient.Response)(using cmd: ChangeChannelTitle): Unit = {
    cmd.replyTo ! StatusReply.success(TwitchApiResponse.ModifyChannelInformation)
  }

  private def processGetChannelInformation()(using cmd: GetChannelInformation, ctx: ActorContext[Command], self: ActorRef[Command], settings: Settings, ec: ExecutionContextExecutor, httpClient: ActorRef[HttpClient.Request], twitchUserSessionsRepository: ActorRef[TwitchUserSessionsRepository.Command]): Unit = {
    self ! Request(
      cmd.replyTo,
      shouldRefresh = true,
      method = HttpMethods.GET,
      uri = Uri("https://api.twitch.tv/helix/channels").withQuery(Query(Map("broadcaster_id" -> cmd.roomId))),
      tokenId = Some(cmd.tokenId),
      onHttpResponse = getChannelInformationResponseCallback
    )
  }

  private def getChannelInformationResponseCallback(r: HttpClient.Response)(using cmd: GetChannelInformation, ctx: ActorContext[Command], settings: Settings, ec: ExecutionContextExecutor, httpClient: ActorRef[HttpClient.Request], twitchUserSessionsRepository: ActorRef[TwitchUserSessionsRepository.Command]): Unit = {
    cmd.replyTo ! StatusReply.success(decodeOrThrow[TwitchApiResponse.GetChannelInformation](r.entityString).data.last)
  }

  private def processGetChatters()(using cmd: GetChatters, ctx: ActorContext[Command], self: ActorRef[Command], settings: Settings, ec: ExecutionContextExecutor, httpClient: ActorRef[HttpClient.Request], twitchUserSessionsRepository: ActorRef[TwitchUserSessionsRepository.Command]): Unit = {
    self ! Request(
      cmd.replyTo,
      shouldRefresh = true,
      method = HttpMethods.GET,
      uri = Uri("https://api.twitch.tv/helix/chat/chatters").withQuery(
        Query(
          "broadcaster_id" -> cmd.roomId,
          "moderator_id" -> cmd.moderatorId,
          "first" -> "1000"
        )
      ),
      tokenId = Some(cmd.tokenId),
      onHttpResponse = getChattersResponseCallback
    )
  }

  private def getChattersResponseCallback(r: HttpClient.Response)(using cmd: GetChatters, ctx: ActorContext[Command], settings: Settings, ec: ExecutionContextExecutor, httpClient: ActorRef[HttpClient.Request], twitchUserSessionsRepository: ActorRef[TwitchUserSessionsRepository.Command]): Unit = {
    cmd.replyTo ! StatusReply.success(decodeOrThrow[TwitchApiResponse.GetChatters](r.entityString))
  }

  private def processGetModerators()(using cmd: GetModerators, self: ActorRef[Command]): Unit = {
    self ! Request(
      cmd.replyTo,
      shouldRefresh = true,
      method = HttpMethods.GET,
      uri = Uri("https://api.twitch.tv/helix/moderation/moderators").withQuery(
        Query(
          "broadcaster_id" -> cmd.roomId,
          "first" -> "100"
        )
      ),
      tokenId = Some(cmd.tokenId),
      onHttpResponse = getModeratorsResponseCallback
    )
  }

  private def getModeratorsResponseCallback(r: HttpClient.Response)(using cmd: GetModerators): Unit = {
    cmd.replyTo ! StatusReply.success(decodeOrThrow[TwitchApiResponse.GetModerators](r.entityString))
  }

  private def processGetVIPs()(using cmd: GetVIPs, self: ActorRef[Command]): Unit = {
    self ! Request(
      cmd.replyTo,
      shouldRefresh = true,
      method = HttpMethods.GET,
      uri = Uri("https://api.twitch.tv/helix/channels/vips").withQuery(
        Query(
          "broadcaster_id" -> cmd.roomId,
          "first" -> "100"
        )
      ),
      tokenId = Some(cmd.tokenId),
      onHttpResponse = getVIPsResponseCallback
    )
  }

  private def getVIPsResponseCallback(r: HttpClient.Response)(using cmd: GetVIPs): Unit = {
    cmd.replyTo ! StatusReply.success(decodeOrThrow[TwitchApiResponse.GetVIPs](r.entityString))
  }

  private def processGetSubs()(using cmd: GetSubs, self: ActorRef[Command]): Unit = {
    self ! Request(
      cmd.replyTo,
      shouldRefresh = true,
      method = HttpMethods.GET,
      uri = Uri("https://api.twitch.tv/helix/subscriptions").withQuery(
        Query(
          "broadcaster_id" -> cmd.roomId,
          "first" -> "100"
        )
      ),
      tokenId = Some(cmd.tokenId),
      onHttpResponse = getSubsResponseCallback
    )
  }

  private def getSubsResponseCallback(r: HttpClient.Response)(using cmd: GetSubs): Unit = {
    cmd.replyTo ! StatusReply.success(decodeOrThrow[TwitchApiResponse.GetSubs](r.entityString))
  }

  private def processSendShoutout()(using cmd: SendShoutout, ctx: ActorContext[Command], self: ActorRef[Command], settings: Settings, ec: ExecutionContextExecutor, httpClient: ActorRef[HttpClient.Request], twitchUserSessionsRepository: ActorRef[TwitchUserSessionsRepository.Command]): Unit = {
    self ! Request(
      cmd.replyTo,
      shouldRefresh = true,
      method = HttpMethods.POST,
      uri = Uri("https://api.twitch.tv/helix/chat/shoutouts").withQuery(
        Query(
          "from_broadcaster_id" -> cmd.roomId,
          "to_broadcaster_id" -> cmd.toBroadcasterId,
          "moderator_id" -> cmd.moderatorId
        )
      ),
      tokenId = Some(cmd.tokenId),
      onHttpResponse = sendShoutoutResponseCallback
    )
  }

  private def sendShoutoutResponseCallback(r: HttpClient.Response)(using cmd: SendShoutout): Unit = {
    cmd.replyTo ! StatusReply.success(TwitchApiResponse.SendShoutout)
  }

  private def processGetChannelFollowers()(using cmd: GetChannelFollowers, ctx: ActorContext[Command], self: ActorRef[Command], settings: Settings, ec: ExecutionContextExecutor, httpClient: ActorRef[HttpClient.Request], twitchUserSessionsRepository: ActorRef[TwitchUserSessionsRepository.Command]): Unit = {
    self ! Request(
      cmd.replyTo,
      shouldRefresh = true,
      method = HttpMethods.GET,
      uri = Uri("https://api.twitch.tv/helix/channels/followers").withQuery(
        Query(
          "broadcaster_id" -> cmd.roomId,
          "first" -> "100",
        )
      ),
      tokenId = Some(cmd.tokenId),
      onHttpResponse = getChannelFollowersResponseCallback
    )
  }

  private def getChannelFollowersResponseCallback(r: HttpClient.Response)(using cmd: GetChannelFollowers): Unit = {
    cmd.replyTo ! StatusReply.success(decodeOrThrow[TwitchApiResponse.GetChannelFollowers](r.entityString))
  }

  private def processCheckUserFollowage()(using cmd: CheckUserFollowage, self: ActorRef[Command]): Unit = {
    self ! Request(
      cmd.replyTo,
      shouldRefresh = true,
      method = HttpMethods.GET,
      uri = Uri("https://api.twitch.tv/helix/channels/followers").withQuery(
        Query(
          "broadcaster_id" -> cmd.roomId,
          "first" -> "100",
          "user_id" -> cmd.userId
        )
      ),
      tokenId = Some(cmd.tokenId),
      onHttpResponse = checkUserFollowageResponseCallback
    )
  }

  private def checkUserFollowageResponseCallback(r: HttpClient.Response)(using cmd: CheckUserFollowage): Unit = {
    val response = decodeOrThrow[TwitchApiResponse.GetChannelFollowers](r.entityString)
    cmd.replyTo ! StatusReply.success(TwitchApiResponse.CheckUserFollowage(response.data.lastOption))
  }

  private def processCreateEventSubWebsocketSubscription()(using cmd: CreateEventSubWebsocketSubscription, self: ActorRef[Command]): Unit = {
    self ! Request(
      cmd.replyTo,
      shouldRefresh = true,
      method = HttpMethods.POST,
      uri = Uri("https://api.twitch.tv/helix/eventsub/subscriptions"),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        TwitchApiRequest.CreateEventSubSubscriptionPayload(cmd.subscriptionType, cmd.subscriptionVersion, cmd.condition, Transport(method = "websocket", session = Some(cmd.websocketSessionId))).asJson.noSpaces
      ),
      tokenId = Some(cmd.tokenId),
      onHttpResponse = createEventSubWebsocketSubscriptionResponseCallback
    )
  }

  private def getStreamResponseCallback(r: HttpClient.Response)(using cmd: GetStream): Unit = {
    cmd.replyTo ! StatusReply.success(decodeOrThrow[TwitchApiResponse.GetStream](r.entityString))
  }

  private def processGetStream()(using cmd: GetStream, self: ActorRef[Command]): Unit = {
    self ! Request(
      cmd.replyTo,
      shouldRefresh = true,
      method = HttpMethods.GET,
      uri = Uri("https://api.twitch.tv/helix/streams"),
      tokenId = Some(cmd.tokenId),
      onHttpResponse = getStreamResponseCallback
    )
  }

  private def getEmoteSetsResponseCallback(r: HttpClient.Response)(using cmd: GetEmoteSets): Unit = {
    cmd.replyTo ! StatusReply.success(decodeOrThrow[TwitchApiResponse.GetEmoteSets](r.entityString))
  }

  private def processGetEmoteSets()(using cmd: GetEmoteSets, self: ActorRef[Command]): Unit = {
    val queries = cmd.emoteSets.map("emote_set_id" -> _)
    self ! Request(
      cmd.replyTo,
      shouldRefresh = true,
      method = HttpMethods.GET,
      uri = Uri("https://api.twitch.tv/helix/chat/emotes/set").withQuery(
        Query(
          queries:_*
        )
      ),
      tokenId = Some(cmd.tokenId),
      onHttpResponse = getEmoteSetsResponseCallback
    )
  }

  private def createEventSubWebsocketSubscriptionResponseCallback(r: HttpClient.Response)(using cmd: CreateEventSubWebsocketSubscription): Unit = {
    cmd.replyTo ! StatusReply.success(decodeOrThrow[TwitchApiResponse.CreateEventSubWebsocketSubscriptionResponse](r.entityString))
  }
}
*/