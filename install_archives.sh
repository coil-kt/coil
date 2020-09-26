#!/bin/bash

# Build the Dokka docs.
./assemble_docs.sh

# Build the new Dokka docs.
./gradlew installArchives --no-daemon --no-parallel
