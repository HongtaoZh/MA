package adaptivecep.machinenodes

import adaptivecep.config.ConfigurationParser
import adaptivecep.placement.vivaldi.VivaldiCoordinates
import akka.actor.{ActorSystem, Props}

/**
  * Startup Subscriber Application and Creates TaskManagerActor
  *
  * @example sbt "runMain adaptivecep.subscriber.SubscriberApp portNo"
  */

object SubscriberApp extends App with ConfigurationParser {
  logger.info("booting subscriber")
  Thread.sleep(10000)

  val actorSystem: ActorSystem = ActorSystem(config.getString("clustering.cluster.name") , config)
  actorSystem.actorOf(Props[Subscriber],"subscriber")

  actorSystem.actorOf(Props(new TaskManagerActor with VivaldiCoordinates), "TaskManager")

  override def getRole: String = "Subscriber"
  override def getArgs: Array[String] = args
}
