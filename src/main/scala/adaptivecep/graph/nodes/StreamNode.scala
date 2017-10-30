package adaptivecep.graph.nodes

import java.util.UUID

import adaptivecep.data.Events._
import adaptivecep.data.Queries._
import adaptivecep.graph.nodes.traits.Mode._
import adaptivecep.graph.nodes.traits.Node.Subscribe
import adaptivecep.graph.nodes.traits._
import adaptivecep.graph.qos._
import adaptivecep.graph.transition.TransitionRequest
import adaptivecep.graph.{CreatedCallback, EventCallback, QueryGraph}
import adaptivecep.placement.benchmarking.PlacementAlgorithm
import adaptivecep.publishers.Publisher._
import akka.actor.{ActorLogging, ActorRef, Deploy, PoisonPill, Props}
import akka.remote.RemoteScope

import scala.concurrent.Future

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

  override def receive: Receive = super.receive orElse {
    case DependenciesRequest => sender ! DependenciesResponse(Seq.empty)
    case AcknowledgeSubscription if sender().equals(publisher) => emitCreated()
    case event: Event if sender().equals(publisher) => emitEvent(event)
    case unhandledMessage => log.info(s"unhandled message $unhandledMessage")
  }


  def handleTransitionRequest(algorithm: PlacementAlgorithm): Unit = {
    log.info("initiating runrun trnansition on StreamNode")

    val requester = sender()

    Future {
      //Don't block normal execution of this actor
      val address = algorithm.algorithm.findOptimalNode(this.context.system, cluster, Seq(publisher), coordinates)
      val successor = context.actorOf(Props(
        StreamNode(
          mode,
          query,
          publisher,
          createdCallback,
          eventCallback
        )).withDeploy(Deploy(scope = RemoteScope(address))), s"StreamNode${UUID.randomUUID.toString}"
      )

      successor ! StartExecutionWithDependencies(subscribers, delta)
      Thread.sleep(delta)
      this.started = false
      requester ! TransferredState(algorithm, successor)
      log.info("Killing Self")
      self ! PoisonPill
    }
  }
}
