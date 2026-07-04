// Heart burst particles — texture is drawn on a canvas at runtime, no files.
import * as THREE from 'three';

function heartTexture() {
  const c = document.createElement('canvas');
  c.width = c.height = 64;
  const ctx = c.getContext('2d');
  ctx.fillStyle = '#ff6fa5';
  ctx.strokeStyle = 'rgba(255,255,255,0.9)';
  ctx.lineWidth = 3;
  ctx.beginPath();
  ctx.moveTo(32, 56);
  ctx.bezierCurveTo(6, 38, 3, 18, 17, 11);
  ctx.bezierCurveTo(27, 6, 32, 15, 32, 21);
  ctx.bezierCurveTo(32, 15, 37, 6, 47, 11);
  ctx.bezierCurveTo(61, 18, 58, 38, 32, 56);
  ctx.closePath();
  ctx.fill();
  ctx.stroke();
  const tex = new THREE.CanvasTexture(c);
  tex.colorSpace = THREE.SRGBColorSpace;
  return tex;
}

const POOL = 48;

export class Hearts {
  constructor(scene) {
    this.group = new THREE.Group();
    scene.add(this.group);
    const tex = heartTexture();
    this.pool = [];
    for (let i = 0; i < POOL; i++) {
      const mat = new THREE.SpriteMaterial({ map: tex, transparent: true, depthWrite: false });
      const s = new THREE.Sprite(mat);
      s.visible = false;
      s.userData = { life: 0, vel: new THREE.Vector3() };
      this.group.add(s);
      this.pool.push(s);
    }
  }

  burst(pos, count = 12) {
    let spawned = 0;
    for (const s of this.pool) {
      if (spawned >= count) break;
      if (s.visible) continue;
      spawned++;
      s.visible = true;
      s.position.copy(pos);
      s.position.x += (Math.random() - 0.5) * 0.5;
      s.position.y += 0.5 + Math.random() * 0.4;
      s.position.z += (Math.random() - 0.5) * 0.5;
      s.userData.life = 1;
      s.userData.vel.set((Math.random() - 0.5) * 1.4, 1.6 + Math.random() * 1.2, (Math.random() - 0.5) * 1.4);
      s.scale.setScalar(0.3 + Math.random() * 0.15);
      s.material.opacity = 1;
    }
  }

  clear() { for (const s of this.pool) s.visible = false; }

  update(dt) {
    for (const s of this.pool) {
      if (!s.visible) continue;
      const u = s.userData;
      u.life -= dt * 1.1;
      if (u.life <= 0) { s.visible = false; continue; }
      u.vel.y -= dt * 0.6;
      s.position.addScaledVector(u.vel, dt);
      s.material.opacity = Math.min(1, u.life * 1.6);
      const sc = 0.12 + u.life * 0.24;
      s.scale.setScalar(sc);
    }
  }
}
