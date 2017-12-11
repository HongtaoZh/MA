package adaptivecep.simulation.trasitivecep

import java.io.{File, PrintStream}
import java.util.concurrent.TimeUnit

import adaptivecep.data.Queries.Query
import adaptivecep.graph.QueryGraph
import adaptivecep.graph.qos._
import adaptivecep.machinenodes.{EventPublishedCallback, GraphCreatedCallback}
import akka.actor.{ActorContext, ActorRef, Cancellable}
import akka.cluster.Cluster
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

class Simulation(name: String, directory: Option[File], query: Query, publishers: Map[String, ActorRef],
                 recordAverageLoad: RecordAverageLoad,
                 recordLatency: RecordLatency,
                 recordOverhead: RecordMessageOverHead, recordFrequency: RecordFrequency,
                 context: ActorContext) {

  private val latencyMonitorFactory = PathLatencyMonitorFactory(query, Some(recordLatency))
  private val overheadMonitorFactory = MessageOverheadMonitorFactory(query, Some(recordOverhead))
  private val loadMonitorFactory = LoadMonitorFactory(query, Some(recordAverageLoad))
  private val frequencyMonitorFactory = AverageFrequencyMonitorFactory(query, Some(recordFrequency))

  private val monitors: Array[MonitorFactory] = Array(latencyMonitorFactory, overheadMonitorFactory,
                                                      loadMonitorFactory, frequencyMonitorFactory)

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
  def startSimulation(startTime:Int, interval: Int, totalTime: Int)(callback: () =>Any) : QueryGraph= {
    val graph = executeQuery()
    initCSVWriter(startTime, interval, totalTime, recordLatency, callback)
    graph
  }

  def executeQuery(): QueryGraph ={
    val queryGraph = new QueryGraph(context,
      Cluster(context.system), query,
      publishers,
      Some(GraphCreatedCallback()),
      monitors)

    queryGraph.createAndStart()(Some(EventPublishedCallback()))
    queryGraph
  }


  def initCSVWriter(startTime:Int, interval: Int, totalTime: Int, recordLatency: RecordLatency, callback: () =>Any) = {
    var time = 0

    def createCSVEntry(): Unit = synchronized {
      if(recordLatency.lastMeasurement.isDefined
        && recordOverhead.lastMeasurement.isDefined
        && recordAverageLoad.lastLoadMeasurement.isDefined
        && recordFrequency.lastMeasurement.isDefined
      ){
        out.append(s"$time,${recordLatency.lastMeasurement.get.toMillis},${recordOverhead.lastMeasurement.get},${recordAverageLoad.lastLoadMeasurement.get.value},${recordFrequency.lastMeasurement.get}")
        out.println()
        time += interval
      }else{
        log.info(s"Data not available yet! ${recordLatency.lastMeasurement} ${recordOverhead.lastMeasurement} ${recordAverageLoad.lastLoadMeasurement} ${recordFrequency.lastMeasurement}")
      }
    }

    val simulation: Cancellable = context.system.scheduler.schedule(FiniteDuration.apply(startTime, TimeUnit.SECONDS),
                                                            FiniteDuration.apply(interval, TimeUnit.SECONDS
                                                            ))(createCSVEntry())

    def stopSimulation(): Unit = {
      simulation.cancel()
      out.close()
      callback.apply()
    }
    context.system.scheduler.scheduleOnce(FiniteDuration.apply(totalTime, TimeUnit.SECONDS))(stopSimulation())
  }
}

