#!/bin/bash
set -e

# Regenerate the baseline profile.
./gradlew generateBaselineProfile

# Build and install the artifacts locally to 'mavenLocal'.
./gradlew publishToMavenLocal
