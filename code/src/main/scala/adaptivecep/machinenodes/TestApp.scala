package adaptivecep.machinenodes

import adaptivecep.data.Queries._
import adaptivecep.dsl.Dsl.{Frequency, _}
import adaptivecep.graph.nodes.traits.Mode
import adaptivecep.graph.nodes.traits.Mode._
import adaptivecep.placement.vivaldi.{CoordinatesRequest, Ping}
import adaptivecep.simulation.trasitivecep.{RecordAverageLoad, RecordFrequency, RecordLatency, RecordMessageOverHead}
import akka.actor.Actor.Receive
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberEvent, MemberUp}

/**
  * Created by mac on 03/11/2017.
  */
object TestApp extends App {
  val recordLatency = RecordLatency()
  val recordMessageOverHead = RecordMessageOverHead()
  val recordAverageLoad = RecordAverageLoad()
  val recordFrequency = RecordFrequency()
  val latencyRequirement = latency < timespan(1.milliseconds) otherwise Some(recordLatency)
  val messageOverheadRequirement = overhead < 10 otherwise Option.empty

  val runrunquery: Query =
    stream[Int]("A")
      .join(
        stream[Int]("B"),
        slidingWindow(5.seconds),
        slidingWindow(5.seconds))
      .join(stream[Int]("C"),
        slidingWindow(5.seconds),
        slidingWindow(5.seconds),
        messageOverheadRequirement, frequency > Frequency(50, 5) otherwise Option.empty)



  val flipquery: Query =
    stream[Int]("A")
      .join(
        stream[Int]("B"),
        slidingWindow(5.seconds),
        slidingWindow(5.seconds))
      .join(stream[Int]("C"), slidingWindow(5.seconds), slidingWindow(5.seconds), messageOverheadRequirement)


  def getTransitionMode(query: Query): Mode = {
    def frequencyReqDefined(query: Query):Boolean = {
      query.requirements.collect { case lr: FrequencyRequirement => lr }.nonEmpty
    }

    def isRunRun(q: Query): Boolean = q match {
      case leafQuery: LeafQuery => frequencyReqDefined(query)
      case unaryQuery: UnaryQuery => frequencyReqDefined(query) || isRunRun(unaryQuery.sq)
      case binaryQuery: BinaryQuery => frequencyReqDefined(query) || isRunRun(binaryQuery.sq1) || isRunRun(binaryQuery.sq2)
    }
    if(isRunRun(query)) Mode.RUN_RUN else Mode.FLIP
  }

  println(s"Mode of flipquery is ${getTransitionMode(flipquery)}")
  println(s"Mode of runrunquery is ${getTransitionMode(runrunquery)}")
}
