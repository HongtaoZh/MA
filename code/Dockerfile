FROM 1science/java:oracle-jre-8

ENV SBT_VERSION 0.13.15
ENV SBT_HOME /usr/local/sbt
ENV PATH ${PATH}:${SBT_HOME}/bin

# Install sbt
RUN curl -sL "http://dl.bintray.com/sbt/native-packages/sbt/$SBT_VERSION/sbt-$SBT_VERSION.tgz" | gunzip | tar -x -C /usr/local && \
    echo -ne "- with sbt $SBT_VERSION\n" >> /root/.built

WORKDIR /app

ARG MAIN
ENV MAIN ${MAIN}
ARG ARGS
ENV ARGS ${ARGS}

VOLUME /root/.ivy2
VOLUME /root/.sbt

RUN apk update && \
    apk upgrade && \
    apk add iproute2

RUN ln -s /usr/lib/tc /lib/tc

COPY ./ /app/
RUN sbt one-jar
ENTRYPOINT java -Done-jar.main.class=${MAIN} -jar /app/target/scala-2.12/adaptivecep_2.12-0.0.1-SNAPSHOT-one-jar.jar ${ARGS}