package com.archimond7450.archiemate.twitch.irc

import org.slf4j.LoggerFactory

class EmotesDecoder {
  private val log = LoggerFactory.getLogger(getClass)

  private val emotesRegex = """(\w+:\d+-\d+(,\d+-\d+)*(/(\w+:\d+-\d+(,\d+-\d+)*))*)""".r

  def decode(emotes: String): Map[String, List[(Int, Int)]] = emotes match {
    case "" => Map.empty
    case emotesRegex(allEmotes, _, _, _, _) =>
      allEmotes
        .split("/")
        .toList
        .map { emoteGroup =>
          val emoteGroupSplit = emoteGroup.split(":")
          (
            emoteGroupSplit(0),
            emoteGroupSplit(1)
              .split(",")
              .flatMap(_
                .split("-")
                .map(_.toInt)
              ).grouped(2).toList
              .map(from_to => (from_to(0), from_to(1)))
          )
        }
        .toMap
  }
}
