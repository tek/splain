package splain

import tools.nsc.Reporting.WarningCategory

trait WarningCompat
{ self: Analyzer =>
  import global._

  def contextWarning(context: Context, pos: Position, message: String): Unit =
    context.warning(pos, message, WarningCategory.Other)
}
