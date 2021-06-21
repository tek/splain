package splain

import scala.reflect.internal.util.StatisticsStatics

object StatisticsStaticsHelper {

  def areSomeColdStatsEnabled: Boolean = {

    val getter = StatisticsStatics.COLD_STATS_GETTER

    getter.invoke().asInstanceOf[Boolean]
  }
}
