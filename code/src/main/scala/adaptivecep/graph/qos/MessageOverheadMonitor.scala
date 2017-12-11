package adaptivecep.graph.qos

import adaptivecep.data.Events.{Event, MessageHops}
import adaptivecep.data.Queries.{MessageOverheadRequirement, _}
import adaptivecep.dsl.Dsl.MessageOverheadMeasurement
import org.slf4j.LoggerFactory

case class MessageOverheadMonitor(query: Query, record: Option[MessageOverheadMeasurement]) extends Monitor{

  var logger = LoggerFactory.getLogger(getClass)
  val overheadRequirement = query.requirements.collect { case lr: MessageOverheadRequirement => lr }.headOption


  override def onEventEmit(event: Event): X = {
    val hopsItem = event.getMonitoringItem[MessageHops]()

    if(record.isDefined && hopsItem.isDefined) record.get.apply(hopsItem.get.hops)

    if(overheadRequirement.isDefined && overheadRequirement.get.otherwise.isDefined){
      val loadRequirementVal = overheadRequirement.get.requirement
      val currentHops = hopsItem.get.hops
      overheadRequirement.get.operator match {
        case Equal =>        if (!(currentHops == loadRequirementVal)) overheadRequirement.get.otherwise.get.apply(hopsItem.get.hops)
        case NotEqual =>     if (!(currentHops != loadRequirementVal)) overheadRequirement.get.otherwise.get.apply(hopsItem.get.hops)
        case Greater =>      if (!(currentHops >  loadRequirementVal)) overheadRequirement.get.otherwise.get.apply(hopsItem.get.hops)
        case GreaterEqual => if (!(currentHops >= loadRequirementVal)) overheadRequirement.get.otherwise.get.apply(hopsItem.get.hops)
        case Smaller =>      if (!(currentHops <  loadRequirementVal)) overheadRequirement.get.otherwise.get.apply(hopsItem.get.hops)
        case SmallerEqual => if (!(currentHops <= loadRequirementVal)) overheadRequirement.get.otherwise.get.apply(hopsItem.get.hops)
      }
    }
  }
}


case class MessageOverheadMonitorFactory(query: Query, record: Option[MessageOverheadMeasurement]) extends MonitorFactory {
  override def createNodeMonitor: Monitor = MessageOverheadMonitor(query, record)
}
