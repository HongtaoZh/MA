package adaptivecep.graph.qos

import java.time._

import adaptivecep.data.Events.Event
import adaptivecep.data.Queries._
import adaptivecep.dsl.Dsl.LatencyMeasurement
import org.slf4j.LoggerFactory

case class PathLatencyMonitor(query : Query, recordLatency: Option[LatencyMeasurement]) extends Monitor{

  val log = LoggerFactory.getLogger(getClass)
  var latencyRequirement: Option[LatencyRequirement] = query.requirements.collect { case lr: LatencyRequirement => lr }.headOption

  def isRequirementNotMet(latency: Duration, lr: LatencyRequirement): Boolean = {
    val met: Boolean = lr.operator match {
      case Equal => latency.compareTo(lr.latency) == 0
      case NotEqual => latency.compareTo(lr.latency) != 0
      case Greater => latency.compareTo(lr.latency) > 0
      case GreaterEqual => latency.compareTo(lr.latency) >= 0
      case Smaller => latency.compareTo(lr.latency) < 0
      case SmallerEqual => latency.compareTo(lr.latency) <= 0
    }
    !met
  }

  override def onEventEmit(event: Event): X = {
    val latencyMeasurement = Duration.ofMillis(System.currentTimeMillis() - event.createTime)
    if(recordLatency.isDefined) recordLatency.get.apply(latencyMeasurement)
    if(latencyRequirement.isDefined && latencyRequirement.get.otherwise.isDefined &&
                                    isRequirementNotMet(latencyMeasurement,latencyRequirement.get)){
      latencyRequirement.get.otherwise.get.apply(latencyMeasurement)
    }
  }
}

case class PathLatencyMonitorFactory(query : Query, recordLatency: Option[LatencyMeasurement]) extends MonitorFactory {
  override def createNodeMonitor: Monitor = PathLatencyMonitor(query, recordLatency)
}
