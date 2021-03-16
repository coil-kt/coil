#!/bin/bash

# Build the Dokka docs.
./assemble_docs.sh

# Build and upload the artifacts to 'mavenCentral'.
./gradlew publish --no-daemon --no-parallel
