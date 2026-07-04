// Player controller (WASD relative to camera yaw) + smooth third-person camera.
import * as THREE from 'three';
import { terrainHeight, resolveCollisions } from './world.js';
import { PLAYER_WALK_SPEED, PLAYER_RUN_SPEED, CAM_MIN_DIST, CAM_MAX_DIST } from './constants.js';

const _target = new THREE.Vector3();
const _desired = new THREE.Vector3();
const _q = new THREE.Quaternion();
const _up = new THREE.Vector3(0, 1, 0);

export class Player {
  constructor(character, scene) {
    this.character = character;
    this.group = new THREE.Group();
    this.group.add(character.root);
    scene.add(this.group);
    this.vel = new THREE.Vector3();
    this.speed = 0;
    this.running = false;
  }

  get position() { return this.group.position; }

  forward(out) {
    return out.set(0, 0, 1).applyQuaternion(this.group.quaternion);
  }

  update(dt, input, camYaw, colliders, stick = null) {
    // input vector in camera space (keys + optional virtual joystick)
    let ix = (input.has('KeyD') || input.has('ArrowRight') ? 1 : 0) - (input.has('KeyA') || input.has('ArrowLeft') ? 1 : 0);
    let iz = (input.has('KeyW') || input.has('ArrowUp') ? 1 : 0) - (input.has('KeyS') || input.has('ArrowDown') ? 1 : 0);
    let stickRun = false;
    if (stick && stick.active && stick.mag > 0.12) {
      ix += stick.x;
      iz += -stick.y; // screen up = forward
      stickRun = stick.mag > 0.88; // pushed to the rim = run
    }
    const len = Math.hypot(ix, iz);
    if (len > 1) { ix /= len; iz /= len; }
    this.running = input.has('ShiftLeft') || input.has('ShiftRight') || stickRun;
    const speed = this.running ? PLAYER_RUN_SPEED : PLAYER_WALK_SPEED;

    // rotate into world space by camera yaw
    const sin = Math.sin(camYaw), cos = Math.cos(camYaw);
    const wx = ix * cos + iz * sin;
    const wz = iz * cos - ix * sin;

    const k = 1 - Math.exp(-10 * dt); // smooth accel/decel
    this.vel.x += (wx * speed - this.vel.x) * k;
    this.vel.z += (wz * speed - this.vel.z) * k;
    this.speed = Math.hypot(this.vel.x, this.vel.z);

    const p = this.group.position;
    p.x += this.vel.x * dt;
    p.z += this.vel.z * dt;
    resolveCollisions(p, 0.4, colliders);
    p.y = terrainHeight(p.x, p.z);

    // face movement direction
    if (this.speed > 0.4) {
      _q.setFromAxisAngle(_up, Math.atan2(this.vel.x, this.vel.z));
      this.group.quaternion.slerp(_q, 1 - Math.exp(-12 * dt));
    }

    const moving = this.speed > 0.35;
    this.character.setMoving(moving, this.running && moving);
    this.character.update(dt);
  }
}

export class FollowCamera {
  constructor(camera, dom) {
    this.camera = camera;
    this.yaw = Math.PI;        // behind the player looking at -z… tuned at start
    this.pitch = 0.42;
    this.dist = 9;
    this.enabled = true;
    this._first = true;

    let dragging = false, lastX = 0, lastY = 0;
    dom.addEventListener('pointerdown', (e) => {
      dragging = true; lastX = e.clientX; lastY = e.clientY;
      dom.setPointerCapture(e.pointerId);
    });
    dom.addEventListener('pointermove', (e) => {
      if (!dragging || !this.enabled) return;
      this.yaw -= (e.clientX - lastX) * 0.0055;
      this.pitch += (e.clientY - lastY) * 0.0045;
      this.pitch = THREE.MathUtils.clamp(this.pitch, 0.08, 1.25);
      lastX = e.clientX; lastY = e.clientY;
    });
    const stop = (e) => { dragging = false; };
    dom.addEventListener('pointerup', stop);
    dom.addEventListener('pointercancel', stop);
    dom.addEventListener('wheel', (e) => {
      if (!this.enabled) return;
      e.preventDefault();
      this.dist = THREE.MathUtils.clamp(this.dist * (1 + e.deltaY * 0.0011), CAM_MIN_DIST, CAM_MAX_DIST);
    }, { passive: false });
  }

  update(dt, targetPos) {
    _target.copy(targetPos).add(_up.set(0, 1.4, 0));
    _up.set(0, 1, 0);
    const cp = Math.cos(this.pitch), sp = Math.sin(this.pitch);
    _desired.set(
      _target.x + Math.sin(this.yaw) * cp * this.dist,
      _target.y + sp * this.dist,
      _target.z + Math.cos(this.yaw) * cp * this.dist,
    );
    const minY = terrainHeight(_desired.x, _desired.z) + 0.5;
    if (_desired.y < minY) _desired.y = minY;
    if (this._first) { this.camera.position.copy(_desired); this._first = false; }
    else this.camera.position.lerp(_desired, 1 - Math.exp(-9 * dt));
    this.camera.lookAt(_target);
  }
}
