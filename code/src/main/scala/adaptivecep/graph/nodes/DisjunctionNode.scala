package adaptivecep.graph.nodes

import java.util.UUID

import adaptivecep.data.Events._
import adaptivecep.data.Queries._
import adaptivecep.factories.NodeFactory
import adaptivecep.graph.nodes.traits._
import adaptivecep.graph.{CreatedCallback, EventCallback, QueryGraph}
import akka.actor.{ActorLogging, ActorRef, Address, Deploy, Props}
import akka.remote.RemoteScope
/**
  * Handling of [[adaptivecep.data.Queries.DisjunctionQuery]] is done by DisjunctionNode.
  *
  * @see [[QueryGraph]]
  * */


case class DisjunctionNode( mode: Mode.Mode,
                            query: DisjunctionQuery,
                            @volatile var parentNode1: ActorRef,
                            @volatile var parentNode2: ActorRef,
                            createdCallback: Option[CreatedCallback],
                            eventCallback: Option[EventCallback])
  extends BinaryNode with ActorLogging {

  var childNode1Created: Boolean = false
  var childNode2Created: Boolean = false

  def fillArray(desiredLength: Int, array: Array[Either[Any, Any]]): Array[Either[Any, Any]] = {
    require(array.length <= desiredLength)
    require(array.length > 0)
    val unit: Either[Unit, Unit] = array(0) match {
      case Left(_) => Left(())
      case Right(_) => Right(())
    }
    (0 until desiredLength).map(i => {
      if (i < array.length) {
        array(i)
      } else {
        unit
      }
    }).toArray
  }

  def handleEvent(array: Array[Either[Any, Any]]): Unit = query match {
    case _: Query1[_] =>
      val filledArray: Array[Either[Any, Any]] = fillArray(1, array)
      emitEvent(Event1(filledArray(0)))
    case _: Query2[_, _] =>
      val filledArray: Array[Either[Any, Any]] = fillArray(2, array)
      emitEvent(Event2(filledArray(0), filledArray(1)))
    case _: Query3[_, _, _] =>
      val filledArray: Array[Either[Any, Any]] = fillArray(3, array)
      emitEvent(Event3(filledArray(0), filledArray(1), filledArray(2)))
    case _: Query4[_, _, _, _] =>
      val filledArray: Array[Either[Any, Any]] = fillArray(4, array)
      emitEvent(Event4(filledArray(0), filledArray(1), filledArray(2), filledArray(3)))
    case _: Query5[_, _, _, _, _] =>
      val filledArray: Array[Either[Any, Any]] = fillArray(5, array)
      emitEvent(Event5(filledArray(0), filledArray(1), filledArray(2), filledArray(3), filledArray(4)))
    case _: Query6[_, _, _, _, _, _] =>
      val filledArray: Array[Either[Any, Any]] = fillArray(6, array)
      emitEvent(Event6(filledArray(0), filledArray(1), filledArray(2), filledArray(3), filledArray(4), filledArray(5)))
  }

  override def childNodeReceive: Receive = super.childNodeReceive orElse {
    case DependenciesRequest =>
      sender ! DependenciesResponse(Seq(parentNode1, parentNode2))
    case Created if sender().equals(parentNode1) =>
      childNode1Created = true
      if (childNode2Created) emitCreated()
    case Created if sender() == parentNode2 =>
      childNode2Created = true
      if (childNode1Created) emitCreated()
    case event: Event if sender().equals(parentNode1) => event match {
      case Event1(e1) => handleEvent(Array(Left(e1)))
      case Event2(e1, e2) => handleEvent(Array(Left(e1), Left(e2)))
      case Event3(e1, e2, e3) => handleEvent(Array(Left(e1), Left(e2), Left(e3)))
      case Event4(e1, e2, e3, e4) => handleEvent(Array(Left(e1), Left(e2), Left(e3), Left(e4)))
      case Event5(e1, e2, e3, e4, e5) => handleEvent(Array(Left(e1), Left(e2), Left(e3), Left(e4), Left(e5)))
      case Event6(e1, e2, e3, e4, e5, e6) => handleEvent(Array(Left(e1), Left(e2), Left(e3), Left(e4), Left(e5), Left(e6)))
    }
    case event: Event if sender().equals(parentNode2) => event match {
      case Event1(e1) => handleEvent(Array(Right(e1)))
      case Event2(e1, e2) => handleEvent(Array(Right(e1), Right(e2)))
      case Event3(e1, e2, e3) => handleEvent(Array(Right(e1), Right(e2), Right(e3)))
      case Event4(e1, e2, e3, e4) => handleEvent(Array(Right(e1), Right(e2), Right(e3), Right(e4)))
      case Event5(e1, e2, e3, e4, e5) => handleEvent(Array(Right(e1), Right(e2), Right(e3), Right(e4), Right(e5)))
      case Event6(e1, e2, e3, e4, e5, e6) => handleEvent(Array(Right(e1), Right(e2), Right(e3), Right(e4), Right(e5), Right(e6)))
    }
    case unhandledMessage => log.info(s"unhandled message $unhandledMessage")
  }

  def createDuplicateNode(address: Address): ActorRef = {
    NodeFactory.createDisjuctionNode(mode, query, parentNode1, parentNode2, createdCallback, eventCallback, address, context)
  }

  override def getParentNodes: Seq[ActorRef] = Seq(parentNode1, parentNode2)
  def maxWindowTime(): Int =0
}
