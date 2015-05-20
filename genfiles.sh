#!/bin/bash

# Converts Google Protocol Buffer files (*.proto) to their respective *.java
# generated sources, putting them in /genfiles/.

protoc \
    src/com/janknspank/database/extensions.proto \
    src/com/janknspank/proto/article.proto \
    src/com/janknspank/proto/classifier.proto \
    src/com/janknspank/proto/core.proto \
    src/com/janknspank/proto/crawler.proto \
    src/com/janknspank/proto/expressions.proto \
    src/com/janknspank/proto/interpreter.proto \
    src/com/janknspank/proto/local.proto \
    src/com/janknspank/proto/notifications.proto \
    src/com/janknspank/proto/rank.proto \
    src/com/janknspank/proto/user.proto \
    -Isrc/ \
    -Isupport/ \
    --java_out=src/

