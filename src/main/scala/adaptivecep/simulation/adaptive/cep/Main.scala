package adaptivecep.simulation.adaptive.cep

import adaptivecep.data.Events._
import adaptivecep.data.Queries._
import adaptivecep.data.Structures.MachineLoad
import adaptivecep.dsl.Dsl._
import adaptivecep.graph.qos._
import adaptivecep.graph.{CreatedCallback, QueryGraph}
import adaptivecep.publishers._
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.Cluster

object Main extends App {

  val actorSystem: ActorSystem = ActorSystem()

  val publisherA: ActorRef = actorSystem.actorOf(Props(RandomPublisher(id => Event1(id))),              "A")
  val publisherB: ActorRef = actorSystem.actorOf(Props(RandomPublisher(id => Event1(id * 2))),          "B")
  val publisherC: ActorRef = actorSystem.actorOf(Props(RandomPublisher(id => Event1(id.toFloat))),      "C")
  val publisherD: ActorRef = actorSystem.actorOf(Props(RandomPublisher(id => Event1(s"String($id)"))),  "D")

  val publishers: Map[String, ActorRef] = Map(
                                              "A" -> publisherA,
                                              "B" -> publisherB,
                                              "C" -> publisherC,
                                              "D" -> publisherD)

  val query1: Query3[Either[Int, String], Either[Int, X], Either[Float, X]] =
    stream[Int]("A")
      .join(
        stream[Int]("B"),
        slidingWindow(2.seconds),
        slidingWindow(2.seconds))
      .where(_ < _)
      .dropElem1(
        latency < timespan(1.milliseconds) then Option.empty otherwise Option.empty)
      .selfJoin(
        tumblingWindow(1.seconds),
        tumblingWindow(1.seconds),
        frequency > ratio(3.instances, 5.seconds) otherwise { (nodeData) => println(s"PROBLEM:\tNode `${nodeData}` emits too few events!") },
        frequency < ratio(12.instances, 15.seconds) otherwise { (nodeData) => println(s"PROBLEM:\tNode `${nodeData}` emits too many events!") })
      .and(stream[Float]("C"))
      .or(stream[String]("D"))

  val query2: Query4[Int, Int, Float, String] =
    stream[Int]("A")
      .and(stream[Int]("B"))
      .join(
        sequence(
          nStream[Float]("C") -> nStream[String]("D"),
          frequency > ratio(1.instances, 5.seconds) otherwise { (nodeData) => println(s"PROBLEM:\tNode `${nodeData}` emits too few events!") }),
        slidingWindow(3.seconds),
        slidingWindow(3.seconds),
        overhead < 10 then Option.empty otherwise Option.empty,
        load < MachineLoad.make(0.4) then Option.empty otherwise Option.empty,
        latency < timespan(20.seconds) then Option.empty otherwise Option.empty)


  val monitors: Array[MonitorFactory] = Array(AverageFrequencyMonitorFactory(interval = 15, query1),
                                              DummyMonitorFactory(query1))

  val cluster = Cluster(actorSystem)
  val graphFactory = new QueryGraph(actorSystem, Cluster(actorSystem),
                                      query1,
                                      publishers,
                                      createdCallback = Some(new CreatedCallback {
                                        override def apply(): Any = println("STATUS:\t\tGraph has been created.")
                                      }), monitors)

  graphFactory.createAndStart()(
    eventCallback = Some({
      // Callback for `query1`:
      case Event3(i1, i2, f) => println(s"COMPLEX EVENT:\tEvent3($i1,$i2,$f)")
      case Event1(s) => println(s"COMPLEX EVENT:\tEvent1($s)")
      // Callback for `query2`:
      //case (i1, i2, f, s)             => println(s"COMPLEX EVENT:\tEvent4($i1, $i2, $f,$s)")
      // This is necessary to avoid warnings about non-exhaustive `match`:
      case _ =>
    }))
}
