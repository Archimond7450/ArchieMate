package com.archimond7450.archiemate.twitch.irc

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class TagsDecoderSpec extends AnyWordSpecLike with Matchers {
  val tagsDecoder = new TagsDecoder

  "A TagsDecoder's decode method" should {
    "return None" when {
      "empty string is provided" in {
        tagsDecoder.decode("") shouldEqual None
      }

      "the '@' character is missing at the start" in {
        val invalidTags = "room-id=1234;user-id=6789"
        val expected = None

        tagsDecoder.decode(invalidTags) shouldEqual expected
      }

      "there is a semicolon in a tag's value" in {
        val invalidTags = "@display-name=Bad;Name;user-id=666"
        val expected = None

        tagsDecoder.decode(invalidTags) shouldEqual expected
      }

      "there is no '=' character in the first tag" in {
        val invalidTags = "@login;tmi-sent-ts=1234567"
        val expected = None

        tagsDecoder.decode(invalidTags) shouldEqual expected
      }

      "there is no '=' character in the second tag" in {
        val invalidTags = "@room-id=2222;tmi-sent-ts"
        val expected = None

        tagsDecoder.decode(invalidTags) shouldEqual expected
      }

      "there is no '=' character in the third tag" in {
        val invalidTags = "@ban-duration=1;room-id=4321;target-user-id;tmi-sent-ts=7654321"
        val expected = None

        tagsDecoder.decode(invalidTags) shouldEqual expected
      }

      "there is an extra '=' character in the first tag" in {
        val invalidTags = "@msg-param-sub-plan-name=Sub\\s=\\sgood;msg-param-sub-plan=1000"
        val expected = None

        tagsDecoder.decode(invalidTags) shouldEqual expected
      }

      "there is an extra '=' character in the second tag" in {
        val invalidTags = "@msg-param-should-share-streak=0;msg-param-sub-plan-name==Sub=;msg-param-sub-plan=3000"
        val expected = None

        tagsDecoder.decode(invalidTags) shouldEqual expected
      }

      "there is an extra '=' character in the third tag" in {
        val invalidTags = "@msg-param-should-share-streak=0;msg-param-streak-months=0;msg-param-sub-plan-name=Sub2=better;msg-param-sub-plan=2000"
        val expected = None

        tagsDecoder.decode(invalidTags) shouldEqual expected
      }

      "there is a space in the first tag" in {
        val invalidTags = "@msg-param-sub-plan-name=Good Sub;msg-id=sub;msg-param-sub-plan=1000"
        val expected = None

        tagsDecoder.decode(invalidTags) shouldEqual expected
      }

      "there is a space in the second tag" in {
        val invalidTags = "msg-id=sub;msg-param-sub-plan-name=Sub Plan Name;msg-param-sender-login=user"
        val expected = None

        tagsDecoder.decode(invalidTags) shouldEqual expected
      }

      "there is a space in the third tag" in {
        val invalidTags = "msg-id=sub;msg-param-sub-plan=3000;msg-param-sub-plan-name=The Best Sub Plan Ever!"
        val expected = None

        tagsDecoder.decode(invalidTags) shouldEqual expected
      }
    }

    "return Some" when {
      "one correct tag is provided" in {
        val tags = "@msg-id=delete_message_success"
        val expected = Some(Map("msg-id" -> "delete_message_success"))

        tagsDecoder.decode(tags) shouldEqual expected
      }

      "two correct tags are provided" in {
        val tags = "@msg-id=sub;msg-param-sub-plan=Prime"
        val expected = Some(
          Map(
            "msg-id" -> "sub",
            "msg-param-sub-plan" -> "Prime"
          )
        )

        tagsDecoder.decode(tags) shouldEqual expected
      }

      "three correct tags are provided" in {
        val tags = "@room-id=1234;target-user-id=8765;tmi-sent-ts=1642715756806"
        val expected = Some(
          Map(
            "room-id" -> "1234",
            "target-user-id" -> "8765",
            "tmi-sent-ts" -> "1642715756806"
          )
        )

        tagsDecoder.decode(tags) shouldEqual expected
      }
    }
  }

  "A TagsDecoder's unescapeValue method" should {
    "return the same value" when {
      "there is nothing to escape" in {
        val value = "TestChannel123"
        val expected = value

        tagsDecoder.unescapeValue(value) shouldEqual expected
      }

      "empty string is provided" in {
        val value = ""
        val expected = value

        tagsDecoder.unescapeValue(value) shouldEqual expected
      }
    }
    
    "returns the correct unescaped value" when {
      "test\\ is passed" in {
        val value = "test\\"
        val expected = "test"

        tagsDecoder.unescapeValue(value) shouldEqual expected
      }
      
      "\\b is passed" in {
        val value = "\\b"
        val expected = "b"

        tagsDecoder.unescapeValue(value) shouldEqual expected
      }
      
      "\\\\ is passed" in {
        val value = "\\\\"
        val expected = "\\"

        tagsDecoder.unescapeValue(value) shouldEqual expected
      }
      
      "a larger example" in {
        val value = "This\\sis\\sa\\stest\\:\\salso\\sthis\\r\\nAnother\\sline\\sof\\stext:\\s\\\\\\\\localhost\\\\someFile.txt"
        val expected = "This is a test; also this\r\nAnother line of text: \\\\localhost\\someFile.txt"

        tagsDecoder.unescapeValue(value) shouldEqual expected
      }
    }
  }
}
