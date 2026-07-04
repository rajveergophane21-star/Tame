# TAME

A cozy third-person browser mini-game built with [three.js](https://threejs.org/) (vendored locally — fully offline, no build step, no CDN).

You are an adventurer in a low-poly meadow. Shy wild animals wander around: **sneak up slowly** (run at them and they bolt), press **E** to tame one, and it joins the happy caravan trotting behind you. Tame them all to win — your time is on the clock.

## Run it

```bash
cd game
python3 -m http.server 8000
```

Open <http://localhost:8000>. Any static file server works; there is no build step.

## Controls

| Input | Action |
| --- | --- |
| WASD / Arrow keys | Move (relative to camera) |
| Shift | Run (scares nearby animals!) |
| Mouse drag | Orbit camera · Wheel: zoom |
| E | Tame the animal you're facing (within ~2 m) |
| M | Mute / unmute sound |

## Assets & fallback mode

On boot the game fetches `assets/manifest.json` and loads the GLB models it lists
(auto-scaled to each entry's `targetHeight`, stood on the ground, animation clips
matched by name heuristics with 0.25 s crossfades). Model credits live in
`assets/CREDITS.md`.

If the manifest or any individual model is missing or fails to load, the game
**degrades per-entity to cute procedural placeholders** (capsule adventurer,
rounded-blob animals, cone trees…) and is fully playable that way — the badge in
the bottom-left corner shows whether you're seeing real models, placeholders, or
a mix. All sound is synthesized with WebAudio at runtime; heart particles are
drawn to a canvas texture at runtime. No binary assets are required.
