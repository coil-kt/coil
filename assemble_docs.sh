#!/bin/bash

# Clean any previous Dokka docs.
rm -rf docs/api

# Work around Dokka failing to link against external links generated from 'gfm' sources.
wget -O package-list-okio https://square.github.io/okio/2.x/okio/package-list
sed -i '' 's/$dokka.linkExtension:md/$dokka.linkExtension:html/g' package-list-okio

# Build the coil-base docs.
./gradlew clean :coil-base:dokkaGfm

# Work around Dokka failing to link against external links generated from 'gfm' sources.
cp docs/api/coil-base/package-list package-list-coil-base
sed -i '' 's/$dokka.linkExtension:md/$dokka.linkExtension:html/g' package-list-coil-base

# Build the remaining docs.
./gradlew :coil-gif:dokkaGfm :coil-svg:dokkaGfm :coil-video:dokkaGfm :coil-singleton:dokkaGfm

# Clean up.
rm package-list-coil-base package-list-okio
