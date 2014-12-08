#!/bin/bash

mvn package
if [ $? -eq 0 ]; then
    java -cp target/classes:"target/NewsServer-1.0-SNAPSHOT.jar:target/dependency/*" \
            -Xdebug \
            -Xrunjdwp:transport=dt_socket,address=8008,server=y,suspend=y \
            -Dcom.sun.media.imageio.disableCodecLib=true \
            com.janknspank.TestRunner
else
    echo "Build failed"
fi

