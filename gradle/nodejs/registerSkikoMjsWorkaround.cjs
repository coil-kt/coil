const Module = require('module');
const fs = require('fs');
const path = require('path');

const TARGET_FILES = new Set(['skiko.mjs', 'skikod8.mjs']);
const ORIGINAL_MJS_HANDLER = Module._extensions['.mjs'];

function transformModuleSource(source) {
  let prelude = '';

  if (source.includes('import.meta.url')) {
    prelude += 'const { pathToFileURL } = require("url");\n';
    source = source.replace(/import\.meta\.url/g, 'pathToFileURL(__filename).href');
  }

  const exportedNames = [];
  source = source.replace(/export\s+(const|let|var)\s+([\w$]+)\s*=/g, (match, declaration, name) => {
    exportedNames.push(name);
    return `${declaration} ${name} =`;
  });

  let defaultExport = null;
  source = source.replace(/export\s+default\s+([^;]+);?/g, (match, expression) => {
    defaultExport = expression.trim().replace(/;+$/, '');
    return '';
  });

  let transformed = prelude + 'const __exports = {};\n' + source + '\n';
  for (const name of exportedNames) {
    transformed += `__exports.${name} = ${name};\n`;
  }
  if (defaultExport !== null) {
    transformed += `__exports.default = ${defaultExport};\n`;
  }
  transformed += 'module.exports = __exports;\n';
  return transformed;
}

Module._extensions['.mjs'] = function registerSkikoMjs(module, filename) {
  const baseName = path.basename(filename);
  if (!TARGET_FILES.has(baseName)) {
    if (ORIGINAL_MJS_HANDLER) {
      return ORIGINAL_MJS_HANDLER(module, filename);
    }
    const error = new Error(`Cannot load ES module ${filename}`);
    error.code = 'ERR_REQUIRE_ESM';
    throw error;
  }

  const originalSource = fs.readFileSync(filename, 'utf8');
  const transformedSource = transformModuleSource(originalSource);
  module._compile(transformedSource, filename);
};
