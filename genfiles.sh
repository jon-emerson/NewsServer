#!/bin/bash

# Converts Google Protocl Buffer files (*.proto) to their respective *.java
# generated sources, putting them in /genfiles/.

protoc \
  src/com/janknspank/proto/core.proto \
  -Isrc/ \
  -Isupport/ \
  --java_out=src/

