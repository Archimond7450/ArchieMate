package com.archimond7450.archiemate.twitch.irc

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class BadgeInfoDecoderSpec extends AnyWordSpecLike with Matchers {
  val badgeInfoDecoder = new BadgeInfoDecoder

  "A badge info decoder's decode method" should {
    "return the correct map" when {
      "there is space in provided value" in {

        val badgeInfo = "predictions/wtiiPig Yes,subscriber/8"
        val expected = Map("predictions" -> "wtiiPig Yes", "subscriber" -> "8")
        badgeInfoDecoder.decode(badgeInfo) shouldEqual expected
      }
    }
  }
}
