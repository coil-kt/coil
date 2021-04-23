#!/bin/bash

# Build and upload the artifacts to 'mavenCentral'.
./gradlew publish --no-daemon --no-parallel
