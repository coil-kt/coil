#!/bin/bash

./gradlew :coil-compose-benchmark:pixel2Api31BenchmarkAndroidTest -P android.testInstrumentationRunnerArguments.class=coil.compose.benchmark.BaselineProfileGenerator#generate
