# D - Existing solutions and where they fall short

Researched 2026-11-06 for the DSO challenge: "Build an app that lets safety personnel keep completing security processes offline and syncs their changes correctly when internet access returns."

Tags: [fact] = vendor documentation or other primary source. [reported] = reviews, news, forums, aggregators. [inference] = my own reasoning.

Source-quality caveats (read first):
- Several primary pages returned HTTP 403 to my fetcher (TrackTik support centre, Capterra, G2, Software Advice, Reddit). Where a claim about those pages comes from a search-result summary rather than a page I read, it is marked "(via search snippet)". Treat those as lower confidence and re-check in a browser if the team will quote them.
- SafetyCulture's help centre, status page and security page now redirect to "Mitti (by SafetyCulture)" (help.mitti.com, status.mitti.com, mitti.com). I read the pages at those redirect targets. The product is still referred to below as SafetyCulture / iAuditor.
- No Reddit thread was retrievable (WebFetch is blocked for reddit.com and searches returned none). Complaint evidence therefore comes from app stores, review-site write-ups, vendor status pages and forums.

---

## 1. Key findings (ranked by importance to the challenge)

1. **Conflict handling is essentially undocumented in the commercial guard-tour and inspection apps.** For TrackTik, Silvertrac, GoAudits and SafetyCulture I found no public statement of what happens when two devices change the same record offline. Their offline model is an append-only queue (scan, report, photo) that uploads later; the Trackforce offline article says nothing about conflicts, and the GoAudits offline page says the same. [fact]
   - https://www.trackforce.com/resources/blog-articles/using-security-guard-software-without-internet-connection/ (2019-11-14, "Conflicts: not discussed")
   - https://goaudits.com/blog/offline-inspections/ (page dated 2026-10-08, conflicts not covered)
   - [inference] Queues of independent events rarely conflict, so vendors have not needed a conflict model. A task where several people edit the same record offline (shift handover log, shared checklist, incident that two guards update) is where these products have no public answer.

2. **Even "offline" products need a connection for the steps that matter most.** [fact]
   - TrackTik: tours and scans work offline, but signing in, clocking in, switching sites and viewing post orders need a connection; new or edited checkpoints may not reach an offline device until "Reload Settings". (via search snippet of https://support.tracktik.com/hc/en-us/articles/1500001470221, page 403 to me)
   - Digital Guard Tour (Trinity Guard): starting and closing a patrol need a connection; QR scans are not verified offline because a server checks for copied codes. https://digitalguardtour.com/offline-guard-tour-system/
   - COREDINATE: offline mode allows NFC scans only; no tours, GPS checkpoints, beacons, tasks or events. https://coredinate.de/en/help/scan-checkpoints-offline
   - GoAudits: checklists must be downloaded before going offline; photo uploads capped at four per checklist item. https://goaudits.com/blog/offline-inspections/
   - [reported] A TrackTik rep told a reviewer guards must reconnect to finalise the patrol report. https://connecteam.com/reviews/tracktik/ (updated 2025-10-26)

3. **Documented real-world failure modes are about starting work offline and about data that exists only on the phone.** [fact]
   - SafetyCulture, Aug 2022: "unable to save responses" error; vendor's belief was that it began when users started an inspection while offline; resolved 2022-08-24 via app update. https://status.mitti.com/incidents/lv30pn0kphs7
   - SafetyCulture, May 2023: some inspections did not sync; users were told not to uninstall, delete or update the app until fixed (the only copy was on the device); Android fix v23.16. https://status.mitti.com/incidents/vx258lbj8bcj
   - COREDINATE silently discards offline scans made under an unregistered badge ("cannot be assigned to a user"). https://coredinate.de/en/help/scan-checkpoints-offline
   - [inference] These are the failure modes a judge will probe: start offline, kill the app, reconnect, check nothing vanished.

4. **Duplicates are a known, still-live problem in the best-known open offline collection tools.** [fact]/[reported]
   - KoboToolbox, July 2023: an Enketo bug (GitHub issue #567) makes the same offline draft opened in several tabs produce submissions sharing a `_uuid`; staff said removing existing duplicates is "not a trivial process" and Kobo planned to reject duplicate UUIDs. A user reported it again in Nov 2023; no confirmed fix in the thread. https://community.kobotoolbox.org/t/submission-duplication-after-version-2-023-2/43149
   - ODK Aggregate (2012 thread): without an instanceID in the form, the server "cannot de-duplicate your data". https://groups.google.com/g/kobo-users/c/wrWgb1E-kOw
   - ODK Central today rejects a repeated create with the same instanceId (HTTP 409). https://docs.getodk.org/central-api-submission-management/
   - [inference] The fix pattern is a client-generated unique ID per record plus a server that treats a repeat as "already have it" instead of an error or a second copy.

5. **ODK Central is the best-documented conflict model and the one to learn from.** [fact] https://docs.getodk.org/central-entities/
   - Every update creates a new version; Central applies updates in the order received ("last update received wins"), but labels versions "Parallel update" (different fields touched) or "Conflict" (same fields touched from an older base) so a human can review and "Mark as Resolved".
   - Offline updates that depend on an earlier submission not yet received are held up to 5 days, then force-processed and flagged as conflicted.
   - Offline entity lists in Collect need Central and Collect v2024.3.0 or later.
   - [inference] Even here, the default merge is last-write-wins with a warning, not a deterministic merge, and the review is manual after the fact.

6. **Everything I could document is cloud-hosted; nothing advertises on-premise, air-gapped or classified deployment.** [fact]/[reported]
   - SafetyCulture: data "stored in-region" on AWS; on-premise not mentioned. https://mitti.com/security/
   - TrackTik/Trackforce product page: no hosting, residency or on-premise statements; lists SOC2/GDPR/ISO badges. https://www.trackforce.com/products/tracktik/security-guard-tour-app/
   - DrPatrol (Singapore vendor): SourceForge lists "cloud" as its only platform; no offline info; 0 reviews. https://www.sourceforge.net/software/product/DrPatrol/
   - Omnigo guard tour: no offline, hosting or certification detail on its page. https://www.omnigo.com/product-suites/guard-tour
   - Wikipedia: guard-tour software is "increasingly delivered as cloud-based software as a service". https://en.wikipedia.org/wiki/Guard_tour_patrol_system
   - Certis: I found no public guard-tour or patrol app with offline mode ("not found"). https://www.certisgroup.com/?p=12997 only describes guarding services and AI/tech in general.
   - A guard-tour search for "air-gapped / classified" returned no vendor claiming it ("not found").

7. **No vendor I read claims tamper evidence for offline records, and none documents lost-device protection for the offline queue.** [fact: absence on the pages read]
   - TrackTik product page: no tamper-proof or audit-trail claim. https://www.trackforce.com/products/tracktik/security-guard-tour-app/
   - Digital Guard Tour: tamper evidence and storage limits "not addressed"; no mention of device-clock risk. https://digitalguardtour.com/offline-guard-tour-system/
   - GoAudits: capture-time timestamp kept, but whether on-device data is encrypted is unconfirmed. https://goaudits.com/blog/offline-inspections/
   - SafetyCulture security page: encryption at rest and in transit, but no device-level encryption or remote wipe described. https://mitti.com/security/
   - [inference] Offline timestamps come from the phone clock, so a guard (or an attacker holding a lost phone) can alter time or data unless entries are signed and chained. Tag-based checkpoints are cheap stickers/tags (Wikipedia notes they can be "printed on stickers or embedded in inexpensive tags"), so copying a checkpoint is plausible; I did not find a sourced cloning study.

8. **ATAK/TAK shows how a defence tool treats disconnection, but it is map/PLI/chat centric, not a form-and-process tool.** 
   - [reported] Ditto (a vendor selling the fix) says ATAK relies on a constant server connection and its mesh mode "doesn't support data sync without a local area network"; its plugin adds peer-to-peer sync over BLE, Wi-Fi Aware, MANET and SATCOM. https://www.ditto.com/blog/new-atak-plugin-unlocks-serverless-peer-to-peer-sync-at-the-edge (dated 2025-04-01 on page, also shows 2024-07-12)
   - [reported] A TAK Data Sync plug-in guide says items added while a user is offline are added to the feed on reconnect (via search snippet; the mirror URL returned 404 when I fetched it): https://freetakteam.github.io/FreeTAKServer-User-Docs/tools/Manuals/ATAK_Data_Sync_User_Guide_Version_1.3_ATAK_4.8.pdf
   - Official TAK.gov documentation was not reachable ("not found"). Military-specific offline form tools: "not found".

---

## 2. Comparison table

| Product | Offline support | Sync and conflict model | Known problems | Fit for a classified site |
|---|---|---|---|---|
| TrackTik Guard Tour (Trackforce) | Tours, GPS/NFC/QR scans, incident reports, photos, notes stored on device [fact] (blog 2019-11-14). Login, clock-in, site switch, post orders need connection (via snippet). | Queued items upload in background, no resubmit [fact]. Manual refresh after reconnect suggested (via snippet). Conflicts: not documented. | App Store US 2.6/5 from 141 ratings (3.16.0; undated "3d ago") [reported]. Reviews: crashes, reports not visible, unsaved report erased [reported]. Rep says final report needs reconnect [reported]. | Poor. Cloud SaaS, no on-premise or tamper-evidence claim found. |
| Silvertrac (Trackforce) | Incidents with voice, photo, video stored locally [fact]. | Auto-upload on "strong connection"; "Uploads" counters show pending; keeps time reported [fact]. Conflicts: not documented. | Capterra 4.7 (279) per search snippet [reported]. An aggregator claims "not offline-capable", which contradicts Trackforce's own article, so low confidence. | Poor. Cloud. |
| Trackforce Valiant | Not found in primary docs. | Not found. One unverified aggregator remark that it does not sync automatically. | Capterra page 403. | Not found / presumed cloud [inference]. |
| SafetyCulture / iAuditor (Mitti by SafetyCulture) | "Store inspections offline" on by default; offline window by last-modified, default 1 week; assets/schedules can be stored offline [fact] https://help.mitti.com/en-US/000034/ | Auto sync interval and Wi-Fi/cellular choice; per-change and per-media-file "sync diagnostics" [fact]. Conflicts: not documented. | 2022 offline-start save bug; 2023 inspections failed to sync [fact]. Review aggregator mentions data loss in remote areas and easy duplicate reports (low confidence) [reported]. | Poor. AWS, in-region, no on-premise. |
| GoAudits | Checklists pre-downloaded; photos, GPS, e-signatures captured offline; capture timestamp kept [fact]. | Upload on reconnect. Conflicts: not documented. Photo cap 4 per item. | On-device encryption unconfirmed. | Poor. Cloud. |
| Lumiform | Not found (search returned other vendors only). | Not found. | Not found. | Not found. |
| QR-Patrol / Easy Guard Tour | Not found in primary docs; listing says QR/NFC/beacon scans go to a cloud web app. | Not found. | Not found. | Presumed cloud [inference]. |
| Guardso, Belfry | Not found. | Not found. | Not found. | Not found. |
| Certis (Singapore) | No public patrol app or offline mode found. | Not found. | Not found. | Not found. |
| DrPatrol (Dr Integrated Systems, Singapore) | Not found; listing shows iButton/RFID/GPS/GPRS patrol monitoring. | Not found. | 0 reviews. | Cloud only per SourceForge listing. |
| COREDINATE (DE) | NFC scans only; login by card or password works offline [fact]. | Sync on connection; scans matched to employee after sync; unregistered-badge scans discarded [fact]. | Data loss by design for unregistered badges. | Poor. Cloud portal. |
| Digital Guard Tour / Trinity Guard | GPS checkpoint confirmations recorded offline; QR needs connection; patrol start/close need connection [fact, vendor]. | Auto-upload on return. Conflicts: not documented. | Storage limits and tamper evidence not addressed. | Poor. |
| ODK Collect + Central | Full forms offline; Collect keeps offline entity lists (v2024.3.0+) [fact]. | Submit on connect; duplicate instanceId gets 409; entity updates last-received-wins with Conflict / Parallel update flags; 5-day hold for out-of-order updates [fact]. | Past duplicate-instance issues (2012 Aggregate) [reported]. | Best of the set: open source, self-hostable [inference, from open-source model; hosting not verified in this research]. Not a patrol/security product. |
| KoboToolbox (Collect + Enketo) | KoboCollect and Enketo offline-capable drafts [reported]. | Server accepts or rejects by `_uuid`; duplicates from multi-tab drafts (2023) [reported]. | Duplicate `_uuid` bug; removal "not a trivial process" [reported]. | Self-host possible [inference, not verified here]; humanitarian focus. |
| ATAK / TAK Server | Local map, PLI and chat keep working; server-dependent sync [reported, vendor]. | Server-centric; data-sync plug-in re-adds offline items on reconnect (via snippet). | Mesh mode without LAN does not sync data (Ditto claim). | Designed for defence use, but TAK.gov docs not reachable here; no process/form workflow. |

---

## 3. User complaints (with links)

All quotes under 15 words. Dates are review dates where the page showed one.

TrackTik Guard Tour
- App Store (US) rating 2.6/5 from 141 ratings, version 3.16.0. [reported] https://apps.apple.com/us/app/id1102822566
- 2021-09-29: reviewer says a dispatch alert opens over a report, "erasing whatever report was being worked it". [reported] same URL.
- App Store (CA) 3.8/5 from 36 ratings. 2024-03-21: "TrackTik consistently crashes and returns to the home screen" when cropping pictures in a report. [reported] https://apps.apple.com/ca/app/tracktik/id1102822566
- 2022-10-15: "I can't see my reports after I submitted." (v3.11.4). [reported] same CA URL.
- Developer's own App Store text warns that background GPS "can significantly reduce battery life" (developer statement, not a user complaint). https://apps.apple.com/us/app/id1102822566
- Connecteam write-up (updated 2025-10-26) lists crashes, glitchy location, photos not saving, removed shifts, "buggy mobile app". Its rating figure (1.9/5 on Apple) conflicts with the App Store page above, so do not quote it. [reported] https://connecteam.com/reviews/tracktik/

GuardMetrics Guard Tour (Google Play, 3.1 stars, 237 reviews) - via search snippet; the page itself did not load for me.
- Reviewer: reports "get clogged up for weeks". Another reports crashes when taking photos and restarts on non-NFC devices. [reported] https://play.google.com/store/apps/details?id=com.guardmetrics&hl=en_US

SafetyCulture
- Zoftwarehub user reviews (via search snippet): data loss or unsaved work when connectivity is unstable; easy to create duplicate reports. Low confidence, single aggregator. [reported] https://zoftwarehub.com/products/safetyculture/reviews
- Official incidents above (2022, 2023) are the firmer evidence.

KoboToolbox / ODK
- Staff on `_uuid` uniqueness: it "sometimes is not" unique. [reported] https://community.kobotoolbox.org/t/submission-duplication-after-version-2-023-2/43149
- Workaround offered: "delete the duplicate `_uuid` manually". Same URL.
- 2012 ODK thread: ~83 duplicates in ~1,900 submissions over 2G/3G in East Africa; reply says duplicates are less serious than lost records. [reported] https://groups.google.com/g/kobo-users/c/wrWgb1E-kOw

Not found
- Reddit (r/securityguards): not retrievable. No Reddit complaint is cited.
- Capterra and G2 review text for Trackforce Valiant, Silvertrac, TrackTik: pages returned 403; only rating numbers via snippet.
- User complaints on NFC scanning specifically: only vendor guidance that NFC needs the phone within roughly 4 cm for 1-2 seconds, and some phones lack NFC (TrackTik support article via snippet, https://support.tracktik.com/hc/en-us/articles/1500001470221 area). Battery: only the developer's GPS warning above and vendor blog advice on charging routines (https://blog.guardspro.com/7-common-problems-of-guard-tour-system-and-how-to-fix-them/, 2026-02-01, vendor content).

---

## 4. The gap we could own

What no reviewed product shows publicly, and what the DSO challenge text asks for ("syncs their changes correctly"):

1. **Explicit, visible conflict handling for shared records**, not last-write-wins in silence. Show the user a per-record state: saved on device, queued, delivered, acknowledged by server, or conflicted with a clear resolution screen. ODK Central flags conflicts after the fact; the guard-tour apps do not document them at all. [inference]
2. **No duplicates and no silent loss by design**: a client-generated unique ID per action, idempotent server (a repeat is "already have it", not 409 and not a second copy), and a reconcile screen that proves counts match ("N created on device, N received"). Directly answers the Kobo/ODK duplicate history and the COREDINATE discard behaviour. [inference]
3. **Tamper-evident offline log**: each entry signed or hash-chained to the previous one on the device, with the device clock backed by a monotonic counter, so edits, deletions or backdating are detectable at sync. No vendor read claims this. [inference]
4. **Lost-device safe**: local store encrypted with a PIN-derived key, auto-wipe after failed attempts, server-side revocation of a device so its queued entries are quarantined for review instead of trusted. No vendor documents this for the offline queue. [inference]
5. **Self-hostable, no-cloud sync server** that runs on a laptop on the site LAN (and ideally device-to-device sync over local Wi-Fi when there is no internet), matching the "on-premise / air-gapped" need that cloud-only products cannot meet. [inference] This also differentiates from ATAK, which is server-centric and map-focused. [reported]
6. **Start-to-finish offline**: login with cached credentials, start and close a patrol, finalise reports offline - the exact steps TrackTik, Digital Guard Tour and COREDINATE push online. [fact for the vendor limits, inference for the opportunity]

Demo idea that makes the gap concrete: two phones, same checklist, both offline, both edit the same item, one phone "lost" mid-sync; show reconnect with a conflict screen, zero duplicates, a tamper check that catches an edited entry, and a revoked device being quarantined. [inference]

---

## 5. Open questions and not found

Not found
- Certis guard-tour or operations-platform offline behaviour: no public documentation found.
- Guardso, Belfry, QR-Patrol (vendor docs), Lumiform: no offline or sync documentation retrieved.
- Trackforce Valiant offline and sync documentation; Capterra/G2 review text (403).
- Reddit threads on guard-tour apps; independent NFC-scan-failure reports.
- TAK.gov official documentation on offline and store-and-forward behaviour; any military offline form tool.
- Whether KoboToolbox actually shipped rejection of duplicate UUIDs (thread does not say).
- Singapore rules: whether Secret-level or DSO data may sit on commercial cloud at all. Search surfaced only second-hand statements (Alibaba page: GCC up to Restricted, https://www.alibabacloud.com/trust-center/singapore ; GovTech ApplySG FAQ: up to Confidential "cloud-eligible", https://www.developer.tech.gov.sg/products/categories/platform/applysg/faqs [reported]). Do not claim a specific IM8 rule in the pitch.
- Whether ODK Central or Kobo can be run fully on-premise/air-gapped for a defence user: believed from their open-source nature but not verified in this research.

Open questions for the team or the DSO judge
- Which processes count as "security processes": patrol rounds, access logs, incident reports, equipment checks? Single-writer events (patrol scans) vs shared records (handover logs) change how much conflict logic matters.
- Is the deployment assumption internet-connected cloud with outages, or an isolated network with a local server? This decides whether the self-hosted sync story is core or optional.
- What "correctly" means to the judge: no data loss, no duplicates, auditable order, or human-resolved conflicts. Ask early at the sponsor table.

Search/fetch notes
- Search summaries returned by the tool are model-generated; I verified key claims by fetching the underlying page where it loaded (TrackTik App Store pages, SafetyCulture/Mitti help, status, security pages, Trackforce blog, GoAudits, COREDINATE, ODK Central docs, Kobo forum, Ditto, GuardsPro, Wikipedia, Digital Guard Tour, DrPatrol, Omnigo). Items marked "via search snippet" were not verified on the page.
