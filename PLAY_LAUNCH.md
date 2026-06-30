# Tame — Play Store launch checklist (AccessibilityService)

Tame uses the AccessibilityService API to detect and block short-form feeds. Google
reviews these apps strictly. The in-app pieces are now built; the items below are
**Play Console steps you do once** when you submit. Source: Google Play "Use of the
AccessibilityService API" policy and the prominent-disclosure requirement.

## Already handled in the app (no action needed)
- **Prominent disclosure + consent**: a full-screen disclosure now appears *before*
  Tame sends you to enable Accessibility (in onboarding and on the "You" screen). It
  explains why the access is needed, what is read, that nothing leaves the device, and
  has a clear "Turn on protection" (affirmative) and "Not now" (decline). This satisfies
  the Play prominent-disclosure rule (must be in-app, in the normal flow, affirmative
  action, not buried in settings).
- **We do NOT claim to be an accessibility tool.** `isAccessibilityTool` is left unset
  (false). App-blockers / digital-wellbeing apps do **not** qualify for that flag, and
  wrongly setting it is a rejection cause. Leave it off.
- **Keep-alive**: a "Keep Tame running" card lets the user exempt Tame from battery
  optimisation, plus a background watchdog notifies them if blocking gets switched off.

## You must do in Play Console at submission
1. **Permissions Declaration Form → AccessibilityService section.** Declare it and state,
   in plain language, roughly:
   - *Functionality*: "Tame helps users reduce doomscrolling. It uses AccessibilityService
     to detect when a short-form video feed (Reels, Shorts, For You, Spotlight) or a
     user-selected app is on screen, and then shows a stop/friction screen or navigates
     back, according to rules the user set."
   - *Why no alternative API works*: detecting in-app feed screens and acting on them is
     only possible via AccessibilityService; there is no other API that exposes which
     screen of a third-party app is showing.
   - *Is user aware?*: "Yes — a prominent in-app disclosure with explicit consent is shown
     before the permission is requested."
2. **Store listing**: in the description, clearly say the app uses Accessibility to block
   distracting feeds/apps, and that all processing is on-device with no data collection.
   Include a short video of the disclosure + blocking flow (Google often asks for one for
   accessibility-using apps).
3. **Privacy policy URL** (required). Since Tame is fully offline, it can be one short
   page stating: no data is collected, stored off-device, or shared; Accessibility content
   is used only on-device to detect feeds/apps and is never transmitted. Host it anywhere
   public (e.g. a GitHub Pages page) and paste the URL in Play Console.
4. **Data safety form**: select "No data collected" and "No data shared" (true for Tame).
5. **Target API level**: keep targetSdk current (35 is fine for now; Play requires recent
   target levels for new apps — bump if Play asks).

## Notes / risks to keep in mind
- Expect a manual review and possibly a request for a demo video — that's normal for
  accessibility apps, not a rejection.
- If a future Android version restricts non-tool accessibility apps further, you may need
  to re-confirm the declaration; watch the Play policy page.
