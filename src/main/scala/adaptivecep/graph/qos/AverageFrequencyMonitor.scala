package adaptivecep.graph.qos

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import adaptivecep.data.Events._
import adaptivecep.data.Queries._
import org.slf4j.LoggerFactory

/**
  * Measures the frequency of messages in the interval
  */
//TODO: repalace lamdas with wrapper classes
case class AverageFrequencyMonitor(interval:Int, query: Query) extends Monitor {
  val log = LoggerFactory.getLogger(getClass)
  var frequencyRequirement: Option[FrequencyRequirement] = query.requirements.collect { case lr: FrequencyRequirement => lr }.headOption

  @volatile
  var eventEmittedInInterval = 0

  override def onEventEmit(event: Event): X = {
      eventEmittedInInterval += 1
  }


  val ex = new ScheduledThreadPoolExecutor(1)
  val task = new Runnable {
    def run() = if(frequencyRequirement.isDefined){
      if(eventEmittedInInterval < frequencyRequirement.get.instances){
        frequencyRequirement.get.callback.apply(eventEmittedInInterval)
      }
    }
  }
  val f = ex.scheduleAtFixedRate(task, interval, interval, TimeUnit.SECONDS)
  f.cancel(false)

}


/**
  * Creates AverageFrequencyMonitor
  * @param interval Time in Seconds
  * @param query CEP query
  */
case class AverageFrequencyMonitorFactory(interval: Int, query: Query) extends MonitorFactory {
  override def createNodeMonitor: Monitor = AverageFrequencyMonitor(interval, query)
}
