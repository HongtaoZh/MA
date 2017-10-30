package adaptivecep.graph.nodes.traits

import adaptivecep.data.Events._
import adaptivecep.data.Queries._
import adaptivecep.graph.nodes.traits.Node.Subscribe
import adaptivecep.graph.qos._
import adaptivecep.graph.transition.TransitionRequest
import adaptivecep.graph.{CreatedCallback, EventCallback}
import akka.actor.ActorRef

/**
  * Handling of [[adaptivecep.data.Queries.UnaryQuery]] is done by UnaryNode
  **/
trait UnaryNode extends Node {

  override val query: UnaryQuery

  var parentNode: ActorRef

  val createdCallback: Option[CreatedCallback]
  val eventCallback: Option[EventCallback]


  override def preStart(): X = {
    super.preStart()
    log.info(s"subscribing for events from ${parentNode.path.name}")
  }

  override def executionStarted(): X = {
    parentNode ! Subscribe()
  }

  override def receive: Receive = super.receive orElse {
    case event: Event if !sender().equals(parentNode) => {
      log.info(s"ParentNode ${parentNode.path} ReceivedFrom: ${sender().path}")
    }
    case TransitionRequest(algorithm) => {
      log.info(s"Asking ${parentNode.path.name} to transit to algorithm ${algorithm.algorithm.getClass}")
      parentNode ! TransitionRequest(algorithm)
    }
    case TransferredState(placementAlgo, successor) => {
      log.info("parent successfully migrated to the new node")

      if(sender().equals(parentNode)){
        parentNode = successor
      }else{
        log.error("unexpected state of the system")
      }

      handleTransitionRequest(placementAlgo)
    }
  }
}