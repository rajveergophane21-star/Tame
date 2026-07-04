// TAME — boot, game loop and wiring.
import * as THREE from 'three';
import { loadAssets, makePlayerCharacter } from './assets.js';
import { buildWorld } from './world.js';
import { Player, FollowCamera } from './player.js';
import { AnimalManager } from './animals.js';
import { Hearts } from './particles.js';
import { Sfx } from './audio.js';
import { UI } from './ui.js';
import { makeRng } from './rng.js';
import { SEED, MAX_DT, TAME_RANGE } from './constants.js';

const _to = new THREE.Vector3();

async function boot() {
  const ui = new UI();
  const sfx = new Sfx();

  // --- renderer -------------------------------------------------------------
  const renderer = new THREE.WebGLRenderer({ antialias: true });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  renderer.setSize(window.innerWidth, window.innerHeight);
  renderer.shadowMap.enabled = true;
  renderer.shadowMap.type = THREE.PCFSoftShadowMap;
  document.getElementById('app').appendChild(renderer.domElement);

  const scene = new THREE.Scene();
  const camera = new THREE.PerspectiveCamera(55, window.innerWidth / window.innerHeight, 0.1, 400);

  window.addEventListener('resize', () => {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
  });

  // --- assets (real GLBs where available, placeholders otherwise) -----------
  const assets = await loadAssets();
  ui.setBadge(assets.attempted, assets.real);

  const rng = makeRng(SEED);
  const world = buildWorld(scene, rng, assets);

  const player = new Player(makePlayerCharacter(assets), scene);
  const followCam = new FollowCamera(camera, renderer.domElement);
  const animals = new AnimalManager(scene, assets, rng);
  const hearts = new Hearts(scene);

  ui.setCount(0, animals.total);

  // --- input -----------------------------------------------------------------
  const keys = new Set();
  window.addEventListener('keydown', (e) => {
    if (e.code.startsWith('Arrow') || e.code === 'Space') e.preventDefault();
    keys.add(e.code);
    if (e.code === 'KeyM') { sfx.setMuted(!sfx.muted); ui.setMuted(sfx.muted); }
    if (e.code === 'KeyE') tryTame();
  });
  window.addEventListener('keyup', (e) => keys.delete(e.code));
  ui.mute.addEventListener('click', () => { sfx.setMuted(!sfx.muted); ui.setMuted(sfx.muted); });

  // --- game state -------------------------------------------------------------
  const state = {
    started: false,
    won: false,
    time: 0,
    tameTarget: null, // animal currently showing the [E] prompt
  };

  function tryTame() {
    if (!state.started || state.won) return;
    const target = animals.findTameable(player);
    if (!target) return;
    target.startTaming(player);
    hearts.burst(target.pos.clone().add(new THREE.Vector3(0, target.character.height * 0.7, 0)));
    sfx.chime();
    ui.hidePrompt();
  }

  function onTamed() {
    ui.setCount(animals.tamedCount, animals.total, true);
    if (animals.tamedCount >= animals.total) {
      state.won = true;
      sfx.fanfare();
      setTimeout(() => ui.showWin(state.time), 700);
    }
  }

  function start() {
    if (state.started) return;
    state.started = true;
    sfx.unlock();
    ui.hideStart();
    ui.fadeHintLater(8000);
  }

  function resetGame() {
    animals.reset();
    hearts.clear();
    player.position.set(0, 0, 0);
    player.vel.set(0, 0, 0);
    state.won = false;
    state.time = 0;
    ui.setCount(0, animals.total);
    ui.hideWin();
    ui.setTimer(0);
  }

  document.getElementById('startOverlay').addEventListener('click', start);
  document.getElementById('againBtn').addEventListener('click', (e) => { e.stopPropagation(); resetGame(); });

  // --- debug/test hooks (used by the Playwright harness) -----------------------
  const debug = {
    mode: assets.attempted === 0 ? 'placeholder' : (assets.real === assets.attempted ? 'real' : (assets.real ? 'mixed' : 'placeholder')),
    attempted: assets.attempted,
    real: assets.real,
    ready: true,
  };
  window.__TAME_DEBUG = debug;
  window.__TAME_TEST = {
    // Teleport just in front of the nearest wild animal, facing it (test only).
    goNearWild() {
      let best = null, bd = Infinity;
      for (const a of animals.animals) {
        if (a.tamed || a.state === 'taming') continue;
        const d = a.pos.distanceTo(player.position);
        if (d < bd) { bd = d; best = a; }
      }
      if (!best) return false;
      _to.subVectors(player.position, best.pos).setY(0);
      if (_to.lengthSq() < 1e-4) _to.set(0, 0, 1);
      _to.normalize().multiplyScalar(TAME_RANGE * 0.6);
      player.position.copy(best.pos).add(_to);
      player.vel.set(0, 0, 0);
      const dx = best.pos.x - player.position.x, dz = best.pos.z - player.position.z;
      player.group.quaternion.setFromAxisAngle(new THREE.Vector3(0, 1, 0), Math.atan2(dx, dz));
      return true;
    },
  };

  // --- main loop ----------------------------------------------------------------
  let last = performance.now();
  document.addEventListener('visibilitychange', () => { last = performance.now(); });

  const ctx = { player, colliders: world.colliders, caravan: animals.caravan, onTamed };

  function frame(now) {
    requestAnimationFrame(frame);
    const dt = Math.min(MAX_DT, (now - last) / 1000);
    last = now;
    if (document.hidden) return; // tab-hidden pause

    if (state.started && !state.won) state.time += dt;

    player.update(dt, state.started ? keys : new Set(), followCam.yaw, world.colliders);
    animals.update(dt, ctx);
    hearts.update(dt);
    world.update(player.position);
    followCam.update(dt, player.position);

    // taming prompt
    if (state.started && !state.won) {
      const target = animals.findTameable(player);
      state.tameTarget = target;
      if (target) {
        _to.copy(target.pos);
        _to.y += target.character.height + 0.45;
        ui.showPrompt(_to, camera);
      } else ui.hidePrompt();
    } else ui.hidePrompt();

    ui.setTimer(state.time);

    // debug snapshot for the test harness
    debug.player = [player.position.x, player.position.y, player.position.z];
    debug.tamed = animals.tamedCount;
    debug.total = animals.total;
    debug.started = state.started;
    debug.won = state.won;
    debug.promptVisible = !!state.tameTarget;

    renderer.render(scene, camera);
  }
  requestAnimationFrame(frame);
}

boot().catch((err) => {
  console.error('[tame] fatal boot error', err);
  const el = document.createElement('div');
  el.className = 'ui card';
  el.style.cssText = 'position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);z-index:99';
  el.textContent = 'Something went wrong starting the game — see console.';
  document.body.appendChild(el);
});
