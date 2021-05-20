package splain

import scala.reflect.internal.util.StatisticsStatics

trait ImplicitStats { self: Analyzer =>
  import global._
  import statistics._

  def withImplicitStats[A](run: () => A): A = {

    def areSomeColdStatsEnabled = StatisticsStatics.areSomeColdStatsEnabled
//        .COLD_STATS_GETTER
//        .invokeExact()
//        .asInstanceOf[Boolean]

    val (
      findMemberStart,
      subtypeStart,
      start,
    ) = {

      if (areSomeColdStatsEnabled)
        (
          statistics.startCounter(findMemberImpl),
          statistics.startCounter(subtypeImpl),
          statistics.startTimer(implicitNanos),
        )
      else
        (null, null, null)
    }

    val result = run()

    if (areSomeColdStatsEnabled) {

      statistics.stopTimer(implicitNanos, start)
      statistics.stopCounter(findMemberImpl, findMemberStart)
      statistics.stopCounter(subtypeImpl, subtypeStart)
    }

    result
  }
}
