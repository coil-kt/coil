#!/bin/bash

# Pull profile from device
adb pull storage/emulated/0/Android/media/coil.compose.benchmark/BaselineProfileGenerator_startup-baseline-prof.txt .

# Rename the file
mv BaselineProfileGenerator_startup-baseline-prof.txt baseline-prof.txt

# Move file to sample app module
mv baseline-prof.txt coil-sample-compose/src/main/baseline-prof.txt
