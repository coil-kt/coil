#!/bin/bash
set -e

# Clean any previous Dokka docs.
rm -rf docs/api

# Build the docs.
./gradlew clean dokkaHtmlMultiModule
