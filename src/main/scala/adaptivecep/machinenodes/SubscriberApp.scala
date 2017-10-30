package adaptivecep.machinenodes

import adaptivecep.config.Parser
import adaptivecep.placement.vivaldi.VivaldiCoordinates
import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

/**
  * Startup Subscriber Application and Creates TaskManagerActor
  *
  * @example sbt "runMain adaptivecep.subscriber.SubscriberApp portNo"
  */

object SubscriberApp extends Parser {

  // Override the configuration of the port when specified as program argument
  /*val clientPort = if (args.isEmpty) "0" else args(0)
  val operatorPort: String = if (!args.isEmpty && args.length > 1) args(1) else "0"*/

  val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + options('port) )
    .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.hostname=" + options('ip) ))
    .withFallback(ConfigFactory.parseString("akka.cluster.roles=[Subscriber]"))
    .withFallback(ConfigFactory.load())

  val actorSystem: ActorSystem = ActorSystem(config.getString("clustering.cluster.name") , config)
  actorSystem.actorOf(Props[Subscriber],"subscriber")

  actorSystem.actorOf(Props(new TaskManagerActor with VivaldiCoordinates), "TaskManager")
}
