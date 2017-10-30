package adaptivecep.machinenodes

import java.util.concurrent.TimeUnit

import adaptivecep.data.Events.{Event, Event1, Event3}
import adaptivecep.data.Queries._
import adaptivecep.data.Structures.MachineLoad
import adaptivecep.dsl.Dsl._
import adaptivecep.graph.qos._
import adaptivecep.graph.{CreatedCallback, EventCallback, QueryGraph}
import adaptivecep.placement.vivaldi.VivaldiCoordinates
import akka.actor.{ActorLogging, ActorRef, RootActorPath}
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.{Member, MemberStatus}
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}


/**
  * Subscriber Actor
  */
class Subscriber extends VivaldiCoordinates with ActorLogging {

  var publishers: Map[String, ActorRef] = Map.empty[String, ActorRef]
  var bechmarkingAppReady = false
  val actorSystem = this.context.system

  override def currentClusterState(state: CurrentClusterState): Unit = {
    super.currentClusterState(state)
    state.members.filter(x => x.status == MemberStatus.Up && x.hasRole("Publisher")) foreach extractProducers

    if (state.members.exists(x => x.status == MemberStatus.Up && x.hasRole("Publisher"))) {
      bechmarkingAppReady = true
      checkAndRunQuery()
    }
  }

  override def memberUp(member: Member): Unit = {
    super.memberUp(member)
    if (member.hasRole("Publisher")) extractProducers(member)
    if (member.hasRole("BenchmarkingApp")) {
      bechmarkingAppReady = true
      checkAndRunQuery()
    }
    else log.info(s"found: ${member.roles}")
  }

  def extractProducers(member: Member): Unit = {
    log.info("Found publisher node")
    implicit val resolveTimeout = Timeout(10, TimeUnit.SECONDS)
    val actorRef = Await.result(context.actorSelection(RootActorPath(member.address) / "user" / "*").resolveOne(), resolveTimeout.duration)
    publishers += (actorRef.path.name -> actorRef)
    log.info(s"saving publisher ${actorRef.path.name}")

    checkAndRunQuery()
  }

  def checkAndRunQuery(): Unit = {
    if (publishers.keySet == Set("A", "B", "C", "D") && bechmarkingAppReady) {
      runQuery()
    }
  }


  def runQuery(): Unit = {
    log.info("All publishers are available now, executing query")

    //TODO: define callbacks
    val latencyRequirement = latency < timespan(1.milliseconds) then Option.empty otherwise Option.empty
    val loadRequirement = load < MachineLoad(0.5) then Option.empty otherwise Option.empty
    val messageOverheadRequirement = overhead < 10 then Option.empty otherwise Option.empty

    val query1: Query3[Either[Int, String], Either[Int, X], Either[Float, X]] =
      stream[Int]("A")
        .join(
          stream[Int]("B"),
          slidingWindow(2.seconds),
          slidingWindow(5.seconds))
        .where(_ < _)
        .dropElem1(latencyRequirement)
        .selfJoin(
          tumblingWindow(1.seconds),
          tumblingWindow(10.seconds))
        .and(stream[Float]("C"))
        .or(stream[String]("D"))

    val monitors: Array[MonitorFactory] = Array(AverageFrequencyMonitorFactory(interval = 15, query1),
                                                DummyMonitorFactory(query1))

    val queryGraph = new QueryGraph(this.context.system,
                                    cluster, query1,
                                    publishers,
                                    Some(GraphCreatedCallback()),
                                    monitors)

    queryGraph.createAndStart()(Some(EventPublishedCallback()))

    //change in requirements after 2 minutes
    val f = Future {
      Thread.sleep(2 * 60 * 1000)
    }

    f.onComplete(_ => {
      queryGraph.removeDemand(latencyRequirement)
      queryGraph.addDemand(messageOverheadRequirement)
    })
  }
}


//Using Named classes for callbacks instead of anonymous ones due to serialization issues
case class GraphCreatedCallback() extends CreatedCallback {
  override def apply(): Any = {
    println("STATUS:\t\tGraph has been created.")
  }
}

case class EventPublishedCallback() extends EventCallback {
  override def apply(event: Event): Any = event match {
    case Event3(i1, i2, f) => println(s"COMPLEX EVENT:\tEvent3($i1,$i2,$f)")
    case Event1(s) => println(s"COMPLEX EVENT:\tEvent1($s)")
    case _ =>
  }
}

/**
  * List of Akka Messages which is being used by Subscriber actor.
  **/
object SubscriberMessages {

  case object SubmitQuery

  //case class SubmitQuery(q: Query)
}
