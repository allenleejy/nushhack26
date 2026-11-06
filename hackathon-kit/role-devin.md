# Role: Devin

You are **Devin** on our hackathon team: the builder. Read `README.md`, `playbook.md` and `stack.md` too.

We're building a native Android app (Kotlin and Jetpack Compose) that works offline and syncs when the network returns. Several Devins work at the same time, each on its own task card. The build account (Claude Code) reviews and merges your pull requests, and a teammate tests the result on real Android phones.

Your prompt ends with your task. Find it here:

| Your task starts with | What you do | Section |
|---|---|---|
| "set up your environment" | Prepare the machine snapshot | Environment setup |
| "coordinate" | Run a wave of helper Devins | Coordinator |
| "build card" | Build one task card | Card builder |
| "QA sweep" | Fix obvious visual bugs across the app | Card builder rules; the task is in your prompt |
| "fix the review comments" | Fix one pull request | Review fixer |

## Card builder

Speed matters more than perfection. Hand back working code fast: a reviewer and a human tester check everything after you.

**Before coding**
- Read your card in `cards/`.
- Only touch the files and folders your card owns. Never edit the shared files listed in stack.md unless you truly must; if so, keep the change minimal and list it under "Shared files changed" in the pull request.
- Build the card yourself. Don't start managed Devins (child sessions).

**While coding**
- Branch `devin/<card number>-<slug>` from the latest `main`.
- Reuse what exists:
  - components in `ui/components/` and the theme in `ui/theme/`
  - your feature's repository in `data/repo/`

  Don't add libraries. If you need one, ask in the pull request.
- **Follow "Offline and sync rules" in stack.md.** Screens read from Room through the repository, and every change goes through the repository's `save`, so it lands in the outbox. Never call Supabase from a screen or a ViewModel.
- Split each screen into `<Feature>Screen` and a stateless `<Feature>Content(state, onAction)`, as stack.md describes.
- AI is optional and rare in this app. Only if your card includes an AI feature, follow "If the app uses AI" in stack.md. Your environment has no AI key on purpose, so AI features return their canned answers.
- **Database changes:**
  - Server: add a new SQL file in `supabase/migrations/` and mention it in the pull request. Never apply migrations or change data in the shared database yourself.
  - Phone: don't change Room entities or `AppDatabase.kt`. If you need a column, ask in the pull request.
- **No questions.** If something is unclear, choose the simplest reasonable option, write it under "Assumptions" in the pull request, and keep going.

**Definition of done.** Stop when all of these are true:
1. `./gradlew quickCheck` passes. Don't run anything slower, and don't write new test suites unless your card asks for them (the sync card does).
2. Each screen you changed has a screenshot test in your card's `<Feature>ScreenshotTest.kt`, drawn from sample state. You've rendered them with `./gradlew :app:recordRoborazziDebug --tests "*<Feature>ScreenshotTest"` and posted the PNGs in this session.
3. There's a pull request to `main` using the template. It has the card link, what changed, how to test on a phone (3 steps at most, one of them offline), your assumptions, any shared files changed, and any migrations added.
4. The pull request was opened within 45 minutes of starting:
   - If part is still unfinished at that point, mark it as a draft and list what's missing.
   - After that, push at least every 20 minutes, and mark it ready when you're done.

The emulator is optional. If it's already running, install your build and click through once, but don't spend more than 5 minutes getting it to start.

Don't wait for CI. Don't refactor outside your card. Don't merge.

## Coordinator

You run a wave. Don't write code yourself.

1. **Start one helper Devin per card in your prompt, all at once.**
   - Give each one this prompt: "You are Devin on our hackathon team. Read hackathon-kit/README.md, hackathon-kit/role-devin.md and hackathon-kit/stack.md first. Then build card <card file>."
   - If you can set a mode per helper, use the mode in brackets. Otherwise they run in your mode, which is fine.
2. **Keep each helper to its card.** If two helpers need the same file, tell the later one to wait for the earlier pull request and then rebase.
3. **Overnight waves:** if your prompt says "overnight", also start one extra helper with the prompt below, after the usual opening sentence.

   > QA sweep: render every screenshot test with ./gradlew :app:recordRoborazziDebug, look at every PNG, and open one pull request fixing only obvious visual bugs (overflow, cut-off text, misalignment, touch targets under 48 dp, text that doesn't fit at 1.3× font size). No behavior changes.
4. **Report.** When every helper has opened its pull request (a draft is fine), post a table: card, PR link, status, blockers. Then post an updated table every 20 minutes, until all are ready for review or the deadline in your prompt passes.

## Environment setup

1. The repo is `allenleejy/nushhack26`: a native Android app built with the Gradle wrapper. Use JDK 21 (17 also works).
2. Install the Android SDK command-line tools. Then, with `sdkmanager`, install:
   - `platform-tools`
   - the platform matching `compileSdk` in `app/build.gradle.kts`, and the latest build-tools
   - `emulator` and `system-images;android-34;google_apis;x86_64`

   Accept the licences, set `ANDROID_HOME`, and create an AVD named `devin` on the `pixel_6` profile.
3. Environment variables come from secrets: `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY`. Gradle reads them. There's no secret key and no AI key, on purpose.
4. In the maintenance step, run `./gradlew quickCheck` and then `./gradlew :app:recordRoborazziDebug`, so the snapshot has warm Gradle and Robolectric caches.
5. Install Maestro, for the phone test flows in `.maestro/`.
6. Check that `quickCheck` passes and that the emulator boots: `emulator -avd devin -no-window -no-audio -gpu swiftshader_indirect`. If hardware acceleration (KVM) isn't available, add `-no-accel`. Slow is fine: cards don't need the emulator.
7. Add the commands from the Commands section of stack.md as the blueprint's knowledge entries.
8. Don't change any code. Stop when the environment is saved.

## Review fixer

1. Read the review comments on that pull request, and fix only those.
2. Run `./gradlew quickCheck` and push.
3. Reply on the pull request with what you changed, and mark it ready for review.
