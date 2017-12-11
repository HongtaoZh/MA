package adaptivecep.simulation.adaptive.cep

import adaptivecep.data.Events._
import adaptivecep.data.Queries._
import adaptivecep.data.Structures.MachineLoad
import adaptivecep.dsl.Dsl._
import adaptivecep.graph.qos._
import adaptivecep.graph.{CreatedCallback, QueryGraph}
import adaptivecep.publishers._
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.Cluster

object Main extends App {
  val actorSystem: ActorSystem = ActorSystem()
  actorSystem.actorOf(Props(BaseActor()))
}
