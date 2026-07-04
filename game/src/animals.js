// Animal wander/flee AI, taming state and caravan following.
import * as THREE from 'three';
import { makeAnimalCharacter } from './assets.js';
import { terrainHeight, resolveCollisions } from './world.js';
import {
  ANIMAL_COUNT, ANIMAL_WALK_SPEED, ANIMAL_FLEE_SPEED,
  FLEE_NOTICE_RADIUS, FLEE_PANIC_RADIUS, SPAWN_CLEAR_RADIUS, WORLD_RADIUS,
  TAME_RANGE, TAME_FACING_DOT, TAME_DURATION, CARAVAN_SPACING,
} from './constants.js';

const IDLE = 'idle', WANDER = 'wander', FLEE = 'flee', TAMING = 'taming', TAMED = 'tamed';
const _v = new THREE.Vector3();
const _q = new THREE.Quaternion();
const _up = new THREE.Vector3(0, 1, 0);
const _fwd = new THREE.Vector3();

class Animal {
  constructor(character, name, home, rng) {
    this.character = character;
    this.name = name;
    this.rng = rng;
    this.group = new THREE.Group();
    this.group.add(character.root);
    this.home = home.clone();
    this.spawn = home.clone();
    this.group.position.copy(home);
    this.group.rotation.y = rng() * Math.PI * 2;
    this.vel = new THREE.Vector3();
    this.reset();
  }

  reset() {
    this.state = IDLE;
    this.timer = this.rng.range(0.5, 4);
    this.waypoint = new THREE.Vector3();
    this.vel.set(0, 0, 0);
    this.fleeCooldown = 0;
    this.celebrateTimer = this.rng.range(6, 14);
    this.tameT = 0;
    this.group.position.copy(this.spawn);
    this.group.position.y = terrainHeight(this.spawn.x, this.spawn.z);
  }

  get tamed() { return this.state === TAMED; }
  get pos() { return this.group.position; }

  faceToward(x, z, dt, rate = 10) {
    const dx = x - this.pos.x, dz = z - this.pos.z;
    if (dx * dx + dz * dz < 1e-6) return;
    _q.setFromAxisAngle(_up, Math.atan2(dx, dz));
    this.group.quaternion.slerp(_q, 1 - Math.exp(-rate * dt));
  }

  pickWaypoint() {
    for (let i = 0; i < 12; i++) {
      const a = this.rng() * Math.PI * 2;
      const r = this.rng.range(2, 14);
      const x = this.home.x + Math.cos(a) * r;
      const z = this.home.z + Math.sin(a) * r;
      if (Math.hypot(x, z) < WORLD_RADIUS - 2) { this.waypoint.set(x, 0, z); return; }
    }
    this.waypoint.copy(this.home);
  }

  startFlee(player) {
    this.state = FLEE;
    this.timer = 1.3;
    _v.subVectors(this.pos, player.position).setY(0);
    if (_v.lengthSq() < 1e-4) _v.set(1, 0, 0);
    _v.normalize().multiplyScalar(8).add(this.pos);
    const r = Math.hypot(_v.x, _v.z);
    if (r > WORLD_RADIUS - 2) { _v.x *= (WORLD_RADIUS - 2) / r; _v.z *= (WORLD_RADIUS - 2) / r; }
    this.waypoint.copy(_v);
  }

  startTaming(player) {
    this.state = TAMING;
    this.tameT = 0;
    this.tamePlayer = player;
  }

  // Move toward this.waypoint at speed; returns remaining distance.
  seek(dt, speed, colliders) {
    const dx = this.waypoint.x - this.pos.x, dz = this.waypoint.z - this.pos.z;
    const d = Math.hypot(dx, dz);
    if (d > 0.001) {
      this.pos.x += (dx / d) * speed * dt;
      this.pos.z += (dz / d) * speed * dt;
      this.faceToward(this.waypoint.x, this.waypoint.z, dt);
    }
    resolveCollisions(this.pos, 0.35, colliders);
    this.pos.y = terrainHeight(this.pos.x, this.pos.z);
    return d;
  }

  update(dt, ctx) {
    const { player, colliders, caravan, onTamed } = ctx;
    const ch = this.character;

    // Spookiness: a fast-approaching player scares wild animals.
    if ((this.state === IDLE || this.state === WANDER) && this.fleeCooldown <= 0) {
      const d = this.pos.distanceTo(player.position);
      if ((d < FLEE_NOTICE_RADIUS && player.speed > 5.0) || (d < FLEE_PANIC_RADIUS && player.speed > 2.4)) {
        this.startFlee(player);
      }
    }
    this.fleeCooldown = Math.max(0, this.fleeCooldown - dt);

    switch (this.state) {
      case IDLE: {
        ch.setMoving(false);
        this.timer -= dt;
        if (this.timer <= 0) { this.state = WANDER; this.pickWaypoint(); }
        break;
      }
      case WANDER: {
        ch.setMoving(true);
        if (this.seek(dt, ANIMAL_WALK_SPEED, colliders) < 0.4) {
          this.state = IDLE;
          this.timer = this.rng.range(2, 5);
        }
        break;
      }
      case FLEE: {
        ch.setMoving(true, true);
        this.timer -= dt;
        if (this.seek(dt, ANIMAL_FLEE_SPEED, colliders) < 0.6 || this.timer <= 0) {
          this.state = IDLE;
          this.timer = this.rng.range(1.5, 3);
          this.fleeCooldown = 1.2;
          this.home.copy(this.pos);
        }
        break;
      }
      case TAMING: {
        ch.setMoving(false);
        this.faceToward(player.position.x, player.position.z, dt, 14);
        this.tameT += dt;
        if (this.tameT >= TAME_DURATION) {
          this.state = TAMED;
          caravan.push(this);
          ch.celebrateOnce();
          if (onTamed) onTamed(this);
        }
        break;
      }
      case TAMED: {
        // Caravan: steer toward a point behind the previous member.
        const idx = caravan.indexOf(this);
        const leader = idx <= 0 ? player : caravan[idx - 1];
        const lp = leader.position || leader.pos;
        if (leader.forward) leader.forward(_fwd);
        else _fwd.set(0, 0, 1).applyQuaternion(leader.group.quaternion);
        _v.copy(lp).addScaledVector(_fwd, -CARAVAN_SPACING);

        // spring-damper toward the follow point
        const K = 16, D = 7.5;
        const ax = (_v.x - this.pos.x) * K - this.vel.x * D;
        const az = (_v.z - this.pos.z) * K - this.vel.z * D;
        this.vel.x += ax * dt; this.vel.z += az * dt;
        const sp = Math.hypot(this.vel.x, this.vel.z);
        const maxSp = 9;
        if (sp > maxSp) { this.vel.x *= maxSp / sp; this.vel.z *= maxSp / sp; }
        this.pos.x += this.vel.x * dt;
        this.pos.z += this.vel.z * dt;
        resolveCollisions(this.pos, 0.3, colliders);
        this.pos.y = terrainHeight(this.pos.x, this.pos.z);

        const moving = sp > 0.6;
        if (moving) this.faceToward(this.pos.x + this.vel.x, this.pos.z + this.vel.z, dt, 9);
        ch.setMoving(moving, sp > 5);

        this.celebrateTimer -= dt;
        if (!moving && this.celebrateTimer <= 0) {
          ch.celebrateOnce();
          this.celebrateTimer = this.rng.range(7, 16);
        }
        break;
      }
    }
    ch.update(dt);
  }
}

export class AnimalManager {
  constructor(scene, assets, rng) {
    this.scene = scene;
    this.rng = rng;
    this.animals = [];
    this.caravan = [];

    for (let i = 0; i < ANIMAL_COUNT; i++) {
      const speciesIndex = i; // cycles inside makeAnimalCharacter
      const repeat = assets.animals.length > 0
        ? i >= assets.animals.length
        : i >= 8; // 8 placeholder species
      const { character, name } = makeAnimalCharacter(assets, speciesIndex, repeat, rng);
      const a = Math.PI * 2 * (i / ANIMAL_COUNT) + rng.range(-0.3, 0.3);
      const r = rng.range(SPAWN_CLEAR_RADIUS + 5, WORLD_RADIUS - 14);
      const home = new THREE.Vector3(Math.cos(a) * r, 0, Math.sin(a) * r);
      home.y = terrainHeight(home.x, home.z);
      const animal = new Animal(character, name, home, rng);
      this.animals.push(animal);
      scene.add(animal.group);
    }
  }

  get tamedCount() { return this.caravan.length; }
  get total() { return this.animals.length; }

  reset() {
    this.caravan.length = 0;
    for (const a of this.animals) {
      a.home.copy(a.spawn);
      a.reset();
      a.character.setMoving(false);
    }
  }

  // Nearest wild animal the player could tame right now (range + facing).
  findTameable(player) {
    let best = null, bestD = TAME_RANGE;
    player.forward(_fwd);
    for (const a of this.animals) {
      if (a.state === TAMED || a.state === TAMING) continue;
      const d = a.pos.distanceTo(player.position);
      if (d > bestD) continue;
      _v.subVectors(a.pos, player.position).setY(0).normalize();
      if (_fwd.dot(_v) < TAME_FACING_DOT && d > 0.8) continue;
      best = a; bestD = d;
    }
    return best;
  }

  update(dt, ctx) {
    ctx.caravan = this.caravan;
    for (const a of this.animals) a.update(dt, ctx);
  }
}
