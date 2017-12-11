package adaptivecep.placement.vivaldi

import adaptivecep.ClusterActor
import akka.actor.ActorLogging

/**
  * Created by raheel
  * on 12/08/2017.
  *
  * All machine nodes send ping messages to this actor to find their coordinates by using latency
  * This implementation is not a full vivaldi's implementation rather its a rudimentary implementation
  */

//TODO: create separate proeject for maintaining vivaldi's coordinates
class VivaldiRefNode extends ClusterActor with ActorLogging {


  override def preStart(): Unit = {
    super.preStart()
    log.info("booting vivaldi ref node")
  }


  override def receive: Receive = {
    case Ping() => {
      log.info(s"sending back ping to ${sender().path.toString}")
      sender() ! Ping()
    }
    case CoordinatesRequest() => {
      log.info("received coordinates request")
      sender() ! CoordinatesResponse(0l)
    }

    case message => log.info(s"ignoring unknown message: $message")
  }

}

