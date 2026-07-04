// Virtual joystick for touch devices. The stick element captures its own
// pointer, so camera drag on the canvas keeps working with a second finger.
export const IS_TOUCH = (typeof window !== 'undefined') &&
  (window.matchMedia('(pointer: coarse)').matches || 'ontouchstart' in window);

export class Joystick {
  constructor(el, nub) {
    this.el = el;
    this.nub = nub;
    this.active = false;
    this.x = 0; // -1..1, right positive
    this.y = 0; // -1..1, down positive (screen space)
    this.mag = 0;
    this._pid = null;

    el.addEventListener('pointerdown', (e) => {
      e.preventDefault();
      this._pid = e.pointerId;
      try { el.setPointerCapture(e.pointerId); } catch (_) { /* synthetic pointers */ }
      this.active = true;
      this._track(e);
    });
    el.addEventListener('pointermove', (e) => {
      if (this.active && e.pointerId === this._pid) this._track(e);
    });
    const end = (e) => {
      if (e.pointerId !== this._pid) return;
      this.active = false;
      this.x = this.y = this.mag = 0;
      nub.style.transform = 'translate(-50%, -50%)';
    };
    el.addEventListener('pointerup', end);
    el.addEventListener('pointercancel', end);
  }

  _track(e) {
    const r = this.el.getBoundingClientRect();
    const radius = r.width / 2;
    let dx = (e.clientX - (r.left + radius)) / radius;
    let dy = (e.clientY - (r.top + radius)) / radius;
    const len = Math.hypot(dx, dy);
    if (len > 1) { dx /= len; dy /= len; }
    this.x = dx;
    this.y = dy;
    this.mag = Math.min(1, len);
    const px = dx * (radius - this.nub.offsetWidth / 2);
    const py = dy * (radius - this.nub.offsetHeight / 2);
    this.nub.style.transform = `translate(calc(-50% + ${px.toFixed(1)}px), calc(-50% + ${py.toFixed(1)}px))`;
  }
}
