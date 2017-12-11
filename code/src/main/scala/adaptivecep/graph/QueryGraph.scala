package adaptivecep.graph

import java.util.UUID

import adaptivecep.data.Events.Event
import adaptivecep.data.Queries._
import adaptivecep.graph.nodes._
import adaptivecep.graph.nodes.traits.Mode
import adaptivecep.graph.nodes.traits.Mode.Mode
import adaptivecep.graph.qos.MonitorFactory
import adaptivecep.graph.transition.{AddNewDemand, MAPEK, RemoveDemand, StartExecution}
import adaptivecep.placement.{PlacementStrategy, PlacementUtils}
import akka.actor.{ActorContext, ActorRef, Address, Deploy, PoisonPill, Props}
import akka.cluster.Cluster
import akka.remote.RemoteScope
import org.slf4j.LoggerFactory


/**
  * Created by raheel
  * on 15/08/2017.
  *
  * Extracts the Operator Graph from Base Query
  */
class QueryGraph(context: ActorContext,
                 cluster: Cluster,
                 query: Query,
                 publishers: Map[String, ActorRef],
                 createdCallback: Option[CreatedCallback],
                 monitors: Array[MonitorFactory]) {

  val log = LoggerFactory.getLogger(getClass)
  var mapek: MAPEK = new MAPEK(context, query, this)
  var placementStrategy: PlacementStrategy = mapek.getBestPlacementStrategy(context, cluster)

  private def deployGraph(q: Query, mode: Mode,
                   publishers: Map[String, ActorRef],
                   createdCallback: Option[CreatedCallback],
                   eventCallback: Option[EventCallback],
                   monitors: Array[MonitorFactory]
                 ): ActorRef = {
    var res: ActorRef = null
    q match {
      case query: StreamQuery => {
        res = deploy(mode, context, cluster, monitors, query, createdCallback, eventCallback, publishers(query.publisherName))
      }

      case query: SequenceQuery => {
        res = deploy(mode, context, cluster, monitors, query, createdCallback, eventCallback,
          publishers(query.s1.publisherName), publishers(query.s2.publisherName))
      }

      case query: UnaryQuery => {
        val deployed = deployGraph(query.sq, mode, publishers, None, None, monitors)
        res = deploy(mode, context, cluster, monitors, query,createdCallback, eventCallback, deployed)
      }
      case query: BinaryQuery => {
        val child1 = deployGraph(query.sq1, mode, publishers, None, None, monitors)
        val child2 = deployGraph(query.sq2, mode, publishers, None, None, monitors)
        res = deploy(mode, context, cluster, monitors, query, createdCallback, eventCallback, child1, child2)
      }

    }
    mapek.knowledge addOperator res
    res
  }

  private def deploy(mode: Mode,
                     context: ActorContext,
                     cluster: Cluster,
                     factories: Array[MonitorFactory],
                     query: Query,
                     createdCallback: Option[CreatedCallback],
                     eventCallback: Option[EventCallback],
                     dependsOn: ActorRef*): ActorRef = {

    val address: Address = placementStrategy.findOptimalNode(context, cluster, dependsOn,
                                                             PlacementUtils.findMyCoordinates(cluster, context))
    query match {

      case streamQuery: StreamQuery =>
        context.actorOf(Props(
          StreamNode(
            mode,
            streamQuery,
            dependsOn.head,
            createdCallback,
            eventCallback
          )).withDeploy(Deploy(scope = RemoteScope(address))),s"StreamNode${UUID.randomUUID.toString}"
          )

      case sequenceQuery: SequenceQuery =>
        context.actorOf(Props(
          SequenceNode(
            mode,
            sequenceQuery,
            dependsOn,
            createdCallback,
            eventCallback
          )).withDeploy(Deploy(scope = RemoteScope(address))),s"SequenceNode${UUID.randomUUID.toString}"
        )

      case filterQuery: FilterQuery =>
        context.actorOf(Props(
          FilterNode(
            mode,
            filterQuery,
            dependsOn.head,
            createdCallback,
            eventCallback)).withDeploy(Deploy(scope = RemoteScope(address))),s"FilterNode${UUID.randomUUID.toString}"
        )

      case dropElemQuery: DropElemQuery =>
        context.actorOf(Props(
          DropElemNode(
            mode,
            dropElemQuery,
            dependsOn.head,
            createdCallback,
            eventCallback)).withDeploy(Deploy(scope = RemoteScope(address))),s"DropElemNode${UUID.randomUUID.toString}"
        )


      case selfJoinQuery: SelfJoinQuery =>
        context.actorOf(Props(
          SelfJoinNode(
            mode,
            selfJoinQuery,
            dependsOn.head,
            createdCallback,
            eventCallback)).withDeploy(Deploy(scope = RemoteScope(address))),s"SelfJoinNode${UUID.randomUUID.toString}"
        )

      case joinQuery: JoinQuery =>
        context.actorOf(Props(
          JoinNode(
            mode,
            joinQuery,
            dependsOn.head,
            dependsOn.tail.head,
            createdCallback,
            eventCallback)).withDeploy(Deploy(scope = RemoteScope(address))),s"JoinNode${UUID.randomUUID.toString}"
        )


      case conjunctionQuery: ConjunctionQuery =>
        context.actorOf(Props(
          ConjunctionNode(
            mode,
            conjunctionQuery,
            dependsOn.head,
            dependsOn.tail.head,
            createdCallback,
            eventCallback)).withDeploy(Deploy(scope = RemoteScope(address))),s"ConjunctionNode${UUID.randomUUID.toString}"
        )

      case disjunctionQuery: DisjunctionQuery =>
        context.actorOf(Props(
          DisjunctionNode(
            mode,
            disjunctionQuery,
            dependsOn.head,
            dependsOn.tail.head,
            createdCallback,
            eventCallback)).withDeploy(Deploy(scope = RemoteScope(address))),s"DisjunctionNode${UUID.randomUUID.toString}"
        )
    }
  }

  def addDemand(demand: Requirement): Unit = {
    log.info("Requirements changed. Notifying Monitor")
    mapek.monitor ! AddNewDemand(demand)
  }

  def removeDemand(demand: Requirement): Unit = {
    log.info("Requirements changed. Notifying Monitor")
    mapek.monitor ! RemoveDemand(demand)
  }

  def createAndStart()(eventCallback: Option[EventCallback]): ActorRef ={
    val root = create()(eventCallback)
    mapek.knowledge notifyOperators StartExecution()

    mapek.knowledge.client = this.context.actorOf(Props(
                                              ClientNode(root, monitors)),s"ClientNode-${UUID.randomUUID.toString}")
    root
  }

  def create()(eventCallback: Option[EventCallback]): ActorRef ={
    mapek.knowledge.mode = getTransitionMode(query)
    log.info(s"executing in mode ${mapek.knowledge.mode}")
    deployGraph(
      query.asInstanceOf[Query],
      mapek.knowledge.mode,
      publishers,
      createdCallback,
      eventCallback,
      monitors)
  }

  def getTransitionMode(query: Query): Mode = {
    def frequencyReqDefined(query: Query):Boolean = {
      query.requirements.collect { case lr: FrequencyRequirement => lr }.nonEmpty
    }

    def isRunRun(q: Query): Boolean = q match {
      case leafQuery: LeafQuery => frequencyReqDefined(query)
      case unaryQuery: UnaryQuery => frequencyReqDefined(query) || isRunRun(unaryQuery.sq)
      case binaryQuery: BinaryQuery => frequencyReqDefined(query) || isRunRun(binaryQuery.sq1) || isRunRun(binaryQuery.sq2)
    }
    if(isRunRun(query)) Mode.RUN_RUN else Mode.FLIP
  }

  def stop(): Unit ={
    mapek.knowledge.client ! PoisonPill
  }
}

//Closures are not serializable so callbacks would need to be wrapped in a class
abstract class CreatedCallback() {
  def apply(): Any
}

abstract class EventCallback(){
  def apply(event: Event): Any
}
