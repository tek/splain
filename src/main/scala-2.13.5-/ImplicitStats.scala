package splain

trait ImplicitStats { self: Analyzer =>
  import global._
  import statistics._

  def withImplicitStats[A](run: () => A): A = {

    val (
      findMemberStart,
      subtypeStart,
      start,
    ) = {

      if (StatisticsStaticsHelper.areSomeColdStatsEnabled)
        (
          statistics.startCounter(findMemberImpl),
          statistics.startCounter(subtypeImpl),
          statistics.startTimer(implicitNanos),
        )
      else
        (null, null, null)
    }

    val result = run()

    if (StatisticsStaticsHelper.areSomeColdStatsEnabled) {

      statistics.stopTimer(implicitNanos, start)
      statistics.stopCounter(findMemberImpl, findMemberStart)
      statistics.stopCounter(subtypeImpl, subtypeStart)
    }

    result
  }
}
