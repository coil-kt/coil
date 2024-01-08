#!/bin/bash
set -e

# Build and install the artifacts locally to 'mavenLocal'.
./gradlew publishToMavenLocal
