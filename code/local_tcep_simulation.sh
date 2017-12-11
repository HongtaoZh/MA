#!/usr/bin/env bash

#find . -name '*.csv' -delete

if [[ "$OSTYPE" == "darwin"* ]]; then
sbt one-jar
ttab 'java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.VivaldiApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2549'
ttab 'java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.BenchmarkingApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2550'
ttab 'java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.EmptyApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2557'
ttab 'java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.EmptyApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2558'
ttab 'java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.PublisherApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2551 --name A'
ttab 'java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.PublisherApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2552 --name B'
ttab 'java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.PublisherApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2553 --name C'
ttab 'java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.simulation.trasitivecep.SimulationRunner -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar .'
else
nohup java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.VivaldiApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2549 &> viv.out&
nohup java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.BenchmarkingApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2550 &> bench.out&
nohup java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.EmptyApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2557 &> empty1.out&
nohup java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.EmptyApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2558 &> empty2.out&
nohup java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.PublisherApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2551 --name A &> puba.out&
nohup java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.PublisherApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2552 --name B &> pubb.out&
nohup java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.machinenodes.PublisherApp -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip 127.0.0.1 --port 2553 --name C &> pubc.out&
nohup java -Dconfig.resource=/simulation.conf -Done-jar.main.class=adaptivecep.simulation.trasitivecep.SimulationRunner -jar ./target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar . &> subs.out&
echo "printing logs in out files, use tail -f <file-name> to see log of particular process"
fi