# APE — Accessibility declaration: video voiceover + caption

Use this for the Play Console **Accessibility services** declaration (the prominent
disclosure video). Record a screen capture of the app flow, then narrate with the
voiceover below and paste the caption into the video's description.

## Form answers (for reference)
- **Why does your app need AccessibilityServices API?** → **App functionality** only.
- **Do you collect/share personal or sensitive data via AccessibilityServices?** → **No.**
- `isAccessibilityTool` → leave unset (APE is not an accessibility tool for disabilities).

## What to record
Open APE → onboarding "Two switches" step (or Home permission banner) → tap
**Accessibility access** → the disclosure card slides up → hold ~3s so all three lines
are legible → tap **Turn on protection** → Android Accessibility settings appears. Stop.
Upload to YouTube (Unlisted) or Google Drive (anyone-with-link) and paste the URL.

---

## Voiceover script (~45s — read slowly)

This is APE — an app that helps you cut down on doomscrolling.

To block or add a pause to the apps and short-form feeds you choose, APE uses Android's
Accessibility service to recognize which app or feed is currently on screen.

Before turning it on, APE shows this disclosure. *(card slides up)*

It explains WHY APE needs Accessibility access — to see what's on your screen, so it can
block the feeds and apps you pick.

It explains WHAT it reads — on-screen content only, to recognize short-form feeds like
Reels, Shorts, For You, and Spotlight, and the apps you chose to limit.

And it makes clear that everything STAYS ON YOUR PHONE — APE has no account and no
servers, and nothing is collected, sent off your device, or shared.

To continue, the user taps "Turn on protection" — or "Not now" to decline.
*(tap Turn on protection)*

Only after this consent does APE open Android's Accessibility settings, where the user
enables it themselves. *(settings appears)*

---

## Caption (YouTube description)

**APE — Prominent disclosure for Accessibility Service use**

This video shows the in-app prominent disclosure that APE (package: com.ape.app)
displays before requesting Accessibility access.

APE is a fully offline focus / anti-doomscroll app. It uses the AccessibilityService API
only for App functionality — to detect the current foreground app or short-form feed
(Instagram Reels, YouTube Shorts, TikTok For You, Snapchat Spotlight, Facebook Reels) so
it can block it or add a mindful pause, according to the user's own rules.

The disclosure appears in the normal app flow, before the permission is granted, and
explains:
- Why access is needed — to see what's on screen in order to block/pause the chosen feeds and apps
- What is read — on-screen content only, to recognize short-form feeds and the user's chosen apps
- Data handling — no account, no servers; nothing is collected, transmitted off the device, or shared

The user gives affirmative consent ("Turn on protection") or declines ("Not now"). No
personal or sensitive user data is collected or shared via the AccessibilityService API.
