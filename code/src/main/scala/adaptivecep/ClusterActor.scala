package adaptivecep

import akka.actor.Actor
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberEvent

/**
  * Created by mac on 09/08/2017.
  */
trait ClusterActor extends Actor {
  val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberEvent])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)
}
