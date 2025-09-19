package com.archimond7450.archiemate.extensions

import scala.util.Try

object TryExtensions {
  extension[T](aTry: Try[T]) {
    def tryMap[U](f: T => U): Try[U] = aTry.flatMap(value => Try(f(value)))
  }
}
