package adaptivecep.placement.mop

import adaptivecep.placement.{PlacementStrategy, PlacementUtils}
import akka.actor.{ActorRef, ActorSystem, Address}
import akka.cluster.{Cluster, Member}
import org.slf4j.LoggerFactory

/**
  * Implementation of MOP algorithm introduced by Stamatia Rizou et. al.
  * http://ieeexplore.ieee.org/abstract/document/5560127/
  */
class RizouAlgorithm extends PlacementStrategy {
  val log = LoggerFactory.getLogger(getClass)

  val forceThreshold = 1000
  //TODO: update dynamically
  val delta = 1 //TODO: update dynamically

  def findOptimalNode(context: ActorSystem, cluster: Cluster, dependencies: Seq[ActorRef], myCoordinates: Long): Address = {
    val members = PlacementUtils.findPossibleNodesToDeploy(cluster)
    val memberCoordinates = PlacementUtils.findCoordinatesOfMembers(context, members)
    multiOperatorPlacement(dependencies, memberCoordinates)
  }

  /**
    * Applies Multi Operator Placement algorithm to find the optimal node for operator deployment
    *
    * @param dependencies  parent nodes on which query is dependent
    * @param coordinatsMap coordinates of the candidate nodes
    * @return the address of member where operator will be deployed
    */

  def multiOperatorPlacement(dependencies: Seq[ActorRef], coordinatsMap: Map[Member, Long]): Address = {
    val virtualCoorinates = singleOperatorPlacement(coordinatsMap, dependencies)
    val node = physicalPlacement(virtualCoorinates, coordinatsMap)
    log.info("found node: " + node)
    node
  }

  /**
    * Finds the virtual location for the placement of a single operator
    * @see http://ieeexplore.ieee.org/abstract/document/5560127/ Listing 2
    */
  def singleOperatorPlacement(memberCoordinates: Map[Member, Long], dependencies: Seq[ActorRef]): Long = {
    val parentCoordinates = PlacementUtils.findVivaldiCoordinatesOfNodes(dependencies)
    var step = getFurthestNode(memberCoordinates.head, memberCoordinates.toList)
    var f = 0l
    var res = 0l //resultant coordinate

    def U(x: Long):Long = {
      var force = 0l
      for (parent <- parentCoordinates) {
        force = force + latencyDiff(parent._2, res) + dataRate(parent._2, res)
      }
      force
    }

    def u(x: Long):Long = ???

    while (f > forceThreshold) {
      for (parent <- parentCoordinates) {
        f = f + latencyDiff(parent._2, res) + dataRate(parent._2, res)
        if (U(res + step * u(f)) < U(res)){
          step = step/2
        }
      }
      res = res + f * delta
    }
    res
  }

  def physicalPlacement(virtualCoorinates: Long, coordinatsMap: Map[Member, Long]) : Address= {
    getClosest(coordinatsMap.head, virtualCoorinates, coordinatsMap.toList).address
  }

  def getFurthestNode(res: (Member, Long), listNums: List[(Member, Long)]): Long = listNums match {
    case Nil => res._2
    case s :: rest => {
      if(Math.abs(s._2 ) > res._2) getFurthestNode(s,  rest)
      else getFurthestNode(res, rest)
    }
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
