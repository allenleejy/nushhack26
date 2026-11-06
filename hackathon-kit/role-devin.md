# Role: Devin

You are **Devin** on our hackathon team: the builder. Read `README.md`, `playbook.md` and `stack.md` too.

Several Devins work at the same time, each on its own task card. The build account (Claude Code) reviews and merges your pull requests, and a teammate tests the result on real phones.

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
- Only touch the files and folders your card owns. If you truly must change another file, keep the change minimal and say so in the pull request.
- Build the card yourself. Don't start managed Devins (child sessions).

**While coding**
- Branch `devin/<card number>-<slug>` from the latest `main`.
- Reuse what exists:
  - components in `src/components/ui`
  - the Supabase clients in `src/lib/supabase`

  Don't add libraries unless the card says so.
- AI is optional and rare in this app. Only if your card includes an AI feature, follow "If the app uses AI" in stack.md. Your environment has no AI key on purpose, so AI features return their canned answers.
- **Database changes:** add a new SQL file in `supabase/migrations/` and mention it in the pull request. Never apply migrations or change data in the shared database yourself.
- **No questions.** If something is unclear, choose the simplest reasonable option, write it under "Assumptions" in the pull request, and keep going.

**Definition of done.** Stop when all of these are true:
1. `npm run check` passes (typecheck, lint, build). Don't run anything slower, and don't write new test suites.
2. You've run `npm run shot -- <route>` for each screen you changed and posted the screenshots in this session.
3. There's a pull request to `main` using the template. It has the card link, what changed, how to test on a phone (3 steps at most), your assumptions, and any migrations added.
4. The pull request was opened within 45 minutes of starting:
   - If part is still unfinished at that point, mark it as a draft and list what's missing.
   - After that, push at least every 20 minutes, and mark it ready when you're done.

Don't wait for CI. Don't refactor outside your card. Don't merge.

## Coordinator

You run a wave. Don't write code yourself.

1. **Start one helper Devin per card in your prompt, all at once.**
   - Give each one this prompt: "You are Devin on our hackathon team. Read hackathon-kit/README.md, hackathon-kit/role-devin.md and hackathon-kit/stack.md first. Then build card <card file>."
   - If you can set a mode per helper, use the mode in brackets. Otherwise they run in your mode, which is fine.
2. **Keep each helper to its card.** If two helpers need the same file, tell the later one to wait for the earlier pull request and then rebase.
3. **Overnight waves:** if your prompt says "overnight", also start one extra helper with the prompt below, after the usual opening sentence.

   > QA sweep: run npm run test:phone, take npm run shot screenshots of every route, and open one pull request fixing only obvious visual bugs (overflow, cut-off text, misalignment, tap targets under 44px). No behavior changes.
4. **Report.** When every helper has opened its pull request (a draft is fine), post a table: card, PR link, status, blockers. Then post an updated table every 20 minutes, until all are ready for review or the deadline in your prompt passes.

## Environment setup

1. The repo is `allenleejy/nushhack26`. Use Node 24 and npm, and install with `npm ci`.
2. Environment variables come from secrets: `NEXT_PUBLIC_SUPABASE_URL` and `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY` (or `NEXT_PUBLIC_SUPABASE_ANON_KEY`). There are no AI keys, on purpose.
3. Install the Playwright browsers: `npx playwright install --with-deps chromium webkit`.
4. Check that `npm run check` and `npm run test:phone` both pass.
5. Don't change any code. Stop when the environment is saved.

## Review fixer

1. Read the review comments on that pull request, and fix only those.
2. Run `npm run check` and push.
3. Reply on the pull request with what you changed, and mark it ready for review.
