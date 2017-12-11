package adaptivecep.graph.nodes.traits

import java.util.concurrent.TimeUnit

import adaptivecep.data.Events.Event
import adaptivecep.graph.nodes.traits.Node.Subscribe
import adaptivecep.graph.transition._
import adaptivecep.placement.benchmarking.PlacementAlgorithm
import adaptivecep.simulation.adaptive.cep.SystemLoad
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.Cluster

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
  * Handling Cases of Flip Transition
  **/
trait FlipMode extends Actor with TransitionMode with ActorLogging {

  val slidingMessageQueue = {
    ListBuffer[(ActorRef, Any)]()
  }
  @volatile var started: Boolean

  // Notice I'm using `PartialFunction[Any,Any]` which is different from akka's default
  // receive partial function which is `PartialFunction[Any,Unit]`
  // because I needed to `cache` some messages before forwarding them to the child nodes
  // @see the receive method in Node trait for more details.

  def flipReceive: PartialFunction[Any, Any] = {
    case Subscribe() => {
      log.info(s"${sender().path.name} subscribed for ${this.self.path.name}")
      subscribers += sender()
      Unit //Nothing to forward for childReceive
    }
    case TransferredState(algorithm, successor) => {
      log.info("parent successfully migrated to a new node")
      TransferredState(algorithm, successor) //childReceive will handle this message
    }
    case TransitionRequest(algorithm) => {
      log.info("received transition request")
      handleTransitionRequest(sender(), algorithm)
      Unit //Nothing to forward for childReceive
    }

    case StartExecution() => {
      started = true
      executionStarted()
      Unit //Nothing to forward for childReceive
    }

    case StartExecutionWithData(subs, data) => {
      log.info("starting execution with data")
      this.started = true
      this.subscribers ++= subs
      data.foreach(m => self.!(m._2)(m._1))
      executionStarted()
      Unit //Nothing to forward for childReceive
    }

    case message: Event => {
      slidingMessageQueue += Tuple2(sender(), message)
      message //caching message and forwarding to the childReceive
    }
  }

  def messageQueueManager() = {
    if (maxWindowTime() != 0) {
      context.system.scheduler.schedule(
        FiniteDuration(maxWindowTime(), TimeUnit.SECONDS),
        FiniteDuration(maxWindowTime(), TimeUnit.SECONDS), new Runnable {
          override def run(): Unit = {
            slidingMessageQueue.clear()
          }
        }
      )
    }
  }

  def executeTransition(requester: ActorRef, algorithm: PlacementAlgorithm): Unit = {
    Future {
      log.info("executing transition")

      val address = algorithm.algorithm.findOptimalNode(this.context, Cluster(context.system),
        getParentNodes, coordinates)

      val successor = createDuplicateNode(address)
      this.started = false

      Thread.sleep(3000) //wait for new actor initalization
      log.info("asking for StartExecutionWithData")
      successor ! StartExecutionWithData(subscribers.toSet, slidingMessageQueue.toSet)


      log.info("updating requester")
      requester ! TransferredState(algorithm, successor)
      log.info(s"${self.path.name} Killing Self")
      SystemLoad.operatorRemoved()

      context.stop(self)
    }
  }

  def executionStarted()

  def handleTransitionRequest(requester: ActorRef, algorithm: PlacementAlgorithm)
}

case class StartExecutionWithData(subscribers: Set[ActorRef], data: Set[Tuple2[ActorRef, Any]])