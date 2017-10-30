package adaptivecep.graph.nodes.traits

import adaptivecep.graph.transition._
import akka.actor.{Actor, ActorLogging, PoisonPill}

import scala.collection.mutable.ListBuffer

/**
  * Handling Cases of Flip Transition
  **/
trait FlipMode extends Actor with ActorLogging {

  val messageQueue = ListBuffer[Any]()
  var started: Boolean

  override def receive: Receive = {

    case StartExecution() => {
      log.info("starting execution")
      started = true
      messageQueue.foreach(m => self ! m)
      executionStarted()
    }

    case StopExecution() => {
      started = false
    }

    case SaveStateAndStartExecution(state) => {
      log.info("starting execution from saved state")

      started = true
      messageQueue ++= state

      messageQueue.foreach(m => self ! m)
      executionStarted()
    }

    case TransferState(successor) => {
      successor ! SaveStateAndStartExecution(messageQueue.toList)
      self ! PoisonPill
    }

    case anyMessage if !started => {
      log.info("Caching messages!")
      messageQueue += anyMessage
    }
  }

  def executionStarted()
}


