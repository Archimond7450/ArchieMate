package com.archimond7450.archiemate.actors.youtube.api

import com.archimond7450.archiemate.actors.{ArchieMateMediator, HttpClient}
import com.archimond7450.archiemate.actors.repositories.sessions.YouTubeChannelSessionsRepository
import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.helpers.JsonHelper.decodeToTry
import com.archimond7450.archiemate.youtube.api.YouTubeApiResponse
import io.circe.Decoder
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.http.scaladsl.marshalling.Marshal
import org.apache.pekko.http.scaladsl.model.Uri.Query
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.model.*
import org.apache.pekko.pattern.StatusReply
import org.apache.pekko.util.Timeout

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success, Try}

object YouTubeApiClient {
  val actorName = "YouTubeApiClient"

  sealed trait Command
  sealed trait PublicCommand extends Command

  private case class Request(
      originalCommand: PublicCommand,
      refreshing: Boolean = false,
      tokenId: Option[String] = None,
      token: Option[YouTubeApiResponse.GetToken] = None,
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
      replyTo: ActorRef[StatusReply[YouTubeApiResponse.GetToken]],
      code: String
  ) extends PublicCommand

  final case class GetChannelFromTokenId(
      replyTo: ActorRef[StatusReply[YouTubeApiResponse.Response[YouTubeApiResponse.Channel]]],
      tokenId: String
  ) extends PublicCommand

  final case class GetChannelFromAccessToken(
      replyTo: ActorRef[StatusReply[YouTubeApiResponse.Response[YouTubeApiResponse.Channel]]],
      accessToken: String
  ) extends PublicCommand

  final case class GetLiveBroadcast(
      replyTo: ActorRef[StatusReply[YouTubeApiResponse.Response[YouTubeApiResponse.LiveBroadcast]]],
      tokenId: String
  ) extends PublicCommand

  def apply()(using mediator: ActorRef[ArchieMateMediator.Command], settings: Settings): Behavior[Command] = Behaviors.setup { ctx =>
    given ActorContext[Command] = ctx
    new YouTubeApiClient().operational()
  }
}

class YouTubeApiClient()(using ctx: ActorContext[YouTubeApiClient.Command], mediator: ActorRef[ArchieMateMediator.Command], settings: Settings) {
  import YouTubeApiClient.*

  given ExecutionContextExecutor = ctx.executionContext
  given Timeout = settings.askTimeout

  private val oauthBaseUrl = "https://oauth2.googleapis.com"
  private val v3baseUrl = "https://www.googleapis.com/youtube/v3"

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

    case cmd: GetChannelFromTokenId =>
      processGetChannelFromTokenId(cmd)
      Behaviors.same

    case cmd: GetChannelFromAccessToken =>
      processGetChannelFromAccessToken(cmd)
      Behaviors.same

    case cmd: GetLiveBroadcast =>
      processGetLiveBroadcast(cmd)
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
    ctx.log.debug("retrieveToken(request: {}, tokenId: \"{}\")", request, tokenId)
    ctx.ask[ArchieMateMediator.Command, YouTubeChannelSessionsRepository.ReturnedTokenFromId](mediator, ref => ArchieMateMediator.SendYouTubeChannelSessionsRepositoryCommand(YouTubeChannelSessionsRepository.GetTokenFromId(ref, tokenId))) {
      case Success(YouTubeChannelSessionsRepository.ReturnedTokenFromId(id, token)) =>
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
          RawHeader("Authorization", s"Bearer ${actualToken.access_token}")
        )

      case None =>
        Nil
    }

    val headers = request.headers ++ authHeaders

    ctx.askWithStatus[ArchieMateMediator.Command, HttpClient.Response](
      mediator,
      ref =>
        ArchieMateMediator.SendHttpClientRequest(HttpClient.Request(
          ref,
          method = request.method,
          uri = request.uri,
          headers = request.headers,
          entity = request.entity
        ))
    ) {
      case Success(response: HttpClient.Response) if response.response.status.isSuccess && request.refreshing =>
        TokenRefreshed(request, response)

      case Success(response: HttpClient.Response) if response.response.status == StatusCodes.Unauthorized && request.shouldRefresh =>
        RefreshToken(request)

      case Success(response: HttpClient.Response) if response.response.status.isSuccess =>
        WrappedSuccessfulResponse(request.originalCommand, response)

      case Success(response: HttpClient.Response) =>
        WrappedFailedResponse(request.originalCommand, RuntimeException("Received error response from YouTube"))

      case Failure(ex) =>
        WrappedFailedResponse(request.originalCommand, ex)
    }
  }

  private def processWrappedSuccessfulResponse(resp: WrappedSuccessfulResponse): Unit = {
    ctx.log.debug("processWrappedSuccessfulResponse(resp: {})", resp)
    val json = resp.response.entityString
    resp.originalCommand match {
      case cmd: GetToken =>
        cmd.replyTo ! tryResponseToStatusReply(decodeResponse[YouTubeApiResponse.GetToken](json))

      case cmd: GetChannelFromTokenId =>
        cmd.replyTo ! tryResponseToStatusReply(decodeResponse[YouTubeApiResponse.Response[YouTubeApiResponse.Channel]](json))

      case cmd: GetChannelFromAccessToken =>
        cmd.replyTo ! tryResponseToStatusReply(decodeResponse[YouTubeApiResponse.Response[YouTubeApiResponse.Channel]](json))

      case cmd: GetLiveBroadcast =>
        cmd.replyTo ! tryResponseToStatusReply(decodeResponse[YouTubeApiResponse.Response[YouTubeApiResponse.LiveBroadcast]](json))
    }
  }

  private def processWrappedFailedResponse(resp: WrappedFailedResponse): Unit = {
    ctx.log.debug("processWrappedFailedResponse(resp: {})", resp)
    resp.originalCommand match {
      case cmd: GetToken =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: GetChannelFromTokenId =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: GetChannelFromAccessToken =>
        cmd.replyTo ! StatusReply.error(resp.cause)

      case cmd: GetLiveBroadcast =>
        cmd.replyTo ! StatusReply.error(resp.cause)
    }
  }

  private def processRefreshToken(cmd: RefreshToken): Unit = {
    ctx.log.debug("processRefreshToken(cmd: {})", cmd)
    val formDataFuture = Marshal(FormData(Map(
      "client_id" -> settings.youTubeAppClientId,
      "client_secret" -> settings.youTubeAppClientSecret,
      "grant_type" -> "refresh_token",
      "refresh_token" -> cmd.originalRequest.token.get.refresh_token
    ))).to[RequestEntity]

    ctx.pipeToSelf(formDataFuture) {
      case Success(formData) =>
        cmd.originalRequest.copy(
          method = HttpMethods.POST,
          uri = Uri(s"$oauthBaseUrl/token"),
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
    decodeToTry[YouTubeApiResponse.GetToken](cmd.response.entityString) match {
      case Success(token) =>
        mediator ! ArchieMateMediator.SendYouTubeChannelSessionsRepositoryCommand(YouTubeChannelSessionsRepository.RefreshToken(cmd.originalRequest.tokenId.get, token))
        ctx.self ! cmd.originalRequest.originalCommand

      case Failure(ex) =>
        ctx.self ! WrappedFailedResponse(cmd.originalRequest.originalCommand, ex)
    }
  }

  private def processGetToken(cmd: GetToken): Unit = {
    ctx.log.debug("processGetToken(cmd: {})", cmd)

    val formDataFuture = Marshal(FormData(Map(
      "client_id" -> settings.youTubeAppClientId,
      "client_secret" -> settings.youTubeAppClientSecret,
      "code" -> cmd.code,
      "grant_type" -> "authorization_code",
      "redirect_uri" -> settings.youTubeAppRedirectUri
    ))).to[RequestEntity]

    ctx.pipeToSelf(formDataFuture) {
      case Success(formData) =>
        Request(
          originalCommand = cmd,
          method = HttpMethods.POST,
          uri = Uri(s"$oauthBaseUrl/token"),
          entity = formData,
          shouldRefresh = false
        )

      case Failure(ex) =>
        WrappedFailedResponse(cmd, ex)
    }
  }

  private def processGetChannelFromTokenId(cmd: GetChannelFromTokenId): Unit = {
    ctx.log.debug("processGetChannelFromTokenId(cmd: {})", cmd)
    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.GET,
      uri = Uri(s"$v3baseUrl/channels").withQuery(Query(
        "part" -> "brandingSettings,contentDetails,contentOwnerDetails,id,localizations,snippet,statistics,status,topicDetails",
        "mine" -> "true"
      )),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def processGetChannelFromAccessToken(cmd: GetChannelFromAccessToken): Unit = {
    ctx.log.debug("processGetChannelFromAccessToken(cmd: {})", cmd)
    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.GET,
      uri = Uri(s"$v3baseUrl/channels").withQuery(Query(
        "part" -> "brandingSettings,contentDetails,contentOwnerDetails,id,localizations,snippet,statistics,status,topicDetails",
        "mine" -> "true"
      )),
      token = Some(YouTubeApiResponse.GetToken(cmd.accessToken, 0, "", None, Nil, "")),
      shouldRefresh = false
    )
  }

  private def processGetLiveBroadcast(cmd: GetLiveBroadcast): Unit = {
    ctx.log.debug("processGetLiveBroadcast(cmd: {})", cmd)
    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.GET,
      uri = Uri(s"$v3baseUrl/liveBroadcasts").withQuery(Query(
        "part" -> "id,snippet,contentDetails,monetizationDetails,status",
        "mine" -> "true"
      )),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def decodeResponse[T <: YouTubeApiResponse](str: String)(using Decoder[T]): Try[T] = {
    decodeToTry[T](str)
  }

  private def tryResponseToStatusReply[T <: YouTubeApiResponse]: PartialFunction[Try[T], StatusReply[T]] = {
    case Success(response) => StatusReply.success(response)
    case Failure(ex) => StatusReply.error(ex)
  }
}
