package splain

object Messages
{
  val hasMatching = "hasMatchingSymbol reported error: "

  val typingTypeApply =
    "typing TypeApply reported errors for the implicit tree: "

  val lazyderiv = "could not find Lazy implicit"
}

trait StringColor
{
  def color(s: String, col: String): String
}

object StringColors
{
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

object StringColor
{
  implicit class StringColorOps(s: String)(implicit sc: StringColor)
  {
    import Console._
    def red = sc.color(s, RED)
    def green = sc.color(s, GREEN)
    def yellow = sc.color(s, YELLOW)
    def blue = sc.color(s, BLUE)
  }
}
