package adaptivecep.data

import adaptivecep.data.Structures.MachineLoad
import akka.actor.ActorRef

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

/**
  * Helpers object for conversions and representation of different events in terms of
  * case classes.
  * */
@SerialVersionUID(15L)
object Events extends java.io.Serializable {

  case object Created
  case object DependenciesRequest
  case class DependenciesResponse(dependencies: Seq[ActorRef])


  sealed trait MonitoringData

  sealed trait Event {
    val createTime = System.currentTimeMillis()
    val monitoringData = ListBuffer.empty[MonitoringData]

    def getMonitoringItem[A <: MonitoringData: ClassTag](): Option[A] = {
      val clazz = implicitly[ClassTag[A]].runtimeClass
      val res = monitoringData.find(clazz.isInstance(_))
      if(res.isDefined){
        Some(res.get.asInstanceOf[A])
      }else{
        Option.empty
      }
    }

    def getOrCreateMonitoringItem[A <: MonitoringData: ClassTag](defaultValue: A): A = {
      val clazz = implicitly[ClassTag[A]].runtimeClass
      val items = monitoringData.find(clazz.isInstance(_))
      if(items.nonEmpty){
        items.head.asInstanceOf[A]
      }else{
        monitoringData += defaultValue
        defaultValue
      }
    }
  }

  case class MessageHops(var hops: Int) extends MonitoringData
  case class AverageLoad(var load: MachineLoad) extends MonitoringData


  case class Event1(e1: Any)                                              extends Event
  case class Event2(e1: Any, e2: Any)                                     extends Event
  case class Event3(e1: Any, e2: Any, e3: Any)                            extends Event
  case class Event4(e1: Any, e2: Any, e3: Any, e4: Any)                   extends Event
  case class Event5(e1: Any, e2: Any, e3: Any, e4: Any, e5: Any)          extends Event
  case class Event6(e1: Any, e2: Any, e3: Any, e4: Any, e5: Any, e6: Any) extends Event

  val errorMsg: String = "Panic! Control flow should never reach this point!"

  case class EventToBoolean[A, B](event2: Event2){
    def apply[A, B](f: (A, B) => Boolean): Event => Boolean = {
      case Event2(e1, e2) => f.asInstanceOf[(Any, Any) => Boolean](e1, e2)
      case _ => sys.error(errorMsg)
    }
  }
  def toFunEventAny[A](f: (A) => Any): Event => Any = {
    case Event1(e1) => f.asInstanceOf[(Any) => Any](e1)
    case _ => sys.error(errorMsg)
  }

  def toFunEventAny[A, B](f: (A, B) => Any): Event => Any = {
    case Event2(e1, e2) => f.asInstanceOf[(Any, Any) => Any](e1, e2)
    case _ => sys.error(errorMsg)
  }

  def toFunEventAny[A, B, C](f: (A, B, C) => Any): Event => Any = {
    case Event3(e1, e2, e3) => f.asInstanceOf[(Any, Any, Any) => Any](e1, e2, e3)
    case _ => sys.error(errorMsg)
  }

  def toFunEventAny[A, B, C, D](f: (A, B, C, D) => Any): Event => Any = {
    case Event4(e1, e2, e3, e4) => f.asInstanceOf[(Any, Any, Any, Any) => Any](e1, e2, e3, e4)
    case _ => sys.error(errorMsg)
  }

  def toFunEventAny[A, B, C, D, E](f: (A, B, C, D, E) => Any): Event => Any = {
    case Event5(e1, e2, e3, e4, e5) => f.asInstanceOf[(Any, Any, Any, Any, Any) => Any](e1, e2, e3, e4, e5)
    case _ => sys.error(errorMsg)
  }

  def toFunEventAny[A, B, C, D, E, F](f: (A, B, C, D, E, F) => Any): Event => Any = {
    case Event6(e1, e2, e3, e4, e5, e6) => f.asInstanceOf[(Any, Any, Any, Any, Any, Any) => Any](e1, e2, e3, e4, e5, e6)
    case _ => sys.error(errorMsg)
  }

  def toFunEventBoolean[A](f: (A) => Boolean): Event => Boolean = {
    case Event1(e1) => f.asInstanceOf[(Any) => Boolean](e1)
    case _ => sys.error(errorMsg)
  }

  def toFunEventBoolean[A, B](f: (A, B) => Boolean): Event => Boolean = {
    case Event2(e1, e2) => f.asInstanceOf[(Any, Any) => Boolean](e1, e2)
    case _ => sys.error(errorMsg)
  }

  def toFunEventBoolean[A, B, C](f: (A, B, C) => Boolean): Event => Boolean = {
    case Event3(e1, e2, e3) => f.asInstanceOf[(Any, Any, Any) => Boolean](e1, e2, e3)
    case _ => sys.error(errorMsg)
  }

  def toFunEventBoolean[A, B, C, D](f: (A, B, C, D) => Boolean): Event => Boolean = {
    case Event4(e1, e2, e3, e4) => f.asInstanceOf[(Any, Any, Any, Any) => Boolean](e1, e2, e3, e4)
    case _ => sys.error(errorMsg)
  }

  def toFunEventBoolean[A, B, C, D, E](f: (A, B, C, D, E) => Boolean): Event => Boolean = {
    case Event5(e1, e2, e3, e4, e5) => f.asInstanceOf[(Any, Any, Any, Any, Any) => Boolean](e1, e2, e3, e4, e5)
    case _ => sys.error(errorMsg)
  }

  def toFunEventBoolean[A, B, C, D, E, F](f: (A, B, C, D, E, F) => Boolean): Event => Boolean = {
    case Event6(e1, e2, e3, e4, e5, e6) => f.asInstanceOf[(Any, Any, Any, Any, Any, Any) => Boolean](e1, e2, e3, e4, e5, e6)
    case _ => sys.error(errorMsg)
  }

}
