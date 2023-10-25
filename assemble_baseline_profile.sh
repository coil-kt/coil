#!/bin/bash

./gradlew :coil-benchmark:pixel7Api33BenchmarkAndroidTest -P android.testInstrumentationRunnerArguments.class=coil.benchmark.BaselineProfileGenerator#generate -Dproject=view
mv coil-benchmark/build/outputs/managed_device_android_test_additional_output/benchmark/pixel7Api33/BaselineProfileGenerator_generate-baseline-prof.txt coil-base/src/main/baseline-prof.txt
./gradlew :coil-benchmark:pixel7Api33BenchmarkAndroidTest -P android.testInstrumentationRunnerArguments.class=coil.benchmark.BaselineProfileGenerator#generate -Dproject=compose
mv coil-benchmark/build/outputs/managed_device_android_test_additional_output/benchmark/pixel7Api33/BaselineProfileGenerator_generate-baseline-prof.txt coil-compose-base/src/main/baseline-prof.txt
