package splain

trait StringColor {
  def color(s: String, col: String): String
}

object StringColors {
  implicit val noColor =
    new StringColor {
      def color(s: String, col: String) = s
    }

  implicit val color =
    new StringColor {
      import Console.RESET

      def color(s: String, col: String) = col + s + RESET
    }
}

object StringColor {
  implicit class StringColorOps(s: String)(implicit sc: StringColor) {
    import Console._
    def red = sc.color(s, RED)
    def green = sc.color(s, GREEN)
    def yellow = sc.color(s, YELLOW)
    def blue = sc.color(s, BLUE)
  }
}
