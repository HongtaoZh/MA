name := "AdaptiveCEP"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  //"com.typesafe.akka" %% "akka-actor"   % "2.5.6",
  "com.typesafe.akka" % "akka-remote_2.12" % "2.5.6",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.6"  % "test",
  "com.espertech"     %  "esper"        % "5.5.0",
  "org.scalatest"     %% "scalatest"    % "3.0.1"   % "test",
  "com.typesafe.akka" % "akka-cluster-metrics_2.12" % "2.5.6",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.6",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  // https://mvnrepository.com/artifact/com.twitter/chill-akka_2.12
  "com.twitter" % "chill-akka_2.12" % "0.9.2"

)

libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.12.1"

import com.github.retronym.SbtOneJar._

oneJarSettings
libraryDependencies += "commons-lang" % "commons-lang" % "2.6"

//mainClass in(Compile, run) := Some("de.kom.tud.cep.Main")
//mainClass in oneJar := Some("de.kom.tud.cep.Main")
mainClass in oneJar := Some("adaptivecep.broker.BrokerApp")
mainClass in oneJar := Some("adaptivecep.simulation.trasitivecep.SimulationRunner")

