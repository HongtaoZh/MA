# TransitiveCEP
Transitive CEP is a research based project based on AdaptiveCEP, the project provides the transitions support in CEP strategies as per cahgne in envronmental conditions and the chagne in user demands

##Running Docker simulation
### Mac users:
* Run docker in the background
* Install PUMBA :https://github.com/gaia-adm/pumba
* Make sure that docker has atleast 6 GB RAM (Simulation wont work if RAM is insufficient)
* Install NPM and ttab
* Run publish_docker.sh shell script. The script will create containers and open the container in new tabs
* run tail -f adaptiveCEP.log to see the logs
* wait for simulation to end and copy pietuzuch.csv and starks.csv files and generate graphs

### Ubuntu users:
* Run docker in the background
* Make sure that docker has atleast 6 GB RAM (Simulation wont work if RAM is insufficient)
* Run publish_docker.sh script
* the script will generate the names of the containers
* use docker exec -it <name> bash to open the docker containers
* run tail -f command to the live logs 


##Running Local simulation
### Mac users:
* Run local_tcep_simulation.sh script

### Ubuntu users:
* Run sbt one-jar command to generate the jar of the project
* Run the following commands in new tabs
```
 java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.VivaldiApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2549
 java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.BenchmarkingApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2550
 java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.EmptyApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2557
 java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.EmptyApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2558
 java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.EmptyApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2559
 java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.EmptyApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2560
 java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.EmptyApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2562
 java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.PublisherApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2551 --name A
 java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.PublisherApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2552 --name B
 java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.PublisherApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2553 --name C
 java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.simulation.trasitivecep.SimulationRunner -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar .

``` 

# AdaptiveCEP

AdaptiveCEP is a research project exploring ways to embed quality demands into queries for event processing systems. As such, its main contributions are:

+ **AdaptiveCEP DSL** for _expressing_ EP queries including quality demands
+ **AdaptiveCEP Akka/Esper Backend** for _executing_ EP queries including quality demands

AdaptiveCEP DSL is simply an embedded Scala DSL. AdaptiveCEP Akka/Esper Backend is a tree of Akka actors representing a query, where each actor represents a primitive or an operator of the query.

**Demo: Doing `sbt run` in the project root will cause a sample query to execute! Check it out!**


## AdaptiveCEP DSL

AdaptiveCEP DSL is the domain-specific language to express event processing queries. Queries are made up of primitives and operators and can be arbitrarily nested and composed.

Internally, queries expressed using AdaptiveCEP DSL are represented by case classes. This case class representation can then be passed to AdaptiveCEP Akka/Esper Backend for execution, but it may also be interpreted by another backend.

### Examples

+ Joining streams

    + Example 1: `join` of two streams:

    ```scala
    val q1 = streamA join streamB in (slidingWindow(3 seconds), tumblingWindow(3 instances))
    ```

    + Example 2: `join` composing a stream with another subquery:

    ```scala
    val q2 = streamC join q1 in (slidingWindow(2 instances), tumblingWindow(2 seconds))
    ```

    + Available windows:

    ```scala
    slidingWindow(x instances)
    slidingWindow(x seconds)
    tumblingWindow(x instances)
    tumblingWindow(x seconds)
    ```

+ Filtering streams

    + Example 1: Only keep those event instances `where` element 2 is smaller than 10.

    ```scala
    val q3 = streamA where { (_, value, _) => value < 10 }
    ```

    + Example 2: Only keep those event instances `where` element 1 equals element 2.

    ```scala
    val q4 = streamA where { (value1, value2, _) => value1 == value2 }
    ```

+ Quality demand

    ```scala
    val q5 =
      (streamA
        demand (frequency > ratio(3 instances, 5 seconds))
        where { (_, value, _) => value < 10 })
    ```


## AdaptiveCEP Akka/Esper Backend

As said, the AdaptiveCEP backend is a tree of Akka actors representing a query, with each actor representing a primitive or an operator of that query. Given a query as well as the `ActorRef`s to the event publishers, the backend will automatically build and start emitting events.

A node representing a primitive (so far, there's only one primitive: a subscription to a stream) is just a plain Akka actor subscribing to the respective publisher, whereas nodes representing operators are all running an independent instance of the Esper event processing engine.

## Deployment of AdaptiveCEP on Docker

### Requirements

Install [docker](https://www.docker.com/what-docker).

### Step-by-step guide

Move to AdaptiveCEP project.

**Configuration files & Docker file**
All the configuration and Docker file can be found under [docker](docker) folder.

**Run docker containers**

Use seven terminal windows.

Now, execute the following command:

    docker run --name=pubA -ti 1science/sbt /bin/bash (terminal#1)
    docker run --name=pubB -ti 1science/sbt /bin/bash (terminal#2)
    docker run --name=pubC -ti 1science/sbt /bin/bash (terminal#3)
    docker run --name=pubD -ti 1science/sbt /bin/bash (terminal#4)
    docker run --name=subs -ti 1science/sbt /bin/bash (terminal#5)
    docker run --name=broker -ti 1science/sbt /bin/bash (terminal#6)


**Copy project into the respective docker container**

In order to see all the runnning containers run the following command.

    docker ps (terminal#7) # to see all the running containers

The CONTAINER ID to [copy](https://docs.docker.com/engine/reference/commandline/cp/) AdaptiveCEP application from local system to docker file system.

    path/to/AdaptiveCEP# docker cp . <CONTAINER-ID>:/app

Make sure to copy application.conf from [docker](docker) folder.

Reference:
- https://docs.docker.com/engine/userguide/networking/
-  https://github.com/1science/docker-sbt

**Run the application**

Make sure to run the command in the same order.

    sbt "runMain adaptivecep.publishers.PublisherApp --ip 127.0.0.1 --port 2551 --name A"         (terminal#1)
    sbt "runMain adaptivecep.publishers.PublisherApp --ip 127.0.0.1 --port 2552 --name B"         (terminal#2)
    sbt "runMain adaptivecep.publishers.PublisherApp --ip 127.0.0.1 --port 2553 --name C"         (terminal#3)
    sbt "runMain adaptivecep.publishers.PublisherApp --ip 127.0.0.1 --port 2554 --name D"         (terminal#4)
    sbt "runMain adaptivecep.broker.BrokerApp --ip 127.0.0.1 --port 2556"                         (terminal#6)
    sbt "runMain adaptivecep.subscriber.SubscriberApp --ip 127.0.0.1 --port 2555"                  (terminal#5)
