package adaptivecep.simulation.adaptive.cep

import java.io.File

import adaptivecep.machinenodes.TaskManagerActor
import adaptivecep.placement.vivaldi.VivaldiCoordinates
import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory


/**
  * Runs the CEP simulation.
  * The code requires an optional commandline parameter for "directory path" where simulation results will be saved as
  * CSV files.
  * see local_simulation.sh file for more details.
  */
object SimulationRunner extends App {

  val defaultConfig = ConfigFactory.load()
  val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=${defaultConfig.getString("simulation.port")}")
                            .withFallback(ConfigFactory.parseString(
                              s"akka.remote.netty.tcp.hostname=${defaultConfig.getString("simulation.host")}"))
                            .withFallback(ConfigFactory.parseString("akka.cluster.roles=[Subscriber]"))
                            .withFallback(defaultConfig)

  val actorSystem: ActorSystem = ActorSystem(config.getString("clustering.cluster.name"), config)

  val directory =
    args.headOption map { new File(_) } flatMap { directory =>
      if (!directory.isDirectory) {
        System.err.println(s"$directory does not exist or is not a directory")
        None
      }
      else
        Some(directory)
    }


  actorSystem.actorOf(Props(new TaskManagerActor with VivaldiCoordinates), "TaskManager")
  actorSystem.actorOf(Props(new SimulationSetup(directory)),"simulationSetup")
}