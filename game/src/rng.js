// Small seeded RNG (mulberry32) so the meadow layout is stable between runs.
export function makeRng(seed) {
  let a = seed >>> 0;
  const next = () => {
    a |= 0; a = (a + 0x6D2B79F5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
  next.range = (min, max) => min + (max - min) * next();
  next.int = (min, max) => Math.floor(next.range(min, max + 1));
  next.pick = (arr) => arr[Math.floor(next() * arr.length) % arr.length];
  return next;
}
