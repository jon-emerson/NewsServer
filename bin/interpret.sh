#!/bin/bash

mvn -Dmaven.test.skip=true package
if [ $? -eq 0 ]; then
  java -cp target/classes:"target/NewsServer-1.0-SNAPSHOT.jar:target/dependency/*" \
      -Dfile.encoding=UTF8 \
      -Xdebug \
      -Xrunjdwp:transport=dt_socket,address=8123,server=y,suspend=n \
      -Dcom.sun.media.imageio.disableCodecLib=true \
      com.janknspank.crawler.Interpreter $@
else
  echo "Build failed"
fi

