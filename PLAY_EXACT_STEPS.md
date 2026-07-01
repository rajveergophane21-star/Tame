# APE → Play Store: exact click-by-click steps

Verified against Google's official Play Console Help (2025): closed-test setup + the
12-testers / 14-day rule for new personal accounts. Do these in order.

Your inputs (have these ready):
- Package name: `com.ape.app`
- Privacy policy URL: `https://rajveergophane21-star.github.io/Tame/privacy-policy.html`
- Contact email: `rajveergophane21@gmail.com`
- Files: `APE-release.aab`, `APE-store-icon-512.png`, `APE-feature-graphic-1024x500.png`,
  screenshots `APE-1-home.jpg` … `APE-6-cta.jpg`, text in `STORE_LISTING.md`.

---

## 1. Create the app
1. Open **play.google.com/console**.
2. Top-right → **Create app**.
3. App name: **APE** · Default language: **English (United States)** · App or game: **App**
   · Free or paid: **Free**.
4. Tick both **Declarations** boxes → **Create app**.

## 2. Dashboard → "Set up your app" tasks (answers below)
On the app **Dashboard**, open the **Set up your app** section → **View tasks**. Complete each:
1. **App access** → choose **All functionality is available without special access** → Save.
2. **Ads** → **No, my app does not contain ads** → Save.
3. **Content ratings** → **Start questionnaire** → email `rajveergophane21@gmail.com`,
   category **Utility, Productivity, Communication, or Other** → answer everything **No**
   → **Save** → **Submit**. (Result: Everyone.)
4. **Target audience and content** → target age group **13–15, 16–17, 18+** (tick these,
   NOT under-13) → "Is your app appealing to children?" **No** → Save.
5. **Data safety** → **Start** → "Does your app collect or share any of the required user
   data types?" **No** → **Next** → **Save** → **Submit**.
6. **Privacy policy** → paste `https://rajveergophane21-star.github.io/Tame/privacy-policy.html`
   → **Save**.
7. **Government apps** → No. **Financial features** → **My app doesn't provide any financial
   features**. **Health apps** → none apply. **Advertising ID** → **No** → Save each.

## 3. Main store listing
Left menu → **Grow → Store presence → Main store listing**:
1. **App name**: `APE`
2. **Short description**: paste from `STORE_LISTING.md`
3. **Full description**: paste from `STORE_LISTING.md`
4. **App icon**: upload `APE-store-icon-512.png`
5. **Feature graphic**: upload `APE-feature-graphic-1024x500.png`
6. **Phone screenshots**: upload all 6 (`APE-1-home` first … `APE-6-cta` last)
7. **Save**.

## 4. Create the closed-testing release + upload the app
1. Left menu → **Test and release → Testing → Closed testing**.
2. On the **Alpha** track → **Manage track**.
3. **Create new release** (top right).
4. **App integrity / signing**: if asked, **Continue** to use **Play App Signing** (default).
5. **App bundles** → **Upload** → pick `APE-release.aab` → wait for processing.
6. **Release name**: leave as-is. **Release notes**: paste the "What's new" line from
   `STORE_LISTING.md` (put it between the `<en-US>` tags shown).
7. **Next** → fix any red errors → **Save and publish** (confirm in the popup).

## 5. Add your 12 testers
1. Still in **Closed testing** → open the **Testers** tab.
2. **Create email list** → name it "APE testers" → paste **12+ Gmail addresses**
   (comma-separated) → **Save changes**.
3. Tick that list so it's selected.
4. **Feedback**: enter `rajveergophane21@gmail.com`.
5. **Save changes**.
6. Under **How testers join your test**, **Copy link** — this is the opt-in URL to share.

## 6. Recruit testers (send the link TODAY)
Message to send with the opt-in link:
> I built an Android app, **APE** (stops doom-scrolling — blocks Reels/Shorts, adds a
> "breathe" pause, caps your daily reels). I need 12 testers for 2 weeks to launch it.
> 2 mins: 1) On your Android phone tap **[opt-in link]** 2) "Become a tester" → "Download
> on Google Play" 3) Install APE and keep it 2 weeks (open it a few times!). Thank you 🐒

## 7. The 14-day wait (rules that avoid rejection)
- Need **12 testers opted-in continuously for 14 days** — get them in on day 1.
- Testers should actually **open/use** the app; Google can reject "no real testing."
- Any fix I make → new `.aab` (bumped version) uploaded as a new release in this track
  (also shows genuine testing).

## 8. Apply for production, then publish
1. After 14 days: **Dashboard** → **Apply for production**.
2. Fill the 3 sections — **About your closed test**, **About your app/game**,
   **Production readiness** → **Apply**. (Google reviews ~7 days, emails you.)
3. Once granted: **Test and release → Production → Create new release** → add the `.aab`
   → **Next** → **Save and publish**.
4. Final app review (a few days; they may ask for a demo video). Approved = **live**. 🎉

---
Sources: Google Play Console Help — "Set up an open, closed, or internal test"
(answer 9845334) and "App testing requirements for new personal developer accounts"
(answer 14151465).
