package adaptivecep.placement.vivaldi

import adaptivecep.ClusterActor
import akka.actor.{ActorLogging, RootActorPath}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberEvent, MemberUp}
import akka.cluster.{Member, MemberStatus}

/**
  * Created by raheel
  * on 04/08/2017.
  */

case class Ping()

case class CoordinatesRequest()

case class CoordinatesResponse(coordinate: Long)


trait VivaldiCoordinates extends ClusterActor with ActorLogging {
  var timer: Long = 0l
  var coordinates: Long = -1

  def getVivaldiCoordinates(member: Member): Unit = {
    timer = System.currentTimeMillis()
    val vivaldiRefActor = context.actorSelection(RootActorPath(member.address) / "user" / "VivaldiRef")
    vivaldiRefActor ! Ping()
    log.info("sending ping to vivaldiRef")
    log.info(s"path of vivaldi node is ${vivaldiRefActor.pathString}")
  }

  def currentClusterState(state: CurrentClusterState): Unit = {
    state.members.filter(_.status == MemberStatus.Up) foreach getVivaldiCoordinates
  }

  def memberUp(member: Member): Unit = if (member.hasRole("VivaldiRef")) {
    Thread.sleep(5000)
    getVivaldiCoordinates(member)
  }


  override def receive: Receive = {
    case MemberUp(member) => memberUp(member)
    case state: CurrentClusterState => currentClusterState(state)
    case Ping() => {
      coordinates = System.currentTimeMillis() - timer
      log.info(s"my coordinates are : $coordinates")
    }
    case CoordinatesRequest() => {
      log.info("received coordinates request")
      sender() ! CoordinatesResponse(coordinates)
    }
    case _: MemberEvent => // ignore
  }

}
