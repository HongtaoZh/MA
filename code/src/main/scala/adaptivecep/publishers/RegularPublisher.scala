package adaptivecep.publishers

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import adaptivecep.data.Events._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

case class RegularPublisher(waitTime: Long, createEventFromId: Integer => Event) extends Publisher {

  val publisherName: String = self.path.name
  val id: AtomicInteger = new AtomicInteger(0)
  val scheduler =  context.system.scheduler.schedule(
                            FiniteDuration(waitTime, TimeUnit.MILLISECONDS),
                            FiniteDuration(waitTime, TimeUnit.MILLISECONDS),
                            runnable = () => {
                              val event: Event = createEventFromId(id.incrementAndGet())
                              subscribers.foreach(_ ! event)
                              log.info(s"STREAM $publisherName:\t$event")
                            }
                          )

  override def postStop(): Unit = {
    scheduler.cancel()
    super.postStop()
  }
}
