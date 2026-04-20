#!/bin/bash
set -e

GRADLE_ARGS=("$@")

# Clean any previous Dokka docs.
rm -rf docs/api

# Build the docs.
./gradlew "${GRADLE_ARGS[@]}" clean dokkaGenerate
