#!/bin/bash
set -e

./gradlew validateDebugScreenshotTest verifyPaparazziDebug verifyRoborazziAndroidHostTest verifyRoborazziJvm
