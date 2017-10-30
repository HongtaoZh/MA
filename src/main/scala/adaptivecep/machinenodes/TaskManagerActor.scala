package adaptivecep.machinenodes

import adaptivecep.placement.manets.StarksAlgorithm
import adaptivecep.placement.vivaldi.VivaldiCoordinates
import akka.actor.{ActorLogging, ActorRef}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by raheel
  * on 09/08/2017.
  */

case class Task()

case class StarksTask(producers: Seq[ActorRef])

case class RunTask(t: Task)

class TaskManagerActor extends VivaldiCoordinates with ActorLogging {

  override def preStart(): Unit = {
    super.preStart()
    log.info("booting up TaskManager")
  }

  override def receive: Receive = super.receive orElse {
    case s: StarksTask =>  {
      val requester = sender()
      log.info(s"received starks task")
      Future {requester ! StarksAlgorithm.findOptimalNode(this.context.system, cluster, s.producers, coordinates)}
    }

    case _ => log.info("ignoring unknown task")
  }
}
