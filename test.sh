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
  --skip-checks                 Skip checkLegacyAbi and spotlessCheck.
  --skip-instrumentation-tests  Skip connectedDebugAndroidTest.
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
    validateDebugScreenshotTest
    verifyPaparazziDebug
    verifyRoborazziAndroidHostTest
    verifyRoborazziJvm
)

if [[ "$skip_instrumentation_tests" != true ]]; then
    test_tasks+=(connectedDebugAndroidTest)
fi

js_wasm_test_tasks=(
    jsTest
    wasmJsTest
)

js_wasm_excluded_tasks=(
    -x jsBrowserTest
    -x jsNodeTest
    -x jsTest
    -x wasmJsBrowserTest
    -x wasmJsNodeTest
    -x wasmJsTest
)

if [[ "$skip_checks" != true ]]; then
    # Run separately to work around https://github.com/diffplug/spotless/issues/1572.
    ./gradlew checkLegacyAbi spotlessCheck
fi

# Run all non-JS/Wasm tests without a worker cap.
./gradlew "${test_tasks[@]}" "${js_wasm_excluded_tasks[@]}"

# Constrain parallelism so JS/Wasm browser tests don't overwhelm local machines
# and get stuck in Chrome/Karma restart loops.
max_workers="${MAX_WORKERS:-1}"
./gradlew --max-workers="$max_workers" "${js_wasm_test_tasks[@]}"
