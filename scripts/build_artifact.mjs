// Builds TAME into a single self-contained HTML file: game code bundled with
// esbuild (three.js vendored in), GLB models embedded as base64 on
// window.__TAME_EMBEDDED__. No network requests at runtime — suitable for
// strict-CSP hosts (e.g. claude.ai Artifacts) or emailing as one file.
//
// Usage:  node scripts/build_artifact.mjs [outFile]
// esbuild is resolved from $ESBUILD_DIR or the current working directory
// (run from any directory whose node_modules contains esbuild).
import { readFileSync, writeFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const GAME = path.join(ROOT, 'game');
const OUT = process.argv[2] || path.join(process.cwd(), 'tame_artifact.html');

// Heaviest models are dropped to keep the single file lean; the game repeats
// species with hue/size variation, so fewer species still fills the meadow.
const EXCLUDE = ['twisted_tree', 'grass', 'horse', 'stag', 'flowers'];

const requireFrom = createRequire(path.join(process.env.ESBUILD_DIR || process.cwd(), 'package.json'));
const { build } = requireFrom('esbuild');

// Resolve the bare "three" specifiers to the vendored copies.
const vendorThree = {
  name: 'vendor-three',
  setup(b) {
    b.onResolve({ filter: /^three$/ }, () => ({ path: path.join(GAME, 'vendor', 'three.module.js') }));
    b.onResolve({ filter: /^three\/addons\// }, (args) => ({
      path: path.join(GAME, 'vendor', 'addons', args.path.slice('three/addons/'.length)),
    }));
  },
};

const bundle = await build({
  entryPoints: [path.join(GAME, 'src', 'main.js')],
  bundle: true,
  minify: true,
  format: 'iife',
  write: false,
  plugins: [vendorThree],
  logLevel: 'silent',
});
const escapeScript = (s) => s.replace(/<\/script/gi, '<\\/script');
const js = escapeScript(bundle.outputFiles[0].text);

// Embed the manifest subset + base64 GLBs.
const manifest = JSON.parse(readFileSync(path.join(GAME, 'assets', 'manifest.json'), 'utf8'));
const keep = (e) => !!e && !EXCLUDE.some((x) => e.file.includes(x));
const emb = {
  player: keep(manifest.player) ? manifest.player : null,
  animals: (manifest.animals || []).filter(keep),
  props: (manifest.props || []).filter(keep),
};
const files = {};
let rawBytes = 0;
for (const e of [emb.player, ...emb.animals, ...emb.props].filter(Boolean)) {
  const buf = readFileSync(path.join(GAME, 'assets', e.file));
  rawBytes += buf.length;
  files[e.file] = buf.toString('base64');
}
const embed = escapeScript(JSON.stringify({ manifest: emb, files }));

// Reuse index.html's styles and DOM so there is one source of truth. The
// output is a body fragment (no doctype/html/head/body) for hosts that wrap it.
const src = readFileSync(path.join(GAME, 'index.html'), 'utf8');
const style = src.slice(src.indexOf('<style>') + '<style>'.length, src.indexOf('</style>'));
const bodyInner = src.slice(src.indexOf('<body>') + '<body>'.length, src.indexOf('<script type="module"'));

const html = `<title>Tame</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>${style}</style>
${bodyInner.trim()}
<script>window.__TAME_EMBEDDED__=${embed};</script>
<script>${js}</script>
`;
writeFileSync(OUT, html);
console.log(JSON.stringify({
  out: OUT,
  models: Object.keys(files),
  rawAssetKB: Math.round(rawBytes / 1024),
  bundleKB: Math.round(js.length / 1024),
  htmlKB: Math.round(html.length / 1024),
}, null, 2));
