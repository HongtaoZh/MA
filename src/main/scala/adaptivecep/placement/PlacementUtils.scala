package adaptivecep.placement

import java.util.concurrent.TimeUnit

import adaptivecep.placement.vivaldi.{CoordinatesRequest, CoordinatesResponse}
import akka.actor.{ActorPath, ActorRef, ActorSystem, RootActorPath}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.pattern.Patterns
import akka.util.Timeout
import org.slf4j.LoggerFactory

import scala.collection.SortedSet
import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration

/**
  * Created by mac on 10/09/2017.
  */

//TODO: cache result for optimization

object PlacementUtils {
  val log = LoggerFactory.getLogger(getClass)
  implicit val resolveTimeout = Timeout(40, TimeUnit.SECONDS)


  def findPossibleNodesToDeploy(cluster: Cluster): SortedSet[Member] = {
    cluster.state.members.filter(x => x.status == MemberStatus.Up
                                 && x.hasRole("Candidate")
                                 && !x.address.equals(cluster.selfAddress)
    )
  }

  def findMyCoordinates(cluster: Cluster, context: ActorSystem): Long = {
    findCoordinatesOfNode(context, RootActorPath(cluster.selfAddress) / "user" / "TaskManager").get
  }

  def findCoordinatesOfMembers(context: ActorSystem, members: SortedSet[Member]): Map[Member, Long] = {
    var result: Map[Member, Long] = Map.empty[Member, Long]

    for (member <- members) {
      val coordinatesOpt = findCoordinatesOfNode(context, RootActorPath(member.address) / "user" / "TaskManager")
      if(coordinatesOpt.isDefined){
        result += (member -> coordinatesOpt.get)
      }
    }
    result
  }

  def findCoordinatesOfNode(context: ActorSystem, path: ActorPath):Option[Long] = {
    log.debug(s"sending path request to ${path.toSerializationFormat}")

    val resultEither = Await.ready(context.actorSelection(path).resolveOne(), resolveTimeout.duration).value.get

    if (resultEither.isSuccess) {
      val actorRef = resultEither.get
      val coordinatesResult = Patterns.ask(actorRef, CoordinatesRequest(), resolveTimeout)
      val coordinates = Await.ready(coordinatesResult, resolveTimeout.duration)
      if (coordinates.isCompleted) {
        Option(coordinates.value.get.get.asInstanceOf[CoordinatesResponse].coordinate)
      } else {
        log.info(s"Task manager not responding ${resultEither.failed.get}")
        Option.empty
      }
    } else {
      log.info(s"unable to find task manager of $path")
      Option.empty
    }
  }

  def findVivaldiCoordinatesOfNodes(nodes: Seq[ActorRef]) : Map[ActorRef, Long]= {
    implicit val resolveTimeout = Timeout(40, TimeUnit.SECONDS)
    var result: Map[ActorRef, Long] = Map.empty[ActorRef, Long]

    for (node <- nodes) {
      val coordinatesResult = Patterns.ask(node, CoordinatesRequest(), resolveTimeout)
      try{
        val coordinates = Await.ready(coordinatesResult, resolveTimeout.duration)
        if (coordinates.isCompleted) {
          result += (node -> coordinates.value.get.get.asInstanceOf[CoordinatesResponse].coordinate)
        } else {
          log.info(s"Unable to find coordinates of producer ${node.path.name}")
        }
      }catch {
        case e: Exception => {
          log.error(s"error in waiting for coordinates request for ${node.path}")
          e.printStackTrace()
        }
      }
    }
    result
  }

}
