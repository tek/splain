package splain

import scala.reflect.internal.util.StatisticsStatics

object StatisticsStaticsHelper {

  def areSomeColdStatsEnabled =
    StatisticsStatics
      .areSomeColdStatsEnabled
}
