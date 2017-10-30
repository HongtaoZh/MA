package adaptivecep.machinenodes

import adaptivecep.config.Parser
import adaptivecep.data.Events.Event1
import adaptivecep.publishers.RandomPublisher
import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

/**
  * Created by raheel
  * on 09/08/2017.
  */
object PublisherApp extends Parser {

  val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + options('port))
    .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.hostname=" + options('ip)))
    .withFallback(ConfigFactory.parseString("akka.cluster.roles=[Publisher]"))
    .withFallback(ConfigFactory.load())

  val actorSystem: ActorSystem = ActorSystem(config.getString("clustering.cluster.name"), config)

  // @TODO revisit code below
  var actorPublisher = options('name) match {
    case "A" => actorSystem.actorOf(Props(RandomPublisher(id => Event1(id))), "A")
    case "B" => actorSystem.actorOf(Props(RandomPublisher(id => Event1(id * 2))), "B")
    case "C" => actorSystem.actorOf(Props(RandomPublisher(id => Event1(id.toFloat))), "C")
    case "D" => actorSystem.actorOf(Props(RandomPublisher(id => Event1(s"String($id)"))), "D")
    case _ => actorSystem.actorOf(Props(RandomPublisher(id => Event1(id))), "A")
  }

}
