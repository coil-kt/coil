#!/bin/bash
set -e

./gradlew testScreenshotTestDefaultDebugTestSuite verifyPaparazziDebug verifyRoborazziAndroidHostTest verifyRoborazziJvm
