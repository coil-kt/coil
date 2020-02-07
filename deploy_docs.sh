#!/bin/bash

# Clean any previous Dokka docs.
rm -rf docs/api

# Build the Dokka docs.
./gradlew clean :coil-base:dokka :coil-default:dokka :coil-gif:dokka :coil-svg:dokka :coil-video:dokka

# Copy outside files into the docs folder.
sed -e '/full documentation here/ { N; d; }' < README.md > docs/index.md
cp CONTRIBUTING.md docs/contributing.md
cp CHANGELOG.md docs/changelog.md
cp coil-gif/README.md docs/gifs.md
cp coil-svg/README.md docs/svgs.md
cp coil-video/README.md docs/videos.md
cp logo.svg docs/logo.svg
cp README-ko.md docs/README-ko.md

# Deploy to Github pages.
mkdocs gh-deploy

# Clean up.
rm docs/index.md docs/contributing.md docs/changelog.md docs/logo.svg docs/gifs.md docs/svgs.md docs/README-ko.md
