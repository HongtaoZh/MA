package adaptivecep.graph.nodes

import java.util.UUID

import adaptivecep.data.Events._
import adaptivecep.data.Queries._
import adaptivecep.factories.NodeFactory
import adaptivecep.graph.nodes.traits.Mode._
import adaptivecep.graph.nodes.traits.Node.Subscribe
import adaptivecep.graph.nodes.traits._
import adaptivecep.graph.{CreatedCallback, EventCallback, QueryGraph}
import adaptivecep.publishers.Publisher._
import akka.actor.{ActorLogging, ActorRef, Address, Deploy, Props}
import akka.remote.RemoteScope

/**
  * Handling of [[adaptivecep.data.Queries.StreamQuery]] is done by StreamNode.
  *
  * @see [[QueryGraph]]
  **/

case class StreamNode( mode: Mode,
                       query: StreamQuery,
                       publisher: ActorRef,
                       createdCallback: Option[CreatedCallback],
                       eventCallback: Option[EventCallback]
                     )
  extends LeafNode with ActorLogging {

  override def executionStarted(): Unit = {
    log.info(s"subscribing for events from ${publisher.path.name}")
    publisher ! Subscribe()
  }

  override def childNodeReceive: Receive = super.childNodeReceive orElse {
    case DependenciesRequest => sender ! DependenciesResponse(Seq.empty)
    case AcknowledgeSubscription if sender().equals(publisher) => emitCreated()
    case event: Event if sender().equals(publisher) => emitEvent(event)
    case unhandledMessage => log.info(s"unhandled message $unhandledMessage by ${sender().path.name}")
  }

  def createDuplicateNode(address: Address): ActorRef = {
    NodeFactory.createStreamNode(mode, query, publisher, createdCallback, eventCallback, address, context)
  }

  def getParentNodes(): Seq[ActorRef] = Seq(publisher)
  def maxWindowTime(): Int =0
}

