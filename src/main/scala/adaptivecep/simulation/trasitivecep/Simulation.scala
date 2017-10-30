package adaptivecep.simulation.trasitivecep

import java.io.{File, PrintStream}
import java.util.concurrent.TimeUnit

import adaptivecep.data.Queries.Query
import adaptivecep.graph.QueryGraph
import adaptivecep.graph.qos.{MessageOverheadMonitorFactory, MonitorFactory, PathLatencyMonitorFactory}
import adaptivecep.machinenodes.{EventPublishedCallback, GraphCreatedCallback}
import akka.actor.{ActorRef, ActorSystem, Cancellable}
import akka.cluster.Cluster
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

class Simulation(name: String, directory: Option[File], query: Query, publishers: Map[String, ActorRef],
                 recordLatency: RecordLatency, recordOverhead: RecordMessageOverHead, system: ActorSystem) {

  private val latencyMonitorFactory = PathLatencyMonitorFactory(query, Some(recordLatency), Option.empty)
  private val overheadMonitorFactory = MessageOverheadMonitorFactory(query, Some(recordOverhead), Option.empty)

  private val monitors: Array[MonitorFactory] = Array(latencyMonitorFactory, overheadMonitorFactory)
  private val out = directory map { directory => new PrintStream(new File(directory, s"$name.csv"))
                                  } getOrElse java.lang.System.out

  private val log = LoggerFactory.getLogger(getClass)

  /**
    *
    * @param startTime Start Time of Simulation (in Seconds)
    * @param interval Interval for recording data in CSV (in Seconds)
    * @param totalTime Total Time for the simulation (in Seconds)
    * @return
    */
  def startSimulation(startTime:Int, interval: Int, totalTime: Int)(callback: () =>Any)= {
    executeQuery()
    initCSVWriter(startTime, interval, totalTime, recordLatency, callback)
  }

  def executeQuery(): Unit ={
    val queryGraph = new QueryGraph(system,
      Cluster(system), query,
      publishers,
      Some(GraphCreatedCallback()),
      monitors)

    queryGraph.createAndStart()(Some(EventPublishedCallback()))
  }


  def initCSVWriter(startTime:Int, interval: Int, totalTime: Int, recordLatency: RecordLatency, callback: () =>Any) = {
    var time = 0

    def createCSVEntry(): Unit = {
      if(recordLatency.lastMeasurement.isDefined && recordOverhead.lastMeasurement.isDefined){
        out.append(s"$time,${recordLatency.lastMeasurement.get.toMillis},${recordOverhead.lastMeasurement.get}\n")
        time += interval
      }else{
        if(recordLatency.lastMeasurement.isEmpty) log.info("Data about latency not available yet!")
        if(recordOverhead.lastMeasurement.isEmpty) log.info("Data about Overhead not available yet!")
      }
    }

    val simulation: Cancellable = system.scheduler.schedule(FiniteDuration.apply(startTime, TimeUnit.SECONDS),
                                                            FiniteDuration.apply(interval, TimeUnit.SECONDS
                                                            ))(createCSVEntry())

    def stopSimulation(): Unit = {
      simulation.cancel()
      out.close()
      callback.apply()
    }
    system.scheduler.scheduleOnce(FiniteDuration.apply(totalTime, TimeUnit.SECONDS))(stopSimulation())
  }
}

