package adaptivecep.placement.benchmarking

import java.util.concurrent.TimeUnit

import adaptivecep.data.Queries.{LatencyRequirement, LoadRequirement, MessageOverheadRequirement, Requirement}
import adaptivecep.placement.PlacementStrategy
import adaptivecep.placement.vivaldi.VivaldiCoordinates
import akka.actor.{ActorLogging, Props}
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.reflect.runtime._

/**
  * Loads benchmarking configuration from conf file
  */
class BenchmarkingNode extends VivaldiCoordinates with ActorLogging {

  private def getConfiguration: Config = ConfigFactory.load("benchmark.conf")

  implicit val resolveTimeout = Timeout(40, TimeUnit.SECONDS)

  val algorithms = new ListBuffer[PlacementAlgorithm]()

  override def preStart(): Unit = {
    super.preStart()
    parseConfiguration()
    context.actorOf(Props(new NetworkStateMonitorNode()), "NetworkStateMonitorNode") ! RegisterForStateUpdate
    log.info("booting BenchmarkingNode")
  }


  def selectBestPlacementAlgorithm(requirements: List[Requirement]): PlacementAlgorithm = {
    var availableAlgos = algorithms.toList

    for (requirement <- requirements) {
      requirement match {
        case LatencyRequirement(_, _, _) => filterAlgos("latency")
        case LoadRequirement(_, _, _) => filterAlgos("machineLoad")
        case MessageOverheadRequirement(_, _, _) => filterAlgos("messageOverhead")
        case _ => //ignoring unsupported requirement
      }
    }

    def filterAlgos(requirement: String) = {
      availableAlgos = availableAlgos.filter(p => p.demands.contains(requirement))
    }

    log.info(s"selected ${availableAlgos.head.algorithm.getClass.getSimpleName}!")
    availableAlgos.head
  }


  override def receive: Receive = {
    case SelectPlacementAlgorithm(requirements) => sender() ! selectBestPlacementAlgorithm(requirements)
    case message => log.info(s"ignoring unknown message: $message")
  }


  def getObjectInstance[A](clsName: String): A = {
    val ms = currentMirror staticModule clsName
    val moduleMirror = currentMirror reflectModule ms
    moduleMirror.instance.asInstanceOf[A]
  }

  override def postStop(): Unit = {
    context.actorSelection("/user/BenchmarkingNode/NetworkStateMonitorNode").resolveOne().value.get.get ! CancelRegistration
    super.postStop()
  }


  def parseConfiguration() = {
    val algorithmNames = getConfiguration.getConfig("general").getStringList("algorithms")

    algorithmNames.forEach(algo => {
      val algoConf = getConfiguration.getConfig(algo)

      val optimizations: List[String] = algoConf.getStringList("optimizationCriteria").toList

      val requirements: List[NetworkProperties] = algoConf.getStringList("requirements").toList
        .map {
          case "HighChurnRate" => HighChurnRate
          case "lowChurnRate" => LowChurnRate
        }

      val strategy = getObjectInstance[PlacementStrategy](algoConf.getString("class"))
      algorithms += PlacementAlgorithm(strategy, requirements, optimizations)
    })
  }

}

case class PlacementAlgorithm(algorithm: PlacementStrategy, requirements: List[NetworkProperties],
                              demands: List[String]) {

  def containsDemand(demand: Requirement): Boolean = {
    demand match {
      case LatencyRequirement(_, _, _) => demands.contains("latency")
      case LoadRequirement(_, _, _) => demands.contains("machineLoad")
      case MessageOverheadRequirement(_, _, _) => demands.contains("messageOverhead")
      case _ => false//ignoring unsupported requirement

    }
  }
}

//DTOs
case class SelectPlacementAlgorithm(requirements: List[Requirement])
