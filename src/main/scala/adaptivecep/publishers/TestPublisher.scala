package adaptivecep.publishers

import adaptivecep.graph.nodes.traits.Node._
import adaptivecep.publishers.Publisher._

case class TestPublisher() extends Publisher {

  override def receive: Receive = {
    case Subscribe() =>
      super.receive(Subscribe())
    case message =>
      subscribers.foreach(_ ! message)
  }

}
