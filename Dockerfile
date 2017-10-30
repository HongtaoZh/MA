FROM 1science/sbt

ARG MAIN
ENV MAIN ${MAIN}
ARG ARGS
ENV ARGS ${ARGS}

VOLUME ["~/.ivy2", "/root/.ivy2"]
VOLUME ["~/.sbt", "/root/.sbt"]

#RUN apk update && \
#    apk upgrade && \
#    apk add vim

COPY ./ /app/

RUN sbt one-jar
RUN export CLUSTER_IP="--ip $(/sbin/ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}')"
RUN echo "java -Done-jar.main.class=${MAIN} -jar /app/target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar ${CLUSTER_IP} ${ARGS}" >>run.sh

ENTRYPOINT java -Done-jar.main.class=${MAIN} -jar /app/target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar --ip $(/sbin/ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}') ${ARGS}


