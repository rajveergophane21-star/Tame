// Meadow construction: terrain, sky/fog, lights, scattered props, colliders.
import * as THREE from 'three';
import { mat } from './assets.js';
import { WORLD_SIZE, WORLD_RADIUS, SPAWN_CLEAR_RADIUS } from './constants.js';

const SKY = 0x9cc8ee;

// Analytic terrain height — shared by the ground mesh and every entity so feet
// always rest on the grass. Gentle, and flat near the centre spawn.
export function terrainHeight(x, z) {
  const r = Math.hypot(x, z);
  const fade = THREE.MathUtils.smoothstep(r, 6, 22);
  const h = Math.sin(x * 0.061) * Math.cos(z * 0.052) * 0.55
    + Math.sin(x * 0.17 + z * 0.13) * 0.18
    + Math.cos(x * 0.023 - z * 0.031) * 0.4;
  return h * fade;
}

export function buildWorld(scene, rng, assets) {
  scene.background = new THREE.Color(SKY);
  scene.fog = new THREE.Fog(SKY, 55, 150);

  // --- lights -------------------------------------------------------------
  const hemi = new THREE.HemisphereLight(0xcfe5ff, 0x8cba70, 1.25);
  scene.add(hemi);
  const sun = new THREE.DirectionalLight(0xfff1d6, 2.4);
  sun.position.set(24, 34, 14);
  sun.castShadow = true;
  sun.shadow.mapSize.set(2048, 2048);
  const s = 34;
  sun.shadow.camera.left = -s; sun.shadow.camera.right = s;
  sun.shadow.camera.top = s; sun.shadow.camera.bottom = -s;
  sun.shadow.camera.near = 2; sun.shadow.camera.far = 110;
  sun.shadow.bias = -0.0004;
  sun.shadow.normalBias = 0.03;
  scene.add(sun, sun.target);

  // --- ground ---------------------------------------------------------------
  const seg = 96;
  const groundGeo = new THREE.PlaneGeometry(WORLD_SIZE, WORLD_SIZE, seg, seg);
  groundGeo.rotateX(-Math.PI / 2);
  const pos = groundGeo.attributes.position;
  const colors = new Float32Array(pos.count * 3);
  const base = new THREE.Color(0x6cb35c);
  const light = new THREE.Color(0x8ecf6d);
  const dark = new THREE.Color(0x549e51);
  const tmp = new THREE.Color();
  for (let i = 0; i < pos.count; i++) {
    const x = pos.getX(i), z = pos.getZ(i);
    pos.setY(i, terrainHeight(x, z));
    // patchy grass colouring from smooth pseudo-noise
    const n = Math.sin(x * 0.11 + 3.1) * Math.cos(z * 0.09 + 1.7) * 0.5 + 0.5;
    tmp.copy(base).lerp(n > 0.5 ? light : dark, Math.abs(n - 0.5) * 1.6);
    tmp.offsetHSL(0, 0, (Math.sin(x * 0.43) * Math.cos(z * 0.37)) * 0.012);
    colors[i * 3] = tmp.r; colors[i * 3 + 1] = tmp.g; colors[i * 3 + 2] = tmp.b;
  }
  groundGeo.setAttribute('color', new THREE.BufferAttribute(colors, 3));
  groundGeo.computeVertexNormals();
  const ground = new THREE.Mesh(groundGeo, new THREE.MeshLambertMaterial({ vertexColors: true }));
  ground.receiveShadow = true;
  scene.add(ground);

  // --- props ----------------------------------------------------------------
  const colliders = [];
  const placed = [];

  // Split any real prop models into rough pools by name.
  const pools = { tree: [], rock: [], small: [] };
  for (const p of assets.props) {
    const n = (p.name || '').toLowerCase();
    if (n.includes('rock') || n.includes('stone')) pools.rock.push(p);
    else if (n.includes('flower') || n.includes('grass') || n.includes('mushroom') || n.includes('bush') || n.includes('plant')) pools.small.push(p);
    else pools.tree.push(p);
  }

  const treeGroup = new THREE.Group();
  scene.add(treeGroup);

  function findSpot(minR, maxR, spacing) {
    for (let tries = 0; tries < 24; tries++) {
      const a = rng() * Math.PI * 2;
      const r = rng.range(minR, maxR);
      const x = Math.cos(a) * r, z = Math.sin(a) * r;
      if (Math.hypot(x, z) < SPAWN_CLEAR_RADIUS + spacing) continue;
      let ok = true;
      for (const p of placed) {
        if ((x - p.x) ** 2 + (z - p.z) ** 2 < (spacing + p.s) ** 2) { ok = false; break; }
      }
      if (ok) { placed.push({ x, z, s: spacing }); return { x, z }; }
    }
    return null;
  }

  function placeReal(pool, spot, scaleJitter, colliderR) {
    const def = pool[rng.int(0, pool.length - 1)];
    const m = def.template.clone(true);
    const sc = rng.range(1 - scaleJitter, 1 + scaleJitter);
    m.scale.multiplyScalar(sc);
    m.rotation.y = rng() * Math.PI * 2;
    m.position.set(spot.x, terrainHeight(spot.x, spot.z), spot.z);
    treeGroup.add(m);
    if (colliderR > 0) colliders.push({ x: spot.x, z: spot.z, r: colliderR * sc });
  }

  // Trees (placeholder: pine = trunk + stacked cones; round tree = trunk + blobs)
  const trunkMat = mat(0x8a5a37);
  const pineMat = mat(0x3f8f4f);
  const pineMat2 = mat(0x51a75b);
  const leafMat = mat(0x63b054);
  const trunkGeo = new THREE.CylinderGeometry(0.16, 0.24, 1.3, 8);
  const coneGeo = new THREE.ConeGeometry(1, 1.6, 9);
  const blobGeo = new THREE.SphereGeometry(1, 12, 9);

  function placeholderTree(spot) {
    const g = new THREE.Group();
    const sc = rng.range(0.85, 1.5);
    const trunk = new THREE.Mesh(trunkGeo, trunkMat);
    trunk.position.y = 0.6; trunk.castShadow = true;
    g.add(trunk);
    if (rng() < 0.55) {
      const c1 = new THREE.Mesh(coneGeo, pineMat); c1.position.y = 1.9; c1.castShadow = true;
      const c2 = new THREE.Mesh(coneGeo, pineMat2); c2.position.y = 2.8; c2.scale.setScalar(0.72); c2.castShadow = true;
      g.add(c1, c2);
    } else {
      const b1 = new THREE.Mesh(blobGeo, leafMat); b1.position.y = 2.0; b1.scale.set(1.15, 1, 1.15); b1.castShadow = true;
      const b2 = new THREE.Mesh(blobGeo, pineMat2); b2.position.set(0.5, 2.5, 0.2); b2.scale.setScalar(0.6); b2.castShadow = true;
      g.add(b1, b2);
    }
    g.scale.setScalar(sc);
    g.rotation.y = rng() * Math.PI * 2;
    g.position.set(spot.x, terrainHeight(spot.x, spot.z), spot.z);
    treeGroup.add(g);
    colliders.push({ x: spot.x, z: spot.z, r: 0.45 * sc });
  }

  const treeCount = 46;
  for (let i = 0; i < treeCount; i++) {
    // bias trees outward so the meadow edge reads as a forest ring
    const edge = i < 18;
    const spot = findSpot(edge ? WORLD_RADIUS - 8 : 16, edge ? WORLD_RADIUS + 4 : WORLD_RADIUS - 6, 2.6);
    if (!spot) continue;
    if (pools.tree.length) placeReal(pools.tree, spot, 0.3, 0.5);
    else placeholderTree(spot);
  }

  // Rocks
  const rockGeo = new THREE.IcosahedronGeometry(0.7, 0);
  const rockMat = mat(0x9aa0a8);
  for (let i = 0; i < 20; i++) {
    const spot = findSpot(15, WORLD_RADIUS - 4, 1.6);
    if (!spot) continue;
    if (pools.rock.length) { placeReal(pools.rock, spot, 0.35, 0.55); continue; }
    const rock = new THREE.Mesh(rockGeo, rockMat);
    const sc = rng.range(0.5, 1.4);
    rock.scale.set(sc, sc * rng.range(0.55, 0.8), sc);
    rock.rotation.set(rng() * 0.4, rng() * Math.PI * 2, rng() * 0.4);
    rock.position.set(spot.x, terrainHeight(spot.x, spot.z) + 0.05, spot.z);
    rock.castShadow = true; rock.receiveShadow = true;
    treeGroup.add(rock);
    colliders.push({ x: spot.x, z: spot.z, r: 0.55 * sc });
  }

  // Small props from the manifest (mushrooms, bushes …), no colliders.
  if (pools.small.length) {
    for (let i = 0; i < 16; i++) {
      const spot = findSpot(14, WORLD_RADIUS - 6, 1.2);
      if (spot) placeReal(pools.small, spot, 0.3, 0);
    }
  }

  // Flowers + grass tufts, instanced (cheap charm).
  const dummy = new THREE.Object3D();
  const stemGeo = new THREE.CylinderGeometry(0.02, 0.03, 0.32, 5);
  const headGeo = new THREE.IcosahedronGeometry(0.09, 0);
  const FLOWERS = 160;
  const stems = new THREE.InstancedMesh(stemGeo, mat(0x4d9e4a), FLOWERS);
  const heads = new THREE.InstancedMesh(headGeo, new THREE.MeshLambertMaterial({ color: 0xffffff }), FLOWERS);
  const petal = [0xff8fb3, 0xffd76e, 0xffffff, 0xc39bff, 0xff9d6e];
  const col = new THREE.Color();
  for (let i = 0; i < FLOWERS; i++) {
    const a = rng() * Math.PI * 2;
    const r = Math.sqrt(rng()) * (WORLD_RADIUS - 4);
    const x = Math.cos(a) * r, z = Math.sin(a) * r;
    const y = terrainHeight(x, z);
    dummy.position.set(x, y + 0.16, z);
    dummy.rotation.set(rng.range(-0.15, 0.15), 0, rng.range(-0.15, 0.15));
    dummy.scale.setScalar(rng.range(0.8, 1.5));
    dummy.updateMatrix();
    stems.setMatrixAt(i, dummy.matrix);
    dummy.position.y = y + 0.34 * dummy.scale.x;
    dummy.updateMatrix();
    heads.setMatrixAt(i, dummy.matrix);
    heads.setColorAt(i, col.setHex(petal[rng.int(0, petal.length - 1)]));
  }
  stems.instanceMatrix.needsUpdate = true;
  heads.instanceMatrix.needsUpdate = true;
  if (heads.instanceColor) heads.instanceColor.needsUpdate = true;
  scene.add(stems, heads);

  const tuftGeo = new THREE.ConeGeometry(0.09, 0.3, 5);
  const TUFTS = 240;
  const tufts = new THREE.InstancedMesh(tuftGeo, new THREE.MeshLambertMaterial({ color: 0xffffff }), TUFTS);
  for (let i = 0; i < TUFTS; i++) {
    const a = rng() * Math.PI * 2;
    const r = Math.sqrt(rng()) * (WORLD_RADIUS - 2);
    const x = Math.cos(a) * r, z = Math.sin(a) * r;
    dummy.position.set(x, terrainHeight(x, z) + 0.12, z);
    dummy.rotation.set(rng.range(-0.2, 0.2), rng() * Math.PI, rng.range(-0.2, 0.2));
    dummy.scale.set(rng.range(0.8, 1.8), rng.range(0.8, 2.1), rng.range(0.8, 1.8));
    dummy.updateMatrix();
    tufts.setMatrixAt(i, dummy.matrix);
    tufts.setColorAt(i, col.setHex(0x5faf55).offsetHSL(rng.range(-0.02, 0.04), rng.range(-0.05, 0.1), rng.range(-0.04, 0.05)));
  }
  tufts.instanceMatrix.needsUpdate = true;
  if (tufts.instanceColor) tufts.instanceColor.needsUpdate = true;
  scene.add(tufts);

  return {
    colliders,
    sun,
    // keep the shadow frustum centred on the player for crisp shadows
    update(playerPos) {
      sun.position.set(playerPos.x + 24, 34, playerPos.z + 14);
      sun.target.position.set(playerPos.x, 0, playerPos.z);
    },
  };
}

// Push a point out of prop colliders and clamp to the world boundary.
export function resolveCollisions(p, radius, colliders) {
  for (const c of colliders) {
    const dx = p.x - c.x, dz = p.z - c.z;
    const min = c.r + radius;
    const d2 = dx * dx + dz * dz;
    if (d2 < min * min && d2 > 1e-8) {
      const d = Math.sqrt(d2);
      p.x = c.x + (dx / d) * min;
      p.z = c.z + (dz / d) * min;
    }
  }
  const r = Math.hypot(p.x, p.z);
  if (r > WORLD_RADIUS) { p.x *= WORLD_RADIUS / r; p.z *= WORLD_RADIUS / r; }
}
