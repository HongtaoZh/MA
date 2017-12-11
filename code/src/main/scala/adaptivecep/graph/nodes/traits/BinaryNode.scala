package adaptivecep.graph.nodes.traits

import java.util.concurrent.TimeUnit

import adaptivecep.data.Events._
import adaptivecep.data.Queries._
import adaptivecep.graph.nodes.traits.Node.{Subscribe, UpdatedAddress}
import adaptivecep.graph.transition.TransitionRequest
import adaptivecep.placement.benchmarking.PlacementAlgorithm
import akka.actor.ActorRef
import akka.util.Timeout

/**
  * Handling of [[adaptivecep.data.Queries.BinaryQuery]] is done by BinaryNode
  **/
trait BinaryNode extends Node {
  override val query: BinaryQuery

  @volatile var parentNode1: ActorRef
  @volatile var parentNode2: ActorRef

  private var parent1TransitInProgress = false
  private var parent2TransitInProgress = false
  private var transitionRequestor: ActorRef = _

  implicit val resolveTimeout = Timeout(60, TimeUnit.SECONDS)

  override def executionStarted(): Unit = {
    parentNode1 ! Subscribe()
    parentNode2 ! Subscribe()
  }

  override def childNodeReceive: Receive = {
    case event: Event if !sender().equals(parentNode1) && !sender().equals(parentNode2) => {
      log.info(s"${self.path.name}: ParentNodes ${parentNode1.path} ${parentNode2.path} ReceivedFrom: ${sender().path}")
    }
    //parent has transitted to a new node
    case TransferredState(algorithm, successor) => {
      log.info(s"${self.path.name}:parent successfully migrated to the new node")
      if (sender().equals(parentNode1)) {
        parentNode1 = successor
        parent1TransitInProgress = false
      } else if (sender().equals(parentNode2)) {
        parentNode2 = successor
        parent2TransitInProgress = false
      } else {
        log.error("unexpected state of the system")
      }

      if (!parent1TransitInProgress && !parent2TransitInProgress) {
        executeTransition(transitionRequestor, algorithm)
      }
    }
  }

  override def handleTransitionRequest(requester: ActorRef, algorithm: PlacementAlgorithm) = {
    log.info(s"Asking ${parentNode1.path.name} and ${parentNode2.path.name} to transit to new algorithm ${algorithm.algorithm.getClass}")
    transitionInitiated = true
    transitionRequestor = requester

    parentNode1 ! TransitionRequest(algorithm)
    parentNode2 ! TransitionRequest(algorithm)
    parent1TransitInProgress = true
    parent2TransitInProgress = true
  }
}
