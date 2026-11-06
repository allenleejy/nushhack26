# NN: <card title>

Wave: 1 · Devin mode: Normal · Depends on: none

## Goal
One or two sentences: what an officer can do once this card is done.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/<feature>/
- app/src/main/java/com/nushhack26/app/data/repo/<Feature>Repository.kt
- app/src/test/java/com/nushhack26/app/screenshots/<Feature>ScreenshotTest.kt

## Build
- What to build, concretely: the data each screen reads and writes (through which repository), its components, and its loading, empty, error and sync states.

## Offline behaviour
- What works with no network (normally everything), and what the officer sees while changes wait to sync.

## Acceptance criteria (checked on a phone)
- [ ] ...
- [ ] It works in airplane mode, and the changes sync once the network is back, with nothing doubled.
- [ ] Loading, empty and error states are shown.
- [ ] It fits at 360 dp wide and at 1.3× font size.

## Test steps
1. Open <screen> ...

## Notes
- Data: the tables and columns used.
- AI: only if the card has an AI feature (see stack.md).
- Design: the matching screen in design/.
