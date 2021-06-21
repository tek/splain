package splain

import scala.reflect.internal.util.StatisticsStatics
import scala.tools.nsc.Reporting

object StatisticsStaticsHelper {

  def areSomeColdStatsEnabled =
    StatisticsStatics
      .areSomeColdStatsEnabled
}
