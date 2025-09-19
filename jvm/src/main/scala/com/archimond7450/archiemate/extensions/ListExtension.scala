package com.archimond7450.archiemate.extensions

import com.archimond7450.archiemate.providers.RandomProvider

object ListExtension {
  extension[T](list: List[T]) {
    def randomOrDefault(default: T)(using random: RandomProvider): T = list match {
      case Nil => default
      case _   => list(random.between(0, list.size))
    }

    def toMapWithKey[K](keyGenerator: T => K): Map[K, T] = {
      list.map(v => (keyGenerator(v) -> v)).toMap
    }
  }
}
