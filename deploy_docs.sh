#!/bin/bash

# Clean any previous Dokka docs.
rm -rf docs/api

# Build the Dokka docs.
./gradlew clean :coil-base:dokka :coil-default:dokka :coil-gif:dokka

# Deploy to Github pages.
mkdocs gh-deploy
