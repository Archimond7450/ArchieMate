package com.archimond7450.archiemate.extensions

object StringExtensions {
  extension (str: String) {
    def asVariableRegex: String = s"(?<!\\$$)\\$$($str(?!\\S)|\\{$str})"
    def asTwitchSubTier(isPrime: Boolean): String = (str, isPrime) match {
      case ("1000", true)  => "Prime"
      case ("1000", false) => "tier 1"
      case ("2000", false) => "tier 2"
      case ("3000", false) => "tier 3"
      case _               => ""
    }
    def asIndex(size: Int): Int = str.trim.toIntOption match {
      case Some(integer) if integer >= 0 => Math.max(integer, size - 1)
      case Some(negative) => Math.clamp(size + negative, 0, size - 1)
      case None =>
        str.trim match {
          case "first"  => 0
          case "second" => if (size > 1) 1 else 0
          case "third"  => if (size > 2) 2 else Math.max(size - 1, 0)
          case "last"   => Math.max(size - 1, 0)
          case order    => Math.clamp(str.orderToIndex, 0, size - 1)
        }
    }
    def orderToIndex: Int = {
      val numberOption = str.substring(0, str.length - 2).toIntOption
      val suffix = str.substring(str.length - 2)

      numberOption match {
        case None => 0
        case Some(number) =>
          (number, suffix) match {
            case (n, "st") if n % 10 == 1 && n % 100 != 11 => n - 1
            case (n, "nd") if n % 10 == 2 && n % 100 != 12 => n - 1
            case (n, "rd") if n % 10 == 3 && n % 100 != 13 => n - 1
            case (n, "th")                                 => n - 1
            case _                                         => 0
          }
      }
    }
  }
}
