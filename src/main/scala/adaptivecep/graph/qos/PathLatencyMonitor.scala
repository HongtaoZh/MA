package adaptivecep.graph.qos

import java.time._

import adaptivecep.data.Events.Event
import adaptivecep.data.Queries._
import adaptivecep.dsl.Dsl.LatencyMeasurement
import adaptivecep.simulation.trasitivecep.RecordLatency
import org.slf4j.LoggerFactory

case class PathLatencyMonitor(query : Query, recordLatency: Option[LatencyMeasurement], latencyVoilation: Option[LatencyMeasurement]) extends Monitor{

  val log = LoggerFactory.getLogger(getClass)
  var latencyRequirement: Option[LatencyRequirement] = query.requirements.collect { case lr: LatencyRequirement => lr }.headOption

  def isRequirementNotMet(latency: Duration, lr: LatencyRequirement): Boolean = {
    val met: Boolean = lr.operator match {
      case Equal => latency.compareTo(lr.duration) == 0
      case NotEqual => latency.compareTo(lr.duration) != 0
      case Greater => latency.compareTo(lr.duration) > 0
      case GreaterEqual => latency.compareTo(lr.duration) >= 0
      case Smaller => latency.compareTo(lr.duration) < 0
      case SmallerEqual => latency.compareTo(lr.duration) <= 0
    }
    !met
  }

  override def onEventEmit(event: Event): X = {
    val latencyMeasurement = Duration.ofMillis(System.currentTimeMillis() - event.createTime)

    if(recordLatency.isDefined) recordLatency.get.apply(latencyMeasurement)

    if(latencyVoilation.isDefined && latencyRequirement.isDefined &&
                                    isRequirementNotMet(latencyMeasurement,latencyRequirement.get)){
      latencyVoilation.get.apply(latencyMeasurement)
    }
  }
}


case class PathLatencyMonitorFactory(query : Query, recordLatency: Option[LatencyMeasurement], latencyVoilation: Option[LatencyMeasurement]) extends MonitorFactory {
  override def createNodeMonitor: Monitor = PathLatencyMonitor(query, recordLatency, latencyVoilation)
}
