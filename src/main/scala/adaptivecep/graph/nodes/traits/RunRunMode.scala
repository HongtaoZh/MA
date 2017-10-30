package adaptivecep.graph.nodes.traits

import adaptivecep.data.Queries.Window
import adaptivecep.graph.transition._
import adaptivecep.placement.PlacementStrategy
import adaptivecep.placement.benchmarking.PlacementAlgorithm
import akka.actor.{Actor, ActorLogging, ActorRef}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Handling cases of RunRun Transition
  **/
trait RunRunMode extends Actor with ActorLogging {

  val subscribers: ListBuffer[ActorRef]
  var started: Boolean
  protected val delta = 5000l

  // val startTime: Long

  override def receive: Receive = {
    case StartExecutionWithDependencies(subs, time) => {
      this.subscribers ++= subs

      Future{
        Thread.sleep(time - System.currentTimeMillis())
        started = true
      }
    }

    case StartExecution() => {
      started = true
    }
  }

  def executionStarted()
}

case class StartExecutionWithDependencies(subscribers: Seq[ActorRef], startTime: Long)
case class TransferredState(placementAlgo: PlacementAlgorithm, replacement: ActorRef)