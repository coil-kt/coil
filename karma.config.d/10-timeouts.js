// Wasm startup can block the browser main thread long enough to exceed Karma's
// defaults on local machines. Increase launcher and activity timeouts so local
// browser tests remain stable without disabling them.
config.client = config.client || {};
config.client.mocha = config.client.mocha || {};
config.client.mocha.timeout = 60000;

config.browserNoActivityTimeout = 600000;
config.captureTimeout = 600000;
config.browserDisconnectTimeout = 10000;
config.browserDisconnectTolerance = 2;
config.processKillTimeout = 30000;

// Use a custom launcher derived from ChromeHeadless to keep its known-stable
// defaults while still avoiding fixed remote debugging port collisions.
config.customLaunchers = config.customLaunchers || {};

if (config.browsers && config.browsers.includes("ChromeHeadless")) {
  config.customLaunchers.ChromeHeadlessCoil = {
    base: "ChromeHeadless",
    flags: [
      "--remote-debugging-port=0",
      // Keep managed/user extensions out of test browsers to reduce flakiness.
      "--disable-extensions",
      "--disable-component-extensions-with-background-pages",
      "--disable-background-networking",
      "--disable-default-apps",
    ],
  };
  config.browsers = config.browsers.map((browser) =>
    browser === "ChromeHeadless" ? "ChromeHeadlessCoil" : browser
  );
}
