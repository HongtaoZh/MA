#!/usr/bin/env bash

echo "stop all docker containers with image-name adaptivecepapp "
docker stop $(docker ps --filter ancestor=adaptivecepapp -a -q)

echo "removing all docker containers before starting new ones"
docker rm -f $(docker ps --filter ancestor=adaptivecepapp -a -q)
docker rm -f nserver

rm adaptiveCEP.log

echo "removing older image"
docker rmi adaptivecepapp -f

echo "building image"
docker build -t adaptivecepapp .

docker network rm isolated_nw
docker network create isolated_nw

pkill -f pumba
echo "running containers"
#docker run --network=isolated_nw --name nserver -d -p 123:123  cloudwattfr/ntpserver:latest
docker run --privileged --network=isolated_nw --name viv -e MAIN="adaptivecep.machinenodes.VivaldiApp" -e ARGS="--port 2549 --ip viv" -d -p 2549:2549 adaptivecepapp
docker run --privileged --network=isolated_nw --name bench -e MAIN="adaptivecep.machinenodes.BenchmarkingApp" -e ARGS="--port 2550 --ip bench" -p 2550:2550 -d adaptivecepapp
sleep 10 #wait for seed nodes to be initialized

docker run --privileged --network=isolated_nw --name simpla -e MAIN="adaptivecep.machinenodes.EmptyApp" -e ARGS="--port 2557 --ip simpla" -d -p 2557:2557 adaptivecepapp
docker run --privileged --network=isolated_nw --name simplb -e MAIN="adaptivecep.machinenodes.EmptyApp" -e ARGS="--port 2558 --ip simplb" -d -p 2558:2558 adaptivecepapp
docker run --privileged --network=isolated_nw --name simplc -e MAIN="adaptivecep.machinenodes.EmptyApp" -e ARGS="--port 2559 --ip simplc" -d -p 2559:2559 adaptivecepapp
docker run --privileged --network=isolated_nw --name simpld -e MAIN="adaptivecep.machinenodes.EmptyApp" -e ARGS="--port 2560 --ip simpld" -d -p 2560:2560 adaptivecepapp
docker run --privileged --network=isolated_nw --name simple -e MAIN="adaptivecep.machinenodes.EmptyApp" -e ARGS="--port 2562 --ip simple" -d -p 2562:2562 adaptivecepapp
docker run --privileged --network=isolated_nw --name simplf -e MAIN="adaptivecep.machinenodes.EmptyApp" -e ARGS="--port 2563 --ip simplf" -d -p 2563:2563 adaptivecepapp
docker run --privileged --network=isolated_nw --name simplg -e MAIN="adaptivecep.machinenodes.EmptyApp" -e ARGS="--port 2564 --ip simplg" -d -p 2564:2564 adaptivecepapp
docker run --privileged --network=isolated_nw --name simplh -e MAIN="adaptivecep.machinenodes.EmptyApp" -e ARGS="--port 2565 --ip simplh" -d -p 2565:2565 adaptivecepapp

docker run --privileged --network=isolated_nw --name puba -e MAIN="adaptivecep.machinenodes.PublisherApp" -e ARGS="--port 2551 --name A --ip puba" -d -p 2551:2551 adaptivecepapp
docker run --privileged --network=isolated_nw --name pubb -e MAIN="adaptivecep.machinenodes.PublisherApp" -e ARGS="--port 2552 --name B --ip pubb" -d -p 2552:2552 adaptivecepapp
docker run --privileged --network=isolated_nw --name pubc -e MAIN="adaptivecep.machinenodes.PublisherApp" -e ARGS="--port 2553 --name C --ip pubc" -d -p 2553:2553 adaptivecepapp
docker run --privileged --network=isolated_nw --name subs -e MAIN="adaptivecep.simulation.trasitivecep.SimulationRunner" -e ARGS=" ." -d -p 2561:2561 adaptivecepapp


if [[ "$OSTYPE" == "darwin"* ]]; then
ttab 'docker exec -it viv bash'
ttab 'docker exec -it bench bash'

ttab 'docker exec -it puba bash'
ttab 'docker exec -it pubb bash'
ttab 'docker exec -it pubc bash'
ttab 'docker exec -it simpla bash'
ttab 'docker exec -it simplb bash'

ttab 'docker exec -it subs bash'
fi

sleep 20
pumba netem --duration 25m delay --time 800 --jitter 300 puba &
pumba netem --duration 25m delay --time 700 --jitter 200 pubb &
pumba netem --duration 25m delay --time 600 --jitter 100 pubc &

pumba netem --duration 25m delay --time 300 --jitter 50 simpla &
pumba netem --duration 25m delay --time 200 --jitter 100 simplb &
pumba netem --duration 25m delay --time 300 --jitter 130 simplc &
pumba netem --duration 25m delay --time 300 --jitter 150 simpld &
pumba netem --duration 25m delay --time 300 --jitter 200 simplf &
pumba netem --duration 25m delay --time 300 --jitter 220 simplg &
pumba netem --duration 25m delay --time 300 --jitter 250 simplh &
pumba netem --duration 25m delay --time 300 --jitter 280 puba &
