#!/bin/bash

# Build the Dokka docs.
./assemble_docs.sh

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
rm docs/index.md docs/contributing.md docs/changelog.md docs/logo.svg docs/gifs.md docs/svgs.md docs/videos.md docs/README-ko.md docs/README-zh.md
