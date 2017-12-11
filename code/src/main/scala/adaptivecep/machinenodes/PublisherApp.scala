package adaptivecep.machinenodes

import adaptivecep.config.ConfigurationParser
import adaptivecep.data.Events.Event1
import adaptivecep.publishers.{RandomPublisher, RegularPublisher}
import akka.actor.{ActorSystem, Props}

/**
  * Created by raheel
  * on 09/08/2017.
  */
object PublisherApp extends ConfigurationParser with App {
  logger.info("booting up PublisherApp")

  Thread.sleep(10000)
  val actorSystem: ActorSystem = ActorSystem(config.getString("clustering.cluster.name"), config)

  var actorPublisher = options('name) match {
    case "A" => actorSystem.actorOf(Props(RegularPublisher(1000, id => Event1(id))), "A")
    case "B" => actorSystem.actorOf(Props(RegularPublisher(2000, id => Event1(id * 2))), "B")
    case "C" => actorSystem.actorOf(Props(RegularPublisher(3000, id => Event1(id.toFloat))), "C")
    case "D" => actorSystem.actorOf(Props(RegularPublisher(4000, id => Event1(s"String($id)"))), "D")
    case _ => actorSystem.actorOf(Props(RegularPublisher(5000, id => Event1(id))), "A")
  }

  override def getRole: String = "Publisher"
  override def getArgs: Array[String] = args
}
