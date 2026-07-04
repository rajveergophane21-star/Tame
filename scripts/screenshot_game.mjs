#!/usr/bin/env node
// Playwright smoke test for the TAME game.
//
// Usage: node scripts/screenshot_game.mjs [shot1.png] [shot2.png] [port] [--no-assets]
// - Serves /home/user/Tame/game with python3 http.server
// - --no-assets serves a shadow copy WITHOUT the assets/ directory, to verify
//   the pure procedural-placeholder fallback mode.
// - Fails (exit 1) on any console error / pageerror (expected asset-404 noise
//   from the optional assets/ directory is ignored), or if the player didn't
//   move, or if the canvas looks blank.
import { createRequire } from 'module';
import { spawn } from 'child_process';
import { fileURLToPath } from 'url';
import path from 'path';
import fs from 'fs';

const SCRATCH = process.env.TAME_SCRATCH
  || '/tmp/claude-0/-home-user-Tame/0bb35419-5523-547c-b7fa-683cd3f3bb6f/scratchpad';
const require2 = createRequire(path.join(SCRATCH, 'package.json'));
const { chromium } = require2('playwright');

const __dirname = path.dirname(fileURLToPath(import.meta.url));
let GAME_DIR = path.resolve(__dirname, '../game');
const args = process.argv.slice(2).filter((a) => a !== '--no-assets');
const noAssets = process.argv.includes('--no-assets');
const shot1 = args[0] || path.join(SCRATCH, noAssets ? 'game_shot_fallback.png' : 'game_shot.png');
const shot2 = args[1] || path.join(SCRATCH, noAssets ? 'game_shot_fallback2.png' : 'game_shot2.png');
const PORT = Number(args[2] || 8123);

if (noAssets) {
  // Shadow copy of the game without assets/ → exercises pure placeholder mode.
  const shadow = path.join(SCRATCH, 'shadow_game');
  fs.rmSync(shadow, { recursive: true, force: true });
  fs.mkdirSync(shadow, { recursive: true });
  for (const entry of ['index.html', 'src', 'vendor']) {
    fs.symlinkSync(path.join(GAME_DIR, entry), path.join(shadow, entry));
  }
  GAME_DIR = shadow;
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function main() {
  const server = spawn('python3', ['-m', 'http.server', String(PORT), '--bind', '127.0.0.1'], {
    cwd: GAME_DIR, stdio: 'ignore',
  });
  const errors = [];
  const ignored = [];
  let browser;
  try {
    // wait for the server
    for (let i = 0; i < 50; i++) {
      try {
        const res = await fetch(`http://127.0.0.1:${PORT}/index.html`);
        if (res.ok) break;
      } catch { /* not up yet */ }
      await sleep(100);
    }

    browser = await chromium.launch({
      args: ['--enable-unsafe-swiftshader', '--no-sandbox'],
    }).catch(() => chromium.launch({
      executablePath: '/opt/pw-browsers/chromium-1194/chrome-linux/chrome',
      args: ['--enable-unsafe-swiftshader', '--no-sandbox'],
    }));
    const page = await browser.newPage({ viewport: { width: 1280, height: 720 } });

    const isExpectedAssetMiss = (text, url) =>
      /\/assets\//.test(url || '') || (/Failed to load resource/.test(text) && /assets\//.test(text));
    page.on('console', (msg) => {
      if (msg.type() !== 'error') return;
      const url = (msg.location() && msg.location().url) || '';
      if (isExpectedAssetMiss(msg.text(), url)) { ignored.push(msg.text()); return; }
      errors.push(`[console] ${msg.text()} (${url})`);
    });
    page.on('pageerror', (err) => errors.push(`[pageerror] ${err.message}`));

    await page.goto(`http://127.0.0.1:${PORT}/`, { waitUntil: 'load' });
    await page.waitForFunction(() => window.__TAME_DEBUG && window.__TAME_DEBUG.ready, null, { timeout: 20000 });

    // click to start
    await page.mouse.click(640, 360);
    await page.waitForFunction(() => window.__TAME_DEBUG.started === true, null, { timeout: 5000 });
    await sleep(2500);
    const posA = await page.evaluate(() => window.__TAME_DEBUG.player.slice());
    await page.screenshot({ path: shot1 });

    // WASD movement — hold W (+ a touch of A) until the player has clearly
    // moved. Software WebGL can run the sim slowly, so wait on the condition.
    await page.keyboard.down('KeyW');
    await page.keyboard.down('KeyA');
    await sleep(600);
    await page.keyboard.up('KeyA');
    let moved = 0;
    await page.waitForFunction(
      (a) => {
        const p = window.__TAME_DEBUG.player;
        return Math.hypot(p[0] - a[0], p[2] - a[2]) > 4;
      }, posA, { timeout: 30000 },
    ).catch(() => {});
    await page.keyboard.up('KeyW');
    const posB = await page.evaluate(() => window.__TAME_DEBUG.player.slice());
    moved = Math.hypot(posB[0] - posA[0], posB[2] - posA[2]);

    // walk up to the nearest wild animal (test hook) and press E
    const nearOk = await page.evaluate(() => window.__TAME_TEST.goNearWild());
    await sleep(400); // let the prompt logic settle
    const promptShown = await page.evaluate(() => window.__TAME_DEBUG.promptVisible);
    await page.keyboard.press('KeyE');
    // taming takes 0.6 s of *sim* time — wait on the counter, not the clock
    await page.waitForFunction(() => window.__TAME_DEBUG.tamed >= 1, null, { timeout: 30000 }).catch(() => {});
    const tamed = await page.evaluate(() => window.__TAME_DEBUG.tamed);
    const mode = await page.evaluate(() => window.__TAME_DEBUG.mode);
    await sleep(300);
    await page.screenshot({ path: shot2 });

    // sanity: screenshots aren't blank — a flat frame compresses to a few KB,
    // a rendered meadow is much larger.
    const size1 = fs.statSync(shot1).size;
    const size2 = fs.statSync(shot2).size;

    console.log(JSON.stringify({
      mode,
      movedMeters: Number(moved.toFixed(2)),
      nearAnimal: nearOk,
      promptShown,
      tamedCount: tamed,
      shotBytes: [size1, size2],
      consoleErrors: errors.length,
      ignoredAssetMisses: ignored.length,
      shots: [shot1, shot2],
    }, null, 2));

    if (errors.length) {
      console.error('ERRORS:\n' + errors.join('\n'));
      process.exitCode = 1;
    }
    if (moved < 2) { console.error(`FAIL: player barely moved (${moved.toFixed(2)}m)`); process.exitCode = 1; }
    if (size1 < 40000 || size2 < 40000) { console.error('FAIL: a screenshot looks blank/flat'); process.exitCode = 1; }
    if (nearOk && tamed < 1) { console.error('FAIL: taming did not register'); process.exitCode = 1; }
  } finally {
    if (browser) await browser.close().catch(() => {});
    server.kill('SIGTERM');
  }
}

main().catch((err) => { console.error(err); process.exit(1); });
