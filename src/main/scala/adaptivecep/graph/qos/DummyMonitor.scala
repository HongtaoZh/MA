package adaptivecep.graph.qos

import adaptivecep.data.Events.Event
import adaptivecep.data.Queries.Query

case class DummyMonitorFactory(query: Query) extends MonitorFactory {

  override def createNodeMonitor: Monitor = (event: Event) => {
    //ignoring messages
  }
}

