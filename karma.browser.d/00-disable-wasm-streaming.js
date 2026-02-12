// Chrome occasionally stalls while compiling large wasm modules with
// instantiateStreaming during Karma runs. Compile from ArrayBuffer instead.
(function patchInstantiateStreaming() {
  if (typeof WebAssembly === "undefined") return;
  if (typeof WebAssembly.instantiate !== "function") return;
  if (typeof WebAssembly.instantiateStreaming !== "function") return;

  const instantiate = WebAssembly.instantiate.bind(WebAssembly);

  WebAssembly.instantiateStreaming = async function instantiateStreamingPatched(
    source,
    imports,
    ...rest
  ) {
    const response = await Promise.resolve(source);
    const bytes = await response.arrayBuffer();
    return instantiate(bytes, imports, ...rest);
  };
})();
