# APE — Closed Testing (the mandatory 14-day step)

New **personal** Google Play developer accounts must run a closed test with **at
least 12 testers who opt in, continuously for 14 days**, before they can apply for
production. (Organization accounts are exempt — check Settings → Developer account →
Account details for your type.)

Upload file: **`APE-1.0.5-v6.aab`** (the .aab, not the .apk). Package: `com.ape.app`.

---

## 1. Go to the closed testing track
Play Console → app **APE** → **Test and release → Testing → Closed testing** →
**Manage track** (the default "Alpha" track is fine).

## 2. Add 12+ testers
- **Testers** tab → **Create email list** → name it → paste **12+ Gmail addresses**
  (people/accounts you can actually get to opt in). Save, and tick the list.
- Adding emails is NOT enough — each person must open the opt-in link and accept
  (step 4). The count that matters is **opted-in testers**, and it must stay ≥12 for
  14 straight days or the clock can reset.

## 3. Create the release
- **Create new release** → **App bundles → Upload** → choose `APE-1.0.5-v6.aab`.
- Accept **Play App Signing** if prompted (normal).
- Release name: `1.0.5 (6)`. Notes: `First closed test build.`
- **Next → Save → Review release → Start rollout to Closed testing** → confirm.

## 4. Clear the checklist
App won't reach testers until these are green: Store listing, Store settings
(category), Content rating, Data safety, Privacy policy, Target audience, App access.
Fix any ⚠️ Play shows.

## 5. Share the opt-in link
- Testers tab → **Copy link** (e.g. `https://play.google.com/apps/testing/com.ape.app`).
- Each tester, on the phone signed in with the Gmail you added:
  open link → **Become a tester** → **Download it on Google Play**.
- Can take a few hours (up to ~1 day) after rollout to appear.

## 6. Wait 14 days → apply for production
- Keep **12+ testers opted in for 14 continuous days**.
- Then **Apply for production access** unlocks on the Production page → fill the form →
  Google reviews → publish publicly.

---

### Gotchas
- Use **real, separate Google accounts** — Google checks for genuine opt-ins.
- If a tester can't see the app: confirm they used the exact Gmail you added, opted in
  via the link, and gave it a few hours.
- Don't remove testers mid-test — dropping below 12 can restart the 14-day timer.
