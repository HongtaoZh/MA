package adaptivecep.graph.nodes.traits

import adaptivecep.graph.transition._
import adaptivecep.placement.benchmarking.PlacementAlgorithm
import adaptivecep.simulation.adaptive.cep.SystemLoad
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill}
import akka.cluster.Cluster
import com.twitter.chill.MeatLocker

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
  * Handling cases of RunRun Transition
  **/
trait RunRunMode extends Actor with TransitionMode with ActorLogging {

  @volatile var started: Boolean
  protected val delta = 10000l

  // val startTime: Long

  override def receive: Receive = {
    case StartExecution() => {
      started = true
    }

    case StartExecutionWithDependencies(subs, time) => {
      log.info("Starting Execution With Dependencies")

      this.subscribers ++= subs.get

      Future {
        Thread.sleep(time)
        started = true
      }
    }
  }

  def executeTransition(requester: ActorRef, algorithm: PlacementAlgorithm): Unit = {
    Future{
      val address = algorithm.algorithm.findOptimalNode(this.context, Cluster(context.system),
                                                        getParentNodes, coordinates)
      val successor = createDuplicateNode(address)

      log.info(s"asking for StartExecutionWithDependencies ${classOf[List[ActorRef]]}")
      successor ! StartExecutionWithDependencies(MeatLocker(subscribers.toList), delta + maxWindowTime()*1000)
      //TODO: find a better solution for this serializaiton problem. Meatlocker works fine but it would be great if we could solve this serialization issue by using kryo configuraiton

      Thread.sleep(delta + maxWindowTime()*1000)

      this.started = false

      log.info("updating requester about new successor")
      requester ! TransferredState(algorithm, successor)
      log.info(s"${self.path.name} Killing Self")
      SystemLoad.operatorRemoved()
      this.context.stop(self)
    }
  }
}

case class StartExecutionWithDependencies(subscribers: MeatLocker[List[ActorRef]], startTime: Long)
case class TransferredState(placementAlgo: PlacementAlgorithm, replacement: ActorRef)