#!/bin/bash
set -e

./gradlew testDebugScreenshotTestDefaultTestSuite verifyPaparazziDebug verifyRoborazziAndroidHostTest verifyRoborazziJvm
