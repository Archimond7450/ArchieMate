package com.archimond7450.archiemate.extensions

object StringExtensions {
  extension(str: String) {
    def asVariableRegex: String = s"(?<!\\$$)\\$$($str(?!\\S)|\\{$str})"
    def asTwitchSubTier(isPrime: Boolean): String = (str, isPrime) match {
      case ("1000", true) => "Prime"
      case ("1000", false) => "tier 1"
      case ("2000", false) => "tier 2"
      case ("3000", false) => "tier 3"
      case _ => ""
    }
  }
}
