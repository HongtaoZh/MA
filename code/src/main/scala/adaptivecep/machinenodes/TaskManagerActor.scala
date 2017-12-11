package adaptivecep.machinenodes

import adaptivecep.placement.manets.StarksAlgorithm
import adaptivecep.placement.vivaldi.VivaldiCoordinates
import adaptivecep.simulation.adaptive.cep.SystemLoad
import akka.actor.{ActorLogging, ActorRef}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by raheel
  * on 09/08/2017.
  */

case class Task()

case class StarksTask(producers: Seq[ActorRef])
case class LoadRequest()
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
      Future {requester ! StarksAlgorithm.findOptimalNode(this.context, cluster, s.producers, coordinates)}
    }

    case LoadRequest() => { sender() ! SystemLoad.getSystemLoad}
    case _ => log.info("ignoring unknown task")
  }
}
