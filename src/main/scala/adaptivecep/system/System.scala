package adaptivecep.system

import java.util.concurrent.atomic.AtomicInteger

import adaptivecep.data.Events
import adaptivecep.data.Events.{DependenciesRequest, DependenciesResponse, Event, _}
import adaptivecep.data.Queries._
import adaptivecep.graph.{CreatedCallback, EventCallback, QueryGraph}
import adaptivecep.graph.{CreatedCallback, EventCallback}
import adaptivecep.graph.qos._
import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.Cluster
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.runtime.Nothing$

object System {
  val index = new AtomicInteger(0)
}

class System(implicit actorSystem: ActorSystem) {
  private val roots = ListBuffer.empty[ActorRef]
  private val placements = mutable.Map.empty[ActorRef, Host] withDefaultValue NoHost

  def runQuery(query: Query, publishers: Map[String, ActorRef], createdCallback: Option[CreatedCallback], eventCallback: Option[EventCallback]) = {
    val monitorFactories: Array[MonitorFactory] = Array(AverageFrequencyMonitorFactory(interval = 15, query),
                                                        DummyMonitorFactory(query))
    val graphFactory = new QueryGraph(
                              this.actorSystem,
                              Cluster(actorSystem),
                              query,
                              publishers,
                              createdCallback, monitorFactories)

    roots += graphFactory.createAndStart()(eventCallback)
  }

  def consumers: Seq[Operator] = {
    import actorSystem.dispatcher
    implicit val timeout = Timeout(20.seconds)

    def operator(actorRef: ActorRef): Future[Operator] =
      actorRef ? DependenciesRequest flatMap {
        case DependenciesResponse(dependencies) =>
          Future sequence (dependencies map operator) map {
            ActorOperator(placements(actorRef), _, actorRef)
          }
      }

    Await result(Future sequence (roots map operator), timeout.duration)
  }

  def place(operator: Operator, host: Host) = {
    val ActorOperator(_, _, actorRef) = operator
    placements += actorRef -> host
  }

  private case class ActorOperator(host: Host, dependencies: Seq[Operator], actorRef: ActorRef) extends Operator

}
