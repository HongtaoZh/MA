package adaptivecep.placement

import java.util.concurrent.TimeUnit

import adaptivecep.machinenodes.LoadRequest
import adaptivecep.placement.vivaldi.{CoordinatesRequest, CoordinatesResponse}
import akka.actor.{ActorContext, ActorPath, ActorRef, RootActorPath}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.pattern.Patterns
import akka.util.Timeout
import org.slf4j.LoggerFactory

import scala.collection.SortedSet
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Utility class for common methods related to operator placement
  */

//TODO: cache result for optimization
object PlacementUtils {
  val log = LoggerFactory.getLogger(getClass)
  implicit val resolveTimeout = Timeout(30, TimeUnit.SECONDS)
  val defaultCoordinates = 10000l

  def findPossibleNodesToDeploy(cluster: Cluster): SortedSet[Member] = {
    cluster.state.members.filter(x => x.status == MemberStatus.Up
      && x.hasRole("Candidate")
      && !x.address.equals(cluster.selfAddress)
    )
  }

  def findMyCoordinates(cluster: Cluster, context: ActorContext): Long = {
    findCoordinatesFromActorPath(context, RootActorPath(cluster.selfAddress) / "user" / "TaskManager")
  }

  def findCoordinatesOfMembers(context: ActorContext, members: SortedSet[Member]): Map[Member, Long] = {
    var result: Map[Member, Long] = Map.empty[Member, Long]

    for (member <- members) {
      val coordinates = findCoordinatesFromActorPath(context, RootActorPath(member.address) / "user" / "TaskManager")
      result += (member -> coordinates)
    }
    result
  }

  def findCoordinatesFromActorPath(context: ActorContext, path: ActorPath): Long = {
    log.debug(s"sending path request to ${path.toSerializationFormat}")

    try {
      val actorRef = Await.result[ActorRef](context.actorSelection(path).resolveOne(), Duration.Inf)
      val coordinatesResult = Patterns.ask(actorRef, CoordinatesRequest(), resolveTimeout)
      val coordinates = Await.result(coordinatesResult, Duration.Inf)
      coordinates.asInstanceOf[CoordinatesResponse].coordinate
    } catch {
      case e: Throwable => {
        log.info(s"unable to determine coordinates of ${path.toSerializationFormat}, using default coords")
        log.error("unable to determine coordinates", e)
        defaultCoordinates
      }
    }
  }

  def findVivaldiCoordinatesOfNodes(nodes: Seq[ActorRef]): Map[ActorRef, Long] = {
    var result: Map[ActorRef, Long] = Map.empty[ActorRef, Long]
    for (node <- nodes) {
      result += (node -> getCoordinatesOfNode(node))
    }
    result
  }

  def findMachineLoad(context: ActorContext, nodes: Seq[Member]): Map[Member, Double] = {
    var result: Map[Member, Double] = Map.empty[Member, Double]
    for (node <- nodes) {
      result += (node -> getLoadOfNode(context, node))
    }
    result
  }

  def getCoordinatesOfNode(node: ActorRef): Long = {
    try {
      implicit val resolveTimeout = Timeout(30, TimeUnit.SECONDS)
      val coordinatesResult = Patterns.ask(node, CoordinatesRequest(), resolveTimeout)
      val coordinates = Await.result(coordinatesResult, Duration.Inf)
      coordinates.asInstanceOf[CoordinatesResponse].coordinate
    } catch {
      case e: Throwable => {
        log.info(s"unable to determine coordinates of ${node.path.name} using default coordinates")
        defaultCoordinates
      }
    }
  }

  def getLoadOfNode(context: ActorContext, node: Member): Double = {
    val defaultLoad = 1d
    val path = RootActorPath(node.address) / "user" / "TaskManager"
    try {
      implicit val resolveTimeout = Timeout(30, TimeUnit.SECONDS)
      val actorRef = Await.result[ActorRef](context.actorSelection(path).resolveOne(), Duration.Inf)
      val coordinatesResult = Patterns.ask(actorRef, LoadRequest(), resolveTimeout)
      val coordinates = Await.result(coordinatesResult, Duration.Inf)
      coordinates.asInstanceOf[Double]
    } catch {
      case e: Throwable => {
        log.info(s"unable to determine load of ${node.address} using default load")
        defaultLoad
      }
    }
  }
}
