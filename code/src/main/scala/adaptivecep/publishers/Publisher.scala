package adaptivecep.publishers

import adaptivecep.graph.nodes.traits.Node._
import adaptivecep.placement.vivaldi.VivaldiCoordinates
import adaptivecep.publishers.Publisher._
import akka.actor.{ActorLogging, ActorRef}
import akka.cluster.ClusterEvent._

import scala.collection.mutable

/**
  * Publisher Actor
  **/
trait Publisher extends VivaldiCoordinates with ActorLogging {

  var subscribers: mutable.Set[ActorRef] = mutable.Set.empty[ActorRef]

  override def receive: Receive = super.receive orElse {
    case Subscribe() =>{
      log.info(s"${sender().path.name} subscribed")
      subscribers += sender()
      sender() ! AcknowledgeSubscription
    }

    case _: MemberEvent => // ignore
  }
}

/**
  * List of Akka Messages which is being used by Publisher actor.
  **/
object Publisher {
  case object AcknowledgeSubscription
}
