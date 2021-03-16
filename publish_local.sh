#!/bin/bash

# Build the Dokka docs.
./assemble_docs.sh

# Build and install the artifacts locally to 'mavenLocal'.
./gradlew publishToMavenLocal --no-daemon --no-parallel
