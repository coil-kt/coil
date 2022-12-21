#!/bin/bash

./gradlew :coil-benchmark:connectedBenchmarkAndroidTest -P android.testInstrumentationRunnerArguments.class=coil.benchmark.BaselineProfileBenchmark
