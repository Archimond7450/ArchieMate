package com.archimond7450.archiemate

import com.raquo.laminar.api.L.*
import com.raquo.laminar.codecs.StringAsIsCodec

object CustomAttrs {
  val scope: HtmlAttr[String] = new HtmlAttr("scope", StringAsIsCodec)
}
