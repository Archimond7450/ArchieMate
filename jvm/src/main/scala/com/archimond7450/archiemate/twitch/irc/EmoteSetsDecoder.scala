package com.archimond7450.archiemate.twitch.irc

import org.slf4j.LoggerFactory

class EmoteSetsDecoder {
  private val log = LoggerFactory.getLogger(getClass)

  private val emoteSetsRegex = """(0(,\d+)*)""".r

  def decode(emoteSets: String): List[Int] = emoteSets match {
    case emoteSetsRegex(allEmoteSets, _) =>
      allEmoteSets
        .split(",")
        .map(_.toInt)
        .toList
    case _ =>
      log.error("Cannot decode invalid emoteSets: {}", emoteSets)
      List(0)
  }
}
