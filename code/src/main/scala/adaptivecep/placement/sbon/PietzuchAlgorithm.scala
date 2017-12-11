package adaptivecep.placement.sbon

import adaptivecep.placement.{PlacementStrategy, PlacementUtils}
import akka.actor.{ActorContext, ActorRef, ActorSystem, Address}
import akka.cluster.{Cluster, Member}
import org.slf4j.LoggerFactory

/**
  * Created by mac on 04/08/2017.
  */
object PietzuchAlgorithm extends PlacementStrategy {
  val log = LoggerFactory.getLogger(getClass)

  val forceThreshold = 1
  //ML: In pietzuch paper F(t) = 1 and delta = 0.1 was determined empirically
  //you can either use these values or do an empirical study to determine these values
  //TODO: update dynamically
  val delta = 0.1 //TODO: update dynamically

  def findOptimalNode(context: ActorContext, cluster: Cluster, dependencies: Seq[ActorRef], myCoordinates: Long): Address = {
    val members = PlacementUtils.findPossibleNodesToDeploy(cluster)
    val memberCoordinates = PlacementUtils.findCoordinatesOfMembers(context, members)
    applyPietzuchAlgorithm(context, dependencies, memberCoordinates)
  }

  /**
    * Applies Pietzuch's algorithm to find the optimal node for operator deployment
    *
    * @param dependencies  parent nodes on which query is dependent
    * @param coordinatsMap coordinates of the candidate nodes
    * @return the address of member where operator will be deployed
    */

  def applyPietzuchAlgorithm(context: ActorContext, dependencies: Seq[ActorRef], coordinatsMap: Map[Member, Long]): Address = {
    val virtualCoorinates = virtualPlacement(dependencies)
    val node = physicalPlacement(context, virtualCoorinates, coordinatsMap)
    log.info("found node: " + node)
    node
  }

  def virtualPlacement(dependencies: Seq[ActorRef]): Long = {
    val parentCoordinates = PlacementUtils.findVivaldiCoordinatesOfNodes(dependencies)
    var f = 0d
    var res = 0d //resultant coordinate
    while (f > forceThreshold) {
      for (parent <- parentCoordinates) {
        f = f + latencyDiff(parent._2.toDouble, res) + dataRate(parent._2.toDouble, res)
      }
      res = res + f * delta
    }
    res.toLong
  }

  def physicalPlacement(context: ActorContext, virtualCoorinates: Long, coordinatsMap: Map[Member, Long]) : Address= {
    val machineLoads = PlacementUtils.findMachineLoad(context, coordinatsMap.keySet.toSeq)
    nodeWithMinLoad(machineLoads.head, machineLoads.toList).address
  }

  def nodeWithMinLoad(res: (Member, Double), membersLoad: List[(Member, Double)]): Member = membersLoad match {
    case Nil => res._1
    case s :: rest => {
      if(Math.abs(s._2 ) < res._2) nodeWithMinLoad(s, rest)
      else nodeWithMinLoad(res, rest)
    }
  }

  def getClosest(res: (Member, Long), num: Long, listNums: List[(Member, Long)]): Member = listNums match {
    case Nil => res._1
    case s :: rest => {
      if(Math.abs(s._2 - num) < res._2) getClosest(s, num, rest)
      else getClosest(res, num, rest)
    }
  }

  def latencyDiff(parentNode: Double, resNode: Double): Double = Math.abs(parentNode - resNode)
  def dataRate(operatorNode: Double, parentNode: Double): Double = 500d //using constant data rate for now
  //ML: data rate = network bandwidth - get from simulation

}
