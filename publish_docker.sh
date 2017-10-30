#!/usr/bin/env bash

echo "stop all docker containers with image-name adaptivecepapp "
docker stop $(docker ps --filter ancestor=adaptivecepapp -a -q)

echo "removing all docker containers before starting new ones"
docker rm -f $(docker ps --filter ancestor=adaptivecepapp -a -q)


if [ $# -ne 0 ]; then
rm adaptiveCEP.log

echo "removing older image"
docker rmi adaptivecepapp -f

echo "building image"
docker build -t adaptivecepapp .
fi

docker network rm isolated_nw
docker network create --driver bridge isolated_nw

echo "running containers"
docker run --privileged --network=isolated_nw --name viv -e MAIN="adaptivecep.machinenodes.VivaldiApp" -e ARGS="--port 2549" -d adaptivecepapp
docker run --privileged --network=isolated_nw --name bma -e MAIN="adaptivecep.machinenodes.BenchmarkingApp" -e ARGS="--port 2550" -d adaptivecepapp
docker run --privileged --network=isolated_nw --name puba -e MAIN="adaptivecep.machinenodes.PublisherApp" -e ARGS="--port 2551 --name A" -d adaptivecepapp
docker run --privileged --network=isolated_nw --name pubb -e MAIN="adaptivecep.machinenodes.PublisherApp" -e ARGS="--port 2552 --name B" -d adaptivecepapp
docker run --privileged --network=isolated_nw --name pubc -e MAIN="adaptivecep.machinenodes.PublisherApp" -e ARGS="--port 2553 --name C" -d adaptivecepapp
docker run --privileged --network=isolated_nw --name pubd -e MAIN="adaptivecep.machinenodes.PublisherApp" -e ARGS="--port 2554 --name D" -d adaptivecepapp
docker run --privileged --network=isolated_nw --name simpla -e MAIN="adaptivecep.machinenodes.EmptyApp" -e ARGS="--port 2557" -d adaptivecepapp
docker run --privileged --network=isolated_nw --name simplb -e MAIN="adaptivecep.machinenodes.EmptyApp" -e ARGS="--port 2558" -d adaptivecepapp
docker run --privileged --network=isolated_nw --name simplc -e MAIN="adaptivecep.machinenodes.EmptyApp" -e ARGS="--port 2559" -d adaptivecepapp
docker run --privileged --network=isolated_nw --name simpld -e MAIN="adaptivecep.machinenodes.EmptyApp" -e ARGS="--port 2560" -d adaptivecepapp
docker run --privileged --network=isolated_nw --name subs -e MAIN="adaptivecep.machinenodes.SubscriberApp" -e ARGS="--port 2561" -d adaptivecepapp


if [[ "$OSTYPE" == "darwin"* ]]; then
ttab 'docker exec -it viv bash'
ttab 'docker exec -it bma bash'

ttab 'docker exec -it brok bash'

ttab 'docker exec -it puba bash'
ttab 'docker exec -it pubb bash'
ttab 'docker exec -it pubc bash'
ttab 'docker exec -it pubd bash'
ttab 'docker exec -it simpla bash'
ttab 'docker exec -it simplb bash'
ttab 'docker exec -it simplc bash'
ttab 'docker exec -it simpld bash'

ttab 'docker exec -it subs bash'
fi