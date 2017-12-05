package adaptivecep.machinenodes

import adaptivecep.config.Parser
import adaptivecep.placement.vivaldi.VivaldiCoordinates
import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

/**
  * Just creates a `TaskManagerActor` which could receive tasks from PlacementAlgorithms
  * Created by raheel
  * on 09/08/2017.
  */
object EmptyApp extends Parser {
  val log = LoggerFactory.getLogger(getClass)
  log.info("booting up EmptyApp")

  val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + options('port))
    .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.hostname=" + options('ip)))
    .withFallback(ConfigFactory.parseString("akka.cluster.roles=[Candidate]"))
    .withFallback(ConfigFactory.load())

  val actorSystem: ActorSystem = ActorSystem(config.getString("clustering.cluster.name"), config)
  actorSystem.actorOf(Props(new TaskManagerActor with VivaldiCoordinates), "TaskManager")
}