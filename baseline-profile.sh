#!/bin/bash

./gradlew :coil-benchmark:pixel2Api31BenchmarkAndroidTest -P android.testInstrumentationRunnerArguments.class=coil.benchmark.BaselineProfileGenerator#generate
cp coil-benchmark/build/outputs/managed_device_android_test_additional_output/pixel2Api31/BaselineProfileGenerator_generate-baseline-prof.txt coil-sample-compose/src/main/baseline-prof.txt
./gradlew :coil-benchmark:connectedBenchmarkAndroidTest -P android.testInstrumentationRunnerArguments.class=coil.benchmark.BaselineProfileBenchmark
