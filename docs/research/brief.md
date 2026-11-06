# Research brief: offline security processes for a DSO judge

6 Nov 2026, 15:55; sections 5 and 7 updated for the native Android kit (commit `a87689e`). Built from five research tracks; the full notes and every source are in `docs/research/`: A-dso, B-evidence, C-workflows, D-solutions, E-sync-tech.
Tags: **fact** = primary or official source. **reported** = news, vendor page or mirror. **inference** = our reasoning.

## 1. The problem, restated

Safety and security staff must keep doing checks, handovers and logs where there is no network, sometimes by accident (outages, dead zones) and sometimes by design (secure zones). When they reconnect, their records must merge with everyone else's: nothing lost, nothing duplicated, nothing silently overwritten.

No defence hackathon publishes a rubric. From past events and DSO's public material, a DSO judge is likely to reward (inference):
- correctness they can check, over a polished UI
- an honest security model
- not depending on someone else's cloud
- a problem framed the way defence frames it

## 2. Evidence that matters most

| # | Claim | Tag | Source |
|---|---|---|---|
| 1 | MINDEF runs classified work on a system "not connected to the Internet"; physical separation contained the 2017 I-net breach | fact | mindef.gov.sg, 28 Feb 2017 release |
| 2 | SAF camp Red Zones: camera phones go in lockers before entry. A 7 Jul 2026 parliamentary reply confirms tiered security zones | fact | mindef.gov.sg, 23 Aug 2013 fact sheet |
| 3 | DSO's own AI model is "deployed entirely on premise"; its next phase targets small models at the edge | reported (vendor page) | mistral.ai/customers/dso |
| 4 | DSO runs high-containment labs (Human Systems) and works to the AS9100 quality standard (Quality division) | fact | dso.org.sg/works |
| 5 | 8 Oct 2024 Singtel outage: 995 and 999 degraded for about 4 hours, with about half of calls connecting. IMDA said the impact "could have been very serious" and fined Singtel S$1m | fact / reported | mddi.gov.sg; The Star, 11 Dec 2025 |
| 6 | 19 Jul 2024 CrowdStrike outage: Changi check-ins went manual for 10+ airlines | reported | The Star, 20 Jul 2024 |
| 7 | A security officer was absent over 5 hours while the guardhouse occurrence book showed him on duty; CCTV caught it, and he was charged with falsifying records | fact | police.gov.sg, 8 Apr 2024 |
| 8 | 28 of 36 licence breaches found by the police licensing department (Sep to Dec 2016) concerned attendance books | reported (deck on a third-party host) | C-workflows |
| 9 | Confined-space rules: the worker's side must tell the manager when the worker has left, and revoking the permit on completion is only "may" | reported (statute mirror) | C-workflows, regs 15(2), 18(3)(a) |
| 10 | Over 80% of security agencies want more technology but struggle to deploy it; MHA cites "persistent manpower constraints" | fact | mha.gov.sg, 12 Nov 2025 |
| 11 | TrackTik, SafetyCulture and GoAudits publish no conflict model; offline just means an upload queue. Login and starting or closing a patrol still need a connection. Every product found is cloud-only | fact (vendor docs) | D-solutions |
| 12 | SafetyCulture's status page logged offline save and sync failures in 2022 and 2023 | fact | D-solutions |
| 13 | A US Army deck (Feb 2026) specifies the pattern: "Upon Disconnect, Edge continues operations"; "Upon Reconnect… resynchronize" | fact | B-evidence |

Don't quote these: the "47,000 officers" headcount, "97% of agencies use digital patrol tools", and the "30–50% of patrols falsified" figure. None has a primary source.

## 3. The insight that should shape the idea

1. **There are two kinds of records.** Logs (patrol scans, incident notes, visitor rows) only ever get added to, so merging them is trivial. Single-holder state (a key, a controlled item, a work permit, a lock-out/tag-out lock) has rules: one holder at a time, and no closing a permit while someone is inside. Plain "latest edit wins" syncing silently breaks those rules. **That is where "syncs correctly" actually matters.**
2. **Existing tools stop at the easy kind.** They queue uploads, say nothing about conflicts, need a connection for key steps, and are cloud-only. A patrol-only app would look like a clone of a Singapore vendor.
3. **For DSO, offline is a security boundary, not bad Wi-Fi.** Frame syncing as a controlled crossing that gets logged: what crosses, who authorised it, and what the audit trail shows.

## 4. Candidate framings, scored 1–5

| Framing | User | Why they're offline | Sync failure it solves | Evidence | DSO fit | Demo wow | 16h feasible | Originality | Total |
|---|---|---|---|---|---|---|---|---|---|
| **A. Custody handover of keys and controlled items, closed out by an end-of-day sweep sign-off** | Duty or security officer in a secure zone | Policy: no networked phones in Red Zones; vaults | Same key checked out twice; item "returned" while still out; a room cleared while a fault is open | 3 | 5 | 5 | 4 | 4 | **21** |
| B. Confined-space permit-to-work with lock-out/tag-out | Permit issuer, attendant, workers | Tanks, ducts, plant rooms | Permit closed while someone is inside; lock removed by the wrong person | 4 | 3 | 4 | 3 | 4 | 18 |
| C. Outage-proof incident log and emergency headcount | Fire wardens, security, emergency response team | Network or cloud outage during the event | Person counted twice or missed; duplicate incident reports | 4 | 3 | 4 | 4 | 3 | 18 |
| D. Lab or containment safety inspection checklist | DSO-style lab safety officer | Shielded labs, basements | Two inspectors tick the same checklist; a "fail" overwritten by a "pass" | 2 | 4 | 3 | 5 | 3 | 17 |
| E. Tamper-evident patrol and occurrence book | Guards on rounds | Basements, roofs | Back-filled or edited entries; duplicate scans | 3 | 3 | 3 | 5 | 1 | 15 |

**Recommendation: A.** Borrow E's tamper-evident audit trail and C's outage evidence for the pitch. A has the clearest "impossible state" for a judge to watch: two phones offline, both check out Key 7, and on reconnect the server flags a conflict instead of guessing. It has the smallest data model. It also matches how defence thinks about custody: the US forms SF 701 and SF 702 (end-of-day checklist and container log) are the public analogue (inference for DSO). B is the runner-up if the team wants life-safety stakes.

## 5. Technical approach

Track E researched a web app. Since then, the kit has switched to a native Android app (commit `a87689e`), so E's findings about iPhones and Safari no longer apply. Its sync design still holds, and the kit's offline rules in `stack.md` already follow most of it.

What still applies:
- **The phone works first, and the server decides.** Room holds the main copy of the data, every change goes into an outbox, and WorkManager sends it. That matches the kit's rules 1–3 and 9.
- **One server function applies every outbox entry.** It skips entries it has already applied, checks the version an edit started from, and checks each table's rules while locking the row. Every entry ends as applied, duplicate, conflict or rejected (with a reason), and nothing is dropped silently.
- **Three kinds of record, each with its own sync rule:**
  - Logs (patrol scans, notes) are only ever new rows, so they never conflict.
  - Checklists merge item by item, using the version each edit started from. If two phones change the same item in different ways, it becomes a conflict.
  - **Single-holder state** (key custody, permits, locks): the server checks a rule such as "one holder at a time" when the row arrives. A second checkout becomes a conflict for a supervisor to resolve. The phone's clock never decides the winner.
- **Skip the sync libraries.** Track E found none that enforces single-holder rules, so we'd have to write that part ourselves either way.
- **Security, cheapest first:**
  - app lock with fingerprint or PIN (BiometricPrompt)
  - an encrypted local database (SQLCipher)
  - a revoked device locked out at its next sync

  All three are already in the kit's "Security extras". As a stretch, each phone can chain a hash through its outbox entries, so tampering shows. That is tamper-evident, not tamper-proof.
- **Budget warning:** track E put the sync core at about 15 hours of agent work on the web stack, and Android will likely take about as long. **The sync engine is the product. Keep the domain screens thin** (a few keys, a few rooms), and build the server function and the outbox in wave 1.
- **Demo scenes, in order:**
  1. Replay a batch of changes: no duplicates
  2. Two offline phones tick different items on one checklist: both ticks are kept
  3. Both change the same item: a conflict is shown to the supervisor
  4. Both check out Key 7 offline: the second checkout becomes a conflict, and Key 7 shows "disputed"
  5. Access revoked while a phone is offline (optional)
  6. A phone's clock set back: flagged by comparing the phone's time with the server's (optional)

  A laptop shows a live audit view throughout. Keep "Sync now" visible, because Android can delay background work (inference).

## 6. Pitch notes for a DSO judge

- Open with how offline happens by design (MINDEF's air gap) and by accident (995 degraded for about 4 hours in 2024).
- Make one measurable claim: "N conflicting edits injected across M disconnections; zero lost or duplicated, every conflict surfaced."
- Answer self-hosting on one slide. Supabase is open source and can run on DSO's own servers, and the app holds no cloud-only logic.
- Say plainly that the app is for unclassified safety and process records. **Don't claim it's fit for classified material.** The hash chain is tamper-evident, not tamper-proof.
- Design for a shared or issued device with PIN login. Camera and QR capture are optional, since camera phones are restricted in SAF Red Zones.
- No bolted-on AI. DSO builds foundation models, so a thin LLM feature reads as shallow.
- Re-check every quote on the live page before it goes on a slide. The agents read pages through a summarising tool.

## 7. Kit check

The Android rewrite already makes offline the core of the app. It has an outbox, IDs created on the phone, edits that carry the version they started from, safe retries, server-side ordering and a visible sync status. One gap matters for the recommended idea:

- **Rule 4 says new rows never conflict, and for custody that's a trap.** Two offline "check out Key 7" rows are both new, so under rule 4 the server would accept both, and the key would have two holders. Suggested extra rule: *"Some new rows can still clash. If a table has a rule such as 'one holder at a time' or 'a permit can't close while someone is inside', the server checks it when the row arrives, and a row that breaks it becomes a conflict. `docs/sync.md` lists these rules for each table."*

Optional additions:
- Every outbox entry ends as applied, duplicate, conflict or rejected.
- Changes made by someone whose access was revoked while offline are set aside for review at sync.
- A hash chain on each phone's outbox entries.

## 8. Questions to ask the organisers or a DSO mentor

1. Are there judging criteria or weights? What does the DSO judge work on?
2. In secure areas, would staff use personal phones or issued shared devices?
3. When two records disagree after reconnecting, who has authority to resolve it, and what must the audit trail show?
4. Must records be editable, or is an append-only history with corrections acceptable?
5. Is self-hosting or air-gapped deployment expected, or is a cloud demo fine?

## 9. Weak spots

- Nothing public describes DSO's own security processes or device rules. Everything about DSO internals is inferred from MINDEF and SAF material.
- There is no public connectivity data for underground facilities, caverns or Tekong.
- The custody-conflict scenario is reasoned from first principles plus US analogues, not from a Singapore incident.
- Statute citations come from mirror sites, so verify them on sso.agc.gov.sg before quoting.
