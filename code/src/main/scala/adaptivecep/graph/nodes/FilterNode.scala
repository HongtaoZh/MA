package adaptivecep.graph.nodes

import java.util.UUID

import adaptivecep.data.Events._
import adaptivecep.data.Queries._
import adaptivecep.factories.NodeFactory
import adaptivecep.graph.nodes.traits._
import adaptivecep.graph.{CreatedCallback, EventCallback, QueryGraph}
import akka.actor.{ActorRef, Address, Deploy, Props}
import akka.remote.RemoteScope

/**
  * Handling of [[adaptivecep.data.Queries.FilterQuery]] is done by FilterNode.
  *
  * @see [[QueryGraph]]
  **/

case class FilterNode(mode: Mode.Mode,
                      query: FilterQuery,
                      @volatile var parentNode: ActorRef,
                      createdCallback: Option[CreatedCallback],
                      eventCallback: Option[EventCallback]
                     )
  extends UnaryNode {

  override def childNodeReceive: Receive = super.childNodeReceive orElse {
    case DependenciesRequest => sender ! DependenciesResponse(Seq(parentNode))
    case Created if sender().equals(parentNode) => emitCreated()
    case event: Event if sender().equals(parentNode) => if (query.cond(event)) emitEvent(event)
    case unhandledMessage => log.info(s"unhandled message $unhandledMessage")
  }

  def createDuplicateNode(address: Address): ActorRef = {
    NodeFactory.createFilterNode(mode, query, parentNode, createdCallback, eventCallback, address, context)
  }

  override def getParentNodes: Seq[ActorRef] = Seq(parentNode)
  def maxWindowTime(): Int =0
}

