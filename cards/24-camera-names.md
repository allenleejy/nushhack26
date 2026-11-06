# 24: Camera names everywhere else, and RECEIVE buttons

Wave: 5 (polish, after the overnight merge) · Devin mode: Normal · Depends on: the overnight merge round (PRs for cards 09, 10 and 13, and the QA sweep), which the build account announces; the `items.name` column (already on `main`)

## Goal
People talk about "Kestrel", not "C-12". Every screen that names a camera leads with its name and labels each code, so a judge never has to guess what C-12, IR-0457 and S-40117 are.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/store/
- app/src/main/java/com/nushhack26/app/ui/mycustody/
- app/src/main/java/com/nushhack26/app/ui/timeline/
- app/src/main/java/com/nushhack26/app/ui/fork/
- app/src/main/java/com/nushhack26/app/ui/alerts/
- app/src/main/java/com/nushhack26/app/ui/incoming/
- app/src/main/java/com/nushhack26/app/data/repo/CustodyRepository.kt, SafetyRepository.kt, RelayRepository.kt (only to carry the item's name through)
- their screenshot tests in app/src/test/java/com/nushhack26/app/screenshots/: Store, MyCustody, Timeline, Fork, Alerts, Incoming

## Build
Two patterns:
- **In a title:** the name, uppercase (`item.displayName`), then a labelled code line in `record`: `CAMERA C-12 · SOURCE IR-0457 · SEAL S-40117`.
- **Inside a sentence:** the name with the code in brackets, "Kestrel (C-12)".

When the name is blank, both fall back to the code alone (`displayName` already does).

- **Store (S1):**
  - row title KESTREL
  - code line `CAMERA C-12 · SOURCE IR-0457 · IR-192`
  - status chip, holder and due lines as now
  - the **Receive return** button uses `Icons.Sharp.Contactless`
- **My custody (P1):**
  - card "You hold Kestrel", then `CAMERA C-12 · SEALED SOURCE IR-0457 · RETURN BY 14:31`
  - overdue banner "Kestrel (C-12) return overdue · 4 min"
  - receipts "Handed Kestrel (C-12) to Tan Wei Ming · 14:30 · signed by Wei Ming ✓"
  - the main **Receive** button uses `Icons.Sharp.Contactless`
  - empty text: "You don't hold anything. Open RECEIVE when someone hands you a camera."
- **Timeline (T):** header KESTREL, then `CAMERA C-12 · SOURCE IR-0457 · IR-192 · SEAL S-40117`. The entries keep their codes.
- **Alerts (O1):**
  - "Ravi Kumar signed two handovers of Kestrel (C-12) after H1 · both signatures valid on this phone"
  - "Kestrel (C-12) overdue 4 min · …"
  - "Kestrel (C-12) accepted by Jun Lim with seal S-40999 (expected S-40117)"
  - the refused line names the camera the same way
- **Fork (O2):** header "KESTREL (C-12) · FORK AFTER H1", and the choices "Kestrel is with Tan Wei Ming" / "Kestrel is with Siti Aminah".
- **Incoming banner (R0):** "**Siti Aminah is handing you Kestrel (C-12)** · Scan its seal to accept".

## Offline behaviour
- Names come down with the normal sync (`items.name`) and are read from Room, so they show offline.
- Before a phone has synced since the column was added, the codes show alone, as today.

## Acceptance criteria (checked on a phone)
- [ ] After a sync: Store shows KESTREL, OSPREY and HERON with labelled code lines.
- [ ] My custody, Timeline, Alerts, Fork and the incoming banner all name Kestrel as above.
- [ ] The Receive and Receive return buttons show the contactless icon, and switch to the RECEIVE tab.
- [ ] Demo script §2 still reads naturally on the projector.
- [ ] It fits at 360 dp and 1.3× font size; long names wrap.

## Test steps
1. Sync both phones, then open each screen above.
2. Run demo script §2 steps 1–7 and read every camera mention.

## Notes
- **Data:** `items.name` → `ItemEntity.name` / `displayName`. Display only: it's never signed and isn't in any record hash.
- **Before starting:** pull `main` after the overnight merge. Cards 09, 13 and the QA sweep changed `timeline/`, `alerts/AlertsModel.kt`, `store/StoreScreen.kt` and `mycustody/CustodyComponents.kt`.
