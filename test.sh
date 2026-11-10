#!/bin/bash
set -eo pipefail

skip_checks=false
skip_instrumentation_tests=false

for arg in "$@"; do
    case "$arg" in
        --skip-checks)
            skip_checks=true
            ;;
        --skip-instrumentation-tests)
            skip_instrumentation_tests=true
            ;;
        --help|-h)
            cat <<'EOF'
Usage: ./test.sh [options]

Options:
  --skip-checks                 Skip checkKotlinAbi and spotlessCheck.
  --skip-instrumentation-tests  Skip Android device tests.
  --help, -h                    Show this help message.
EOF
            exit 0
            ;;
        *)
            echo "Unknown option: $arg" >&2
            exit 1
            ;;
    esac
done

test_tasks=(
    allTests
    testDebugUnitTest
    testDebugScreenshotTestDefaultTestSuite
    verifyPaparazziDebug
    verifyRoborazziAndroidHostTest
    verifyRoborazziJvm
    jsTest
    wasmJsTest
)

if [[ "$skip_instrumentation_tests" != true ]]; then
    test_tasks+=(androidDeviceTest)
fi

if [[ "$skip_checks" != true ]]; then
    test_tasks=(checkKotlinAbi spotlessCheck "${test_tasks[@]}")
fi

./gradlew "${test_tasks[@]}"
