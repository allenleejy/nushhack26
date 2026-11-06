# Playbook: how we work

Every machine reads this. The goal is to **win with a working, polished 3-minute demo**. Speed beats perfection, and the demo path beats everything else.

## The speed plan

1. **Demo path first.** The first wave builds the demo from start to finish, even if it's ugly. Polish comes after it works.
2. **Parallel by default.** Subagents and parallel sessions are allowed and expected.
   - **Build account:** use the helper subagents to search code, run tests, review each pull request, and fix small independent bugs. Run up to 4 at once. Never do two independent things one after the other when they can run together.
   - **Devin:** one helper Devin per card, all at once. The only limit is that no two cards in the same wave edit the same file.
   - **Design account:** give options side by side in one answer (for example, 8 ideas scored in one table), not one at a time.
3. **Heavy typing goes to Devin.** Each Claude account has a small allowance (a 5-hour window plus a weekly cap). Devin Max has far more. So Claude plans, checks, merges and makes small fixes, and Devin builds the features. Don't build whole features with Claude subagents.
4. **Use the cheapest model that can do the job.** See the table below.
5. **Ship, then check.** A builder is done when `npm run check` passes and it has posted a phone-sized screenshot. Deeper checking happens in batches: the reviewer subagent, then Q on real phones.
6. **Merge in batches.** The free hosting plan allows 100 deploys a day, so the build account merges a whole round of pull requests and pushes once.
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
| Turning the spec into a database and task cards | Build account, Claude Code | Opus 5.5 · high |
| Small fixes, copy and CSS tweaks, or anything when the account's budget is low | Build account, Claude Code | Sonnet 5.5 · medium |
| Searching the code (helper `scout`) | Claude Code subagent | Haiku 5.5 · low |
| Running the phone tests (helper `phone-tester`) | Claude Code subagent | Haiku 5.5 · low |
| Reviewing one pull request (helper `reviewer`) | Claude Code subagent | Opus 5.5 · high |
| Fixing one small bug in parallel (helper `fixer`) | Claude Code subagent | Sonnet 5.5 · medium |
| A normal feature card | Devin | Normal |
| A hard card (realtime, tricky data, complex logic) | Devin, in its own session | Fusion |
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
| 14:15–15:30 | Ideas with the design account, with P and Q. When the build account pushes, import to Vercel and set up Devin. | Argue, score, push for the demo "wow" | Play devil's advocate on feasibility |
| 15:30 | **Idea locked** | | |
| 15:30–16:30 | The design account writes the spec, then the design brief. Run the brief in Claude Design and export the result into `design/`. | Demo script and real content | Open the app on both phones; draft the demo data |
| 16:30–17:00 | The build account plans: database and cards | Check the cards against the demo script | |
| 17:00 | Paste the build account's Devin line: wave 1, the demo path | Pitch outline | |
| 17:00–01:30 | The loop: merge round, Q tests, fix round, next Devin wave | Pitch story, copy, demo content | Test, file bugs, add seed data |
| 01:30 | Overnight wave: paste the build account's Devin line. Re-arm the timers (`setup.md` Part 4). Sleep 02:00–05:00. | Sleep 23:30–03:30 | Sleep 03:30–07:00 |
| 05:00 to S−2h | Last waves. Ask the design account to rescope if you're behind. | Pitch, then slides, through the design account (you type, P directs) | Full demo-path test on both phones |
| S−2h | The build account freezes features | Rehearse the pitch 3 times | Record a backup demo video |
| S | Submit | | |

Never have you and Q asleep at the same time during a build wave.

## Team

- **You:** drive all three machines and have the final say on merges. The AI accounts are in your name, and Anthropic's terms don't allow sharing a login, so only you use them.
- **P:** owns the problem and judging criteria, the idea pick (with you), the demo script, copy and content, the pitch, and timekeeping.
- **Q:** tests the live link on an iPhone and an Android after every merge round, files bugs, prepares demo data, and records the backup video.

## Rules

1. Only `main` deploys, and only the build account pushes to it.
2. No two cards in the same wave edit the same file.
3. Only the build account changes the database.
4. Secret keys stay on the server, never in the browser.
5. No offline caching (service worker). It would make phones show old versions.
6. Feature freeze at S−2h. After that, only fix the demo path.
7. Nothing merges unless `npm run check` passes.

## Bug reports (Q)

File a GitHub issue with the label `bug`:

```
Title: [P1] <screen> - <what's wrong>
Device: iPhone 15 / Safari   (or Pixel / Chrome)
Steps: 1. ...  2. ...
Expected: ...
Actual: ...
Screenshot: attach it
```

Priorities:
- **P1:** breaks the demo path.
- **P2:** visible, but the demo can avoid it.
- **P3:** polish.

To see error messages on an iPhone, add `?debug=1` to the URL.

## When things go wrong

| Problem | What to do |
|---|---|
| An AI feature fails or hits its limit (only if the app has one) | Nothing to do: it shows its canned answer instead. |
| You're near the deploy cap | Stop pushing; the live link keeps working. Ask the build account for its status, which counts deploys used. |
| The build account's allowance is running low | Switch Claude Code to Sonnet 5.5 · medium, and say "run the reviewer on Sonnet from now on". When it runs out, switch accounts (see "The two Claude windows"). |
| Both Claude accounts are out | Devin keeps building. Review pull requests by eye on GitHub. |
| You're behind schedule | Ask the design account to rescope: what to cut or fake. |
