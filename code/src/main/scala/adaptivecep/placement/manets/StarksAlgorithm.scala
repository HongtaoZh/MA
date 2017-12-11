package adaptivecep.placement.manets

import java.util.concurrent.TimeUnit

import adaptivecep.machinenodes.StarksTask
import adaptivecep.placement.{PlacementStrategy, PlacementUtils}
import akka.actor.{ActorContext, ActorPath, ActorRef, ActorSystem, Address, RootActorPath}
import akka.cluster.{Cluster, Member}
import akka.pattern.Patterns
import akka.util.Timeout
import org.slf4j.LoggerFactory

import scala.collection.immutable.ListMap
import scala.concurrent.Await

/**
  * Created by mac on 10/10/2017.
  */
object StarksAlgorithm extends PlacementStrategy {

  val log = LoggerFactory.getLogger(getClass)
  implicit val resolveTimeout = Timeout(60, TimeUnit.SECONDS)

  // using distance till first neighbour + $neighbourhoodArea to find nodes in neighbours
  // as in akka we don't have concept of neighbors and the links
  val neighbourhoodArea = 1000

  def findOptimalNode(context: ActorContext, cluster: Cluster, dependencies: Seq[ActorRef], myCoordinates: Long): Address = {
    val members = PlacementUtils.findPossibleNodesToDeploy(cluster)
    log.info(s"the number of available members are: ${members.size} ")

    val memberCoordinates = PlacementUtils.findCoordinatesOfMembers(context, members)
    val neighbors = getNeighbors(memberCoordinates)
    val producersCords = PlacementUtils.findVivaldiCoordinatesOfNodes(dependencies)

    applyStarksAlgorithm(context, cluster, myCoordinates, producersCords, neighbors)
  }


  def getNeighbors(members: Map[Member, Long]): Map[Member, Long] = {
    if(members.isEmpty){
      return members
    }

    var result: Map[Member, Long] = Map.empty[Member, Long]
    val sortedMembers = ListMap(members.toSeq.sortBy(_._2): _*)
    val maxDistance = members.head._2 + neighbourhoodArea
    for (member <- sortedMembers) {
      if (member._2 <= maxDistance) {
        result += member
      }
    }
    result
  }

  /**
    * Applies Starks's algorithm to find the optimal node for operator deployment
    *
    * @param producers      producers nodes on which this operator is dependent
    * @param candidateNodes coordinates of the candidate nodes
    * @return the address of member where operator will be deployed
    */

  def applyStarksAlgorithm(context: ActorContext,
                           cluster: Cluster,
                           mycords: Long,
                           producers: Map[ActorRef, Long],
                           candidateNodes: Map[Member, Long]): Address = {

    def sortNeighbours(lft: (Member, Long), rght: (Member, Long)) = {
      findDistanceFromProducers(lft._2) < findDistanceFromProducers(rght._2)
    }

    def findDistanceFromProducers(memberCord: Long): Long = {
      producers.values.reduce((total, cur) => total + Math.abs(memberCord - cur))
    }

    val neighbours = ListMap(candidateNodes.toSeq.sortWith(sortNeighbours): _*)
    if (neighbours.nonEmpty && findDistanceFromProducers(neighbours.head._2) < findDistanceFromProducers(mycords)) {
      log.info(s"neighbour will host the operator ${neighbours.head._1.address}")
      log.info(s"mine distance from producers ${findDistanceFromProducers(mycords)}, neighbours ${neighbours.head._2}")
      deployOnNeighbor(context, producers, RootActorPath(neighbours.head._1.address) / "user" / "TaskManager")
    } else {
      log.info(s"i'm hosting the operator: ${cluster.selfAddress}")
      cluster.selfAddress
    }
  }

  def deployOnNeighbor(context: ActorContext,
                       producers: Map[ActorRef, Long],
                       path: ActorPath): Address = {
    log.info(s"asking ${path.toSerializationFormat} to deploy")
    val resultEither = Await.ready(context.actorSelection(path).resolveOne(), resolveTimeout.duration).value.get

    if (resultEither.isSuccess) {
      val actorRef = resultEither.get
      val deployResult = Patterns.ask(actorRef, StarksTask(producers.keySet.toSeq), resolveTimeout)
      val coordinates = Await.ready(deployResult, resolveTimeout.duration)
      if (coordinates.isCompleted) {
        log.info("Got the response from the neighbour")
        coordinates.value.get.get.asInstanceOf[Address]
      } else {
        throw new Exception("Unable to contact neighbour nodes")
      }
    } else {
      throw new Exception("Unable to contact neighbour nodes")
    }
  }
}