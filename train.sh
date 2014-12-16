#!/bin/bash

# This will cause the shell to exit immediately if a simple command exits with a
# nonzero exit value. 
set -e

mvn package

java -cp target/classes:"target/NewsServer-1.0-SNAPSHOT.jar:target/dependency/*" \
    -Xdebug \
    -Xrunjdwp:transport=dt_socket,address=8008,server=y,suspend=n \
    -Dcom.sun.media.imageio.disableCodecLib=true \
    com.janknspank.dom.training.TrainingDataCollator

opennlp TokenNameFinderTrainer \
    -model opennlp/en-newsserver-person.bin \
    -lang en \
    -data trainingdata/en-newsserver-person.train \
    -encoding UTF-8

opennlp TokenNameFinderTrainer \
    -model opennlp/en-newsserver-organization.bin \
    -lang en \
    -data trainingdata/en-newsserver-organization.train \
    -encoding UTF-8

opennlp TokenNameFinderTrainer \
    -model opennlp/en-newsserver-location.bin \
    -lang en \
    -data trainingdata/en-newsserver-location.train \
    -encoding UTF-8
