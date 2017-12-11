package adaptivecep.graph.nodes.traits

import akka.actor.{ActorRef, Address}

import scala.collection.mutable.ListBuffer

/**
  * Common methods for different transition modes
  */
trait TransitionMode {
  var coordinates: Long
  val subscribers: ListBuffer[ActorRef]

  def createDuplicateNode(address: Address): ActorRef
  def executionStarted()
  def getParentNodes: Seq[ActorRef]
  def maxWindowTime(): Int

}
