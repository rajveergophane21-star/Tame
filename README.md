# APE 🐒

A calmer relationship with your phone. APE is a **private, fully‑offline Android app** that helps you stop doom‑scrolling — especially short‑form video — with the help of *Ape the monkey*, your scroll instinct.

> No account. No servers. No analytics. Everything stays on your device.

This is a native Android implementation (Kotlin + Jetpack Compose) of the "Ape the monkey" design.

---

## What it does

- **Block or add Friction to apps** — on a schedule or an on‑the‑spot timer. *Block* shows a full‑screen stop screen and sends you home; *Friction* shows a short breathing pause, then lets you choose to stay out or open anyway.
- **Block or add Friction to short‑form feeds** — Instagram Reels, YouTube Shorts, TikTok For You, Snapchat Spotlight, Facebook Reels — without blocking the whole app (you can still message).
- **Live reel counter** — a floating counter over the feed shows how many reels you've watched today against your limit; once you hit the limit, that feed switches to Friction/Block for the rest of the day.
- **Daily habits** — a check‑off list with a history grid, streaks, and alarm‑style reminders that ring until you stop them.
- **Commitment lock** — mark a rule as committed and it can't be edited or turned off until its schedule ends.

## How it works (for the technically curious)

| Capability | Android mechanism |
|---|---|
| Detect the foreground app & the short‑form feed | `AccessibilityService` (`TameAccessibilityService`) |
| Stop screen + floating reel counter over other apps | "Display over other apps" overlay (`SYSTEM_ALERT_WINDOW`) — `StopActivity` + `OverlayManager` |
| Habit reminders that ring until stopped | `AlarmManager` exact alarms + full‑screen‑intent notification → `AlarmRingActivity` |
| Local, offline storage | Jetpack DataStore (JSON), no network |

All UI is Jetpack Compose, pixel‑matched to the design, with the Ape mascot (6 moods), bundled Bricolage Grotesque + Hanken Grotesk fonts, and three accent themes.

---

## Building it

You need **Android Studio** (latest stable). Then:

1. Open this folder in Android Studio and let it sync Gradle.
2. Press **Run** with a phone connected (or an emulator), or build an APK from **Build → Build APK(s)**.

From the command line (with the Android SDK installed):

```bash
./gradlew assembleDebug      # debug APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # release build (configure signing first)
```

- **minSdk 26** (Android 8.0) · **targetSdk 35** (Android 15) · Kotlin 2.0 · AGP 8.7

## First‑run setup on the phone

APE asks for two permissions during onboarding — both are required for blocking to work:

1. **Accessibility access** — so APE can notice which app/feed is on screen.
2. **Display over other apps** — so the stop screen and floating counter can appear over other apps.

On Android 13+ it will also ask to **post notifications** (for habit alarms).

---

## Before publishing to Google Play — checklist

The app is structured to be Play‑ready, but a few items are **Play Console / policy steps** that only you can complete as the developer:

- **App signing** — create a release keystore and configure `signingConfigs` (or use Play App Signing). The repo ships a debug build only.
- **`applicationId`** — `com.ape.app` (the public package id; the internal source namespace stays `com.tame.app`, which is invisible to users).
- **Accessibility use declaration** — Play requires a short form explaining *why* the app uses accessibility (here: to detect the foreground app/feed for digital‑wellbeing blocking). A video demo is usually requested.
- **Full‑screen‑intent declaration** — habit alarms use `USE_FULL_SCREEN_INTENT` (an allowed "alarm" use case); declare it in the Play Console.
- **Exact alarms** — uses `SCHEDULE_EXACT_ALARM` for reminders, guarded at runtime; no extra declaration needed for user‑set alarms, but verify on your target devices.
- **App icon / store listing** — supply a 512×512 store icon, screenshots, and a privacy policy (the app collects no data, which simplifies this).

## Known limitations & notes

- **Feed detection is heuristic.** Recognising the *feed* inside apps like Instagram/YouTube relies on view‑id signatures (see `data/model/AppCatalog.kt`). Apps change their internals over time, so these may need occasional updates. Whole‑app blocking is robust; per‑feed detection is best‑effort. TikTok is treated as feed‑is‑whole‑app.
- **Blocking requires the "Display over other apps" permission** — APE launches the stop screen using the overlay permission's background‑activity allowance. If that permission is off, blocking can't appear.
- **The in‑app "Reels" screen is a demo** that simulates a feed so you can see the counter and stop screens without leaving the app; real enforcement happens through the accessibility service on actual apps.
- Screens are laid out edge‑to‑edge with the design's spacing; the bottom navigation respects the gesture‑bar inset.
