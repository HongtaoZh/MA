package adaptivecep.graph.nodes.traits

import adaptivecep.data.Events.{AverageLoad, Created, Event, MessageHops}
import adaptivecep.data.Queries._
import adaptivecep.data.Structures.MachineLoad
import adaptivecep.graph.nodes.traits.Mode.Mode
import adaptivecep.graph.nodes.traits.Node.Subscribe
import adaptivecep.graph.transition.TransitionRequest
import adaptivecep.graph.{CreatedCallback, EventCallback}
import adaptivecep.placement.benchmarking.PlacementAlgorithm
import adaptivecep.placement.vivaldi.VivaldiCoordinates
import adaptivecep.simulation.adaptive.cep.SystemLoad
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
  @volatile var started: Boolean = false
  implicit val excutionContext = global
  var transitionInitiated = false


  val subscribers: ListBuffer[ActorRef] = ListBuffer.empty

  val createdCallback: Option[CreatedCallback]
  val eventCallback: Option[EventCallback]

  SystemLoad.newOperatorAdded()


  def handleTransitionRequest(requester: ActorRef, algorithm: PlacementAlgorithm)

  override def executeTransition(requester: ActorRef, algorithm: PlacementAlgorithm) = {
    log.info(s"executing transition on $mode")
    if (mode == Mode.RUN_RUN) super[RunRunMode].executeTransition(requester, algorithm)
    else super[FlipMode].executeTransition(requester, algorithm)
  }

  override def preStart(): X = {
    super.preStart()
    executionStarted()
    log.debug(s"prestarting with mode $mode")
  }

  def childNodeReceive: Receive

  val localReceive: Receive = {
    case Subscribe() => {
      log.info(s"${sender().path.name} subscribed for ${this.self.path.name}")
      subscribers += sender()
    }
    case TransitionRequest(algorithm) => {
      log.debug("received transition request")
      handleTransitionRequest(sender(), algorithm)
    }
  }

  override def receive: Receive =
    if (mode == Mode.RUN_RUN) super[VivaldiCoordinates].receive orElse ( super[RunRunMode].receive orElse (localReceive orElse (childNodeReceive)))
    else super[VivaldiCoordinates].receive orElse (super[FlipMode].flipReceive andThen childNodeReceive)

  def executionStarted()

  def emitCreated(): Unit = {
    if (createdCallback.isDefined) createdCallback.get.apply() else subscribers.foreach(_ ! Created)
  }

  def updateMonitoringData(event: Event) = {
    val messageHops = event.getOrCreateMonitoringItem[MessageHops](MessageHops(0))
    messageHops.hops += 1
    val currentLoad = SystemLoad.getSystemLoad
    val load = event.getOrCreateMonitoringItem[AverageLoad](AverageLoad(MachineLoad(currentLoad))).load
    load.value = (load.value + currentLoad) / messageHops.hops
  }

  def emitEvent(event: Event): Unit = {
    if (started) {
      updateMonitoringData(event)
      subscribers.foreach(_ ! event)
      if (eventCallback.isDefined) eventCallback.get.apply(event)
    }else{
      log.info("discarding event")
    }
  }
}

object Mode extends Enumeration {
  type Mode = Value
  val RUN_RUN, FLIP = Value
}

object Node {
  case class Subscribe()
  case class UpdatedAddress(oldActor: ActorRef, newActor: ActorRef)
}

