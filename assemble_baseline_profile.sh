#!/bin/bash

./gradlew :coil-benchmark:pixel7Api34BenchmarkAndroidTest -P android.testInstrumentationRunnerArguments.class=coil3.benchmark.BaselineProfileGenerator#generate -Dproject=view
cp coil-benchmark/build/outputs/managed_device_android_test_additional_output/benchmark/pixel7Api34/BaselineProfileGenerator_generate-baseline-prof.txt coil-base/src/androidMain/baseline-prof.txt

./gradlew :coil-benchmark:pixel7Api34BenchmarkAndroidTest -P android.testInstrumentationRunnerArguments.class=coil3.benchmark.BaselineProfileGenerator#generate -Dproject=compose
cp coil-benchmark/build/outputs/managed_device_android_test_additional_output/benchmark/pixel7Api34/BaselineProfileGenerator_generate-baseline-prof.txt coil-compose-base/src/androidMain/baseline-prof.txt
