package adaptivecep.graph.nodes.traits

import java.util.concurrent.TimeUnit

import adaptivecep.data.Events._
import adaptivecep.data.Queries._
import adaptivecep.graph.nodes.traits.Node.Subscribe
import adaptivecep.graph.qos._
import adaptivecep.graph.transition.TransitionRequest
import adaptivecep.graph.{CreatedCallback, EventCallback}
import akka.actor.ActorRef
import akka.util.Timeout

/**
  * Handling of [[adaptivecep.data.Queries.BinaryQuery]] is done by BinaryNode
  **/
trait BinaryNode extends Node {
  override val query: BinaryQuery

  var parentNode1: ActorRef
  var parentNode2: ActorRef

  private var parent1TransitInProgress = false
  private var parent2TransitInProgress = false

  implicit val resolveTimeout = Timeout(10, TimeUnit.SECONDS)

  override def executionStarted(): Unit = {
    parentNode1 ! Subscribe()
    parentNode2 ! Subscribe()
  }

  override def receive: Receive = super.receive orElse {
    case event: Event if !sender().equals(parentNode1) && !sender().equals(parentNode2) =>{
      log.info(s"ParentNodes ${parentNode1.path} ${parentNode2.path} ReceivedFrom: ${sender().path}")
    }
    case TransitionRequest(algorithm) => {
      log.info(s"Asking ${parentNode1.path.name} and ${parentNode2.path.name} to transit to new algorithm ${algorithm.algorithm.getClass}")
      transitionInitiated = true
      parentNode1 ! TransitionRequest(algorithm)
      parentNode2 ! TransitionRequest(algorithm)
      parent1TransitInProgress = true
      parent2TransitInProgress = true
    }

    //parent has transitted to a new node
    case TransferredState(algorithm, successor) => {
        log.info("parent successfully migrated to the new node")
        if(sender().equals(parentNode1)){
          parentNode1 = successor
          parent1TransitInProgress = false
        }else if(sender().equals(parentNode2)){
          parentNode2 = successor
          parent2TransitInProgress = false
        }else{
          log.error("unexpected state of the system")
        }

      if(!parent1TransitInProgress && !parent2TransitInProgress){
        handleTransitionRequest(algorithm)
      }
    }
  }
}
