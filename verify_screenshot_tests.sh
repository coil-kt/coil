#!/bin/bash
set -e

./gradlew validateDebugScreenshotTest verifyPaparazziDebug verifyRoborazziDebug verifyRoborazziJvm
