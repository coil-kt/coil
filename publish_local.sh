#!/bin/bash
set -e

# Regenerate the baseline profiles.
./gradlew generateBaselineProfile

# Build and install the artifacts locally to 'mavenLocal'.
./gradlew publishToMavenLocal
