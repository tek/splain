package splain

import scala.reflect.internal.util.StatisticsStatics

trait ImplicitStats
{ self: Analyzer =>
  import global._
  import statistics._

  def withImplicitStats[A](run: () => A) = {
    val findMemberStart =
      if (StatisticsStatics.areSomeColdStatsEnabled) statistics.startCounter(findMemberImpl) else null
    val subtypeStart = if (StatisticsStatics.areSomeColdStatsEnabled) statistics.startCounter(subtypeImpl) else null
    val start = if (StatisticsStatics.areSomeColdStatsEnabled) statistics.startTimer(implicitNanos) else null
    val result = run()
    if (StatisticsStatics.areSomeColdStatsEnabled) statistics.stopTimer(implicitNanos, start)
    if (StatisticsStatics.areSomeColdStatsEnabled) statistics.stopCounter(findMemberImpl, findMemberStart)
    if (StatisticsStatics.areSomeColdStatsEnabled) statistics.stopCounter(subtypeImpl, subtypeStart)
    result
  }
}
