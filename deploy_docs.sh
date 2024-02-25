#!/bin/bash
set -e

# Build the Dokka docs.
./assemble_docs.sh

# Copy outside files into the docs folder.
sed -e '/full documentation here/ { N; d; }' < README.md > docs/index.md
cp .github/ISSUE_TEMPLATE/CONTRIBUTING.md docs/contributing.md
cp CHANGELOG.md docs/changelog.md
cp coil-compose/README.md docs/compose.md
cp coil-gif/README.md docs/gifs.md
cp coil-svg/README.md docs/svgs.md
cp coil-test/README.md docs/testing.md
cp coil-video/README.md docs/videos.md
cp logo.svg docs/logo.svg
cp README-ja.md docs/README-ja.md
cp README-ko.md docs/README-ko.md
cp README-ru.md docs/README-ru.md
cp README-sv.md docs/README-sv.md
cp README-tr.md docs/README-tr.md
cp README-zh.md docs/README-zh.md

# Deploy to Github pages.
python3 -m mkdocs gh-deploy --force

# Clean up.
rm docs/index.md \
   docs/contributing.md \
   docs/changelog.md \
   docs/compose.md \
   docs/logo.svg \
   docs/gifs.md \
   docs/svgs.md \
   docs/testing.md \
   docs/videos.md \
   docs/README-ja.md \
   docs/README-ko.md \
   docs/README-ru.md \
   docs/README-sv.md \
   docs/README-tr.md \
   docs/README-zh.md
rm -r docs/api
rm -r site
