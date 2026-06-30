# APE → Play Store: the simple, do‑this‑in‑order guide

Written for: a brand‑new developer account, where Claude builds & signs the app for you.
You mostly click in a web browser and wait. You do **not** need a computer or Android Studio.

What you already have from Claude:
- **APE-release.aab** — the file you upload to Play.
- **Tame-signing-key.zip** — your signing key + password. **Save it forever, keep it private.**
- Privacy policy page (`docs/privacy-policy.html`) — host it (see Step 2).
- Store text — in `STORE_LISTING.md` (copy/paste).

---

### Step 1 — Create your Play developer account  (~30 min + a few days' wait)
1. Go to **play.google.com/console** and sign in with the Google account you want to own
   the app.
2. Choose an **individual/personal** account, pay the **one‑time $25** fee.
3. Complete **identity verification** (name, address, phone; maybe an ID photo). Google
   takes a few days to approve — start this now, the rest can wait.

### Step 2 — Put your privacy policy online (free)  (~5 min)
You need a public web link to the privacy policy. Easiest free way (GitHub):
1. Open `docs/privacy-policy.html`, replace the placeholder email with yours.
2. In your GitHub repo → **Settings → Pages** → Source: *Deploy from a branch* →
   pick your branch and the **/docs** folder → Save.
3. After a minute, your link is `https://<your-username>.github.io/<repo>/privacy-policy.html`.
   Open it to make sure it loads, and keep the link for Step 5.
(No GitHub? Tell Claude — any free host or a Google Site works; the only rule is a public link.)

### Step 3 — Create the app in Play Console  (~5 min)
1. Play Console → **Create app**.
2. App name: **APE** · Default language: English · App or game: **App** · Free.
3. Tick the policy declarations and create.
(If it says the package name `com.tame.app` is taken, tell Claude and we'll change it.)

### Step 4 — Turn on Play App Signing & upload the app  (~10 min)
1. Left menu → **Test and release → Testing → Closed testing** → **Create new release**.
2. When asked about signing, **accept Play App Signing** (the default — Google safely keeps
   your real key; your upload key is what you keep).
3. **Upload** the `APE-release.aab` file Claude sent.
4. Add **release notes** (copy the "What's new" text from `STORE_LISTING.md`). Save.

### Step 5 — Fill the required forms  (~30 min, copy/paste from STORE_LISTING.md)
In the left menu, work through each item until it has a green check:
1. **Store listing** — app name, short + full description, then upload the icon, feature
   graphic, and 2+ screenshots (just screenshot the app on your phone).
2. **App content → Privacy policy** — paste your link from Step 2.
3. **Data safety** — answer **No data collected / shared**.
4. **App access** — "All functionality available without special access" (APE has no login).
   Add a note: "Enable Accessibility for APE in Settings to see blocking work."
5. **Content rating** — fill the questionnaire (comes out *Everyone*).
6. **Target audience** — 13+; not designed for children.
7. **Permissions / Accessibility declaration** — when prompted, paste the wording from
   `PLAY_SUBMISSION_GUIDE.md` (Step 4c). Do **not** mark it an "accessibility tool."

### Step 6 — Run the required test  (14 days, mostly waiting)
New accounts must test before going public:
1. In your Closed testing track, add **12+ tester emails** (friends/family with Android).
2. Send them the opt‑in link; they install APE from Play and use it.
3. Keep the test running **14 days**. If something breaks, tell Claude — we fix it, Claude
   builds a new `.aab` (with a bumped version), you upload it.

### Step 7 — Go live  (~10 min + review)
1. After the 14 days, Play shows **"Apply for production access"** — complete it.
2. Create a **Production** release → upload the latest `.aab` → submit.
3. Google reviews it (a few days; sometimes they ask for a short demo video of the
   blocking — that's normal). Once approved, APE is live. 🎉

---

## When you need an update later
Tell Claude what to change. Claude builds a new signed `.aab` (you'll provide your
`Tame-signing-key.zip` if it's a fresh session), you upload it to a new release, submit.
Always keep your signing key — it's how Google knows updates really come from you.

## If you get stuck
Tell Claude exactly what the screen says (or screenshot it). Most "rejections" for
accessibility apps are just a request for the demo video or clearer wording — fixable.
