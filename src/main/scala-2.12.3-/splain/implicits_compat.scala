package splain

import reflect.internal.util.Statistics
import tools.nsc._

trait ImplicitStatsCompat
{ self: Analyzer =>
  import global._

  def withImplicitStats[A](run: () => A) = {
    import typechecker.ImplicitsStats._
    val rawTypeStart = if (Statistics.canEnable) Statistics.startCounter(rawTypeImpl) else null
    val findMemberStart = if (Statistics.canEnable) Statistics.startCounter(findMemberImpl) else null
    val subtypeStart = if (Statistics.canEnable) Statistics.startCounter(subtypeImpl) else null
    val start = if (Statistics.canEnable) Statistics.startTimer(implicitNanos) else null
    val result = run()
    if (Statistics.canEnable) Statistics.stopTimer(implicitNanos, start)
    if (Statistics.canEnable) Statistics.stopCounter(rawTypeImpl, rawTypeStart)
    if (Statistics.canEnable)
      Statistics.stopCounter(findMemberImpl, findMemberStart)
    if (Statistics.canEnable) Statistics.stopCounter(subtypeImpl, subtypeStart)
    result
  }
}
