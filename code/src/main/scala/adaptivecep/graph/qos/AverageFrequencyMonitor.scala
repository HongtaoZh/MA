package adaptivecep.graph.qos

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import adaptivecep.data.Events._
import adaptivecep.data.Queries._
import adaptivecep.dsl.Dsl.FrequencyMeasurement
import org.slf4j.LoggerFactory

/**
  * Measures the frequency of messages in the interval
  */
case class AverageFrequencyMonitor(query: Query, record: Option[FrequencyMeasurement]) extends Monitor {
  val log = LoggerFactory.getLogger(getClass)
  var frequencyRequirement: Option[FrequencyRequirement] = query.requirements.collect { case lr: FrequencyRequirement => lr }.headOption

  @volatile
  var eventEmittedInInterval : AtomicInteger = new AtomicInteger(0)

  override def onEventEmit(event: Event): X = {
    eventEmittedInInterval.incrementAndGet()
  }

  val ex = new ScheduledThreadPoolExecutor(1)
  val task = new Runnable {
    def run() = synchronized {
      if (frequencyRequirement.isDefined && frequencyRequirement.get.otherwise.isDefined) {
        frequencyRequirement.get.operator match {
          case Equal =>        if (!(eventEmittedInInterval.get() == frequencyRequirement.get.frequency.frequency)) frequencyRequirement.get.otherwise.get.apply(eventEmittedInInterval.get())
          case NotEqual =>     if (!(eventEmittedInInterval.get() != frequencyRequirement.get.frequency.frequency)) frequencyRequirement.get.otherwise.get.apply(eventEmittedInInterval.get())
          case Greater =>      if (!(eventEmittedInInterval.get() >  frequencyRequirement.get.frequency.frequency)) frequencyRequirement.get.otherwise.get.apply(eventEmittedInInterval.get())
          case GreaterEqual => if (!(eventEmittedInInterval.get() >= frequencyRequirement.get.frequency.frequency)) frequencyRequirement.get.otherwise.get.apply(eventEmittedInInterval.get())
          case Smaller =>      if (!(eventEmittedInInterval.get() <  frequencyRequirement.get.frequency.frequency)) frequencyRequirement.get.otherwise.get.apply(eventEmittedInInterval.get())
          case SmallerEqual => if (!(eventEmittedInInterval.get() <= frequencyRequirement.get.frequency.frequency)) frequencyRequirement.get.otherwise.get.apply(eventEmittedInInterval.get())
        }
      }

      if(record.isDefined){
        record.get.apply(eventEmittedInInterval.get())
      }
      eventEmittedInInterval.set(0)
    }
  }

  if(frequencyRequirement.isDefined){
    ex.scheduleAtFixedRate(task, frequencyRequirement.get.frequency.interval, frequencyRequirement.get.frequency.interval, TimeUnit.SECONDS)
  }else{
    ex.scheduleAtFixedRate(task, 5, 5, TimeUnit.SECONDS)
  }
}

/**
  * Creates AverageFrequencyMonitor
  * @param query CEP query
  * @param record callback for the udpated values of frequency per interval
  */
case class AverageFrequencyMonitorFactory(query: Query, record: Option[FrequencyMeasurement]) extends MonitorFactory {
  override def createNodeMonitor: Monitor = AverageFrequencyMonitor(query,record)
}
