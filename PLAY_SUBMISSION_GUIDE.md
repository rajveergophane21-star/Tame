# Tame — first Play Store submission guide

A plain-English, do-this-in-order guide for getting Tame onto Google Play, written for a
**new personal developer account**, an **accessibility-using app**, and a **fully offline /
no-data** app. Nothing here needs deep coding — where a command is needed, it's spelled out.

> Realistic timeline: **2–4 weeks**, mostly waiting. The long poles are (1) Google verifying
> your developer identity (days) and (2) the **required 14-day closed test** before you can
> publish to the public. Budget for it; none of it is hard.

---

## Step 0 — One-time setup you do once

### 0a. Create a Google Play developer account
- Go to <https://play.google.com/console>, sign in, pay the **one-time $25** fee.
- New personal accounts must **verify your identity** (name, address, phone, sometimes a
  document). This can take a few days — start it now, it runs in the background.

### 0b. Host the privacy policy (you must have a public URL)
You already have the page: `docs/privacy-policy.html` in this repo.
1. Open `docs/privacy-policy.html` and replace `REPLACE_WITH_YOUR_EMAIL@example.com` with a
   real contact email (your Gmail is fine, or make a dedicated one like `tame.app@gmail.com`).
2. Publish it free with **GitHub Pages**:
   - In your GitHub repo → **Settings → Pages**.
   - Under "Build and deployment", set **Source = Deploy from a branch**, **Branch = your branch
     (or `main`) / folder `/docs`**, Save.
   - After a minute your policy is live at
     `https://<your-github-username>.github.io/<repo>/privacy-policy.html`.
   - Open that URL to confirm it loads. **Keep it** — you'll paste it into Play Console.
- (Alternative if you don't want GitHub Pages: paste the same text into a free Google Site or
  any host. The only requirement is a public, stable URL.)

---

## Step 1 — Make a proper signing key (do NOT ship the debug key)

Play production needs an app signed with **your own upload key**, not the debug key the test
APKs use. You generate it once and **must never lose it** (losing it means you can't update the
app later).

Run this once (needs Java installed; it's the `keytool` that ships with the JDK):

```
keytool -genkey -v -keystore tame-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias tame
```

- It asks for a password (twice) and some name/org fields — any answers are fine.
- It produces a file `tame-upload.jks`. **Back this file and the password up somewhere safe**
  (password manager + a copy off your computer). If you lose it you lose the ability to update
  Tame on Play.
- Turn ON **Play App Signing** when Console offers it (default, recommended). Your upload key
  signs the upload; Google manages the real distribution key.

> When you've created the keystore, tell me and I'll wire the Gradle release config to sign with
> it (reading the password from a local, git-ignored `keystore.properties`) — a 5-minute change.

---

## Step 2 — Build the upload file (an `.aab`, not an `.apk`)

Play requires an **Android App Bundle (`.aab`)** for new apps, not the APKs you've been testing
with. Once Step 1's signing is wired up, the command is:

```
./gradlew bundleRelease
```

The file lands at `app/build/outputs/bundle/release/app-release.aab`. That's what you upload.

> Before you build the final one, bump the version for each new upload in `app/build.gradle.kts`:
> increase `versionCode` by 1 (e.g. 1 → 2) every upload; `versionName` ("1.0") is the label users
> see. I can do these bumps for you.

---

## Step 3 — Create the app in Play Console
- Play Console → **Create app**. Name: **Tame**. Type: **App**. Free. Accept the declarations.

### Store listing — assets you'll need to prepare
- **App name:** Tame
- **Short description** (≤80 chars) — e.g. "Stop doomscrolling. Block reels & shorts, build
  better habits."
- **Full description** — say plainly that it blocks/adds friction to short-form feeds and apps
  you choose, counts reels, and does it all **on-device with no data collection**. Mention it
  uses Accessibility to detect feeds.
- **App icon:** 512×512 PNG.
- **Feature graphic:** 1024×500 PNG.
- **Phone screenshots:** at least 2 (4–8 recommended), taken from the app.
- (I can help you write the descriptions and a screenshot shot-list.)

---

## Step 4 — The compliance forms (this is where accessibility apps live or die)

### 4a. App content → Privacy policy
- Paste your public privacy-policy URL from Step 0b.

### 4b. Data safety form
- **Does your app collect or share user data?** → **No.**
- This is true for Tame (no servers, no analytics). Answer No to collection and sharing.

### 4c. Permissions Declaration — AccessibilityService (the important one)
Play will flag the accessibility use. Declare it honestly. Suggested wording:
- *What it does:* "Tame helps users reduce doomscrolling. It uses AccessibilityService to detect
  when a short-form video feed (Reels, Shorts, For You, Spotlight, etc.) or a user-selected app
  is on screen, then shows a stop/friction screen or navigates back, according to rules the user
  set."
- *Why no other API works:* "There is no alternative API that reveals which screen of a
  third-party app is currently displayed; detecting the feed requires AccessibilityService."
- *Is the user informed and consenting?* "Yes — a prominent in-app disclosure with explicit
  consent is shown before the permission is requested." (Tame already does this.)
- **Do NOT** mark Tame as an accessibility tool / set `isAccessibilityTool` — wellbeing/blocker
  apps don't qualify, and claiming it is a rejection cause. (Tame is already correct here.)

### 4d. Other declarations you may be asked
- **Full-screen intent / alarms:** if asked, the justification is "habit reminder alarms that
  ring at a user-set time, like an alarm clock."
- **Content rating:** fill the questionnaire (Tame is a utility; answer truthfully — it'll come
  out "Everyone").
- **Target audience:** 13+ / not designed for children.
- **App access:** there's no login, so choose "All functionality available without special
  access." If a reviewer needs to see blocking work, add a short note on how to enable the
  accessibility service.

---

## Step 5 — Required closed testing (new accounts can't skip this)
New personal developer accounts must run a **closed test with at least 12 testers for 14 days**
before applying for production.
1. In Console → **Testing → Closed testing → create a track**.
2. Upload your `.aab` there.
3. Add **12+ tester emails** (friends/family with Android phones; they opt in via a link).
4. Keep the test running **14 days**. Use this time to confirm Reels/Shorts/TikTok/Snap/Facebook
   detection works across their different phones and Android versions — this is exactly the
   real-world coverage you can't get from one device.
5. Fix anything that breaks, upload a new build (bump `versionCode`), continue.

---

## Step 6 — Go to production
- After the 14-day closed test, Console shows an **"Apply for production access"** step. Complete it.
- Create a **Production** release, upload the final `.aab`, fill the release notes, and submit.
- Expect a **manual review** (often a few days, sometimes longer for accessibility apps). A
  reviewer may ask for a demo video of the disclosure + blocking — that's normal, not a rejection.

---

## Quick pre-submit checklist
- [ ] Developer account created + identity verified
- [ ] Privacy policy live at a public URL (email filled in)
- [ ] Upload keystore created **and backed up**; Gradle signing wired; Play App Signing on
- [ ] `versionCode` bumped; built a signed **`.aab`**
- [ ] Store listing: icon, feature graphic, 2+ screenshots, short + full description
- [ ] Data safety = "no data collected/shared"
- [ ] Accessibility permission declared (and `isAccessibilityTool` left off)
- [ ] Content rating + target audience completed
- [ ] 12 testers added, 14-day closed test run
- [ ] Verified each feed (IG, YT, TikTok, Snap, FB) blocks on a real device

## Most common reasons accessibility apps get rejected (avoid these)
1. **No prominent disclosure before the permission** — you have this; don't remove it.
2. **Claiming to be an accessibility tool** when you're a blocker — don't.
3. **Missing/!working privacy policy URL** — make sure the link actually loads.
4. **Data safety form contradicting reality** — answer "no data," which is true.
5. **Vague permission justification** — use the concrete wording in Step 4c.
