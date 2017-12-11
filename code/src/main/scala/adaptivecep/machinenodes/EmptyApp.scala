package adaptivecep.machinenodes

import adaptivecep.config.ConfigurationParser
import adaptivecep.placement.vivaldi.VivaldiCoordinates
import akka.actor.{ActorSystem, Props}

/**
  * Just creates a `TaskManagerActor` which could receive tasks from PlacementAlgorithms
  * Created by raheel
  * on 09/08/2017.
  */
object EmptyApp extends ConfigurationParser with App {
  logger.info("booting up EmptyApp")
  Thread.sleep(10000)

  val actorSystem: ActorSystem = ActorSystem(config.getString("clustering.cluster.name"), config)
  actorSystem.actorOf(Props(new TaskManagerActor()), "TaskManager")

  override def getRole: String = "Candidate"
  override def getArgs: Array[String] = args
}