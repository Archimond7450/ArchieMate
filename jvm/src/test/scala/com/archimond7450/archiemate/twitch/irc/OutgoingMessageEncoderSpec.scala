package com.archimond7450.archiemate.twitch.irc

import com.archimond7450.archiemate.twitch.irc.OutgoingMessages.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class OutgoingMessageEncoderSpec extends AnyWordSpecLike with Matchers {
  val outgoingMessageEncoder = new OutgoingMessageEncoder

  "Pass" should {
    "be properly encoded" in {
      val pass = Pass("myverysecrettoken")
      val expected = "PASS oauth:myverysecrettoken"

      outgoingMessageEncoder.encode(pass) shouldEqual expected
    }
    
    "be properly encoded with other token" in {
      val pass = Pass("anothertoken")
      val expected = "PASS oauth:anothertoken"

      outgoingMessageEncoder.encode(pass) shouldEqual expected
    }
  }

  "Nick" should {
    "be properly encoded" in {
      val nick = Nick("archiemate")
      val expected = "NICK archiemate"

      outgoingMessageEncoder.encode(nick) shouldEqual expected
    }

    "be properly encoded with other username" in {
      val nick = Nick("archimond7450")
      val expected = "NICK archimond7450"

      outgoingMessageEncoder.encode(nick) shouldEqual expected
    }
  }

  "CapabilityRequest" should {
    "be properly encoded" in {
      val capabilityRequest = CapabilityRequest(List("twitch.tv/membership", "twitch.tv/tags", "twitch.tv/commands"))
      val expected = "CAP REQ :twitch.tv/membership twitch.tv/tags twitch.tv/commands"

      outgoingMessageEncoder.encode(capabilityRequest) shouldEqual expected
    }

    "be properly encoded with other capabilities" in {
      val capabilityRequest = CapabilityRequest(List("twitch.tv/tags"))
      val expected = "CAP REQ :twitch.tv/tags"

      outgoingMessageEncoder.encode(capabilityRequest) shouldEqual expected
    }
  }

  "Join" should {
    "be properly encoded" in {
      val join = Join(List("archiemate"))
      val expected = "JOIN #archiemate"

      outgoingMessageEncoder.encode(join) shouldEqual expected
    }

    "be properly encoded for other channels" in {
      val join = Join(List("archimond7450"))
      val expected = "JOIN #archimond7450"

      outgoingMessageEncoder.encode(join) shouldEqual expected
    }

    "be properly encoded for multiple channels" in {
      val join = Join(List("archiemate", "archimond7450", "wtii"))
      val expected = "JOIN #archiemate,#archimond7450,#wtii"

      outgoingMessageEncoder.encode(join) shouldEqual expected
    }
  }

  "Pong" should {
    "be properly encoded" in {
      val pong = Pong
      val expected = "PONG :tmi.twitch.tv"

      outgoingMessageEncoder.encode(pong) shouldEqual expected
    }
  }

  "PrivMsg" should {
    "be properly encoded" in {
      val privMsg = PrivMsg("archimond7450", "Hi, I'm back! HeyGuys")
      val expected = "PRIVMSG #archimond7450 :Hi, I'm back! HeyGuys"

      outgoingMessageEncoder.encode(privMsg) shouldEqual expected
    }

    "be properly encoded with other channels and messages" in {
      val privMsg = PrivMsg("wtii", "Highrise DOES it again!")
      val expected = "PRIVMSG #wtii :Highrise DOES it again!"

      outgoingMessageEncoder.encode(privMsg) shouldEqual expected
    }

    "be properly encoded with trimmed messages" in {
      val privMsg = PrivMsg("archiemate", "This message doesn't have spaces at the end.              ")
      val expected = "PRIVMSG #archiemate :This message doesn't have spaces at the end."

      outgoingMessageEncoder.encode(privMsg) shouldEqual expected
    }

    "be properly encoded as PRIVMSGs for each line ending with LF in message parameter" in {
      val message =
        """This is a long message divided into three lines.
          |Each line should be sent out to Twitch as separate PRIVMSG.
          |The message ends with this sentence.""".stripMargin
      val privMsg = PrivMsg("archimond7450", message)

      val messageLines = message.split("\n").map(_.trim) // LF only
      val expected =
        s"""PRIVMSG #archimond7450 :${messageLines(0)}
           |PRIVMSG #archimond7450 :${messageLines(1)}
           |PRIVMSG #archimond7450 :${messageLines(2)}""".stripMargin

      outgoingMessageEncoder.encode(privMsg) shouldEqual expected
    }

    "be properly encoded as PRIVMSGs for each line ending with CRLF in message parameter" in {
      val message =
        """This is a long message divided into three lines.
          |Each line should be sent out to Twitch as separate PRIVMSG.
          |The message ends with this sentence.""".stripMargin
      val privMsg = PrivMsg("archimond7450", message)

      val messageLines = message.split("\n").map(_.trim).map(line => s"$line\r") // CRLF
      val expected =
        s"""PRIVMSG #archimond7450 :${messageLines(0).trim}
           |PRIVMSG #archimond7450 :${messageLines(1).trim}
           |PRIVMSG #archimond7450 :${messageLines(2).trim}""".stripMargin

      outgoingMessageEncoder.encode(privMsg) shouldEqual expected
    }

    "be properly encoded as PRIVMSG with the reply-parent-msg-id tag" in {
      val privMsg = PrivMsg("ronni", "Good idea!", Some("b34ccfc7-4977-403a-8a94-33c6bac34fb8"))
      val expected = "@reply-parent-msg-id=b34ccfc7-4977-403a-8a94-33c6bac34fb8 PRIVMSG #ronni :Good idea!"
      outgoingMessageEncoder.encode(privMsg) shouldEqual expected
    }
  }
}
