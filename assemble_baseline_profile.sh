#!/bin/bash

./gradlew :internal:benchmark:pixel7Api34BenchmarkAndroidTest -P android.testInstrumentationRunnerArguments.class=coil3.benchmark.BaselineProfileGenerator#generate -Dproject=view
cp internal/benchmark/build/outputs/managed_device_android_test_additional_output/benchmark/pixel7Api34/BaselineProfileGenerator_generate-baseline-prof.txt coil-core/src/androidMain/baseline-prof.txt

./gradlew :internal:benchmark:pixel7Api34BenchmarkAndroidTest -P android.testInstrumentationRunnerArguments.class=coil3.benchmark.BaselineProfileGenerator#generate -Dproject=compose
cp internal/benchmark/build/outputs/managed_device_android_test_additional_output/benchmark/pixel7Api34/BaselineProfileGenerator_generate-baseline-prof.txt coil-compose-core/src/androidMain/baseline-prof.txt
