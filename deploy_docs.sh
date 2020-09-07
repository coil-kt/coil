#!/bin/bash

# Clean any previous Dokka docs.
rm -rf docs/api

# Work around Dokka failing to link against external links generated from 'gfm' sources.
wget -O package-list-okio https://square.github.io/okio/2.x/okio/package-list
sed -i '' 's/$dokka.linkExtension:md/$dokka.linkExtension:html/g' package-list-okio

# Build the coil-base docs.
./gradlew clean :coil-base:dokka

# Work around Dokka failing to link against external links generated from 'gfm' sources.
cp docs/api/coil-base/package-list package-list-coil-base
sed -i '' 's/$dokka.linkExtension:md/$dokka.linkExtension:html/g' package-list-coil-base

# Build the remaining docs.
./gradlew :coil-gif:dokka :coil-svg:dokka :coil-video:dokka :coil-singleton:dokka

# Copy outside files into the docs folder.
sed -e '/full documentation here/ { N; d; }' < README.md > docs/index.md
cp CONTRIBUTING.md docs/contributing.md
cp CHANGELOG.md docs/changelog.md
cp coil-gif/README.md docs/gifs.md
cp coil-svg/README.md docs/svgs.md
cp coil-video/README.md docs/videos.md
cp logo.svg docs/logo.svg
cp README-ko.md docs/README-ko.md
cp README-zh.md docs/README-zh.md

# Deploy to Github pages.
mkdocs gh-deploy

# Clean up.
rm package-list-coil-base package-list-okio docs/index.md docs/contributing.md docs/changelog.md docs/logo.svg docs/gifs.md docs/svgs.md docs/videos.md docs/README-ko.md docs/README-zh.md
