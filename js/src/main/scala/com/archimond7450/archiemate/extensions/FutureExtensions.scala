package com.archimond7450.archiemate.extensions

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object FutureExtensions {
  extension[T](future: Future[T]) {
    def onError(errorCallback: Throwable => Unit)(using ExecutionContextExecutor): Future[T] = {
      future.transform {
        case failure @ Failure(ex) =>
          errorCallback(ex)
          failure

        case success: Success[T] =>
          success
      }
    }
  }
}
