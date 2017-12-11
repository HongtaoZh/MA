package adaptivecep.machinenodes

import adaptivecep.config.ConfigurationParser
import adaptivecep.placement.benchmarking.BenchmarkingNode
import akka.actor.{ActorSystem, Props}

/**
  * Just creates a `Benchmarking` Actor which uses bechmarking configuration to categorize algorithms
  * Created by raheel
  * on 09/08/2017.
  */

object BenchmarkingApp extends App with ConfigurationParser {
  logger.info("booting up BenchmarkingApp")

  val actorSystem: ActorSystem = ActorSystem(config.getString("clustering.cluster.name"), config)
  actorSystem.actorOf(Props(new BenchmarkingNode()), "BenchmarkingNode")

  override def getRole: String = "BenchmarkingApp"

  override def getArgs: Array[String] = args
}
