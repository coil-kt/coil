#!/bin/bash

# Build and install the artifacts locally to 'mavenLocal'.
./gradlew publishToMavenLocal --no-daemon --no-parallel
