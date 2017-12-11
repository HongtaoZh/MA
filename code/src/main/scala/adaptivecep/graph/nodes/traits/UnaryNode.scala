package adaptivecep.graph.nodes.traits

import adaptivecep.data.Events._
import adaptivecep.data.Queries._
import adaptivecep.graph.nodes.traits.Node.{Subscribe, UpdatedAddress}
import adaptivecep.graph.transition.TransitionRequest
import adaptivecep.graph.{CreatedCallback, EventCallback}
import adaptivecep.placement.benchmarking.PlacementAlgorithm
import akka.actor.ActorRef

/**
  * Handling of [[adaptivecep.data.Queries.UnaryQuery]] is done by UnaryNode
  **/
trait UnaryNode extends Node {

  override val query: UnaryQuery

  var parentNode: ActorRef

  val createdCallback: Option[CreatedCallback]
  val eventCallback: Option[EventCallback]
  private var transitionRequestor: ActorRef = _


  override def preStart(): X = {
    super.preStart()
    log.info(s"subscribing for events from ${parentNode.path.name}")
  }

  override def executionStarted(): X = {
    parentNode ! Subscribe()
  }

  override def childNodeReceive: Receive = {
    case event: Event if !sender().equals(parentNode) => {
      log.info(s"ParentNode ${parentNode.path} ReceivedFrom: ${sender().path}")
    }

    case TransferredState(placementAlgo, successor) => {
      log.info("parent successfully migrated to the new node")

      if(sender().equals(parentNode)){
        parentNode = successor
      }else{
        log.error("unexpected state of the system")
      }

      executeTransition(transitionRequestor, placementAlgo)
    }
  }

  override def handleTransitionRequest(requester: ActorRef, algorithm: PlacementAlgorithm) = {
    log.info(s"Asking ${parentNode.path.name} to transit to algorithm ${algorithm.algorithm.getClass}")
    transitionRequestor = requester
    parentNode ! TransitionRequest(algorithm)
  }
}