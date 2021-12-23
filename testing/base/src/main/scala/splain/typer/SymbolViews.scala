package splain.typer

import scala.collection.mutable

trait SymbolViews extends UniverseViews {
//  self: Reflection =>

  case class SymbolView(
      self: universe.Symbol
  ) extends ViewBase[universe.Symbol] {

    lazy val ownerOpt: Option[universe.Symbol] = {

      val owner = self.owner

      if (owner == universe.NoSymbol) None
      else Some(owner)
    }

    object Owners extends Breadcrumbs {

      lazy val internal: BreadcrumbView = {

        val chain = ownerOpt.toList.flatMap { owner =>
          val v1: List[universe.Symbol] = List(owner)
          val v2: List[universe.Symbol] = copy(owner).Owners.internal.list

          v1 ++ v2
        }

        BreadcrumbView(chain)
      }

      lazy val all: BreadcrumbView = {
        val chain: List[universe.Symbol] = internal.list.filterNot { owner =>
          owner.fullName == "<root>"
        }
        val result = BreadcrumbView(chain)
//        result.validate()
        result
      }

      lazy val packages: BreadcrumbView = {

        val list = all.list.reverse.takeWhile { owner =>
          owner.isPackage
        }.reverse

        BreadcrumbView(list)
      }

      lazy val static: BreadcrumbView = {

        val list = all.list.reverse.takeWhile { owner =>
          owner.isStatic
        }.reverse

        BreadcrumbView(list)
      }
    }

    override def getCanonicalName(v: universe.Symbol): String = v.fullName
  }

  val symbolCache = mutable.Map.empty[Symbol, SymbolView]

  def symbolView(ss: Symbol): SymbolView = symbolCache.getOrElseUpdate(
    ss,
    SymbolView(ss)
  )
}
