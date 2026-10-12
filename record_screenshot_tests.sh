#!/bin/bash
set -e

./gradlew updateDebugScreenshotTestDefaultTestSuite recordPaparazziDebug recordRoborazziAndroidHostTest recordRoborazziJvm
