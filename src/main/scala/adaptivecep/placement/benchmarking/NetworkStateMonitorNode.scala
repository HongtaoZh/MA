package adaptivecep.placement.benchmarking

import adaptivecep.ClusterActor
import akka.actor.{ActorLogging, ActorRef, Cancellable}
import akka.cluster.ClusterEvent.MemberUp

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by Raheel
  * on 21/09/2017.
  */
class NetworkStateMonitorNode extends ClusterActor with ActorLogging {

  var updateStateScheduler: Cancellable = _
  val registeredSubscribers = new ListBuffer[ActorRef]()
  var lastNewMemberCreatedTime: Long = _


  override def preStart(): Unit = {
    super.preStart()
    updateStateScheduler = this.context.system.scheduler.schedule(5 minutes, 5 minutes, this.self, UpdateState)
  }

  override def receive: Receive = {
    case MemberUp(member) => {
      lastNewMemberCreatedTime = System.currentTimeMillis()
    }
    case RegisterForStateUpdate => registeredSubscribers += sender()
    case CancelRegistration => registeredSubscribers -= sender()
    case UpdateState => updateState()
  }

  def updateState(): Unit = {
    val updatedProperties: ListBuffer[NetworkProperties] = new ListBuffer[NetworkProperties]()
    if(System.currentTimeMillis() - lastNewMemberCreatedTime > 5000){
      updatedProperties += LowChurnRate
    }

    registeredSubscribers.foreach(s => updatedProperties.foreach( p => s ! p))
  }
}

case object UpdateState
case object RegisterForStateUpdate
case object CancelRegistration

abstract class NetworkProperties()
case object HighChurnRate extends NetworkProperties
case object LowChurnRate extends NetworkProperties
