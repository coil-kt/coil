config.customLaunchers = {
    ChromeForComposeTests: {
        base: "Chrome",
        flags: [
            "--no-sandbox",
            "--disable-search-engine-choice-screen",
            "--disable-setuid-sandbox",
            "--enable-webgl",
            "--ignore-gpu-blocklist",
            "--in-process-gpu"
        ]
    }
}
