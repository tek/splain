package splain

import scala.reflect.internal.util.StatisticsStatics

object StatisticsStaticsHelper {

  def areSomeColdStatsEnabled =
    StatisticsStatics
      .COLD_STATS_GETTER
      .invokeExact()
      .asInstanceOf[Boolean]
}
