package splain

trait WarningCompat
{ self: Analyzer =>
  import global._

  def contextWarning(context: Context, pos: Position, message: String): Unit =
    context.warning(pos, message)
}
