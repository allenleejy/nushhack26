# 25: Enrol fields only take the expected format

Wave: 6 (bug fixes) · Devin mode: Lite · Depends on: none

## Goal
On E1 Enrol, the staff number fills in its own hyphen and can't grow past its length, and the enrolment code takes exactly six digits, so nobody can type a value that can never match.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/enrol/EnrolScreen.kt
- app/src/main/java/com/nushhack26/app/ui/enrol/EnrolViewModel.kt
- app/src/main/java/com/nushhack26/app/ui/enrol/TenetTextField.kt
- app/src/test/java/com/nushhack26/app/ui/enrol/StaffNoFormatTest.kt (new)
- app/src/test/java/com/nushhack26/app/screenshots/EnrolScreenshotTest.kt

## Build
**Staff number** format: one letter, a hyphen, four digits (`R-2210`, `S-1021`, `O-3001`; every staff number in `supabase/seed.sql` has this shape).
- Keep the raw value in state as the letter plus up to 4 digits, with no hyphen (`R2210`). Show the hyphen with a `VisualTransformation` and a correct `OffsetMapping`, so the cursor never lands in the wrong place.
  - Add an optional `visualTransformation: VisualTransformation = VisualTransformation.None` parameter to `TenetTextField` and pass it to the `OutlinedTextField`.
- The hyphen appears as soon as the letter is typed: typing `R` shows `R-` with the cursor after the hyphen. One backspace from there removes the `R`, so the field never gets stuck.
- `onStaffNo` normalises whatever arrives, including a paste:
  - uppercase it, and drop spaces and hyphens
  - the first character must be a letter A–Z; anything else is ignored until a letter comes
  - after the letter, only digits, at most 4. Extra characters are dropped, so the field can't hold more than `R-2210`.
  - So `r2210`, `R-2210`, `r 2210` and `R22105` all become `R2210`.
- When enrolling, send the formatted value, `R-2210`, exactly as the server stores it.
- `canSubmit` needs a complete staff number (a letter and 4 digits) as well as the 6-digit code.
- Keyboard: keep `KeyboardCapitalization.Characters`, `KeyboardType.Ascii` and autocorrect off. The placeholder stays `R-2210`.

**Enrolment code:** digits only, at most `CODE_LENGTH` (6). `onCode` already filters; make sure the field itself can't show a seventh character, even for a moment, by passing the filtered value straight back. Keep `KeyboardType.NumberPassword`.

Keep the test tags `staff_no` and `enrol_code`. `.maestro/enrol-wrong-code.yaml` types `R-2210` into the field, so typing the hyphen yourself must give `R-2210`, not `R--2210`.

## Offline behaviour
- Input only. Enrolling still needs the network once, as now.

## Acceptance criteria (checked on a phone)
- [ ] Type `r2210`: the field shows `R-2210`. Type another digit: nothing changes.
- [ ] Type `R`: the field shows `R-`. One backspace leaves it empty.
- [ ] Type `R-2210` with the hyphen: the field shows `R-2210`.
- [ ] Paste `r 2210 9`: the field shows `R-2210`.
- [ ] The code field takes no letters and no seventh digit.
- [ ] CREATE SIGNING KEY stays disabled until both fields are complete, and enrolling as R-2210 with the right code still works.
- [ ] It fits at 360 dp wide and at 1.3× font size.

## Test steps
1. Clear the app's data (or install on a fresh phone) to reach E1.
2. Try each input above, then enrol with the real staff number and code.

## Notes
- **Tests:** `StaffNoFormatTest` covers the normalising (the cases above) and the `OffsetMapping` (original offsets 0, 1, 2 and 5 map to 0, 2, 3 and 6, and back). Add a screenshot case with a half-typed `R-22`.
- **Data:** `people.staff_no`, unchanged.
