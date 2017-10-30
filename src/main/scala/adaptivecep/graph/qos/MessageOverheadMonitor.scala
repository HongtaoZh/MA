package adaptivecep.graph.qos

import adaptivecep.data.Events.Event
import adaptivecep.data.Queries.{MessageOverheadRequirement, _}
import adaptivecep.dsl.Dsl.MessageOverheadMeasurement
import org.slf4j.LoggerFactory

case class MessageOverheadMonitor(query: Query, record: Option[MessageOverheadMeasurement],
                                 voilation: Option[MessageOverheadMeasurement]) extends Monitor{

  var logger = LoggerFactory.getLogger(getClass)
  val overheadRequirement = query.requirements.collect { case lr: MessageOverheadRequirement => lr }.headOption

  override def onEventEmit(event: Event): X = {
    if(record.isDefined) record.get.apply(event.hops)

    if(voilation.isDefined && overheadRequirement.isDefined && event.hops > overheadRequirement.get.requirement){
      voilation.get.apply(event.hops)
    }
  }
}


case class MessageOverheadMonitorFactory(query: Query, record: Option[MessageOverheadMeasurement],
                                         voilation: Option[MessageOverheadMeasurement]) extends MonitorFactory {
  override def createNodeMonitor: Monitor = MessageOverheadMonitor(query, record, voilation)
}
