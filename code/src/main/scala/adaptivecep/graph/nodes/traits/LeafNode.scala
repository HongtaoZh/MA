package adaptivecep.graph.nodes.traits

import adaptivecep.data.Events._
import adaptivecep.data.Queries.LeafQuery
import adaptivecep.graph.transition.TransitionRequest
import adaptivecep.graph.{CreatedCallback, EventCallback}
import adaptivecep.placement.benchmarking.PlacementAlgorithm
import akka.actor.ActorRef

/**
  * Handling of [[adaptivecep.data.Queries.LeafQuery]] is done by LeafNode
  * */
trait LeafNode extends Node {

  val createdCallback: Option[CreatedCallback]
  val eventCallback: Option[EventCallback]
  val query: LeafQuery

  override def emitCreated(): Unit = {
    super.emitCreated()
  }

  override def childNodeReceive: Receive = {
    case TransitionRequest(algorithm) => handleTransitionRequest(sender(), algorithm)
  }

  override def emitEvent(event: Event): Unit = {
    super.emitEvent(event)
  }

  override def handleTransitionRequest(requester: ActorRef, algorithm: PlacementAlgorithm) = {
    executeTransition(requester, algorithm)
  }
}
