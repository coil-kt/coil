#!/bin/bash
./gradlew clean androidSourcesJar androidJavadocsJar uploadArchives --no-daemon --no-parallel
