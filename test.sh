#!/bin/bash
set -e

# Run separately to work around https://github.com/diffplug/spotless/issues/1572.
./gradlew apiCheck spotlessCheck
./gradlew allTests testDebugUnitTest connectedDebugAndroidTest verifyPaparazziDebug verifyRoborazziDebug
