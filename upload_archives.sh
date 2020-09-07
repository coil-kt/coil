#!/bin/bash

# Build the Dokka docs.
./assemble_docs.sh

# Build the new Dokka docs and upload the artifacts.
./gradlew uploadArchives --no-daemon --no-parallel
