package adaptivecep.publishers

import java.util.concurrent.TimeUnit
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import adaptivecep.data.Events._
import akka.event.Logging

case class RandomPublisher(createEventFromId: Integer => Event) extends Publisher {

  val publisherName: String = self.path.name

  def publish(id: Integer): Unit = {
    val event: Event = createEventFromId(id)
    subscribers.foreach(_ ! event)
    log.info(s"STREAM $publisherName:\t$event")
    context.system.scheduler.scheduleOnce(
      delay = FiniteDuration(Random.nextInt(5000), TimeUnit.MILLISECONDS),
      runnable = () => publish(id + 1)
    )
  }

  publish(0)

}
