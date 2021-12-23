package splain

package object typer {

  type RuntimeUniverse = scala.reflect.runtime.universe.type
  val RuntimeUniverse: RuntimeUniverse = scala.reflect.runtime.universe

//  val ScalaReflection: Reflection.Runtime.type = Reflection.Runtime
//
//  val MacroReflection: Reflection.CompileTime.type = Reflection.CompileTime
}
