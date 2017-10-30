package adaptivecep.graph.qos

import akka.actor.{ActorContext, ActorRef}
import adaptivecep.data.Events._
import adaptivecep.data.Queries._

import scala.collection.mutable.ListBuffer

trait Monitor {
  def onEventEmit(event: Event): Unit
}

trait MonitorFactory {
  val query: Query
  def createNodeMonitor: Monitor
}
