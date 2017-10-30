package adaptivecep.simulation.trasitivecep

import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit

import adaptivecep.data.Queries.Query
import adaptivecep.dsl.Dsl._
import adaptivecep.placement.vivaldi.VivaldiCoordinates
import akka.actor.{ActorLogging, ActorRef, RootActorPath}
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.{Member, MemberStatus}
import akka.util.Timeout
import org.slf4j.LoggerFactory

import scala.concurrent.Await

class SimulationSetup (directory: Option[File]) extends VivaldiCoordinates with ActorLogging {

  val messageOverheadRequirement = overhead < 10 then Option.empty otherwise Option.empty
  var latencyMeasurement: Duration = _

  var publishers: Map[String, ActorRef] = Map.empty[String, ActorRef]
  var benchmarkingAppReady = false

  //used for finding Publisher nodes and Benchmark node
  override def currentClusterState(state: CurrentClusterState): Unit = {
    super.currentClusterState(state)
    state.members.filter(x => x.status == MemberStatus.Up && x.hasRole("Publisher")) foreach extractProducers
    if (state.members.exists(x => x.status == MemberStatus.Up && x.hasRole("Publisher"))) {
      benchmarkingAppReady = true
      log.info("found benchmark node")
      checkAndRunQuery()
    }
  }

  //used for finding Publisher nodes and Benchmark node
  override def memberUp(member: Member): Unit = {
    super.memberUp(member)
    if (member.hasRole("Publisher")) extractProducers(member)
    if (member.hasRole("BenchmarkingApp")) {
      log.info("found benchmark node")
      benchmarkingAppReady = true
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

  //If all publishers required in the query are available then run the simulation
  def checkAndRunQuery(): Unit = {
    if (Set("A", "B", "C").subsetOf(publishers.keySet) && benchmarkingAppReady) {
      executeSimulation()
    }else{
      log.info(s"not executing $publishers $benchmarkingAppReady")
    }
  }

  def executeSimulation(): Unit ={

    log.info("starting simulation")
    val lrPietzuch = RecordLatency()
    val moPietzuch = RecordMessageOverHead()
    val latencyRequirement = latency < timespan(1.milliseconds) then Some(lrPietzuch) otherwise Some(lrPietzuch)

    val query1: Query =
      stream[Int]("A")
        .join(
          stream[Int]("B"),
          slidingWindow(30.seconds),
          slidingWindow(30.seconds))
        .where(_ <= _)
        .dropElem1(latencyRequirement)

    val pietzuchSimulation = new Simulation("pietzuch", directory, query1, publishers, lrPietzuch, moPietzuch, this.context.system)
    pietzuchSimulation.startSimulation(20, 5, 200)(()=>log.info("Simulation Ended"))

    val messageOverheadRequirement = overhead < 10 then Option.empty otherwise Option.empty
    val lrStarks = RecordLatency()
    val moStarks = RecordMessageOverHead()

    val query2: Query =
      stream[Int]("A")
        .join(
          stream[Int]("B"),
          slidingWindow(30.seconds),
          slidingWindow(30.seconds))
        .where(_ <= _)
        .dropElem1(messageOverheadRequirement)

    val starksSimulation = new Simulation("starks", directory, query2, publishers, lrStarks, moStarks, this.context.system)
    starksSimulation.startSimulation(20, 5, 200)(()=>log.info("Starks Simulation Ended"))
  }
}


case class RecordLatency() extends LatencyMeasurement {
  var lastMeasurement: Option[Duration] = Option.empty

  override def apply(latency: Duration): Any = {
    lastMeasurement = Some(latency)
  }
}

case class RecordMessageOverHead() extends MessageOverheadMeasurement {
  var lastMeasurement: Option[Int] = Option.empty

  override def apply(overhead: Int): Any = {
    lastMeasurement = Some(overhead)
  }
}