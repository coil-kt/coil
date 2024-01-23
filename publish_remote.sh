#!/bin/bash
set -e

# Regenerate the baseline profile.
./gradlew generateBaselineProfile

# Build and upload the artifacts to 'mavenCentral'.
./gradlew publish
