# Prompt: finish the Tenet pitch deck

Paste everything below the line into a new Claude Code session opened in `C:\Users\leeju\Desktop\nushhack26`. If you use claude.ai instead, upload `docs/pitch/tenet-pitch-plan.html` and every file in `docs/pitch/deck/` with the prompt.

---

Finish the pitch deck for **Tenet**, our NUSH Hack 2026 app. Make it a Slides artifact deck (start from the Slides artifact type).

**Read these first:**
- `docs/pitch/tenet-pitch-plan.html`: the full plan. It has 13 timed talk slides, each with a sketch, what goes on the slide, and the script, plus the live demo that comes after the talk.
- `docs/pitch/deck/deck.json`: the deck index (title, slide order, sections, fonts).
- `docs/pitch/deck/slides/*.html`: the 4 finished slides (`cover`, `problem`, `why`, `context`). Copy them unchanged, and match their style exactly in the new ones.
- Background, only if you need it: `docs/spec.md` (the app and demo script), `docs/setting.md` (radiography research), `design/brand.md` (voice).

**The talk:** 4:00, with the live demo afterwards (not counted in the 4:00). The judges score Functionality /10, and Originality, Design, Pitching and Sudden Dev /5 each. The guidelines ask for the prototype's functionality and an evaluation of how effective the product is.

**Write the remaining 10 slides** in the order `deck.json` gives. Content, script and timing for each come from the plan:

| # | id | Plan slide | Time |
|---|---|---|---|
| 5 | keywords | The problem statement, in radiography | 20 s |
| 6 | gap | The gap | 22 s |
| 7 | cases | It has already happened | 15 s |
| 8 | rule | Tenet in one line (the one amber statement slide) | 8 s |
| 9 | handover | How a handover works | 27 s |
| 10 | sync | Sync: one checked chain | 20 s |
| 11 | suddendev | Sudden Dev: only enrolled staff get in | 25 s |
| 12 | evaluation | Does it work? Evaluation | 25 s |
| 13 | close | Close, then the demo | 5 s |
| 14 | demo | Live demo: 3 phones, 7 beats (after the 4:00) | about 3:00 |

**Style rules, taken from the finished slides:**
- **Canvas:** 1920×1080. Each section has `padding:128px 128px 160px`, a flex column and `gap:40–48px`.
- **Colours:**
  - background `#262522`, panels `#302f2b` and `#3b3a35`
  - text `#f4f1e8`, muted `#c2beb2`
  - amber `#f2b81f`, with `#1f1e1b` text on it
  - good `#66d2ee`
  - alert `#ff8066` for text, `#ff6a4d` for strokes, `#44241d` behind it
  - slide 8 is the only one with an amber background
- **Type:** headings, labels and buttons in `'Space Mono'` 700, uppercase; body text in `'Space Grotesk'`. Sizes 72px (h2), 32 or 28px (card titles and body), 24px (labels and footer). Nothing under 24px.
- **Header:** a 24px amber mono eyebrow, then a 72px h2.
- **Cards:** a panel colour with an 8px amber left or top border and 24–28px padding. Square corners.
- **Footer:** one pinned row at `left:128px; bottom:64px`, 24px muted mono: `Tenet · NN · <short source or label>`.
- **Diagrams:** an `<svg>` with no `<text>`, and the labels as pinned `<p>` elements over it (see `why.html` and `context.html`). Use the hazard-stripe pattern and the tilted (-9°) "COUNTERSIGNED" stamp where the plan shows them.
- **Speaker notes:** one `<aside>` as the last child of each slide: the time range, the script from the plan, and any stage direction.
- **Voice:** formal and matter-of-fact. Use typographic quotes (’ “ ”). Never invent figures. The test counts (110 logic tests, 16 sync-engine, 13 server-rule, 171 screenshot tests) and the IAEA figures (55% of thefts in transport, 70% in the last decade) are in the plan.

**Placeholders to keep:** "[name]" for the team name, on slides 1 and 13.

**When you're done:** give me the deck's link and list anything you couldn't fit or had to change. Don't commit or push anything.
