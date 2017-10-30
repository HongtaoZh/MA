package adaptivecep.graph.nodes

import adaptivecep.ClusterActor
import adaptivecep.data.Events._
import adaptivecep.graph.nodes.traits.Node.Subscribe
import adaptivecep.graph.nodes.traits.TransferredState
import adaptivecep.graph.qos.MonitorFactory
import adaptivecep.graph.transition.TransitionRequest
import akka.actor.{ActorLogging, ActorRef}

/**
  * Receives the final output of the query
  *
  **/

case class ClientNode(publisher: ActorRef, monitorFactories: Array[MonitorFactory]) extends ClusterActor with ActorLogging {

  val monitors = monitorFactories.map(f=> f.createNodeMonitor)

  override def receive: Receive = {
    case TransitionRequest(algorithm) => {
      log.info("initiating the Transition")
      publisher ! TransitionRequest(algorithm)
    }

    case TransferredState(_,_) => log.info("Transition Completed")

    case event: Event => {
      printEvent(event)
      monitors.foreach(monitor=>monitor.onEventEmit(event))
    }
  }

  def printEvent(event: Event): Unit = event match {
    case Event1(e1) => log.info(s"EVENT:\tEvent1($e1)")
    case Event2(e1, e2) =>  log.info(s"Event:\tEvent2($e1, $e2)")
    case Event3(e1, e2, e3) => log.info(s"Event:\tEvent3($e1, $e2, $e3)")
    case Event4(e1, e2, e3, e4) => log.info(s"Event:\tEvent4($e1, $e2, $e3, $e4)")
    case Event5(e1, e2, e3, e4, e5) => log.info(s"Event:\tEvent5($e1, $e2, $e3, $e4, $e5)")
    case Event6(e1, e2, e3, e4, e5, e6) => log.info(s"Event:\tEvent6($e1, $e2, $e3, $e4, $e5, $e6)")
  }

  override def preStart(): Unit = {
    super.preStart()
    log.info(s"Subscribing for events from ${publisher.path.name}")
    publisher ! Subscribe()
  }
}


