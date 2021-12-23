package splain.typer

import scala.reflect.api.Universe

class Typer[U <: Universe](val universe: U) extends TypeViews {}

object Typer {}
