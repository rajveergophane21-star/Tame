// HTML overlay HUD: counter, timer, tame prompt, badge, start/win overlays.
import * as THREE from 'three';

const _v = new THREE.Vector3();

export class UI {
  constructor() {
    this.count = document.getElementById('hudCount');
    this.timer = document.getElementById('hudTimer');
    this.mute = document.getElementById('hudMute');
    this.hint = document.getElementById('hint');
    this.badge = document.getElementById('badge');
    this.prompt = document.getElementById('prompt');
    this.startOverlay = document.getElementById('startOverlay');
    this.winOverlay = document.getElementById('winOverlay');
    this.winTime = document.getElementById('winTime');
  }

  setCount(n, total, pop = false) {
    this.count.textContent = `🐾 Tamed ${n} / ${total}`;
    if (pop) {
      this.count.classList.remove('pop');
      void this.count.offsetWidth; // restart the animation
      this.count.classList.add('pop');
    }
  }

  setTimer(seconds) {
    this.timer.textContent = formatTime(seconds);
  }

  setMuted(m) { this.mute.textContent = m ? '🔇 muted' : '🔊 sound'; }

  setBadge(attempted, real) {
    if (attempted === 0) this.badge.textContent = 'assets: placeholder (no manifest)';
    else if (real === attempted) this.badge.textContent = `assets: real (${real}/${attempted} models)`;
    else if (real === 0) this.badge.textContent = `assets: placeholder (0/${attempted} models loaded)`;
    else this.badge.textContent = `assets: mixed (${real}/${attempted} real, rest placeholder)`;
  }

  fadeHintLater(ms = 8000) {
    setTimeout(() => this.hint.classList.add('faded'), ms);
  }

  // Project a world position to the screen and park the prompt there.
  showPrompt(worldPos, camera) {
    _v.copy(worldPos);
    _v.project(camera);
    if (_v.z > 1) { this.hidePrompt(); return; }
    const x = (_v.x * 0.5 + 0.5) * window.innerWidth;
    const y = (-_v.y * 0.5 + 0.5) * window.innerHeight;
    this.prompt.style.display = 'block';
    this.prompt.style.left = `${x.toFixed(1)}px`;
    this.prompt.style.top = `${y.toFixed(1)}px`;
  }

  hidePrompt() { this.prompt.style.display = 'none'; }

  showWin(seconds) {
    this.winTime.textContent = formatTime(seconds);
    this.winOverlay.classList.remove('hidden');
  }

  hideWin() { this.winOverlay.classList.add('hidden'); }
  hideStart() { this.startOverlay.classList.add('hidden'); }
}

export function formatTime(seconds) {
  const s = Math.max(0, Math.floor(seconds));
  const mm = String(Math.floor(s / 60)).padStart(2, '0');
  const ss = String(s % 60).padStart(2, '0');
  return `${mm}:${ss}`;
}
