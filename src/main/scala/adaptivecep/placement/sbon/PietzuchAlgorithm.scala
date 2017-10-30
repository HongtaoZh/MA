package adaptivecep.placement.sbon

import java.util.concurrent.TimeUnit

import adaptivecep.placement.{PlacementStrategy, PlacementUtils}
import adaptivecep.placement.vivaldi.{CoordinatesRequest, CoordinatesResponse}
import akka.actor.{ActorRef, ActorSystem, Address, RootActorPath}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.pattern.Patterns
import akka.util.Timeout
import org.slf4j.LoggerFactory

import scala.collection.SortedSet
import scala.concurrent.Await

/**
  * Created by mac on 04/08/2017.
  */
object PietzuchAlgorithm extends PlacementStrategy {
  val log = LoggerFactory.getLogger(getClass)

  val forceThreshold = 1000
  //TODO: update dynamically
  val delta = 1 //TODO: update dynamically

  def findOptimalNode(context: ActorSystem, cluster: Cluster, dependencies: Seq[ActorRef], myCoordinates: Long): Address = {
    val members = PlacementUtils.findPossibleNodesToDeploy(cluster)
    val memberCoordinates = PlacementUtils.findCoordinatesOfMembers(context, members)
    applyPietzuchAlgorithm(dependencies, memberCoordinates)
  }

  /**
    * Applies Pietzuch's algorithm to find the optimal node for operator deployment
    *
    * @param dependencies  parent nodes on which query is dependent
    * @param coordinatsMap coordinates of the candidate nodes
    * @return the address of member where operator will be deployed
    */

  def applyPietzuchAlgorithm(dependencies: Seq[ActorRef], coordinatsMap: Map[Member, Long]): Address = {
    val virtualCoorinates = virtualPlacement(dependencies)
    val node = physicalPlacement(virtualCoorinates, coordinatsMap)
    log.info("found node: " + node)
    node
  }

  def virtualPlacement(dependencies: Seq[ActorRef]): Long = {
    val parentCoordinates = PlacementUtils.findVivaldiCoordinatesOfNodes(dependencies)
    var f = 0l
    var res = 0l //resultant coordinate
    while (f > forceThreshold) {
      for (parent <- parentCoordinates) {
        f = f + latencyDiff(parent._2, res) + dataRate(parent._2, res)
      }
      res = res + f * delta
    }
    res
  }

  def physicalPlacement(virtualCoorinates: Long, coordinatsMap: Map[Member, Long]) : Address= {
    getClosest(coordinatsMap.head, virtualCoorinates, coordinatsMap.toList).address
  }


  def getClosest(res: (Member, Long), num: Long, listNums: List[(Member, Long)]): Member = listNums match {
    case Nil => res._1
    case s :: rest => {
      if(Math.abs(s._2 - num) < res._2) getClosest(s, num, rest)
      else getClosest(res, num, rest)
    }
  }

  def latencyDiff(parentNode: Long, resNode: Long): Long = Math.abs(parentNode - resNode)
  def dataRate(operatorNode: Long, parentNode: Long): Int = 500 //using constant data rate for now
}
