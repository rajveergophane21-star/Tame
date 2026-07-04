// Asset pipeline: manifest loading, GLB normalisation, animation matching and
// first-class procedural placeholders. Every model load is independently
// try/caught — the game must run perfectly with no assets/ directory at all.
import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { clone as skeletonClone } from 'three/addons/utils/SkeletonUtils.js';

const DEFAULT_HEIGHTS = { player: 1.6, animal: 0.9, prop: 2.2 };

// Single-file builds (scripts/build_artifact.mjs) inject the manifest and the
// GLB payloads as base64 on this global so no fetch is ever made.
const EMBED = (typeof window !== 'undefined' && window.__TAME_EMBEDDED__) || null;

// ---------------------------------------------------------------------------
// Manifest + GLB loading
// ---------------------------------------------------------------------------

function validEntry(e) {
  return !!e && typeof e === 'object' && typeof e.file === 'string' && e.file.length > 0;
}

function shapeManifest(json) {
  if (!json || typeof json !== 'object') return null;
  return {
    player: validEntry(json.player) ? json.player : null,
    animals: Array.isArray(json.animals) ? json.animals.filter(validEntry) : [],
    props: Array.isArray(json.props) ? json.props.filter(validEntry) : [],
  };
}

async function fetchManifest() {
  if (EMBED) return shapeManifest(EMBED.manifest);
  try {
    const res = await fetch('assets/manifest.json', { cache: 'no-store' });
    if (!res.ok) return null;
    return shapeManifest(await res.json());
  } catch (err) {
    return null; // missing / invalid manifest → pure placeholder mode
  }
}

function b64ToArrayBuffer(b64) {
  const bin = atob(b64);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return bytes.buffer;
}

function loadGltf(loader, entry) {
  if (EMBED) {
    const b64 = EMBED.files && EMBED.files[entry.file];
    if (!b64) return Promise.reject(new Error('not embedded: ' + entry.file));
    return loader.parseAsync(b64ToArrayBuffer(b64), '');
  }
  return loader.loadAsync('assets/' + entry.file);
}

// Scale a loaded scene so its bounding-box height equals targetHeight, then
// recentre it so it stands on y=0 centred on the origin.
function normalizeModel(scene, targetHeight) {
  const box = new THREE.Box3().setFromObject(scene);
  const size = box.getSize(new THREE.Vector3());
  const s = targetHeight / Math.max(size.y, 1e-6);
  scene.scale.setScalar(s);
  const box2 = new THREE.Box3().setFromObject(scene);
  const center = box2.getCenter(new THREE.Vector3());
  scene.position.x -= center.x;
  scene.position.z -= center.z;
  scene.position.y -= box2.min.y;
  const wrapper = new THREE.Group();
  wrapper.add(scene);
  return wrapper;
}

// Heuristic clip matching: case-insensitive substring, words in priority
// order, and the shortest clip name wins (so "Idle" beats "Idle_HitReact_Left").
function matchClips(clips) {
  const byPriority = (words) => {
    for (const w of words) {
      const hits = clips.filter((c) => c.name.toLowerCase().includes(w));
      if (hits.length) return hits.sort((a, b) => a.name.length - b.name.length)[0];
    }
    return null;
  };
  const idle = byPriority(['idle']);
  const walk = byPriority(['walk']);
  const move = byPriority(['run', 'gallop']) || walk;
  const celebrate = byPriority(['jump', 'attack', 'bounce', 'spin', 'roll', 'eat', 'clicked']);
  return { idle, move, walk, celebrate };
}

async function loadEntry(loader, entry, kind) {
  try {
    const gltf = await loadGltf(loader, entry);
    const target = (typeof entry.targetHeight === 'number' && entry.targetHeight > 0)
      ? entry.targetHeight : DEFAULT_HEIGHTS[kind];
    const template = normalizeModel(gltf.scene, target);
    template.traverse((o) => {
      if (o.isMesh) { o.castShadow = true; o.receiveShadow = false; }
    });
    const clips = gltf.animations || [];
    return { name: entry.name || entry.file, template, clips, matched: matchClips(clips), real: true };
  } catch (err) {
    console.warn(`[tame] model "${entry.file}" failed to load — using placeholder.`, err.message || err);
    return null;
  }
}

// Loads everything the manifest offers. Nulls mean "use placeholder".
export async function loadAssets() {
  const out = { player: null, animals: [], props: [], attempted: 0, real: 0 };
  const manifest = await fetchManifest();
  if (!manifest) return out;

  const loader = new GLTFLoader();
  const jobs = [];
  if (manifest.player) {
    out.attempted++;
    jobs.push(loadEntry(loader, manifest.player, 'player').then((r) => { out.player = r; }));
  }
  manifest.animals.forEach((e, i) => {
    out.attempted++;
    jobs.push(loadEntry(loader, e, 'animal').then((r) => { out.animals[i] = r; }));
  });
  manifest.props.forEach((e, i) => {
    out.attempted++;
    jobs.push(loadEntry(loader, e, 'prop').then((r) => { out.props[i] = r; }));
  });
  await Promise.all(jobs);
  out.animals = out.animals.filter(Boolean);
  out.props = out.props.filter(Boolean);
  out.real = (out.player ? 1 : 0) + out.animals.length + out.props.length;
  return out;
}

// ---------------------------------------------------------------------------
// Character: wraps a model (real or placeholder) + animation state
// ---------------------------------------------------------------------------

export class Character {
  constructor(root, { clips = null, matched = null, height = 1 } = {}) {
    this.root = root;                 // stands on y=0
    this.height = height;
    this.mixer = null;
    this.actions = {};
    this.current = null;
    this.proc = { t: Math.random() * 10, base: root.children[0] ? root.children[0].position.y : 0 };
    this.speed01 = 0;                 // 0 idle … 1 running, drives procedural bob

    if (clips && matched && (matched.idle || matched.move)) {
      this.mixer = new THREE.AnimationMixer(root);
      for (const key of ['idle', 'move', 'walk', 'celebrate']) {
        const clip = matched[key];
        if (clip) this.actions[key] = this.mixer.clipAction(clip);
      }
      if (this.actions.celebrate) {
        this.actions.celebrate.setLoop(THREE.LoopOnce);
        this.actions.celebrate.clampWhenFinished = false;
      }
      this.mixer.addEventListener('finished', () => this.play(this._afterOnce || 'idle'));
      this.play(this.actions.idle ? 'idle' : 'move');
    }
  }

  play(name) {
    const next = this.actions[name];
    if (!next || this.current === name) { if (!next) this.current = name; return; }
    const prev = this.actions[this.current];
    next.enabled = true;
    next.reset().play();
    if (prev && prev !== next) next.crossFadeFrom(prev, 0.25, true);
    this.current = name;
  }

  celebrateOnce(after = 'idle') {
    if (this.actions.celebrate) { this._afterOnce = after; this.play('celebrate'); }
    else this.proc.hop = 0.55; // procedural hop
  }

  setMoving(moving, running = false) {
    if (this.mixer && this.current !== 'celebrate') {
      if (!moving) {
        if (this.actions.idle) this.play('idle');
      } else if (!running && this.actions.walk) {
        this.play('walk');
      } else if (this.actions.move) {
        this.play('move');
        this.actions.move.timeScale = running ? 1.15 : 1;
      } else if (this.actions.walk) {
        this.play('walk');
      }
    }
    this.speed01 += ((moving ? (running ? 1 : 0.55) : 0) - this.speed01) * 0.2;
  }

  update(dt) {
    if (this.mixer) { this.mixer.update(dt); return; }
    // Procedural animation: gentle bob + lean while moving, soft breathing at rest.
    const p = this.proc;
    p.t += dt * (2 + this.speed01 * 9);
    const model = this.root.children[0];
    if (!model) return;
    let y = p.base + Math.abs(Math.sin(p.t)) * 0.09 * this.speed01 + Math.sin(p.t * 0.4) * 0.008;
    if (p.hop && p.hop > 0) { y += Math.sin((0.55 - p.hop) / 0.55 * Math.PI) * 0.3; p.hop -= dt; }
    model.position.y = y;
    model.rotation.z = Math.sin(p.t) * 0.05 * this.speed01;
    model.rotation.x = this.speed01 * 0.06;
  }
}

// ---------------------------------------------------------------------------
// Placeholder builders (cute low-poly primitives) + factory helpers
// ---------------------------------------------------------------------------

const mats = new Map();
export function mat(color, opts = {}) {
  const key = color + JSON.stringify(opts);
  if (!mats.has(key)) mats.set(key, new THREE.MeshLambertMaterial({ color, ...opts }));
  return mats.get(key);
}

function meshPart(geo, material, x, y, z) {
  const m = new THREE.Mesh(geo, material);
  m.position.set(x, y, z);
  m.castShadow = true;
  return m;
}

const G = {
  sphere: new THREE.SphereGeometry(0.5, 20, 14),
  cone: new THREE.ConeGeometry(0.5, 1, 10),
  cyl: new THREE.CylinderGeometry(0.5, 0.5, 1, 10),
  capsule: new THREE.CapsuleGeometry(0.5, 0.6, 6, 12),
};

export function buildPlayerPlaceholder() {
  const g = new THREE.Group();
  const body = meshPart(G.capsule, mat(0x4f7dd9), 0, 0.78, 0);      // ~1.6 tall total
  body.scale.set(0.62, 0.62, 0.62);
  const head = meshPart(G.sphere, mat(0xf2c79b), 0, 1.32, 0);
  head.scale.setScalar(0.56);
  const hat = meshPart(G.cone, mat(0xd9584f), 0, 1.62, 0);
  hat.scale.set(0.46, 0.5, 0.46);
  const brim = meshPart(G.cyl, mat(0xd9584f), 0, 1.44, 0);
  brim.scale.set(0.72, 0.06, 0.72);
  g.add(body, head, hat, brim);
  const wrapper = new THREE.Group();
  wrapper.add(g);
  return wrapper;
}

export const PLACEHOLDER_SPECIES = [
  { name: 'Bunny', color: 0xf5f0e6, ear: 1.6 },
  { name: 'Fox', color: 0xe08a3c, ear: 1.0 },
  { name: 'Piglet', color: 0xf0a8b8, ear: 0.6 },
  { name: 'Frog', color: 0x7cc95e, ear: 0.35 },
  { name: 'Lamb', color: 0xd8d3f0, ear: 0.8 },
  { name: 'Bear cub', color: 0x9a6b45, ear: 0.7 },
  { name: 'Chick', color: 0xf7d558, ear: 0.4 },
  { name: 'Kitten', color: 0x8f9bb0, ear: 0.9 },
];

export function buildAnimalPlaceholder(species, hueShift = 0) {
  const c = new THREE.Color(species.color);
  if (hueShift) c.offsetHSL(hueShift, 0, 0);
  const bodyMat = new THREE.MeshLambertMaterial({ color: c });
  const g = new THREE.Group();
  const body = meshPart(G.sphere, bodyMat, 0, 0.42, 0);             // rounded body
  body.scale.set(0.62, 0.5, 0.86);
  const head = meshPart(G.sphere, bodyMat, 0, 0.68, 0.38);
  head.scale.setScalar(0.44);
  const earGeo = G.cone;
  const earH = 0.34 * species.ear;
  for (const side of [-1, 1]) {
    const ear = meshPart(earGeo, bodyMat, side * 0.12, 0.88 + earH * 0.3, 0.34);
    ear.scale.set(0.12, earH, 0.12);
    g.add(ear);
  }
  const eyeMat = mat(0x2b2b33);
  for (const side of [-1, 1]) {
    const eye = meshPart(G.sphere, eyeMat, side * 0.12, 0.72, 0.57);
    eye.scale.setScalar(0.055);
    g.add(eye);
  }
  const nose = meshPart(G.sphere, mat(0xffffff), 0, 0.62, 0.6);
  nose.scale.setScalar(0.07);
  const tail = meshPart(G.sphere, bodyMat, 0, 0.45, -0.44);
  tail.scale.setScalar(0.13);
  g.add(body, head, nose, tail);
  const wrapper = new THREE.Group();
  wrapper.add(g);
  return wrapper;
}

// Clone a real template (SkeletonUtils handles skinned meshes) and optionally
// tint it slightly so repeated species don't look identical.
function cloneReal(def, hueShift = 0) {
  const root = skeletonClone(def.template);
  if (hueShift) {
    root.traverse((o) => {
      if (o.isMesh && o.material && o.material.color) {
        o.material = o.material.clone();
        o.material.color.offsetHSL(hueShift, 0, 0);
      }
    });
  }
  return root;
}

export function makePlayerCharacter(assets) {
  if (assets.player) {
    const root = cloneReal(assets.player);
    return new Character(root, { clips: assets.player.clips, matched: assets.player.matched, height: 1.6 });
  }
  return new Character(buildPlayerPlaceholder(), { height: 1.6 });
}

// speciesIndex cycles through real species first, then placeholder species.
export function makeAnimalCharacter(assets, speciesIndex, repeat, rng) {
  const hueShift = repeat ? rng.range(-0.05, 0.05) : 0;
  const sizeJitter = repeat ? rng.range(0.88, 1.12) : 1;
  let ch; let name;
  if (assets.animals.length > 0) {
    const def = assets.animals[speciesIndex % assets.animals.length];
    const root = cloneReal(def, hueShift);
    root.scale.multiplyScalar(sizeJitter);
    ch = new Character(root, { clips: def.clips, matched: def.matched, height: 0.9 * sizeJitter });
    name = def.name;
  } else {
    const species = PLACEHOLDER_SPECIES[speciesIndex % PLACEHOLDER_SPECIES.length];
    const root = buildAnimalPlaceholder(species, hueShift);
    root.scale.multiplyScalar(sizeJitter);
    ch = new Character(root, { height: 0.9 * sizeJitter });
    name = species.name;
  }
  ch.root.traverse((o) => { if (o.isMesh) o.castShadow = true; });
  return { character: ch, name };
}
