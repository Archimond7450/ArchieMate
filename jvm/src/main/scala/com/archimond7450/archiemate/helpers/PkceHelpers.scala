package com.archimond7450.archiemate.helpers

import java.security.{MessageDigest, SecureRandom}
import java.util.Base64

final class PkceHelpers(private val random: SecureRandom) {
  def generateCodeVerifier(length: Int = 64): String = {
    val bytes = new Array[Byte](length)
    random.nextBytes(bytes)
    base64UrlNoPadding(bytes)
  }

  def codeChallengeS256(codeVerifier: String): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(codeVerifier.getBytes("US-ASCII"))
    base64UrlNoPadding(hash)
  }

  private def base64UrlNoPadding(bytes: Array[Byte]): String = {
    Base64.getUrlEncoder.withoutPadding().encodeToString(bytes)
  }
}
