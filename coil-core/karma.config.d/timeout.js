config.client.mocha = config.client.mocha || {};
config.client.mocha.timeout = 60000;

config.browserNoActivityTimeout = 100000;
config.browserDisconnectTimeout = 50000;
config.browserDisconnectTolerance = 3;

// A workaround from https://android-review.googlesource.com/c/platform/frameworks/support/+/3413540
(function() {
    const originalExit = process.exit;
    process.exit = function(code) {
        console.log('Delaying exit for logs...');
        // This extra time allows any pending I/O operations (such as printing logs) to complete,
        // preventing flakiness when Kotlin marks a test as complete.
        setTimeout(() => {
            originalExit(code);
        }, 5000);
    };
})();
