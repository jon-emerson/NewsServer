#!/bin/bash

mvn package
if [ $? -eq 0 ]; then
  java -cp target/classes:"target/NewsServer-1.0-SNAPSHOT.jar:target/dependency/*" \
      -Dfile.encoding=UTF8 \
      -Xdebug \
      -Xrunjdwp:transport=dt_socket,address=8018,server=y,suspend=n \
      -Dcom.sun.media.imageio.disableCodecLib=true \
      -Dnewrelic.environment=development \
      -javaagent:newrelic/newrelic.jar \
      -Dnewrelic.config.file=newrelic.yml \
      com.janknspank.server.NewsServer
else
  echo "Build failed"
fi

