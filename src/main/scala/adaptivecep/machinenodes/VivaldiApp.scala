package adaptivecep.machinenodes

import adaptivecep.config.Parser
import adaptivecep.placement.vivaldi.VivaldiRefNode
import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

/**
  * Just creates a `VivaldiRefNode` which is used by other nodes to set their coordinates
  * Created by raheel
  * on 09/08/2017.
  */
object VivaldiApp extends Parser {
  val log = LoggerFactory.getLogger(getClass)
  log.info("booting up Vivaldi's App")

  val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + options('port))
    .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.hostname=" + options('ip)))
    .withFallback(ConfigFactory.parseString("akka.cluster.roles=[VivaldiRef]"))
    .withFallback(ConfigFactory.load())

  val actorSystem: ActorSystem = ActorSystem(config.getString("clustering.cluster.name"), config)
  actorSystem.actorOf(Props(classOf[VivaldiRefNode]), "VivaldiRef")
}
