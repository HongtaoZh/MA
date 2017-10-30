package adaptivecep.graph.nodes.traits

import adaptivecep.data.Events.{Created, Event}
import adaptivecep.data.Queries._
import adaptivecep.graph.nodes.traits.Mode.Mode
import adaptivecep.graph.nodes.traits.Node.Subscribe
import adaptivecep.graph.qos._
import adaptivecep.graph.{CreatedCallback, EventCallback}
import adaptivecep.placement.benchmarking.PlacementAlgorithm
import adaptivecep.placement.vivaldi.VivaldiCoordinates
import akka.actor.{ActorLogging, ActorRef}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * The base class for all of the nodes
  **/
trait Node extends VivaldiCoordinates with FlipMode with RunRunMode with ActorLogging {

  val name: String = self.path.name
  val query: Query
  val mode: Mode
  var started: Boolean =false
  implicit val excutionContext = global
  var transitionInitiated = false

  val subscribers: ListBuffer[ActorRef] = ListBuffer.empty

  val createdCallback: Option[CreatedCallback]
  val eventCallback: Option[EventCallback]

  val localReceive: Receive = {
    case Subscribe() => {
      log.info(s"${sender().path.name} subscribed for ${this.self.path.name}")
      subscribers += sender()
    }
  }

  def handleTransitionRequest(algorithm: PlacementAlgorithm)

  override def preStart(): X = {
    super.preStart()
    if(mode == Mode.RUN_RUN) {
      executionStarted()
    }
  }

  override def receive: Receive =
    if (mode == Mode.RUN_RUN) super[VivaldiCoordinates].receive orElse super[RunRunMode].receive orElse localReceive
    else super[VivaldiCoordinates].receive orElse super[FlipMode].receive orElse localReceive

  def executionStarted()

  def emitCreated(): Unit = {
    if (createdCallback.isDefined) createdCallback.get.apply() else subscribers.foreach(_ ! Created)
  }

  def emitEvent(event: Event): Unit = {
    if(!started) {
      log.info(s"discarding event $event")
    }
    event.hops +=1
    subscribers.foreach(_ ! event)

    if (eventCallback.isDefined) eventCallback.get.apply(event)
  }

}

object Mode extends Enumeration {
  type Mode = Value
  val RUN_RUN, FLIP = Value
}

object Node {
  case class Subscribe()
}

