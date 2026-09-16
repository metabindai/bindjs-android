// Renders preview fixtures to their component trees with the bundled BindJS runtime.
//
//   node render-fixtures.js <script.js> <fixtures.json> <out-dir>
//
// `fixtures.json` is `[{ name, componentName, source }]`, where `source` is the full
// component module (what PreviewFixture.componentSource() returns). One `<name>.json`
// per fixture is written to `<out-dir>`, pretty-printed the way the runtime hands trees
// to Kotlin. Driven by FixtureTreesTest; not meant to be run by hand.
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const [scriptPath, fixturesPath, outDir] = process.argv.slice(2);
if (!scriptPath || !fixturesPath || !outDir) {
  console.error('usage: node render-fixtures.js <script.js> <fixtures.json> <out-dir>');
  process.exit(2);
}

// The runtime reports failures through console.error; a rerender request and other
// bridge traffic through console.log. Only the errors matter here.
const errors = [];
console.log = () => {};
console.warn = (...args) => errors.push(args.join(' '));
console.error = (...args) => errors.push(args.join(' '));

vm.runInThisContext(fs.readFileSync(scriptPath, 'utf8'), { filename: scriptPath });

const fixtures = JSON.parse(fs.readFileSync(fixturesPath, 'utf8'));
fs.mkdirSync(outDir, { recursive: true });

let failed = false;
for (const fixture of fixtures) {
  const before = errors.length;
  setComponents({ [fixture.componentName]: fixture.source });
  willRender();
  const tree = callComponent([fixture.componentName, {}]);
  if (tree == null || tree === 'null' || errors.length > before) {
    failed = true;
    process.stderr.write(`${fixture.name}: ${errors.slice(before).join(' | ') || 'no tree'}\n`);
    continue;
  }
  fs.writeFileSync(path.join(outDir, `${fixture.name}.json`), tree + '\n');
}
process.exit(failed ? 1 : 0);
