#!/bin/bash
set -e

# Build the Compose WASM sample.
./gradlew samples:compose:wasmJsBrowserDistribution

# Copy outside files into the docs folder.
cp -R samples/compose/build/dist/wasmJs/productionExecutable docs/sample

# Build the Dokka docs.
./assemble_docs.sh

# Copy outside files into the docs folder.
sed -e '/full documentation here/ { N; d; }' < README.md > docs/index.md
cp .github/ISSUE_TEMPLATE/CONTRIBUTING.md docs/contributing.md
cp CHANGELOG.md docs/changelog.md
cp coil-network-core/README.md docs/network.md
cp coil-compose/README.md docs/compose.md
cp coil-gif/README.md docs/gifs.md
cp coil-svg/README.md docs/svgs.md
cp coil-test/README.md docs/testing.md
cp coil-video/README.md docs/videos.md
cp logo.svg docs/logo.svg
cp README-*.md docs/

# Deploy to Github pages.
python3 -m mkdocs gh-deploy --force

# Clean up.
rm -r docs/index.md \
   docs/contributing.md \
   docs/changelog.md \
   docs/network.md \
   docs/compose.md \
   docs/logo.svg \
   docs/gifs.md \
   docs/svgs.md \
   docs/testing.md \
   docs/videos.md \
   docs/README-*.md \
   docs/api \
   docs/sample \
   site
