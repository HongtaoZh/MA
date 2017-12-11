package adaptivecep.factories

import java.util.UUID

import adaptivecep.data.Queries._
import adaptivecep.graph.nodes._
import adaptivecep.graph.nodes.traits.Mode
import adaptivecep.graph.nodes.traits.Mode._
import adaptivecep.graph.{CreatedCallback, EventCallback}
import akka.actor.{ActorContext, ActorRef, Address, Deploy, Props}
import akka.remote.RemoteScope

object NodeFactory {

  def createConjunctionNode(mode: Mode.Mode,
                            query: ConjunctionQuery,
                            parentNode1: ActorRef,
                            parentNode2: ActorRef,
                            createdCallback: Option[CreatedCallback],
                            eventCallback: Option[EventCallback],
                            address: Address, context: ActorContext): ActorRef = {
    context.actorOf(Props(
      ConjunctionNode(
        mode,
        query,
        parentNode1,
        parentNode2,
        createdCallback,
        eventCallback
      )).withDeploy(Deploy(scope = RemoteScope(address))), s"ConjunctionNode${UUID.randomUUID.toString}"
    )
  }

  def createDisjuctionNode(mode: Mode.Mode,
                           query: DisjunctionQuery,
                           parentNode1: ActorRef,
                           parentNode2: ActorRef,
                           createdCallback: Option[CreatedCallback],
                           eventCallback: Option[EventCallback],
                           address: Address, context: ActorContext): ActorRef = {

    context.system.actorOf(Props(
      DisjunctionNode(
        mode,
        query,
        parentNode1,
        parentNode2,
        createdCallback,
        eventCallback
      )).withDeploy(Deploy(scope = RemoteScope(address))), s"DisjunctionNode${UUID.randomUUID.toString}"
    )
  }

  def createDropElementNode(mode: Mode.Mode,
                            query: DropElemQuery,
                            parentNode: ActorRef,
                            createdCallback: Option[CreatedCallback],
                            eventCallback: Option[EventCallback],
                            address: Address, context: ActorContext): ActorRef = {
    context.system.actorOf(Props(
      DropElemNode(
        mode,
        query,
        parentNode,
        createdCallback,
        eventCallback
      )).withDeploy(Deploy(scope = RemoteScope(address))), s"DropElemNode${UUID.randomUUID.toString}"
    )
  }

  def createFilterNode(mode: Mode.Mode,
                       query: FilterQuery,
                       parentNode: ActorRef,
                       createdCallback: Option[CreatedCallback],
                       eventCallback: Option[EventCallback],
                       address: Address, context: ActorContext): ActorRef = {
    context.system.actorOf(Props(
      FilterNode(
        mode,
        query,
        parentNode,
        createdCallback,
        eventCallback
      )).withDeploy(Deploy(scope = RemoteScope(address))), s"FilterNode${UUID.randomUUID.toString}"
    )
  }

  def createJoinNode(mode: Mode,
                     query: JoinQuery,
                     parentNode1: ActorRef,
                     parentNode2: ActorRef,
                     createdCallback: Option[CreatedCallback],
                     eventCallback: Option[EventCallback],
                     address: Address, context: ActorContext): ActorRef = {

    context.system.actorOf(Props(
      JoinNode(
        mode,
        query,
        parentNode1,
        parentNode2,
        createdCallback,
        eventCallback
      )).withDeploy(Deploy(scope = RemoteScope(address))), s"JoinNode${UUID.randomUUID.toString}"
    )
  }

  def createSelfJoinNode(mode: Mode,
                         query: SelfJoinQuery,
                         parentNode: ActorRef,
                         createdCallback: Option[CreatedCallback],
                         eventCallback: Option[EventCallback],
                         address: Address, context: ActorContext): ActorRef = {

    context.system.actorOf(Props(
      SelfJoinNode(
        mode,
        query,
        parentNode,
        createdCallback,
        eventCallback
      )).withDeploy(Deploy(scope = RemoteScope(address))), s"SelfJoinNode${UUID.randomUUID.toString}"
    )
  }

  def createSequenceNode(mode: Mode,
                       query: SequenceQuery,
                       publishers: Seq[ActorRef],
                       createdCallback: Option[CreatedCallback],
                       eventCallback: Option[EventCallback],
                       address: Address, context: ActorContext): ActorRef = {

    context.system.actorOf(Props(
      SequenceNode(
        mode,
        query,
        publishers,
        createdCallback,
        eventCallback
      )).withDeploy(Deploy(scope = RemoteScope(address))),s"SequenceNode${UUID.randomUUID.toString}"
    )
  }

  def createStreamNode( mode: Mode,
                        query: StreamQuery,
                        publisher: ActorRef,
                        createdCallback: Option[CreatedCallback],
                        eventCallback: Option[EventCallback],
                        address: Address, context: ActorContext): ActorRef = {

    context.system.actorOf(Props(
      StreamNode(
        mode,
        query,
        publisher,
        createdCallback,
        eventCallback
      )).withDeploy(Deploy(scope = RemoteScope(address))), s"StreamNode${UUID.randomUUID.toString}"
    )
  }
}
