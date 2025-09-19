package com.archimond7450.archiemate.providers

import java.time.OffsetDateTime

class TimeProvider {
  def now(): OffsetDateTime = OffsetDateTime.now()
}
