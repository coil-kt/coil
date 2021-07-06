#!/bin/bash
if [ "$1" -ge 21 ]
then
    # Run all instrumentation test.
    ./gradlew \
        coil-base:connectedDebugAndroidTest \
        coil-compose-base:connectedDebugAndroidTest \
        coil-gif:connectedDebugAndroidTest \
        coil-svg:connectedDebugAndroidTest \
        coil-video:connectedDebugAndroidTest
else
    # Do not run 'coil-compose-base' tests since it requires minSdk >= 21.
    ./gradlew \
        coil-base:connectedDebugAndroidTest \
        coil-gif:connectedDebugAndroidTest \
        coil-svg:connectedDebugAndroidTest \
        coil-video:connectedDebugAndroidTest
fi
