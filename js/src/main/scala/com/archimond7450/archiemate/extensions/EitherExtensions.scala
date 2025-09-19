package com.archimond7450.archiemate.extensions

object EitherExtensions {
  extension[A, B](either: Either[A, B]) {
    def whenLeft(onLeft: A => Unit): Either[A, B] = {
      either match {
        case Left(value) =>
          onLeft(value)

        case _ =>
      }
      either
    }
  }
}
