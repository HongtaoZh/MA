package adaptivecep.machinenodes

import adaptivecep.config.ConfigurationParser
import adaptivecep.placement.vivaldi.VivaldiRefNode
import akka.actor.{ActorSystem, Props}

/**
  * Just creates a `VivaldiRefNode` which is used by other nodes to set their coordinates
  * Created by raheel
  * on 09/08/2017.
  */
object VivaldiApp extends ConfigurationParser with App {
  logger.info("booting up Vivaldi's App")

  val actorSystem: ActorSystem = ActorSystem(config.getString("clustering.cluster.name"), config)
  actorSystem.actorOf(Props(classOf[VivaldiRefNode]), "VivaldiRef")

  override def getRole: String = "VivaldiRef"
  override def getArgs: Array[String] = args
}
