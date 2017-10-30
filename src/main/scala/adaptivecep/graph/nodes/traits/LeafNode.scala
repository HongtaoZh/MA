package adaptivecep.graph.nodes.traits

import java.util.UUID

import adaptivecep.data.Events._
import adaptivecep.data.Queries.{LeafQuery, SequenceQuery}
import adaptivecep.graph.{CreatedCallback, EventCallback}
import adaptivecep.graph.EventCallback
import adaptivecep.graph.nodes.SequenceNode
import adaptivecep.graph.qos._
import adaptivecep.graph.transition.TransitionRequest
import adaptivecep.placement.benchmarking.PlacementAlgorithm
import adaptivecep.publishers.Publisher.AcknowledgeSubscription
import akka.actor.{ActorRef, Deploy, Props}
import akka.remote.RemoteScope

import scala.concurrent.Future

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

  override def receive: Receive = super.receive orElse {
    case TransitionRequest(algorithm) => handleTransitionRequest(algorithm)
  }

  override def emitEvent(event: Event): Unit = {
    super.emitEvent(event)
  }
}
