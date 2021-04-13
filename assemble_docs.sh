#!/bin/bash

# Clean any previous Dokka docs.
rm -rf docs/api
./gradlew clean

# Build the coil-base docs.
./gradlew :coil-base:dokkaGfm
sed -e 's/coil-base\///g' < docs/api/index.md > docs/api/coil-base/index.md
rm docs/api/index.md

# Work around Dokka failing to link against external links generated from 'gfm' sources.
cp docs/api/coil-base/package-list package-list-coil-base
sed -i '' 's/$dokka.linkExtension:md/$dokka.linkExtension:html/g' package-list-coil-base

# Build the coil-gif docs.
./gradlew :coil-gif:dokkaGfm
sed -e 's/coil-gif\///g' < docs/api/index.md > docs/api/coil-gif/index.md
rm docs/api/index.md

# Build the coil-svg docs.
./gradlew :coil-svg:dokkaGfm
sed -e 's/coil-svg\///g' < docs/api/index.md > docs/api/coil-svg/index.md
rm docs/api/index.md

# Build the coil-video docs.
./gradlew :coil-video:dokkaGfm
sed -e 's/coil-video\///g' < docs/api/index.md > docs/api/coil-video/index.md
rm docs/api/index.md

# Build the coil-singleton docs.
./gradlew :coil-singleton:dokkaGfm
sed -e 's/coil-singleton\///g' < docs/api/index.md > docs/api/coil-singleton/index.md
rm docs/api/index.md

# Clean up.
rm package-list-coil-base
