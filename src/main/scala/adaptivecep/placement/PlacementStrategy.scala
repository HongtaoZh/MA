package adaptivecep.placement

import akka.actor.{ActorRef, ActorSystem, Address}
import akka.cluster.Cluster

/**
  * Created by raheel
  * on 17/08/2017.
  */
trait PlacementStrategy {
  def findOptimalNode(context: ActorSystem, cluster: Cluster, dependencies: Seq[ActorRef], myCoordinates: Long): Address
}
