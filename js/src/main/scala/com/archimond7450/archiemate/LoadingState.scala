package com.archimond7450.archiemate

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

sealed trait LoadingState[+T] {
  def map[S](f: T => S): LoadingState[S]
  def flatMap[S](f: T => LoadingState[S]): LoadingState[S]
  def filter(f: T => Boolean): LoadingState[T]
}

case object Loading extends LoadingState[Nothing] {
  override def map[S](f: Nothing => S): LoadingState[S] = this
  override def flatMap[S](f: Nothing => LoadingState[S]): LoadingState[S] = this
  override def filter(f: Nothing => Boolean): LoadingState[Nothing] = this
}
case class Loaded[T](tryValue: Try[T]) extends LoadingState[T] {
  override def map[S](f: T => S): LoadingState[S] = Loaded(tryValue.map(f))
  override def flatMap[S](f: T => LoadingState[S]): LoadingState[S] = tryValue match {
    case Success(value) => f(value)
    case Failure(ex) => Loaded[S](Failure(ex))

  }
  override def filter(f: T => Boolean): LoadingState[T] = Loaded(tryValue.filter(f))
}

object LoadingState {
  def fromFuture[T](f: Future[T])(using ExecutionContextExecutor): Future[LoadingState[T]] = f.transform(res => Success(Loaded[T](res)))
}
