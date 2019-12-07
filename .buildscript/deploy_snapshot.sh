#!/bin/bash

BRANCH="master"

set -e

if [ "$GITHUB_REF" == "$BRANCH" ]; then
  echo "Deploying snapshot..."
  ./upload_archives.sh
  echo "Snapshot deployed!"
else
  echo "Skipping snapshot deployment: wrong branch. Expected '$BRANCH' but was '$GITHUB_REF'."
fi
