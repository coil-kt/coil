#!/bin/bash

# Clean any previous Dokka docs.
rm -rf docs/api

# Build the new Dokka docs.
./gradlew clean installArchives --no-daemon --no-parallel
