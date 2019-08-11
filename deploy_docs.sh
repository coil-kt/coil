#!/bin/bash

# Clean any previous Dokka docs.
rm -rf docs/api

# Build the Dokka docs.
./gradlew clean :coil-base:dokka :coil-default:dokka :coil-gif:dokka

# Copy outside files into the docs folder.
sed -e '/full documentation here/ { N; d; }' < README.md > docs/index.md
cp CONTRIBUTING.md docs/contributing.md
cp CHANGELOG.md docs/changelog.md
cp logo.svg docs/logo.svg

# Deploy to Github pages.
mkdocs gh-deploy

# Clean up.
rm docs/index.md docs/contributing.md docs/changelog.md docs/logo.svg
