package adaptivecep.graph.transition

import java.util.concurrent.TimeUnit

import adaptivecep.data.Events.Event
import adaptivecep.data.Queries._
import adaptivecep.graph.nodes.traits.Mode
import adaptivecep.graph.nodes.traits.Mode.Mode
import adaptivecep.graph.{EventCallback, QueryGraph}
import adaptivecep.machinenodes.EventPublishedCallback
import adaptivecep.placement.PlacementStrategy
import adaptivecep.placement.benchmarking.{PlacementAlgorithm, SelectPlacementAlgorithm}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, RootActorPath}
import akka.cluster.{Cluster, MemberStatus}
import akka.pattern.Patterns
import akka.util.Timeout
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await

/**
  * Created by raheel
  * on 02/10/2017.
  */

case class AddNewDemand(requirement: Requirement)

case class RemoveDemand(requirement: Requirement)

case class ScheduleTransition(strategy: PlacementStrategy)

class MAPEK(system: ActorSystem, val query: Query, val queryGraph: QueryGraph) {
  val knowledge: KnowledgeComponent = new KnowledgeComponent(query)
  val monitor: ActorRef = system.actorOf(Props(new Monitor()))
  val analyzer: ActorRef = system.actorOf(Props(new Analyzer()))
  val planner: ActorRef = system.actorOf(Props(new Planner()))
  val executor: ActorRef = system.actorOf(Props(new Executor()))
  var benchmarkingNode: ActorRef = _

  def getBestPlacementStrategy(context: ActorSystem, cluster: Cluster) = {
    var success = false
    var strategy: PlacementAlgorithm = null

    implicit val resolveTimeout = Timeout(2, TimeUnit.MINUTES)
    val benchmarkingMember = cluster.state.members.filter(x => x.status == MemberStatus.Up && x.hasRole("BenchmarkingApp")).head
    val benchmarkingNodePath = RootActorPath(benchmarkingMember.address) / "user" / "BenchmarkingNode"

    val resolveActor = Await.ready(context.actorSelection(benchmarkingNodePath).resolveOne(), resolveTimeout.duration).value.get
    if (resolveActor.isSuccess) {
      benchmarkingNode = resolveActor.get
      val res = Patterns.ask(benchmarkingNode, SelectPlacementAlgorithm(knowledge.getDemands()), resolveTimeout)

      val strategyOpt = Await.ready(res, resolveTimeout.duration)
      if (strategyOpt.isCompleted) {
        strategy = strategyOpt.value.get.get.asInstanceOf[PlacementAlgorithm]
        success = true
      }
    }
    if (!success) {
      throw new RuntimeException("Unable to load placement strategy")
    }

    knowledge.setStrategy(strategy)
    strategy.algorithm
  }

  class Monitor extends Actor {
    val log = LoggerFactory.getLogger(getClass)
    val demands = List[String]()

    override def receive: Receive = {
      case AddNewDemand(newDemand) => {
        saveDemand(newDemand)
        analyzer ! AddNewDemand(newDemand)
      }
      case RemoveDemand(demand) => removeDemand(demand)

      case _ => {
        log.info("ignoring unknown messages")
      }

      //TODO: monitor change in latency
    }

    def saveDemand(newDemand: Requirement) = {
      knowledge.addDemand(newDemand)
    }

    def removeDemand(demand: Requirement): Unit = {
      knowledge.removeDemand(demand)
    }
  }

  class Analyzer extends Actor {
    override def receive: Receive = {
      case AddNewDemand(newDemand) => {

        if (!knowledge.getStrategy().containsDemand(newDemand)) {
          planner ! AddNewDemand(newDemand)
        }
      }
    }

    def notifyPlanner(newDemand: Requirement) = {

    }
  }

  class Planner extends Actor with ActorLogging {
    override def receive: Receive = {
      case AddNewDemand(newDemand) => {
        benchmarkingNode ! SelectPlacementAlgorithm(knowledge.getDemands())
      }
      case PlacementAlgorithm(s, r, d) => {
        log.info("Got new Strategy from Benchmarking Node")
        log.info("Time for execution of Transition")
        executor ! PlacementAlgorithm(s, r, d)
      }
    }
  }

  class Executor extends Actor with ActorLogging {

    override def receive: Receive = {
      case PlacementAlgorithm(s, r, d) => {
        log.info("runrun or flip???")
        scheduleTransition(PlacementAlgorithm(s, r, d))
      }
    }

    def scheduleTransition(placementAlgo: PlacementAlgorithm): Unit = {
      system.actorOf(Props(new TransitionManager())) ! TransitionRequest(placementAlgo)
    }
  }


  class TransitionManager extends Actor with ActorLogging {

    override def receive: Receive = {
      case TransitionRequest(algorithm) => {
        knowledge.transitionStarted()
        if(knowledge.flipMode == Mode.FLIP)
          flipTransition(query, algorithm)
        else
          runRunTransition(algorithm)
        knowledge.transitionEnded()
      }
    }


    def flipTransition(query: Query, algorithm: PlacementAlgorithm) = {
      val oldOperators = knowledge.getOperators()
      oldOperators.foreach(operator => operator ! StopExecution())

      knowledge.setStrategy(algorithm)
      knowledge.clearOperators()

      queryGraph.create()(Some(EventPublishedCallback())) //deploy new operators
      knowledge.getOperators().foldLeft(0) ((itr, opr) => {oldOperators(itr) ! TransferState(opr); itr + 1;} )
    }

    def runRunTransition(algorithm: PlacementAlgorithm) = {
      knowledge.client ! TransitionRequest(algorithm)
    }
  }

  class KnowledgeComponent(val query: Query) {


    private val demands = pullDemands(query, List()).to[ListBuffer]
    private val operators: ListBuffer[ActorRef] = ListBuffer.empty
    private var placementStrategy: PlacementAlgorithm = _
    private var transitionStartTime: Long = _
    private var totalTransitionTime: Long = _
    var flipMode: Mode = _
    var client: ActorRef = _

    def notifyOperators(message: Any): Unit = {
      operators.toList.reverse.foreach(opr => opr ! message)
    }

    def addOperator(x: ActorRef): Unit = {
      operators += x
    }

    def getDemands() = demands.toList

    def addDemand(demand: Requirement): Unit = {
      demands += demand
    }

    def removeDemand(demand: Requirement): Unit = {
      demands -= demand
    }

    def getOperators() = operators.toList

    def clearOperators() = operators.clear()

    def setStrategy(placementStrategy: PlacementAlgorithm) = this.placementStrategy = placementStrategy

    def getStrategy() = placementStrategy

    def pullDemands(q: Query, accumulatedReq: List[Requirement]): List[Requirement] = q match {
      case query: LeafQuery => q.requirements.toList ++ accumulatedReq
      case query: UnaryQuery => q.requirements.toList ++ pullDemands(query.sq, accumulatedReq)
      case query: BinaryQuery => {
        val child1 = pullDemands(query.sq1, q.requirements.toList ++ accumulatedReq)
        val child2 = pullDemands(query.sq2, q.requirements.toList ++ accumulatedReq)
        q.requirements.toList ++ child1 ++ child2
      }
    }

    def transitionStarted() = {
      transitionStartTime = System.currentTimeMillis()
    }

    def constantDataRateDemands(): Boolean = {
      false
    }

    def transitionEnded() = {
      totalTransitionTime = System.currentTimeMillis() - transitionStartTime
    }
  }
}

case class TransitionRequest(placementAlgo: PlacementAlgorithm)

case class StopExecution()
case class StartExecution()

case class SaveStateAndStartExecution(state: List[Any])
case class TransferState(newActor: ActorRef)
