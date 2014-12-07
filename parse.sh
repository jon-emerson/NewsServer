#!/bin/bash

ant
if [ $? -eq 0 ]; then
    java -cp target/classes:"dist/lib/twitternews.jar:lib/*" \
            -Xdebug \
            -Xrunjdwp:transport=dt_socket,address=8009,server=y,suspend=n \
            -Dcom.sun.media.imageio.disableCodecLib=true \
            com.janknspank.dom.Engine
else
    echo "Build failed"
fi

