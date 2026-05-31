package com.archimond7450.archiemate.actors.twitch.api

import com.archimond7450.archiemate.CirceConfiguration.twitchConfiguration
import com.archimond7450.archiemate.actors.{ArchieMateMediator, HttpClient}
import com.archimond7450.archiemate.actors.repositories.sessions.TwitchUserSessionsRepository
import com.archimond7450.archiemate.actors.services.caches.TwitchTokenUserCacheService
import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.helpers.JsonHelper.{
  decodeOrThrow,
  decodeToTry
}
import com.archimond7450.archiemate.twitch.api.{
  TwitchApiRequest,
  TwitchApiResponse
}
import com.archimond7450.archiemate.twitch.eventsub.{Condition, Transport}
import io.circe.Decoder
import io.circe.syntax.EncoderOps
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import org.apache.pekko.http.scaladsl.marshalling.Marshal
import org.apache.pekko.http.scaladsl.model.Uri.Query
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.model.{
  ContentTypes,
  FormData,
  HttpEntity,
  HttpHeader,
  HttpMethod,
  HttpMethods,
  RequestEntity,
  StatusCodes,
  Uri
}
import org.apache.pekko.pattern.StatusReply
import org.apache.pekko.util.Timeout

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success, Try}

object TwitchApiClient {
  val actorName = "TwitchApiClient"

  sealed trait Command
  sealed trait PublicCommand extends Command

  private case class Request(
      originalCommand: PublicCommand,
      refreshing: Boolean = false,
      tokenId: Option[String] = None,
      token: Option[TwitchApiResponse.GetToken] = None,
      shouldRefresh: Boolean = true,
      method: HttpMethod,
      uri: Uri,
      headers: Seq[HttpHeader] = Seq.empty,
      entity: RequestEntity = HttpEntity.Empty
  ) extends Command

  private final case class WrappedSuccessfulResponse(
      originalCommand: PublicCommand,
      response: HttpClient.Response
  ) extends Command

  private final case class WrappedFailedResponse(
      originalCommand: PublicCommand,
      cause: Throwable
  ) extends Command

  private final case class RefreshToken(
      originalRequest: Request
  ) extends Command

  private final case class TokenRefreshed(
      originalRequest: Request,
      response: HttpClient.Response
  ) extends Command

  final case class GetToken(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.GetToken]],
      code: String
  ) extends PublicCommand

  final case class GetTokenUserFromTokenId(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.GetTokenUser]],
      tokenId: String
  ) extends PublicCommand

  final case class GetTokenUserFromAccessToken(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.GetTokenUser]],
      accessToken: String
  ) extends PublicCommand

  final case class GetGameByName(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.GameResponse]],
      tokenId: String,
      gameName: String
  ) extends PublicCommand

  final case class ChangeChannelGame(
      replyTo: ActorRef[
        StatusReply[TwitchApiResponse.ModifyChannelInformation.type]
      ],
      tokenId: String,
      roomId: String,
      gameId: String
  ) extends PublicCommand

  final case class ChangeChannelToUnknownGame(
      replyTo: ActorRef[
        StatusReply[TwitchApiResponse.ModifyChannelInformation.type]
      ],
      tokenId: String,
      roomId: String,
      gameName: String
  ) extends PublicCommand

  final case class ChangeChannelTitle(
      replyTo: ActorRef[
        StatusReply[TwitchApiResponse.ModifyChannelInformation.type]
      ],
      tokenId: String,
      roomId: String,
      title: String
  ) extends PublicCommand

  final case class GetChannelInformation(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.ChannelInformation]],
      tokenId: String,
      roomId: String
  ) extends PublicCommand

  final case class GetChatters(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.GetChatters]],
      tokenId: String,
      roomId: String,
      moderatorId: String,
      cursor: Option[String] = None
  ) extends PublicCommand

  final case class GetModerators(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.GetModerators]],
      tokenId: String,
      roomId: String,
      cursor: Option[String] = None
  ) extends PublicCommand

  final case class GetVIPs(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.GetVIPs]],
      tokenId: String,
      roomId: String,
      cursor: Option[String] = None
  ) extends PublicCommand

  final case class GetSubs(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.GetSubs]],
      tokenId: String,
      roomId: String,
      cursor: Option[String] = None
  ) extends PublicCommand

  final case class SendShoutout(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.SendShoutout.type]],
      tokenId: String,
      roomId: String,
      toBroadcasterId: String,
      moderatorId: String
  ) extends PublicCommand

  final case class GetChannelFollowers(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.GetChannelFollowers]],
      tokenId: String,
      roomId: String,
      cursor: Option[String] = None
  ) extends PublicCommand

  final case class CheckUserFollowage(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.CheckUserFollowage]],
      tokenId: String,
      roomId: String,
      userId: String
  ) extends PublicCommand

  final case class CreateEventSubWebsocketSubscription(
      replyTo: ActorRef[StatusReply[
        TwitchApiResponse.CreateEventSubWebsocketSubscriptionResponse
      ]],
      tokenId: String,
      websocketSessionId: String,
      subscriptionType: String,
      subscriptionVersion: String,
      condition: Condition
  ) extends PublicCommand

  final case class GetStream(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.GetStream]],
      tokenId: String,
      roomId: String,
      cursor: Option[String] = None
  ) extends PublicCommand

  final case class GetEmoteSets(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.GetEmoteSets]],
      tokenId: String,
      emoteSets: List[String]
  ) extends PublicCommand

  final case class GetPolls(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.GetPolls]],
      tokenId: String,
      roomId: String,
      cursor: Option[String] = None
  ) extends PublicCommand

  final case class CreatePoll(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.CreateOrEndPoll]],
      tokenId: String,
      roomId: String,
      title: String,
      choices: Set[String],
      durationSeconds: Int,
      channelPointsVotingEnabled: Boolean = false,
      channelPointsPerVote: Int = 1
  ) extends PublicCommand

  final case class EndPoll(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.CreateOrEndPoll]],
      tokenId: String,
      roomId: String,
      pollId: String,
      withoutTrace: Boolean = false
  ) extends PublicCommand

  final case class GetPredictions(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.GetPredictions]],
      tokenId: String,
      roomId: String,
      cursor: Option[String] = None
  ) extends PublicCommand

  final case class CreatePrediction(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.CreateOrEndPrediction]],
      tokenId: String,
      roomId: String,
      title: String,
      outcomes: Set[String],
      predictionWindow: Int
  ) extends PublicCommand

  final case class ResolvePrediction(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.CreateOrEndPrediction]],
      tokenId: String,
      roomId: String,
      predictionId: String,
      winningOutcomeId: String
  ) extends Command

  final case class CancelPrediction(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.CreateOrEndPrediction]],
      tokenId: String,
      roomId: String,
      predictionId: String
  ) extends Command

  final case class LockPrediction(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.CreateOrEndPrediction]],
      tokenId: String,
      roomId: String,
      predictionId: String
  ) extends Command

  private final case class EndPrediction(
      replyTo: ActorRef[StatusReply[TwitchApiResponse.CreateOrEndPrediction]],
      tokenId: String,
      roomId: String,
      predictionId: String,
      status: String,
      winningOutcomeId: Option[String] = None
  ) extends PublicCommand

  def apply()(using
      mediator: ActorRef[ArchieMateMediator.Command]
  ): Behavior[Command] = Behaviors
    .supervise[Command] {
      Behaviors.setup { ctx =>
        given ActorContext[Command] = ctx

        new TwitchApiClient().operational()
      }
    }
    .onFailure[Throwable](SupervisorStrategy.resume)
}

class TwitchApiClient(using
    ctx: ActorContext[TwitchApiClient.Command],
    mediator: ActorRef[ArchieMateMediator.Command]
) {
  import TwitchApiClient.*

  given ExecutionContextExecutor = ctx.executionContext
  val settings = Settings(ctx.system)
  given Timeout = settings.askTimeout

  def operational(): Behavior[Command] = Behaviors.receiveAndLogMessage {
    case cmd: Request =>
      processRequest(cmd)
      Behaviors.same

    case cmd: WrappedSuccessfulResponse =>
      processWrappedSuccessfulResponse(cmd)
      Behaviors.same

    case cmd: WrappedFailedResponse =>
      processWrappedFailedResponse(cmd)
      Behaviors.same

    case cmd: RefreshToken =>
      processRefreshToken(cmd)
      Behaviors.same

    case cmd: TokenRefreshed =>
      processTokenRefreshed(cmd)
      Behaviors.same

    case cmd: GetToken =>
      processGetToken(cmd)
      Behaviors.same

    case cmd: GetTokenUserFromTokenId =>
      processGetTokenUserFromTokenId(cmd)
      Behaviors.same

    case cmd: GetTokenUserFromAccessToken =>
      processGetTokenUserFromAccessToken(cmd)
      Behaviors.same

    case cmd: GetGameByName =>
      processGetGameByName(cmd)
      Behaviors.same

    case cmd: ChangeChannelToUnknownGame =>
      processChangeChannelToUnknownGame(cmd)
      Behaviors.same

    case cmd: ChangeChannelGame =>
      processChangeChannelGame(cmd)
      Behaviors.same

    case cmd: ChangeChannelTitle =>
      processChangeChannelTitle(cmd)
      Behaviors.same

    case cmd: GetChannelInformation =>
      processGetChannelInformation(cmd)
      Behaviors.same

    case cmd: GetChatters =>
      processGetChatters(cmd)
      Behaviors.same

    case cmd: GetModerators =>
      processGetModerators(cmd)
      Behaviors.same

    case cmd: GetVIPs =>
      processGetVIPs(cmd)
      Behaviors.same

    case cmd: GetSubs =>
      processGetSubs(cmd)
      Behaviors.same

    case cmd: SendShoutout =>
      processSendShoutout(cmd)
      Behaviors.same

    case cmd: GetChannelFollowers =>
      processGetChannelFollowers(cmd)
      Behaviors.same

    case cmd: CheckUserFollowage =>
      processCheckUserFollowage(cmd)
      Behaviors.same

    case cmd: CreateEventSubWebsocketSubscription =>
      processCreateEventSubWebsocketSubscription(cmd)
      Behaviors.same

    case cmd: GetStream =>
      processGetStream(cmd)
      Behaviors.same

    case cmd: GetEmoteSets =>
      processGetEmoteSets(cmd)
      Behaviors.same

    case cmd: GetPolls =>
      processGetPolls(cmd)
      Behaviors.same

    case cmd: CreatePoll =>
      processCreatePoll(cmd)
      Behaviors.same

    case cmd: EndPoll =>
      processEndPoll(cmd)
      Behaviors.same

    case cmd: GetPredictions =>
      processGetPredictions(cmd)
      Behaviors.same

    case cmd: CreatePrediction =>
      processCreatePrediction(cmd)
      Behaviors.same

    case cmd: ResolvePrediction =>
      processResolvePrediction(cmd)
      Behaviors.same

    case cmd: CancelPrediction =>
      processCancelPrediction(cmd)
      Behaviors.same

    case cmd: LockPrediction =>
      processLockPrediction(cmd)
      Behaviors.same

    case cmd: EndPrediction =>
      processEndPrediction(cmd)
      Behaviors.same
  }

  private def processRequest(request: Request): Unit = {
    ctx.log.debug("processRequest(request: {})", request)
    (request.tokenId, request.token) match {
      case (Some(tokenId), None) =>
        retrieveToken(request, tokenId)

      case _ =>
        sendRequest(request)
    }
  }

  private def retrieveToken(request: Request, tokenId: String): Unit = {
    ctx.log.debug(
      "retrieveToken(request: {}, tokenId: \"{}\")",
      request,
      tokenId
    )
    ctx.ask[
      ArchieMateMediator.Command,
      TwitchUserSessionsRepository.ReturnedTokenFromId
    ](
      mediator,
      ref =>
        ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand(
          TwitchUserSessionsRepository.GetTokenFromId(ref, tokenId)
        )
    ) {
      case Success(TwitchUserSessionsRepository.ReturnedTokenFromId(token)) =>
        request.copy(token = token)

      case Failure(ex) =>
        WrappedFailedResponse(request.originalCommand, ex)
    }
  }

  private def sendRequest(request: Request): Unit = {
    ctx.log.debug("sendRequest(request: {})", request)

    val authHeaders = request.token match {
      case Some(actualToken) =>
        List(
          RawHeader("Authorization", s"Bearer ${actualToken.accessToken}"),
          RawHeader("Client-Id", settings.twitchAppClientId)
        )

      case None =>
        Nil
    }

    val headers = request.headers ++ authHeaders

    ctx.askWithStatus[ArchieMateMediator.Command, HttpClient.Response](
      mediator,
      ref =>
        ArchieMateMediator.SendHttpClientRequest(
          HttpClient.Request(
            ref,
            method = request.method,
            uri = request.uri,
            headers = headers,
            entity = request.entity
          )
        )
    ) {
      case Success(response: HttpClient.Response)
          if response.response.status.isSuccess && request.refreshing =>
        TokenRefreshed(request, response)

      case Success(response: HttpClient.Response)
          if response.response.status == StatusCodes.Unauthorized && request.shouldRefresh =>
        RefreshToken(request)

      case Success(response: HttpClient.Response)
          if response.response.status.isSuccess =>
        WrappedSuccessfulResponse(request.originalCommand, response)

      case Success(response: HttpClient.Response) =>
        WrappedFailedResponse(
          request.originalCommand,
          RuntimeException("Received error response from Twitch")
        )

      case Failure(ex) =>
        WrappedFailedResponse(request.originalCommand, ex)
    }
  }

  private def processWrappedSuccessfulResponse(
      resp: WrappedSuccessfulResponse
  ): Unit = {
    ctx.log.debug("processWrappedSuccessfulResponse(resp: {})", resp)
    val json = resp.response.entityString
    resp.originalCommand match {
      case cmd: GetToken =>
        cmd.replyTo ! tryResponseToStatusReply(
          decodeResponse[TwitchApiResponse.GetToken](json)
        )

      case cmd: GetTokenUserFromTokenId =>
        val tryUsers = decodeResponse[TwitchApiResponse.GetUsers](json)
        val tryResponse = tryUsers.map(users => users.data.last)
        cmd.replyTo ! tryResponseToStatusReply(tryResponse)
        tryResponse.foreach { tokenUser =>
          mediator ! ArchieMateMediator.SendTwitchTokenUserCacheServiceCommand(
            TwitchTokenUserCacheService.CacheTokenUser(tokenUser)
          )
        }

      case cmd: GetTokenUserFromAccessToken =>
        val tryUsers = decodeResponse[TwitchApiResponse.GetUsers](json)
        val tryResponse = tryUsers.map(users => users.data.last)
        cmd.replyTo ! tryResponseToStatusReply(tryResponse)

      case cmd: GetGameByName =>
        val tryGames = decodeResponse[TwitchApiResponse.GetGames](json)
        val tryResponse = tryGames.map(games =>
          TwitchApiResponse.GameResponse(games.data.lastOption)
        )

        cmd.replyTo ! tryResponseToStatusReply(tryResponse)

      case cmd: ChangeChannelGame =>
        cmd.replyTo ! StatusReply.success(
          TwitchApiResponse.ModifyChannelInformation
        )

      case cmd: ChangeChannelToUnknownGame =>
        cmd.replyTo ! StatusReply.success(
          TwitchApiResponse.ModifyChannelInformation
        )

      case cmd: ChangeChannelTitle =>
        cmd.replyTo ! StatusReply.success(
          TwitchApiResponse.ModifyChannelInformation
        )

      case cmd: GetChannelInformation =>
        val tryInformation =
          decodeResponse[TwitchApiResponse.GetChannelInformation](json)
        val tryResponse = tryInformation.map(_.data.last)
        cmd.replyTo ! tryResponseToStatusReply(tryResponse)

      case cmd: GetChatters =>
        cmd.replyTo ! tryResponseToStatusReply(
          decodeResponse[TwitchApiResponse.GetChatters](json)
        )

      case cmd: GetModerators =>
        cmd.replyTo ! tryResponseToStatusReply(
          decodeResponse[TwitchApiResponse.GetModerators](json)
        )

      case cmd: GetVIPs =>
        cmd.replyTo ! tryResponseToStatusReply(
          decodeResponse[TwitchApiResponse.GetVIPs](json)
        )

      case cmd: GetSubs =>
        cmd.replyTo ! tryResponseToStatusReply(
          decodeResponse[TwitchApiResponse.GetSubs](json)
        )

      case cmd: SendShoutout =>
        cmd.replyTo ! StatusReply.success(TwitchApiResponse.SendShoutout)

      case cmd: GetChannelFollowers =>
        cmd.replyTo ! tryResponseToStatusReply(
          decodeResponse[TwitchApiResponse.GetChannelFollowers](json)
        )

      case cmd: CheckUserFollowage =>
        val tryFollowers =
          decodeResponse[TwitchApiResponse.GetChannelFollowers](json)
        val tryResponse = tryFollowers.map(followers =>
          TwitchApiResponse.CheckUserFollowage(followers.data.lastOption)
        )

        cmd.replyTo ! tryResponseToStatusReply(tryResponse)

      case cmd: CreateEventSubWebsocketSubscription =>
        cmd.replyTo ! tryResponseToStatusReply(
          decodeResponse[
            TwitchApiResponse.CreateEventSubWebsocketSubscriptionResponse
          ](json)
        )

      case cmd: GetStream =>
        cmd.replyTo ! tryResponseToStatusReply(
          decodeResponse[TwitchApiResponse.GetStream](json)
        )

      case cmd: GetEmoteSets =>
        cmd.replyTo ! tryResponseToStatusReply(
          decodeResponse[TwitchApiResponse.GetEmoteSets](json)
        )

      case cmd: GetPolls =>
        cmd.replyTo ! tryResponseToStatusReply(
          decodeResponse[TwitchApiResponse.GetPolls](json)
        )

      case cmd: CreatePoll =>
        cmd.replyTo ! tryResponseToStatusReply(
          decodeResponse[TwitchApiResponse.CreateOrEndPoll](json)
        )

      case cmd: EndPoll =>
        cmd.replyTo ! tryResponseToStatusReply(
          decodeResponse[TwitchApiResponse.CreateOrEndPoll](json)
        )

      case cmd: GetPredictions =>
        cmd.replyTo ! tryResponseToStatusReply(
          decodeResponse[TwitchApiResponse.GetPredictions](json)
        )

      case cmd: CreatePrediction =>
        cmd.replyTo ! tryResponseToStatusReply(
          decodeResponse[TwitchApiResponse.CreateOrEndPrediction](json)
        )

      case cmd: EndPrediction =>
        cmd.replyTo ! tryResponseToStatusReply(
          decodeResponse[TwitchApiResponse.CreateOrEndPrediction](json)
        )
    }
  }

  private def processWrappedFailedResponse(
      resp: WrappedFailedResponse
  ): Unit = {
    ctx.log.debug("processWrappedFailedResponse(resp: {})", resp)
    resp.originalCommand match {
      case cmd: GetToken =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: GetTokenUserFromTokenId =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: GetTokenUserFromAccessToken =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: GetGameByName =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: ChangeChannelGame =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: ChangeChannelToUnknownGame =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: ChangeChannelTitle =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: GetChannelInformation =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: GetChatters =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: GetModerators =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: GetVIPs =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: GetSubs =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: SendShoutout =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: GetChannelFollowers =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: CheckUserFollowage =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: CreateEventSubWebsocketSubscription =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: GetStream =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: GetEmoteSets =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: GetPolls =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: CreatePoll =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: EndPoll =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: GetPredictions =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: CreatePrediction =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: EndPrediction =>
        cmd.replyTo ! StatusReply.error(resp.cause)
    }
  }

  private def processRefreshToken(cmd: RefreshToken): Unit = {
    ctx.log.debug("processRefreshToken(cmd: {})", cmd)
    val formDataFuture = Marshal(
      FormData(
        Map(
          "client_id" -> settings.twitchAppClientId,
          "client_secret" -> settings.twitchAppClientSecret,
          "grant_type" -> "refresh_token",
          "refresh_token" -> cmd.originalRequest.token.get.refreshToken
        )
      )
    ).to[RequestEntity]

    ctx.pipeToSelf(formDataFuture) {
      case Success(formData) =>
        cmd.originalRequest.copy(
          method = HttpMethods.POST,
          uri = "https://id.twitch.tv/oauth2/token",
          entity = formData,
          refreshing = true,
          shouldRefresh = false
        )

      case Failure(ex) =>
        WrappedFailedResponse(cmd.originalRequest.originalCommand, ex)
    }
  }

  private def processTokenRefreshed(cmd: TokenRefreshed): Unit = {
    ctx.log.debug("processTokenRefreshed(cmd: {})", cmd)
    decodeToTry[TwitchApiResponse.GetToken](cmd.response.entityString) match {
      case Success(token) =>
        mediator ! ArchieMateMediator.SendTwitchUserSessionsRepositoryCommand(
          TwitchUserSessionsRepository.RefreshToken(
            cmd.originalRequest.tokenId.get,
            token
          )
        )
        ctx.self ! cmd.originalRequest.originalCommand

      case Failure(ex) =>
        ctx.self ! WrappedFailedResponse(
          cmd.originalRequest.originalCommand,
          ex
        )
    }
  }

  private def processGetToken(cmd: GetToken): Unit = {
    ctx.log.debug("processGetToken(cmd: {})", cmd)
    val formDataFuture = Marshal(
      FormData(
        Map(
          "client_id" -> settings.twitchAppClientId,
          "client_secret" -> settings.twitchAppClientSecret,
          "code" -> cmd.code,
          "grant_type" -> "authorization_code",
          "redirect_uri" -> settings.twitchAppRedirectUri
        )
      )
    ).to[RequestEntity]

    ctx.pipeToSelf(formDataFuture) {
      case Success(formData) =>
        Request(
          originalCommand = cmd,
          method = HttpMethods.POST,
          uri = "https://id.twitch.tv/oauth2/token",
          entity = formData,
          shouldRefresh = false
        )

      case Failure(ex) =>
        WrappedFailedResponse(cmd, ex)
    }
  }

  private def processGetTokenUserFromTokenId(
      cmd: GetTokenUserFromTokenId
  ): Unit = {
    ctx.log.debug("processGetTokenUserFromTokenId(cmd: {})", cmd)
    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.GET,
      uri = "https://api.twitch.tv/helix/users",
      tokenId = Some(cmd.tokenId)
    )
  }

  private def processGetTokenUserFromAccessToken(
      cmd: GetTokenUserFromAccessToken
  ): Unit = {
    ctx.log.debug("processGetTokenUserFromAccessToken(cmd: {})", cmd)
    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.GET,
      uri = "https://api.twitch.tv/helix/users",
      token =
        Some(TwitchApiResponse.GetToken(cmd.accessToken, 0, "", None, "")),
      shouldRefresh = false
    )
  }

  private def processGetGameByName(cmd: GetGameByName): Unit = {
    ctx.log.debug("processGetGameByName(cmd: {})", cmd)
    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.GET,
      uri = Uri("https://api.twitch.tv/helix/games").withQuery(
        Query("name" -> cmd.gameName)
      ),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def processChangeChannelToUnknownGame(
      cmd: ChangeChannelToUnknownGame
  ): Unit = {
    ctx.log.debug("processChangeChannelToUnknownGame(cmd: {})", cmd)
    ctx.askWithStatus[Command, TwitchApiResponse.GameResponse](
      ctx.self,
      ref => GetGameByName(ref, cmd.tokenId, cmd.gameName)
    ) {
      case Success(TwitchApiResponse.GameResponse(Some(game))) =>
        ChangeChannelGame(cmd.replyTo, cmd.tokenId, cmd.roomId, game.id)

      case Success(TwitchApiResponse.GameResponse(None)) =>
        WrappedFailedResponse(
          cmd,
          TwitchApiResponse.GameNotFoundException(cmd.gameName)
        )

      case Failure(ex) =>
        WrappedFailedResponse(cmd, ex)
    }
  }

  private def processChangeChannelGame(cmd: ChangeChannelGame): Unit = {
    ctx.log.debug("processChangeChannelGame(cmd: {})", cmd)
    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.PATCH,
      uri = Uri("https://api.twitch.tv/helix/channels").withQuery(
        Query("broadcaster_id" -> cmd.roomId)
      ),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        TwitchApiRequest
          .ModifyChannelInformationRequestData(gameId = Some(cmd.gameId))
          .asJson
          .noSpaces
      ),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def processChangeChannelTitle(cmd: ChangeChannelTitle): Unit = {
    ctx.log.debug("processChangeChannelTitle(cmd: {})", cmd)
    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.PATCH,
      uri = Uri("https://api.twitch.tv/helix/channels").withQuery(
        Query("broadcaster_id" -> cmd.roomId)
      ),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        TwitchApiRequest
          .ModifyChannelInformationRequestData(title = Some(cmd.title))
          .asJson
          .noSpaces
      ),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def processGetChannelInformation(cmd: GetChannelInformation): Unit = {
    ctx.log.debug("processGetChannelInformation(cmd: {})", cmd)
    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.GET,
      uri = Uri("https://api.twitch.tv/helix/channels").withQuery(
        Query("broadcaster_id" -> cmd.roomId)
      ),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def addCursor(uri: Uri, query: Query, cursor: Option[String]): Uri =
    cursor match {
      case None => uri.withQuery(query)
      case Some(cursor) =>
        uri.withQuery(Query(query.toMap + ("after" -> cursor)))
    }

  private def processGetChatters(cmd: GetChatters): Unit = {
    ctx.log.debug("processGetChatters(cmd: {})", cmd)
    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.GET,
      uri = addCursor(
        Uri("https://api.twitch.tv/helix/chat/chatters"),
        Query(
          "broadcaster_id" -> cmd.roomId,
          "moderator_id" -> cmd.moderatorId,
          "first" -> "1000"
        ),
        cmd.cursor
      ),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def processGetModerators(cmd: GetModerators): Unit = {
    ctx.log.debug("processGetModerators(cmd: {})", cmd)
    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.GET,
      uri = addCursor(
        Uri("https://api.twitch.tv/helix/moderation/moderators"),
        Query(
          "broadcaster_id" -> cmd.roomId,
          "first" -> "100"
        ),
        cmd.cursor
      ),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def processGetVIPs(cmd: GetVIPs): Unit = {
    ctx.log.debug("processGetVIPs(cmd: {})", cmd)
    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.GET,
      uri = addCursor(
        Uri("https://api.twitch.tv/helix/channels/vips"),
        Query(
          "broadcaster_id" -> cmd.roomId,
          "first" -> "100"
        ),
        cmd.cursor
      ),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def processGetSubs(cmd: GetSubs): Unit = {
    ctx.log.debug("processGetSubs(cmd: {})", cmd)
    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.GET,
      uri = addCursor(
        Uri("https://api.twitch.tv/helix/subscriptions"),
        Query(
          "broadcaster_id" -> cmd.roomId,
          "first" -> "100"
        ),
        cmd.cursor
      ),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def processSendShoutout(cmd: SendShoutout): Unit = {
    ctx.log.debug("processSendShoutout(cmd: {})", cmd)
    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.POST,
      uri = Uri("https://api.twitch.tv/helix/chat/shoutouts").withQuery(
        Query(
          "from_broadcaster_id" -> cmd.roomId,
          "to_broadcaster_id" -> cmd.toBroadcasterId,
          "moderator_id" -> cmd.moderatorId
        )
      ),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def processGetChannelFollowers(cmd: GetChannelFollowers): Unit = {
    ctx.log.debug("processGetChannelFollowers(cmd: {})", cmd)
    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.GET,
      uri = addCursor(
        Uri("https://api.twitch.tv/helix/channels/followers"),
        Query(
          "broadcaster_id" -> cmd.roomId,
          "first" -> "100"
        ),
        cmd.cursor
      ),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def processCheckUserFollowage(cmd: CheckUserFollowage): Unit = {
    ctx.log.debug("processCheckUserFollowage(cmd: {})", cmd)
    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.GET,
      uri = Uri("https://api.twitch.tv/helix/channels/followers").withQuery(
        Query(
          "broadcaster_id" -> cmd.roomId,
          "first" -> "100",
          "user_id" -> cmd.userId
        )
      ),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def processCreateEventSubWebsocketSubscription(
      cmd: CreateEventSubWebsocketSubscription
  ): Unit = {
    ctx.log.debug("processCreateEventSubWebsocketSubcription(cmd: {})", cmd)
    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.POST,
      uri = Uri("https://api.twitch.tv/helix/eventsub/subscriptions"),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        TwitchApiRequest
          .CreateEventSubSubscriptionPayload(
            cmd.subscriptionType,
            cmd.subscriptionVersion,
            cmd.condition,
            Transport(
              method = "websocket",
              session = Some(cmd.websocketSessionId)
            )
          )
          .asJson
          .noSpaces
      ),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def processGetStream(cmd: GetStream): Unit = {
    ctx.log.debug("processGetStream(cmd: {})", cmd)
    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.GET,
      uri = addCursor(
        Uri("https://api.twitch.tv/helix/streams"),
        Query(
          "user_id" -> cmd.roomId
        ),
        cmd.cursor
      ),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def processGetEmoteSets(cmd: GetEmoteSets): Unit = {
    ctx.log.debug("processGetEmoteSets(cmd: {})", cmd)

    val queries = cmd.emoteSets.map("emote_set_id" -> _)

    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.GET,
      uri = Uri("https://api.twitch.tv/helix/chat/emotes/set").withQuery(
        Query(
          queries: _*
        )
      ),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def processGetPolls(cmd: GetPolls): Unit = {
    ctx.log.debug("processGetPolls(cmd: {})", cmd)

    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.GET,
      uri = addCursor(
        Uri("https://api.twitch.tv/helix/polls"),
        Query(
          "broadcaster_id" -> cmd.roomId,
          "first" -> "20"
        ),
        cmd.cursor
      ),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def processCreatePoll(cmd: CreatePoll): Unit = {
    ctx.log.debug("processCreatePoll(cmd: {})", cmd)

    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.POST,
      uri = Uri("https://api.twitch.tv/helix/polls"),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        TwitchApiRequest
          .CreatePollRequestData(
            broadcasterId = cmd.roomId,
            title = cmd.title,
            choices = cmd.choices.map(TwitchApiRequest.PollChoice.apply).toList,
            channelPointsVotingEnabled =
              if (cmd.channelPointsVotingEnabled) Some(true) else None,
            channelPointsPerVote =
              if (cmd.channelPointsVotingEnabled) Some(cmd.channelPointsPerVote)
              else None,
            duration = cmd.durationSeconds
          )
          .asJson
          .noSpaces
      ),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def processEndPoll(cmd: EndPoll): Unit = {
    ctx.log.debug("processEndPoll(cmd: {})", cmd)

    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.PATCH,
      uri = Uri("https://api.twitch.tv/helix/polls"),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        TwitchApiRequest
          .EndPollRequestData(
            broadcasterId = cmd.roomId,
            id = cmd.pollId,
            status = if (cmd.withoutTrace) "ARCHIVED" else "TERMINATED"
          )
          .asJson
          .noSpaces
      ),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def processGetPredictions(cmd: GetPredictions): Unit = {
    ctx.log.debug("processGetPredictions(cmd: {})", cmd)

    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.GET,
      uri = addCursor(
        uri = Uri("https://api.twitch.tv/helix/predictions"),
        query = Query(Map("broadcaster_id" -> cmd.roomId, "first" -> "25")),
        cursor = cmd.cursor
      ),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def processCreatePrediction(cmd: CreatePrediction): Unit = {
    ctx.log.debug("processCreatePrediction(cmd: {})", cmd)

    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.POST,
      uri = Uri("https://api.twitch.tv/helix/predictions"),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        TwitchApiRequest
          .CreatePredictionRequestData(
            broadcasterId = cmd.roomId,
            title = cmd.title,
            outcomes =
              cmd.outcomes.map(TwitchApiRequest.PredictionOutcome.apply).toList,
            predictionWindow = cmd.predictionWindow
          )
          .asJson
          .noSpaces
      ),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def processResolvePrediction(cmd: ResolvePrediction): Unit = {
    ctx.log.debug("processResolvePrediction(cmd: {})", cmd)

    ctx.self ! EndPrediction(
      replyTo = cmd.replyTo,
      tokenId = cmd.tokenId,
      roomId = cmd.roomId,
      predictionId = cmd.predictionId,
      status = "RESOLVED",
      winningOutcomeId = Some(cmd.winningOutcomeId)
    )
  }

  private def processCancelPrediction(cmd: CancelPrediction): Unit = {
    ctx.log.debug("processCancelPrediction(cmd: {})", cmd)

    ctx.self ! EndPrediction(
      replyTo = cmd.replyTo,
      tokenId = cmd.tokenId,
      roomId = cmd.roomId,
      predictionId = cmd.predictionId,
      status = "CANCELED"
    )
  }

  private def processLockPrediction(cmd: LockPrediction): Unit = {
    ctx.log.debug("processLockPrediction(cmd: {})", cmd)

    ctx.self ! EndPrediction(
      replyTo = cmd.replyTo,
      tokenId = cmd.tokenId,
      roomId = cmd.roomId,
      predictionId = cmd.predictionId,
      status = "LOCKED"
    )
  }

  private def processEndPrediction(cmd: EndPrediction): Unit = {
    ctx.log.debug("processEndPrediction(cmd: {})", cmd)

    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.PATCH,
      uri = Uri("https://api.twitch.tv/helix/predictions"),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        TwitchApiRequest
          .EndPredictionRequestData(
            broadcasterId = cmd.roomId,
            id = cmd.predictionId,
            status = cmd.status,
            winningOutcomeId = cmd.winningOutcomeId
          )
          .asJson
          .noSpaces
      ),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def decodeResponse[T <: TwitchApiResponse](
      str: String
  )(using Decoder[T]): Try[T] = {
    decodeToTry[T](str)
  }

  private def tryResponseToStatusReply[T <: TwitchApiResponse]
      : PartialFunction[Try[T], StatusReply[T]] = {
    case Success(response) => StatusReply.success(response)
    case Failure(ex)       => StatusReply.error(ex)
  }
}
