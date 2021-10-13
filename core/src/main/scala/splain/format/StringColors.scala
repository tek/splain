package splain.format

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
