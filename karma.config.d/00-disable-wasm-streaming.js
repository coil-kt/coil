// Files in this directory run inside Karma's Node config context. Inject a
// browser-side shim that patches instantiateStreaming before test modules load.
const path = require("path");
const rootDir = path.resolve(config.basePath, "../../../../");

config.files = config.files || [];
config.files.unshift({
  pattern: path.join(
    rootDir,
    "karma.browser.d",
    "00-disable-wasm-streaming.js",
  ),
  included: true,
  served: true,
  watched: false,
});
