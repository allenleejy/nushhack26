# Playbook: how we work

Every machine reads this. The goal is to **win with a working, polished 3-minute demo** of a native Android app that keeps working offline and syncs correctly when the network returns. Speed beats perfection, and the demo path beats everything else.

## The speed plan

1. **Demo path first.** The first wave builds the demo from start to finish, even if it's ugly, including going offline and syncing back. Polish comes after it works.
2. **Parallel by default.** Subagents and parallel sessions are allowed and expected.
   - **Build account:** use the helper subagents to search code, run phone tests, review each pull request, and fix small independent bugs. Run up to 4 at once, but only one at a time may run Gradle or the emulator (the laptop has 16 GB of RAM). Never do two independent things one after the other when they can run together.
   - **Devin:** one helper Devin per card, all at once. Each runs in its own cloud machine, so the only limit is that no two cards in the same wave edit the same file.
   - **Design account:** give options side by side in one answer (for example, 8 ideas scored in one table), not one at a time.
3. **Heavy typing goes to Devin.** Each Claude account has a small allowance (a 5-hour window plus a weekly cap). Devin Max has far more. So Claude plans, checks, merges and makes small fixes, and Devin builds the features. Don't build whole features with Claude subagents.
4. **Use the cheapest model that can do the job.** See the table below.
5. **Ship, then check.** A builder is done when `./gradlew quickCheck` passes and it has posted screenshots of its screens. Deeper checking happens in batches: the reviewer subagents, then Q on real phones.
6. **Merge in batches.** The build account merges a whole round of pull requests, pushes once, and publishes one APK for the round (`r01`, `r02`, …). Q always knows which build they're testing.
7. **Don't wait for answers.** If something is unclear, make the simplest reasonable assumption, write it down (in the pull request or your reply), and keep going.
8. **Keep sessions lean.** Start fresh between big jobs: `/clear` in Claude Code, or a new chat in claude.ai. The spec, the cards and this kit hold the state, so nothing is lost. Subagents report back in a few lines, not pages.
9. **Never idle.** While a Devin wave builds, the build account fixes bugs and prepares the next cards, and the design account works on the pitch.

## Which model for what

| Work | Where | Model · effort |
|---|---|---|
| Discussing the problem, picking the idea, the spec | Design account, claude.ai | Opus 5.5 · high |
| Design and slides briefs, the pitch, rescoping | Design account, claude.ai | Opus 5.5 · medium |
| Screens and slides | Claude Design | Its own model; nothing to choose. It uses the design account's allowance. |
| Kickoff setup, merge rounds, freeze, integration bugs | Build account, Claude Code | Opus 5.5 · medium |
| The plan: database, sync rules and their tests, task cards | Build account, Claude Code | Opus 5.5 · high |
| Small fixes, copy and layout tweaks, or anything when the account's budget is low | Build account, Claude Code | Sonnet 5.5 · medium |
| Searching the code (helper `scout`) | Claude Code subagent | Haiku 5.5 · low |
| Running the phone tests (helper `phone-tester`) | Claude Code subagent | Haiku 5.5 · low |
| Reviewing a pull request that touches sync, data, the server or a shared file (helper `reviewer`) | Claude Code subagent | Opus 5.5 · high |
| Reviewing a UI-only pull request (helper `ui-reviewer`) | Claude Code subagent | Sonnet 5.5 · medium |
| Fixing one small bug in parallel (helper `fixer`) | Claude Code subagent | Sonnet 5.5 · medium |
| A normal feature card, such as a screen | Devin | Normal |
| The sync engine card, and other hard cards (conflicts, tricky data) | Devin, in its own session | Fusion |
| A trivial card (copy, seed data, tiny UI) | Devin | Lite |
| Stuck on a nasty bug | Devin | Ultra |
| The critical path in the last 2 hours | Devin | Fast (about 2× the speed, about 4× the quota) |
| Never | | Fable 5.1, and Claude Code's `/fast`. On Pro, both are paid only from usage credits. |

How to switch:
- **claude.ai:** click the model name next to the send button, choose Opus 5.5, then pick the level under **Effort**. Thinking is always on for Opus 5.5, so there's nothing else to turn on.
- **Claude Code, desktop app:** open the model picker at the right end of the row under the prompt box (Ctrl+Shift+I) and choose the model. Then pick the level under **Effort** (Ctrl+Shift+E).
- **Claude Code, terminal:** type `/model opus` or `/model sonnet`, then `/effort medium` or `/effort high`.
- **Devin:** use the mode selector next to the prompt box. Helpers that a coordinator starts take the coordinator's mode. So start every coordinator in Normal, and start each Fusion or Ultra card yourself, in its own session.

The helper agents set their own models, so switching the main model doesn't change them. Skip Extra high and Max effort: they use up a Pro window fastest.

## The two Claude windows

Each Claude account gets a 5-hour window that starts at its first message. Everything on the account shares it: chat, Claude Design, Claude Code and scheduled tasks. On Friday, a scheduled task called `Prime window` sends each account's first message: at 11:05 for the design account and 13:35 for the build account. As long as each account is used again soon after each reset, a fresh window opens about every 2.5 hours, alternating:
- **Design account:** about 16:05, 21:05, 02:05, 07:05, 12:05
- **Build account:** about 18:35, 23:35, 04:35, 09:35

What to do with them:
- **When Claude Code runs out,** switch it to the other account. In a terminal session, type `/login`. In the desktop app, sign out, then sign back in with the other account. Switch back after the reset time. The build account's role lives in the repo files, so it carries over to either account.
- **Before 16:05,** save the design account for ideas, the spec and the design.
- **Before sleeping,** re-arm both timers so new windows start while you sleep. The steps are in `setup.md`, Part 4.

## Timeline

Submission time **S**: ________. The timeline assumes S is around Sat 09:00; shift the last rows to match.

| Time | You (all machines) | P (product and pitch) | Q (testing and data) |
|---|---|---|---|
| 11:05 / 13:35 | Timers open the Claude windows. No Claude messages before them. | | |
| 14:00 | Kickoff (`setup.md` Part 3). The build account starts its kickoff job. | Read the problem and pull out the judging criteria | Read the problem and list device and data constraints |
| 14:15–15:30 | Ideas with the design account, with P and Q. When the build account says "Ready for Devin setup", set up Devin. | Argue, score, push for the demo "wow" | Turn on USB debugging on both Android phones; play devil's advocate on feasibility |
| 15:30 | **Idea locked** | | |
| 15:30–16:30 | The design account writes the spec, then the design brief. Run the brief in Claude Design and export the result into `design/`. | Demo script and real content | Install release `r01` on both phones; draft the demo data |
| 16:30–17:00 | The build account plans: database, sync rules and cards | Check the cards against the demo script | |
| 17:00 | Paste the build account's Devin lines: wave 1, the demo path, plus the sync card in Fusion | Pitch outline | |
| 17:00–01:30 | The loop: merge round and release, Q tests, fix round, next Devin wave | Pitch story, copy, demo content | Test each release online and offline, file bugs, add seed data |
| 01:30 | Overnight wave: paste the build account's Devin line. Re-arm the timers (`setup.md` Part 4). Sleep 02:00–05:00. | Sleep 23:30–03:30 | Sleep 03:30–07:00 |
| 05:00 to S−2h | Last waves. Ask the design account to rescope if you're behind. | Pitch, then slides, through the design account (you type, P directs) | Full demo-path test on both phones, offline and back |
| S−2h | The build account freezes features | Rehearse the pitch 3 times, with the phones mirrored | Record a backup demo video |
| S | Submit | | |

Never have you and Q asleep at the same time during a build wave.

## Team

- **You:** drive all three machines and have the final say on merges. The AI accounts are in your name, and Anthropic's terms don't allow sharing a login, so only you use them.
- **P:** owns the problem and judging criteria, the idea pick (with you), the demo script, copy and content, the pitch, and timekeeping.
- **Q:** installs each new release on two Android phones (the laptop's emulator can stand in for the second), tests it online and offline, files bugs, prepares demo data, and records the backup video.

## Rules

1. Only the build account pushes to `main` and publishes APKs.
2. No two cards in the same wave edit the same file, and only the build account edits the shared files listed in `stack.md`.
3. Only the build account changes the server database and the phone's database tables.
4. Secret keys never go in the app. It holds only the publishable key; row-level security protects the data.
5. Offline is the product. Every action on the demo path works in airplane mode and syncs later, with nothing lost or doubled. Never make a local action wait for the network.
6. Feature freeze at S−2h. After that, only fix the demo path.
7. Nothing merges unless `quickCheck` passes.

## Testing on phones and bug reports (Q)

After each release:
1. Sync the phone (the sync bar shows the last sync time), then install the new APK over the old one. Each release replaces the previous one and keeps its data, but a release that changes the phone's tables wipes anything not yet synced.
2. Run the "What to test" list the build account printed.
3. Do the offline check: airplane mode on, run the demo path, airplane mode off, then check that everything synced, on both phones, with nothing doubled.

To mirror a phone onto the laptop or projector, plug it in and run `scrcpy`.

File each bug as a GitHub issue with the label `bug`:

```
Title: [P1] <screen> - <what's wrong>
Release: r07   (shown on Me)
Device: Pixel 7 / Android 14
Network: online / offline / just came back online
Steps: 1. ...  2. ...
Expected: ...
Actual: ...
Screenshot: attach it, plus the Debug screen for sync problems
```

Priorities:
- **P1:** breaks the demo path, or loses or doubles data.
- **P2:** visible, but the demo can avoid it.
- **P3:** polish.

To see what's waiting to sync and the last sync error, open Me and tap the version number 5 times: that opens the Debug screen. If the phone is plugged into the laptop, the build account can also read its logs.

## When things go wrong

| Problem | What to do |
|---|---|
| An AI feature fails or hits its limit (only if the app has one) | Nothing to do: it shows its canned answer instead. |
| The laptop is slow, or Gradle runs out of memory | Close Android Studio and any extra emulators. Run `.\gradlew.bat --stop`, then try again, one Gradle job at a time. |
| A phone says "App not installed" for a new release | It has a build signed with another key, or a newer one. Sync it, uninstall the app, then install the new APK. |
| Sync is stuck | Open the Debug screen and screenshot the last error into a bug. Try **Sync now**. If the venue Wi-Fi blocks Supabase, switch the phone to the hotspot. |
| The build account's allowance is running low | Switch Claude Code to Sonnet 5.5 · medium, and say "run the reviewer on Sonnet from now on". When it runs out, switch accounts (see "The two Claude windows"). |
| Both Claude accounts are out | Devin keeps building. Review pull requests by eye on GitHub. |
| You're behind schedule | Ask the design account to rescope: what to cut or fake. |
