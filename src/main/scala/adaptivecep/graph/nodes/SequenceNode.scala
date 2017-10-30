package adaptivecep.graph.nodes

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.{ActorLogging, ActorRef, Deploy, PoisonPill, Props}
import com.espertech.esper.client._
import adaptivecep.data.Events._
import adaptivecep.data.Queries._
import adaptivecep.graph.{CreatedCallback, EventCallback, QueryGraph}
import adaptivecep.graph.{CreatedCallback, EventCallback}
import adaptivecep.graph.nodes.traits._
import adaptivecep.graph.nodes.traits.EsperEngine._
import adaptivecep.graph.nodes.traits.Mode._
import adaptivecep.graph.nodes.traits.Node.Subscribe
import adaptivecep.graph.qos._
import adaptivecep.graph.transition.{MAPEK, TransitionRequest}
import adaptivecep.placement.benchmarking.PlacementAlgorithm
import adaptivecep.publishers.Publisher._
import akka.cluster.ClusterEvent.{MemberEvent, MemberUp}
import akka.remote.RemoteScope
import akka.stream.impl.SubscriberSink
import akka.util.Timeout

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, Future}

/**
  * Handling of [[adaptivecep.data.Queries.SequenceQuery]] is done by SequenceNode.
  *
  * @see [[QueryGraph]]
  * */

case class SequenceNode(mode: Mode,
                         query: SequenceQuery,
                         publishers: Seq[ActorRef],
                         createdCallback: Option[CreatedCallback],
                         eventCallback: Option[EventCallback])

  extends LeafNode with EsperEngine with ActorLogging{

  override val esperServiceProviderUri: String = name

  implicit val resolveTimeout = Timeout(10, TimeUnit.SECONDS)

  var subscription1Acknowledged: Boolean = false
  var subscription2Acknowledged: Boolean = false

  override def executionStarted(): X = {
    log.info(s"subscribing for events from $publishers")

    publishers.foreach(_ ! Subscribe())

    addEventType("sq1", SequenceNode.createArrayOfNames(query.s1), SequenceNode.createArrayOfClasses(query.s1))
    addEventType("sq2", SequenceNode.createArrayOfNames(query.s2), SequenceNode.createArrayOfClasses(query.s2))

    val epStatement: EPStatement = createEpStatement("select * from pattern [every (sq1=sq1 -> sq2=sq2)]")

    val updateListener: UpdateListener = (newEventBeans: Array[EventBean], _) => newEventBeans.foreach(eventBean => {
      val values: Array[Any] =
        eventBean.get("sq1").asInstanceOf[Array[Any]] ++
          eventBean.get("sq2").asInstanceOf[Array[Any]]
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
      sender ! DependenciesResponse(Seq.empty)
    case AcknowledgeSubscription if sender().equals(publishers.head) =>
      subscription1Acknowledged = true
      if (subscription2Acknowledged) emitCreated()
    case AcknowledgeSubscription if sender().equals(publishers(1)) =>
      subscription2Acknowledged = true
      if (subscription1Acknowledged) emitCreated()
    case event: Event if sender().equals(publishers.head) => event match {
      case Event1(e1) => sendEvent("sq1", Array(toAnyRef(e1)))
      case Event2(e1, e2) => sendEvent("sq1", Array(toAnyRef(e1), toAnyRef(e2)))
      case Event3(e1, e2, e3) => sendEvent("sq1", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3)))
      case Event4(e1, e2, e3, e4) => sendEvent("sq1", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3), toAnyRef(e4)))
      case Event5(e1, e2, e3, e4, e5) => sendEvent("sq1", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3), toAnyRef(e4), toAnyRef(e5)))
      case Event6(e1, e2, e3, e4, e5, e6) => sendEvent("sq1", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3), toAnyRef(e4), toAnyRef(e5), toAnyRef(e6)))
    }
    case event: Event if sender().equals(publishers(1)) => event match {
      case Event1(e1) => sendEvent("sq2", Array(toAnyRef(e1)))
      case Event2(e1, e2) => sendEvent("sq2", Array(toAnyRef(e1), toAnyRef(e2)))
      case Event3(e1, e2, e3) => sendEvent("sq2", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3)))
      case Event4(e1, e2, e3, e4) => sendEvent("sq2", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3), toAnyRef(e4)))
      case Event5(e1, e2, e3, e4, e5) => sendEvent("sq2", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3), toAnyRef(e4), toAnyRef(e5)))
      case Event6(e1, e2, e3, e4, e5, e6) => sendEvent("sq2", Array(toAnyRef(e1), toAnyRef(e2), toAnyRef(e3), toAnyRef(e4), toAnyRef(e5), toAnyRef(e6)))
    }
    case unhandledMessage =>log.info(s"unhandled message $unhandledMessage")
  }



  def handleTransitionRequest(algorithm: PlacementAlgorithm): Unit = {
    log.info("initiating runrun trnansition on SequenceNode")
    val requester = sender()

    Future{
      //Don't block normal execution of this actor
      val address = algorithm.algorithm.findOptimalNode(this.context.system, cluster, publishers, coordinates)
      val successor = context.actorOf(Props(
                        SequenceNode(
                          mode,
                          query,
                          publishers,
                          createdCallback,
                          eventCallback
                        )).withDeploy(Deploy(scope = RemoteScope(address))),s"SequenceNode${UUID.randomUUID.toString}"
                      )

      successor ! StartExecutionWithDependencies(subscribers, delta)
      Thread.sleep(delta)
      this.started = false
      requester ! TransferredState(algorithm, successor)
      self ! PoisonPill
    }
  }


  override def postStop(): Unit = {
    destroyServiceProvider()
  }

}

object SequenceNode {

  def createArrayOfNames(noReqStream: NStream): Array[String] = noReqStream match {
    case _: NStream1[_] => Array("e1")
    case _: NStream2[_, _] => Array("e1", "e2")
    case _: NStream3[_, _, _] => Array("e1", "e2", "e3")
    case _: NStream4[_, _, _, _] => Array("e1", "e2", "e3", "e4")
    case _: NStream5[_, _, _, _, _] => Array("e1", "e2", "e3", "e4", "e5")
  }

  def createArrayOfClasses(noReqStream: NStream): Array[Class[_]] = {
    val clazz: Class[_] = classOf[AnyRef]
    noReqStream match {
      case _: NStream1[_] => Array(clazz)
      case _: NStream2[_, _] => Array(clazz, clazz)
      case _: NStream3[_, _, _] => Array(clazz, clazz, clazz)
      case _: NStream4[_, _, _, _] => Array(clazz, clazz, clazz, clazz)
      case _: NStream5[_, _, _, _, _] => Array(clazz, clazz, clazz, clazz, clazz)
    }
  }

}
