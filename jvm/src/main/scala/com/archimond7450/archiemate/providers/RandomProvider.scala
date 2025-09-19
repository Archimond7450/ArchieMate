package com.archimond7450.archiemate.providers

import java.util.UUID
import scala.util.Random

class RandomProvider {
  private val random = new Random

  def between(firstInt: Int, secondInt: Int): Int = {
    val (min, max) = if (firstInt < secondInt) (firstInt, secondInt) else (secondInt, firstInt)
    random.between(min, max)
  }

  def between(firstLong: Long, secondLong: Long): Long = {
    val (min, max) = if (firstLong < secondLong) (firstLong, secondLong) else (secondLong, firstLong)
    random.between(min, max)
  }

  def uuid(): UUID = UUID.randomUUID()
}
