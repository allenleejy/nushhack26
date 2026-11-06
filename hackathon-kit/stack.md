# Stack and coding rules

The build account and Devin read this before writing code.

## The app

The app is a mobile-first web app. Judges open it from a link or QR code, and it installs to the home screen on iPhone and Android.

- **Next.js** (App Router, TypeScript), with Tailwind and shadcn/ui.
- **Supabase:** Postgres, anonymous sign-in, file storage, realtime.
- **Vercel:** the free Hobby plan.
- **AI:** optional, and most features won't need it. Add an AI feature only when the spec asks for one, following "If the app uses AI" at the end of this file.

Where things live:
- `docs/spec.md`: the product spec and demo script.
- `cards/`: the task cards.
- `design/`: the design export. It's read-only.

## Commands

- `npm run dev`: local server on port 3000
- `npm run check`: typecheck, lint and build. It must pass before any pull request.
- `npm run test:phone`: Playwright smoke tests at iPhone and Pixel sizes
- `npm run shot -- /route`: saves a 390×844 screenshot into `shots/`

## Where code goes

- **`src/app/`:** routes.
  - Interactive screens are client components.
  - Use server components only for layouts and pages with no data.
- **`src/components/`:**
  - `ui/`: shadcn components. Don't edit them unless your card says so.
  - `shell/`: the top bar, bottom tab bar, install hint, sign-in bootstrap and debug console.
  - `<feature>/`: app components.
- **`src/lib/supabase/`:**
  - `client.ts`: browser client.
  - `server.ts`: server client, using cookies.
  - `admin.ts`: server-only; uses the secret key.
  - `types.ts`: generated types.
- **`supabase/migrations/`:** one new SQL file per change. Never edit a migration that has been applied.
- **`supabase/seed.sql`:** demo data.

## Coding rules

1. **Mobile first.**
   - Build for 390×844 and check at 375 wide.
   - Touch targets at least 44px. Input text at least 16px.
   - Respect safe areas and use `100dvh`. No hover-only interactions.
   - Every screen has loading, empty and error states.
2. **Data.**
   - Use the Supabase clients with row-level security (RLS). Every table has RLS enabled.
   - The secret key is used only in server-only files.
   - Tables with live updates are added to the `supabase_realtime` publication in their migration.
3. **Sign-in.** Every visitor gets an anonymous Supabase session automatically, and `auth.uid()` identifies them. Build no sign-up flow unless a card asks for one.
4. **Keys stay on the server.** Anything that uses a secret key or an external API (including AI, if the app has any) runs in server code, never in the browser.
5. **Don't add** a service worker, offline caching, a state library, an ORM, or styling other than Tailwind. Ask in the pull request before adding any dependency over about 50 kB.
6. **Deploys.** Only `main` deploys (`vercel.json`), and only the build account pushes to it.
7. **Stay in scope.** Edit only the files your card owns.
8. **Branches:**
   - Devin: `devin/NN-slug`
   - Claude Code: `claude/NN-slug`, and `claude/fix-<issue>-<slug>` for fixes
9. **When unclear,** choose the simplest reasonable option and list it under "Assumptions" in the pull request.

## Environment variables

Names only. The values live in `.env.local`, in Vercel and in Devin's secrets.

| Name | `.env.local` | Vercel | Devin |
|---|---|---|---|
| `NEXT_PUBLIC_SUPABASE_URL` | yes | yes | yes |
| `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY` | yes | yes | yes |
| `SUPABASE_SECRET_KEY` | yes | yes | **no** |

## Free-plan limits that matter

- **Vercel:** 100 deploys a day and one build at a time. Functions can run for up to 300 seconds. That's why only `main` deploys and merges happen in batches.
- **Supabase:**
  - 500 MB database, 1 GB of files, 200 live connections.
  - Its built-in email only reaches the team, so visitors sign in anonymously.
  - Anonymous sign-ins are raised to 300 per hour per IP address.

## If the app uses AI (optional)

Most of this app won't need AI. Add an AI feature only when the spec asks for one, and keep it small.

- **Where:** one server-side helper (for example `src/lib/ai.ts`) behind one API route. Never call an AI provider from the browser, and never put its key in client code.
- **Provider:** the owner's free Gemini key, as `GOOGLE_GENERATIVE_AI_API_KEY`. Add it to `.env.local` and Vercel only when the first AI feature lands, and use a stable Flash model.
- **Always have a fallback.** Give every AI call a canned answer. Use it whenever:
  - the key is missing (as in Devin's environment and in tests)
  - the call fails or takes longer than about 12 seconds
  - the free quota runs out

  The app must never break because of AI.
- **Save results** in a small table if the demo repeats the same inputs, so the live demo doesn't depend on the free quota.
- **Limits:** Gemini's free quotas are small and reset at midnight US Pacific time. If a feature turns out to need more, Groq's free tier (about 1,000 requests a day) can be added as a second provider.
