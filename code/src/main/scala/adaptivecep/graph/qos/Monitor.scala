package adaptivecep.graph.qos

import adaptivecep.data.Events._
import adaptivecep.data.Queries._

trait Monitor {
  def onEventEmit(event: Event): Unit
}

trait MonitorFactory {
  val query: Query
  def createNodeMonitor: Monitor
}
