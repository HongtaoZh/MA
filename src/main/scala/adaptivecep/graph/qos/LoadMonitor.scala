package adaptivecep.graph.qos

import adaptivecep.data.Events.Event
import adaptivecep.data.Queries._
import adaptivecep.data.Structures.MachineLoad
import adaptivecep.dsl.Dsl.LoadMeasurement
import adaptivecep.simulation.adaptive.cep.SystemLoad
import org.slf4j.LoggerFactory

case class LoadMonitor(query: Query, record: Option[LoadMeasurement], voilation: Option[LoadMeasurement]) extends Monitor{
  val log = LoggerFactory.getLogger(getClass)
  var loadRequirement: Option[LoadRequirement] = query.requirements.collect { case lr: LoadRequirement => lr }.headOption

  override def onEventEmit(event: Event): X = {
    val currentLoad = MachineLoad.make(SystemLoad.getSystemLoad)

    if(record.isDefined) record.get.apply(currentLoad)
    if(voilation.isDefined &&   loadRequirement.isDefined &&
                                SystemLoad.isSystemOverloaded(loadRequirement.get.machineLoad.value)){
      record.get.apply(currentLoad)
    }
  }
}

case class LoadMonitorFactory(query: Query, record: Option[LoadMeasurement], voilation: Option[LoadMeasurement]) extends MonitorFactory {
  override def createNodeMonitor: Monitor = LoadMonitor(query, record, voilation)
}
