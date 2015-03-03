#!/bin/bash

mvn package
if [ $? -eq 0 ]; then
  java -cp "target/NewsServer-1.0-SNAPSHOT.jar:target/dependency/*" \
      -Dfile.encoding=UTF8 \
      -Xdebug \
      -Xrunjdwp:transport=dt_socket,address=8009,server=y,suspend=n \
      -Dcom.sun.media.imageio.disableCodecLib=true \
      com.janknspank.bizness.IosPushNotificationHelper $@
else
  echo "Build failed"
fi