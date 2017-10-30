package adaptivecep.graph.nodes

import java.util.UUID

import akka.actor.{ActorRef, Deploy, PoisonPill, Props}
import adaptivecep.data.Events._
import adaptivecep.data.Queries._
import adaptivecep.graph.nodes.traits.Mode._
import adaptivecep.graph.{CreatedCallback, EventCallback, QueryGraph}
import adaptivecep.graph.{CreatedCallback, EventCallback}
import adaptivecep.graph.nodes.traits._
import adaptivecep.graph.qos._
import adaptivecep.graph.transition.{MAPEK, TransitionRequest}
import adaptivecep.placement.benchmarking.PlacementAlgorithm
import akka.cluster.ClusterEvent.{MemberEvent, MemberUp}
import akka.remote.RemoteScope

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

/**
  * Handling of [[adaptivecep.data.Queries.FilterQuery]] is done by FilterNode.
  *
  * @see [[QueryGraph]]
  * */

case class FilterNode( mode: Mode.Mode,
                       query: FilterQuery,
                       var parentNode: ActorRef,
                       createdCallback: Option[CreatedCallback],
                       eventCallback: Option[EventCallback]
)
  extends UnaryNode {

  override def receive: Receive = super.receive orElse {
    case DependenciesRequest => sender ! DependenciesResponse(Seq(parentNode))
    case Created if sender().equals(parentNode) => emitCreated()
    case event: Event if sender().equals(parentNode)=> if (query.cond(event)) emitEvent(event)
    case unhandledMessage => log.info(s"unhandled message $unhandledMessage")
  }

  def handleTransitionRequest(algorithm: PlacementAlgorithm): Unit = {
    log.info("initiating runrun trnansition on FilterNode")
    val requester = sender()

    Future {
      //Don't block normal execution of this actor
      val address = algorithm.algorithm.findOptimalNode(this.context.system, cluster, Seq(parentNode), coordinates)
      val successor = context.actorOf(Props(
        FilterNode(
          mode,
          query,
          parentNode,
          createdCallback,
          eventCallback
        )).withDeploy(Deploy(scope = RemoteScope(address))), s"FilterNode${UUID.randomUUID.toString}"
      )

      successor ! StartExecutionWithDependencies(subscribers, delta)
      Thread.sleep(delta)
      this.started = false
      requester ! TransferredState(algorithm, successor)
      self ! PoisonPill
    }
  }
}
