#!/bin/bash
set -e

./gradlew updateScreenshotTestDefaultDebugTestSuite recordPaparazziDebug recordRoborazziAndroidHostTest recordRoborazziJvm
