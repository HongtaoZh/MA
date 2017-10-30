package adaptivecep.graph.nodes

import java.util.UUID

import akka.actor.{ActorRef, Deploy, PoisonPill, Props}
import com.espertech.esper.client._
import adaptivecep.data.Events._
import adaptivecep.data.Queries._
import adaptivecep.graph.nodes.traits._
import adaptivecep.graph.nodes.traits.EsperEngine._
import adaptivecep.graph.qos._
import JoinNode._
import adaptivecep.graph.nodes.traits.Mode.{apply => _, _}
import adaptivecep.graph.{CreatedCallback, EventCallback, QueryGraph}
import adaptivecep.graph.{CreatedCallback, EventCallback}
import adaptivecep.graph.transition.MAPEK
import adaptivecep.placement.benchmarking.PlacementAlgorithm
import akka.cluster.ClusterEvent.{MemberEvent, MemberUp}
import akka.remote.RemoteScope

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
/**
  * Handling of [[adaptivecep.data.Queries.JoinQuery]] is done by JoinNode.
  *
  * @see [[QueryGraph]]
  * */

case class JoinNode( mode: Mode,
                     query: JoinQuery,
                     var parentNode1: ActorRef,
                     var parentNode2: ActorRef,
                     createdCallback: Option[CreatedCallback],
                     eventCallback: Option[EventCallback])
  extends BinaryNode with EsperEngine {

  override val esperServiceProviderUri: String = name

  var childNode1Created: Boolean = false
  var childNode2Created: Boolean = false


  override def preStart(): X = {
    super.preStart()

    val epStatement: EPStatement = createEpStatement(
      s"select * from " +
        s"sq1.${createWindowEplString(query.w1)} as sq1, " +
        s"sq2.${createWindowEplString(query.w2)} as sq2")

    val updateListener: UpdateListener = (newEventBeans: Array[EventBean], _) => newEventBeans.foreach(eventBean => {
      val values: Array[Any] =
        eventBean.get("sq1").asInstanceOf[Array[Any]] ++ eventBean.get("sq2").asInstanceOf[Array[Any]]
      val event: Event = values.length match {
        case 2 => Event2(values(0), values(1))
        case 3 => Event3(values(0), values(1), values(2))
        case 4 => Event4(values(0), values(1), values(2), values(3))
        case 5 => Event5(values(0), values(1), values(2), values(3), values(4))
        case 6 => Event6(values(0), values(1), values(2), values(3), values(4), values(5))
      }
      emitEvent(event)
    })

    epStatement.addListener(updateListener)

  }

  override def receive: Receive = super.receive orElse {
    case DependenciesRequest =>
      sender ! DependenciesResponse(Seq(parentNode1, parentNode2))
    case Created if sender().equals(parentNode1) =>
      childNode1Created = true
      if (childNode2Created) emitCreated()
    case Created if sender().equals(parentNode2) =>
      childNode2Created = true
      if (childNode1Created) emitCreated()
    case event: Event if sender().equals(parentNode1) => event match {
      case Event1(e1) => sendEvent("sq1", Array(toAnyRef(e1)))
      case Event2(e1, e2) => sendEvent("sq1", Array(toAnyRef(e1), toAnyRef(e2)))
      case Event3(e1, e2, e3) => sendEvent("sq1", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3)))
      case Event4(e1, e2, e3, e4) => sendEvent("sq1", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3), toAnyRef(e4)))
      case Event5(e1, e2, e3, e4, e5) => sendEvent("sq1", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3), toAnyRef(e4), toAnyRef(e5)))
      case Event6(e1, e2, e3, e4, e5, e6) => sendEvent("sq1", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3), toAnyRef(e4), toAnyRef(e5), toAnyRef(e6)))
    }
    case event: Event if sender().equals(parentNode2) => event match {
      case Event1(e1) => sendEvent("sq2", Array(toAnyRef(e1)))
      case Event2(e1, e2) => sendEvent("sq2", Array(toAnyRef(e1), toAnyRef(e2)))
      case Event3(e1, e2, e3) => sendEvent("sq2", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3)))
      case Event4(e1, e2, e3, e4) => sendEvent("sq2", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3), toAnyRef(e4)))
      case Event5(e1, e2, e3, e4, e5) => sendEvent("sq2", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3), toAnyRef(e4), toAnyRef(e5)))
      case Event6(e1, e2, e3, e4, e5, e6) => sendEvent("sq2", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3), toAnyRef(e4), toAnyRef(e5), toAnyRef(e6)))
    }
    case unhandledMessage => log.info(s"Unhandled message $unhandledMessage")
  }


  def handleTransitionRequest(algorithm: PlacementAlgorithm): Unit = {
    log.info("initiating runrun trnansition on JoinNode")
    val requester = sender()

    Future {
      //Don't block normal execution of this actor
      val address = algorithm.algorithm.findOptimalNode(this.context.system, cluster, Seq(parentNode1, parentNode2), coordinates)
      val successor = context.actorOf(Props(
        JoinNode(
          mode,
          query,
          parentNode1,
          parentNode2,
          createdCallback,
          eventCallback
        )).withDeploy(Deploy(scope = RemoteScope(address))), s"JoinNode${UUID.randomUUID.toString}"
      )

      successor ! StartExecutionWithDependencies(subscribers, delta + maxWindowTime()*1000 )
      Thread.sleep(delta)
      this.started = false
      requester ! TransferredState(algorithm, successor)
      self ! PoisonPill
    }
  }

  def maxWindowTime(): Int ={
    def windowTime(w: Window): Int = w match {
      case SlidingTime(seconds) => seconds
      case TumblingTime(seconds) => seconds
    }

    val w1 = windowTime(query.w1)
    val w2 = windowTime(query.w2)
    if(w1 > w2) w1 else w2
  }

  override def postStop(): Unit = {
    destroyServiceProvider()
  }

  addEventType("sq1", createArrayOfNames(query.sq1), createArrayOfClasses(query.sq1))
  addEventType("sq2", createArrayOfNames(query.sq2), createArrayOfClasses(query.sq2))
}

object JoinNode {

  def createWindowEplString(window: Window): String = window match {
    case SlidingTime(seconds) => s"win:time($seconds)"
    case TumblingTime(seconds) => s"win:time_batch($seconds)"

    //case SlidingInstances(instances) => s"win:length($instances)"
    //case TumblingInstances(instances) => s"win:length_batch($instances)"
  }

}
