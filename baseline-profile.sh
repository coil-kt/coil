#!/bin/bash

./gradlew :coil-compose-benchmark:pixel2Api31BenchmarkAndroidTest -P android.testInstrumentationRunnerArguments.class=coil.compose.benchmark.BaselineProfileGenerator#generate
cp coil-compose-benchmark/build/outputs/managed_device_android_test_additional_output/pixel2Api31/BaselineProfileGenerator_generate-baseline-prof.txt coil-sample-compose/src/main/baseline-prof.txt
./gradlew :coil-compose-benchmark:connectedBenchmarkAndroidTest -P android.testInstrumentationRunnerArguments.class=coil.compose.benchmark.BaselineProfileBenchmark
