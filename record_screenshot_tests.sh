#!/bin/bash
set -e

./gradlew updateDebugScreenshotTest recordPaparazziDebug recordRoborazziDebug recordRoborazziJvm
