#!/bin/bash
set -e

# Build and upload the artifacts to 'mavenCentral'.
./gradlew publish
