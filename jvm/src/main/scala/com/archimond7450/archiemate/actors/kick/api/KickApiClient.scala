package com.archimond7450.archiemate.actors.kick.api

import com.archimond7450.archiemate.actors.repositories.sessions.KickUserSessionsRepository
import com.archimond7450.archiemate.actors.services.caches.KickTokenUserCacheService
import com.archimond7450.archiemate.actors.twitch.api.TwitchApiClient.{
  RefreshToken,
  TokenRefreshed,
  WrappedFailedResponse,
  WrappedSuccessfulResponse
}
import com.archimond7450.archiemate.actors.{ArchieMateMediator, HttpClient}
import com.archimond7450.archiemate.extensions.BehaviorsExtensions.receiveAndLogMessage
import com.archimond7450.archiemate.extensions.Settings
import com.archimond7450.archiemate.helpers.JsonHelper.decodeToTry
import com.archimond7450.archiemate.kick.api.{KickApiRequest, KickApiResponse}
import io.circe.Decoder
import io.circe.syntax.EncoderOps
import org.apache.pekko.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.http.scaladsl.marshalling.Marshal
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

object KickApiClient {
  val actorName = "KickApiClient"

  sealed trait Command
  sealed trait PublicCommand extends Command

  private case class Request(
      originalCommand: PublicCommand,
      refreshing: Boolean = false,
      tokenId: Option[String] = None,
      token: Option[KickApiResponse.GetToken] = None,
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

  private final case class RefreshToken(originalRequest: Request)
      extends Command

  private final case class TokenRefreshed(
      originalRequest: Request,
      response: HttpClient.Response
  ) extends Command

  final case class GetToken(
      replyTo: ActorRef[StatusReply[KickApiResponse.GetToken]],
      code: String,
      codeVerifier: String
  ) extends PublicCommand

  final case class GetTokenUserFromTokenId(
      replyTo: ActorRef[StatusReply[KickApiResponse.User]],
      tokenId: String
  ) extends PublicCommand

  final case class GetTokenUserFromAccessToken(
      replyTo: ActorRef[StatusReply[KickApiResponse.User]],
      accessToken: String
  ) extends PublicCommand

  final case class PostChatMessage(
      replyTo: ActorRef[StatusReply[KickApiResponse.PostChatMessage]],
      tokenId: String,
      content: String,
      replyToMessageId: Option[String] = None
  ) extends PublicCommand

  def apply()(using
      mediator: ActorRef[ArchieMateMediator.Command]
  ): Behavior[Command] = Behaviors
    .supervise[Command] {
      Behaviors.setup { ctx =>
        given ActorContext[Command] = ctx
        new KickApiClient().operational()
      }
    }
    .onFailure[Throwable](SupervisorStrategy.resume)
}

class KickApiClient(using
    ctx: ActorContext[KickApiClient.Command],
    mediator: ActorRef[ArchieMateMediator.Command]
) {
  import KickApiClient.*

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

    case cmd: PostChatMessage =>
      processPostChatMessage(cmd)
      Behaviors.same
  }

  private def processRequest(request: Request): Unit = {
    ctx.log.debug("processRequest(request: {}), request")
    (request.tokenId, request.token) match {
      case (Some(tokenId), None) =>
        retrieveToken(request, tokenId)

      case _ =>
        sendRequest(request)
    }
  }

  private def retrieveToken(request: Request, tokenId: String): Unit = {
    ctx.log.debug(
      "retrieveToken(request: {}, tokenId: \"{}\"",
      request,
      tokenId
    )
    ctx.ask[
      ArchieMateMediator.Command,
      KickUserSessionsRepository.ReturnedTokenFromId
    ](
      mediator,
      ref =>
        ArchieMateMediator.SendKickUserSessionsRepositoryCommand(
          KickUserSessionsRepository.GetTokenFromId(ref, tokenId)
        )
    ) {
      case Success(KickUserSessionsRepository.ReturnedTokenFromId(token)) =>
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
          RawHeader("Client-Id", settings.kickAppClientId)
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
          RuntimeException("Received error response from Kick")
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
          decodeResponse[KickApiResponse.GetToken](json)
        )

      case cmd: GetTokenUserFromTokenId =>
        val tryUsers = decodeResponse[KickApiResponse.GetUsers](json)
        val tryResponse = tryUsers.map(users => users.data.last)
        cmd.replyTo ! tryResponseToStatusReply(tryResponse)
        tryResponse.foreach { tokenUser =>
          mediator ! ArchieMateMediator.SendKickTokenUserCacheServiceCommand(
            KickTokenUserCacheService.CacheTokenUser(tokenUser)
          )
        }

      case cmd: GetTokenUserFromAccessToken =>
        val tryUsers = decodeResponse[KickApiResponse.GetUsers](json)
        val tryResponse = tryUsers.map(users => users.data.last)
        cmd.replyTo ! tryResponseToStatusReply(tryResponse)

      case cmd: PostChatMessage =>
        cmd.replyTo ! tryResponseToStatusReply(
          decodeResponse[KickApiResponse.PostChatMessage](json)
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

      case cmd: PostChatMessage =>
        cmd.replyTo ! StatusReply.error(resp.cause)
    }
  }

  private def processRefreshToken(cmd: RefreshToken): Unit = {
    ctx.log.debug("processRefreshToken(cmd: {})", cmd)
    val formDataFuture = Marshal(
      FormData(
        Map(
          "client_id" -> settings.kickAppClientId,
          "client_secret" -> settings.kickAppClientSecret,
          "grant_type" -> "refresh_token",
          "refresh_token" -> cmd.originalRequest.token.get.refreshToken
        )
      )
    ).to[RequestEntity]

    ctx.pipeToSelf(formDataFuture) {
      case Success(formData) =>
        cmd.originalRequest.copy(
          method = HttpMethods.POST,
          uri = "https://id.kick.com/oauth/token",
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
    decodeToTry[KickApiResponse.GetToken](cmd.response.entityString) match {
      case Success(token) =>
        mediator ! ArchieMateMediator.SendKickUserSessionsRepositoryCommand(
          KickUserSessionsRepository.RefreshToken(
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
          "client_id" -> settings.kickAppClientId,
          "client_secret" -> settings.kickAppClientSecret,
          "code" -> cmd.code,
          "grant_type" -> "authorization_code",
          "redirect_uri" -> settings.kickAppRedirectUri,
          "code_verifier" -> cmd.codeVerifier
        )
      )
    ).to[RequestEntity]

    ctx.pipeToSelf(formDataFuture) {
      case Success(formData) =>
        Request(
          originalCommand = cmd,
          method = HttpMethods.POST,
          uri = "https://id.kick.com/oauth/token",
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
      uri = "https://api.kick.com/public/v1/users",
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
      uri = "https://api.kick.com/public/v1/users",
      token = Some(KickApiResponse.GetToken(cmd.accessToken, "", "", "", "")),
      shouldRefresh = false
    )
  }

  private def processPostChatMessage(cmd: PostChatMessage): Unit = {
    ctx.log.debug("processPostChatMessage(cmd: {})", cmd)
    ctx.self ! Request(
      originalCommand = cmd,
      method = HttpMethods.POST,
      uri = "https://api.kick.com/public/v1/chat",
      entity = HttpEntity(
        ContentTypes.`application/json`,
        KickApiRequest
          .PostChatMessage(
            content = cmd.content,
            `type` = "bot",
            replyToMessageId = cmd.replyToMessageId
          )
          .asJson
          .noSpaces
      ),
      tokenId = Some(cmd.tokenId)
    )
  }

  private def decodeResponse[T <: KickApiResponse](
      str: String
  )(using Decoder[T]): Try[T] = {
    decodeToTry[T](str)
  }

  private def tryResponseToStatusReply[T <: KickApiResponse]
      : PartialFunction[Try[T], StatusReply[T]] = {
    case Success(response) => StatusReply.success(response)
    case Failure(ex)       => StatusReply.error(ex)
  }
}
