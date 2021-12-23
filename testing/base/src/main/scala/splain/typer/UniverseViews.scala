package splain.typer

import scala.reflect.api.Universe

trait UniverseViews {

  val universe: Universe
  def rootMirror: universe.Mirror = universe.rootMirror

  final type UU = Universe with universe.type
  final lazy val getUniverse: UU = universe

  type TypeTag[T] = universe.TypeTag[T]
  type WeakTypeTag[T] = universe.WeakTypeTag[T]
  type Type = universe.Type
  type Symbol = universe.Symbol

  trait ViewBase[T] {

    def self: T

    def getCanonicalName(v: T): String

    final lazy val canonicalName = getCanonicalName(self)
    override def toString: String = canonicalName

    trait Breadcrumbs {

      def getCanonicalName(v: T): String = ViewBase.this.getCanonicalName(v)

      case class BreadcrumbView(
          list: List[T]
      ) {

        def rightOpt: Option[T] = list.headOption
        def leftOpt: Option[T] = list.lastOption

        lazy val prefixName: String = rightOpt.map(getCanonicalName).getOrElse("")

        lazy val simpleName: String = {
          canonicalName.stripPrefix(prefixName).stripPrefix(".").stripPrefix("#")
        }
      }
    }
  }
}
