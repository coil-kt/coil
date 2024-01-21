#!/bin/bash
set -e

# Regenerate the baseline profiles.
./gradlew generateBaselineProfile

# Build and upload the artifacts to 'mavenCentral'.
./gradlew publish
