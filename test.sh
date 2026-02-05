#!/bin/bash
set -e

# Run separately to work around https://github.com/diffplug/spotless/issues/1572.
./gradlew checkLegacyAbi spotlessCheck
./gradlew allTests testDebugUnitTest connectedDebugAndroidTest validateDebugScreenshotTest verifyPaparazziDebug verifyRoborazziAndroidHostTest verifyRoborazziJvm
