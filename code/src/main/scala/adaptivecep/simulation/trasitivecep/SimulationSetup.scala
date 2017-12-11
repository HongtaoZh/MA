package adaptivecep.simulation.trasitivecep

import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit

import adaptivecep.data.Queries.Query
import adaptivecep.data.Structures.MachineLoad
import adaptivecep.dsl.Dsl._
import adaptivecep.placement.vivaldi.VivaldiCoordinates
import akka.actor.{ActorLogging, ActorRef, RootActorPath}
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.{Member, MemberStatus}
import akka.util.Timeout

import scala.concurrent.Await

class SimulationSetup(directory: Option[File]) extends VivaldiCoordinates with ActorLogging {

  val messageOverheadRequirement = overhead < 10 otherwise Option.empty
  var latencyMeasurement: Duration = _

  var publishers: Map[String, ActorRef] = Map.empty[String, ActorRef]
  var benchmarkingAppReady = false
  var vivaldiAppReady = false

  //used for finding Publisher nodes and Benchmark node
  override def currentClusterState(state: CurrentClusterState): Unit = {
    super.currentClusterState(state)
    state.members.filter(x => x.status == MemberStatus.Up && x.hasRole("Publisher")) foreach extractProducers
    if (state.members.exists(x => x.status == MemberStatus.Up && x.hasRole("BenchmarkingApp"))) {
      benchmarkingAppReady = true
      log.info("found benchmark node")
    } else if (state.members.exists(x => x.status == MemberStatus.Up && x.hasRole("VivaldiRef"))) {
      vivaldiAppReady = true
      log.info("found vivaldi node")
    }
    checkAndRunQuery()
  }

  //used for finding Publisher nodes and Benchmark node
  override def memberUp(member: Member): Unit = {
    super.memberUp(member)
    if (member.hasRole("Publisher")) extractProducers(member)
    if (member.hasRole("BenchmarkingApp")) {
      log.info("found benchmark node")
      benchmarkingAppReady = true
    } else if (member.hasRole("VivaldiRef")) {
      vivaldiAppReady = true
      log.info("found vivaldi node")
    } else {
      log.info(s"found member with role: ${member.roles}")
    }

    checkAndRunQuery()
  }

  def extractProducers(member: Member): Unit = {
    log.info("Found publisher node")
    implicit val resolveTimeout = Timeout(60, TimeUnit.SECONDS)
    val actorRef = Await.result(context.actorSelection(RootActorPath(member.address) / "user" / "*").resolveOne(), resolveTimeout.duration)
    publishers += (actorRef.path.name -> actorRef)
    log.info(s"saving publisher ${actorRef.path.name}")

    checkAndRunQuery()
  }

  //If all publishers required in the query are available then run the simulation
  def checkAndRunQuery(): Unit = {
    if (Set("A", "B", "C").subsetOf(publishers.keySet) && benchmarkingAppReady && vivaldiAppReady) {
      Thread.sleep(10000) //wait for all actors to be initialized
      executeSimulation()
    } else {
      log.info(s"not executing publishers: $publishers benchmarkApp: $benchmarkingAppReady vivApp: $vivaldiAppReady")
    }
  }

  def executePietzuchAlgorithm(callback: () => Any): Unit = {
    log.info("starting Pietzuch algorithm simulation")
    val recordLatency = RecordLatency()
    val recordMessageOverHead = RecordMessageOverHead()
    val recordLoad = RecordAverageLoad()
    val recordFrequency = RecordFrequency()
    val latencyRequirement = latency < timespan(1.milliseconds) otherwise Some(recordLatency)

    val query1: Query =
      stream[Int]("A")
        .join(
          stream[Int]("B"),
          slidingWindow(5.seconds),
          slidingWindow(5.seconds))
        .join(stream[Int]("C"),
          slidingWindow(10.seconds),
          slidingWindow(10.seconds))
        .selfJoin(
          slidingWindow(25.seconds),
          slidingWindow(25.seconds), latencyRequirement)

    val pietzuchSimulation = new Simulation("pietzuch", directory, query1, publishers, recordLoad, recordLatency, recordMessageOverHead, recordFrequency, this.context)
    pietzuchSimulation.startSimulation(20, 5, 400)(callback)
  }

  def executeStarksAlgorithm(callback: () => Any): Unit = {
    val messageOverheadRequirement = overhead < 10 otherwise Option.empty
    val recordLatency = RecordLatency()
    val recordMessageOverHead = RecordMessageOverHead()
    val recordAverageLoad = RecordAverageLoad()
    val recordFrequency = RecordFrequency()

    val query2 =
      stream[Int]("A")
        .join(
          stream[Int]("B"),
          slidingWindow(5.seconds),
          slidingWindow(5.seconds))
        .join(stream[Int]("C"),
          slidingWindow(10.seconds),
          slidingWindow(10.seconds))
        .selfJoin(
          slidingWindow(25.seconds),
          slidingWindow(25.seconds), messageOverheadRequirement)


    val starksSimulation = new Simulation("starks", directory, query2, publishers, recordAverageLoad, recordLatency, recordMessageOverHead, recordFrequency, this.context)
    starksSimulation.startSimulation(20, 5, 400)(callback)
  }

  def testRunRunTransition(callback: () => Any): Unit = {
    val recordLatency = RecordLatency()
    val recordMessageOverHead = RecordMessageOverHead()
    val recordAverageLoad = RecordAverageLoad()
    val recordFrequency = RecordFrequency()
    val latencyRequirement = latency < timespan(1.milliseconds) otherwise Some(recordLatency)
    /*

        stream[Int]("EnteredUsers")
              .join(
                stream[Int]("SantizedUsers"),
                slidingWindow(10.minutes),
                slidingWindow(10.minutes)
              )
              .where((enteredUsers: List[Int], sanitizedUsers: List[Int]) => {
                !sanitizedUsers.toSet.subsetOf(enteredUsers.toSet)
              })



        stream[Int]("EnteredUsers")
          .join(
            stream[Int]("SantizedUsers"),
            slidingWindow(10.minutes),
            slidingWindow(10.minutes)
          )
          .where((enteredUsers: List[Int], sanitizedUsers: List[Int]) => {
            !sanitizedUsers.toSet.subsetOf(enteredUsers.toSet)
          }, latency < timespan(1.milliseconds) otherwise Some(new LatencyMeasurement() {
            override def apply(latency: Duration): Any = println("unable to meet latency demand")
          }))



        stream[Int]("EnteredUsers")
          .join(
            stream[Int]("SantizedUsers"),
            slidingWindow(10.minutes),
            slidingWindow(10.minutes)
          )
          .where((enteredUsers: List[Int], sanitizedUsers: List[Int]) => {
            !sanitizedUsers.toSet.subsetOf(enteredUsers.toSet)
          }, overhead < 10 otherwise Some(new MessageOverheadMeasurement() {
            override def apply(overHead: Int): Any = println("Unable to meet message overhead Requirement")
          }))



        stream[Int]("EnteredUsers")
          .join(
            stream[Int]("SantizedUsers"),
            slidingWindow(10.minutes),
            slidingWindow(10.minutes)
          )
          .where((enteredUsers: List[Int], sanitizedUsers: List[Int]) => {
            !sanitizedUsers.toSet.subsetOf(enteredUsers.toSet)
          })
    */


    val query3: Query =
      stream[Int]("A")
        .join(
          stream[Int]("B"),
          slidingWindow(5.seconds),
          slidingWindow(5.seconds))
        .join(stream[Int]("C"),
          slidingWindow(5.seconds),
          slidingWindow(5.seconds),
          messageOverheadRequirement, frequency > Frequency(50, 5) otherwise Option.empty)



    val runrunSimulation = new Simulation("runrun", directory, query3, publishers, recordAverageLoad, recordLatency, recordMessageOverHead, recordFrequency, this.context)
    val graph = runrunSimulation.startSimulation(20, 5, 400)(callback)

    Thread.sleep(20000)
    log.info("changing demands")
    graph.removeDemand(messageOverheadRequirement)
    graph.addDemand(latencyRequirement)
  }

  def testFlipTransition(callback: () => Any): Unit = {
    val recordLatency = RecordLatency()
    val recordMessageOverHead = RecordMessageOverHead()
    val recordAverageLoad = RecordAverageLoad()
    val recordFrequency = RecordFrequency()
    val latencyRequirement = latency < timespan(1.milliseconds) otherwise Some(recordLatency)

    val query3: Query =
      stream[Int]("A")
        .join(
          stream[Int]("B"),
          slidingWindow(5.seconds),
          slidingWindow(5.seconds))
        .join(stream[Int]("C"), slidingWindow(5.seconds), slidingWindow(5.seconds), messageOverheadRequirement)

    val flipSim = new Simulation("flip", directory, query3, publishers, recordAverageLoad,
      recordLatency, recordMessageOverHead, recordFrequency, this.context)

    val graph = flipSim.startSimulation(20, 5, 400)(callback)

    Thread.sleep(20000)
    log.info("changing demands")
    graph.removeDemand(messageOverheadRequirement)
    graph.addDemand(latencyRequirement)
  }

  def executeSimulation(): Unit = {
    this.executePietzuchAlgorithm(() => {
      this.context.children.foreach(c => context.stop(c))
      log.info("Pietzuch algorithm Simulation Ended")
      Thread.sleep(10000)

      executeStarksAlgorithm(() => {
        log.info("Starks Simulation Ended")
        this.context.children.foreach(c => context.stop(c))
        Thread.sleep(10000)
        log.info("killing myself !")
        context.stop(self)
      })
    })

    /*this.executePietzuchAlgorithm(() => {
      this.context.children.foreach(c => context.stop(c))
      log.info("Pietzuch algorithm Simulation Ended")
      Thread.sleep(10000)


      executeStarksAlgorithm(() => {
        log.info("Starks Simulation Ended")
        this.context.children.foreach(c => context.stop(c))

        Thread.sleep(10000)
        testRunRunTransition(() => {
          this.context.children.foreach(c => context.stop(c))
          log.info("runrun transition ended")
          Thread.sleep(10000)

          testFlipTransition(() => {
            this.context.children.foreach(c => context.stop(c))
            log.info("flip transition ended")

            log.info("killing myself !")
            context.stop(self)
          })
        })
      })

    })*/
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

case class RecordAverageLoad() extends LoadMeasurement {
  var lastLoadMeasurement: Option[MachineLoad] = Option.empty

  def apply(load: MachineLoad): Any = {
    lastLoadMeasurement = Some(load)
  }
}

case class RecordFrequency() extends FrequencyMeasurement {
  var lastMeasurement: Option[Int] = Option.empty

  override def apply(frequency: Int): Any = {
    lastMeasurement = Some(frequency)
  }
}