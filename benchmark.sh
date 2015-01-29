#!/bin/bash

mvn package
if [ $? -eq 0 ]; then
  java -cp "target/NewsServer-1.0-SNAPSHOT/WEB-INF/classes:target/NewsServer-1.0-SNAPSHOT.war:target/dependency/*" \
      -Dfile.encoding=UTF8 \
      -Xdebug \
      -Xrunjdwp:transport=dt_socket,address=8009,server=y,suspend=y \
      -Dcom.sun.media.imageio.disableCodecLib=true \
      com.janknspank.classifier.Benchmark $@
else
  echo "Build failed"
fi