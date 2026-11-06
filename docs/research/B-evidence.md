# Research B: Evidence for why people are offline, and what goes wrong

Researched 2026-11-06 for the NUS Hack 26 team (DSO challenge: offline security processes that sync correctly).

Tags: **[fact]** = primary or authoritative source I opened. **[reported]** = news or secondary source. **[inference]** = my own reasoning. Items marked "(search summary only)" were seen in a search-result summary but the page itself would not load (403/404), so verify before quoting on stage.

Budget note: I used more than the planned 12-15 searches (about 27, plus fetches) because several primary Singapore sources (AGO, MHA, NATO STO, Straits Times, CNA) blocked or returned 404 to the fetch tool.

---

## 1. Key findings (ranked by importance to the challenge)

### 1. US defence documents describe the challenge almost exactly [fact]
- The US Army's "Army Intelligence Data Platform (AIDP) NEXT" Industry Day deck (12 Feb 2026) lists "Denied, degraded, intermittent, and limited (DDIL) connectivity" as the first operational constraint. Its notional architecture says: "Upon Disconnect, Edge continues operations using last known Global CIP + continued local updates" and "Upon Reconnect, Edge and Cloud resynchronize the global and local CIP based on missed reporting". Its stated problem space includes "Peer to Peer, and Mesh Synchronization Technologies" and "Able to address Data Primacy challenges".
- The Office of Naval Research call N0001424SBC10 (13 Aug 2024) asks for "Data queue management for disconnected, denied, intermittent and/or limited bandwidth (DDIL) environments - Policies to decide what data to push over the air across D-DIL links".
- The acronym is not standardised. The Army expands it as "Denied, degraded, intermittent, and limited". The Navy expands it as "disconnected, denied, intermittent and/or limited bandwidth". [inference] A pitch should define DDIL in one line and say "disconnected, intermittent, limited-bandwidth" rather than claim one official wording.
- I did not find an official NATO or DoD-wide DDIL definition. The NATO STO paper I tried (MP-IST-208-12) returned 403.

### 2. Singapore has had three large, documented connectivity failures in 2 years, including one that hit 995 and 999 [fact + reported]
- 8 Oct 2024, Singtel fixed-line voice: the Ministry of Digital Development and Information (MDDI) said 995 and 999 were hit, services came back over about 4 hours, and during the disruption "about half of calls could still connect". The failover to the second system "did not happen seamlessly". IMDA later fined Singtel S$1m (reported 11 Dec 2025): about 500,000 users, over four hours, calls to government agencies, healthcare organisations, banks and emergency hotlines affected. IMDA said "The potential impact on the safety and security of the public could have been very serious."
- 19 Jul 2024, CrowdStrike: Changi Airport check-ins went manual for more than 10 airlines, with more than 40 flights delayed (The Star, 20 Jul 2024). AirAsia staff issued boarding passes by hand until 2pm on 20 Jul. SingPost's tracking, bill payment and kiosks were disrupted until the evening of 19 Jul. Minister Josephine Teo said Singapore's impact was less severe because "essential and government services were mostly unaffected".
- 16, 18 and 19 Mar 2026, Singtel mobile: 720,000 customers, a three-day outage that "disrupted online payments, ride-hailing, and food delivery". IMDA fined Singtel S$1m (reported 20 Oct 2026). The article does not mention frontline or security work.
- [inference] Not one of these outages was reported as hitting security guards or SAF directly. They support "networks fail, even in Singapore, and frontline work has to carry on", but not "security staff lost data in an outage". I found no Singapore case of that.

### 3. Paper records in guardhouses can be falsified, and CCTV, not the record, caught it [fact + reported]
- Police statement, 8 Apr 2024: a night-shift security officer in Ang Mo Kio was away from the guardhouse for more than five hours on 4-5 Jan 2024 (per CCTV). He "admitted to making false entries in the guardhouse occurrence book to cover his absence". Charges included falsification of accounts under s 477A of the Penal Code (up to 10 years' jail, fine, or both) and absence from duty under the Private Security Industry (Conduct) Regulations.
- He had also been called at about 4.30am to dispose of a fake IED left by a red-teaming exercise and did not. He pleaded guilty on 12 Dec 2024 to false entries and absence, and was fined S$1,800 (reported). Over 100 people were evacuated or held back for about 90 minutes (reported).
- This is one case with a modest penalty. I did not find a Singapore case of a patrol record falsified at a defence site, or any jail sentence for falsified patrol or attendance records. See section 4.

### 4. Singapore has formal rules that put devices out of reach in secure areas, so "offline by design" is real [fact]
- MINDEF's security zoning (fact sheet, 23 Aug 2013): Red Zones are where "classified information and equipment are processed and managed". Camera-enabled phones must be deposited in lockers before entry.
- MINDEF's reply to a parliamentary question (7 Jul 2026, Minister Chan Chun Sing): MINDEF/SAF has a "tiered security management plan" with "clearly demarcated zones", also applied "during sensitive operations and trainings". Personal devices and apps are allowed in "security-cleared zones".
- (search summary only) The Online Citizen, 27 Apr 2026, quotes a MINDEF spokesperson on mandatory safekeeping of devices at designated storage areas before sensitive or classified operations or training. Page returned 403.
- [inference] For DSO and SAF, offline is often a policy choice, not a signal problem. The app must work on a managed or dedicated device with no network at all, and must sync only when the device moves to a cleared network or zone. This is an inference from the rules above. I found no public MINDEF statement about security rooms specifically needing offline apps.

### 5. Digital capture is not automatically more accurate, and Singapore's security industry has already gone digital [reported]
- Shelby-James et al., Trials 2007: handheld entry produced 67.5 errors per 1,000 fields, against an accepted paper benchmark of 10 per 10,000. Walther et al., PLoS ONE 2011: tablet and netbook accuracy was not significantly different from paper (5.1% and 5.2% versus 3.6% error), while PDA entry (7.9%) was worse. Both are clinical-trial data capture, not security work. (search summary only)
- MHA's Security Digital Roadmap lists "Mobile-enabled Patrol & Incident Management" (route tracking and documentation). A 2021 MHA speech reportedly said 97% of security agencies use at least one Stage 1 or 2 Industry Digital Plan solution (search summary only; both pages returned 404 or were unreachable).
- [inference] "Digitise the logbook" is not a differentiator in Singapore. The differentiators are working with no network at all, merging edits from several devices correctly, and making records hard to alter afterwards.

### 6. Processes fail when staffing or conditions shift and rules are only verbal [fact]
- PDPC, Eagle Eye Security [2017] SGPDPC 11 (29 Jun 2017): a condo visitor logbook (with NRIC numbers) sat open and unattended from 8pm to 10.30pm because the night guard was ill and the replacement was late. The PDPC found the guards' instructions were "empty instructions with little effect", not documented, and there was no process to return the logbook to the guardhouse by default when no guard was at the table. Same pattern in Spear Security Force [2016] SGPDPC 12 (search summary only).
- [inference] Useful for the claim that paper-based security records depend on individual memory, and on staffing, rather than on enforced steps. It is a privacy-law decision, not an offline-connectivity case.

### 7. SAF safety incidents show the cost of slow or non-compliant frontline processes, but the sources I read do not show an offline or reporting-system cause [reported]
- CFC Dave Lee (18 Apr 2018, Bedok Camp, not an offline site). COI preliminary findings (Parliament, 6 Aug 2018, via Mothership): a "significant gap" between symptoms and arrival at the medical centre against a protocol of cooling "no later than 30 minutes"; training deviations the CO did not approve; rest cut from 7 hours to 6 hours 15 minutes by an informal punishment. State Coroner, 27 Jan 2021 (via Yahoo): "inordinate delay"; evacuation took 35 minutes to 1 hour; an expert said evacuation within 10 to 15 minutes would have cut mortality from 33% to 10%. Six servicemen were fined S$1,800 to S$4,500 in military court.
- Neither source I read mentions communication, reporting-system or record-keeping failures. Do not claim this case was about records or connectivity.
- Aloysius Pang (Jan 2019, New Zealand training area): the COI (Parliament, 6 May 2019) found lapses by all three servicemen in the gun and that safety rules were not followed. Not a records or connectivity finding either (reported).

### 8. Weak or absent public evidence (see section 4)
- No public source found for mobile or data coverage inside the Underground Ammunition Facility, Jurong Rock Caverns, DTSS, Pulau Tekong or live-firing areas, naval bases or MRT tunnels.
- No AGO finding found on physical access control, key control or guard logbooks. The AGO FY2013/14 report did find an NParks officer backdating and falsifying supplier letters "to regularise gaps... for completeness of record" (Yahoo/AGO via news, 17 Jul 2014).
- MINDEF's I-net breach (28 Feb 2017, about 850 servicemen and staff, NRIC numbers, phone numbers and dates of birth) is confirmed. It is a cyber breach on an internet-facing system that is physically separate from internal systems, not a paper or offline-process failure, so it is weak evidence for this challenge. A "2019 vendor breach" was not found.

---

## 2. Evidence table

| # | Claim | Tag | Source URL | Date |
|---|---|---|---|---|
| 1 | US Army AIDP NEXT lists DDIL connectivity as an operational constraint; edge "continues operations using last known Global CIP + continued local updates" on disconnect and "resynchronize[s]... based on missed reporting" on reconnect; problem space includes peer-to-peer and mesh synchronisation and "Data Primacy" | fact | https://cpeisw.army.mil/wp-content/uploads/2026/03/AIDP-Next_-Industry-Day-Slides_for-Public-Release-1.pdf | 12 Feb 2026 |
| 2 | ONR call defines DDIL as "disconnected, denied, intermittent and/or limited bandwidth" and asks for policies on what data to push across D-DIL links | fact | https://www.onr.navy.mil/assets/2024-08/N0001424SBC10.pdf | 13 Aug 2024 |
| 3 | CDAO request for information concerns data synchronisation across cloud and on-prem in connected and denied, disconnected, intermittent and limited environments (search summary only; page 403) | reported | https://www.executivebiz.com/articles/ddil-dod-cyber-cloud-cjadc2-low-bandwidth | May 2025 |
| 4 | DDIL expansion varies by source (denied/disconnected/degraded/disrupted; limited/low-bandwidth) | inference | Items 1, 2 above | n/a |
| 5 | Singtel fixed-line outage 8 Oct 2024 hit 995 and 999; restored over about 4 hours; about half of calls could connect; failover not seamless; no evidence of cyber attack | fact | https://www.mddi.gov.sg/newsroom/pqs-related-to-the-8-october-singtel-disruption/ | 11 Nov 2024 |
| 6 | Same outage: S$1m fine, about 500,000 users, over 4 hours, hit government agencies, healthcare, banks and 995/999; IMDA said impact on public safety and security "could have been very serious" | reported | https://www.thestar.com.my/aseanplus/aseanplus-news/2025/12/11/singtel-fined-s1mil-for-fixed-line-voice-outage-that-disrupted-emergency-call-services-in-oct-2024 | 11 Dec 2025 |
| 7 | Singtel mobile outage 16, 18, 19 Mar 2026: 720,000 customers, three days, S$1m fine, cause included a manual water valve not reopened after maintenance | reported | https://www.thestar.com.my/aseanplus/aseanplus-news/2026/10/20/singapore039s-singtel-fined-s1mil-for-three-day-outage-in-march-caused-by-chiller-system-failure-software-bugs | 20 Oct 2026 |
| 8 | CrowdStrike 19 Jul 2024: more than 10 airlines and more than 40 flights at Changi affected, manual check-in; SingPost systems disrupted; minister said essential and government services mostly unaffected | reported | https://thestar.com.my/aseanplus/aseanplus-news/2024/07/20/most-singapore-services-including-changi-airport-check-ins-back-to-normal-after-global-outage | 20 Jul 2024 |
| 9 | Early Changi report on the same day described manual check-in at some airlines and a few flights affected (count rose later) | reported | https://mothership.sg/2024/07/changi-airport-global-it-outage | 19 Jul 2024 |
| 10 | Security officer absent more than 5 hours per CCTV, admitted false entries in guardhouse occurrence book; charges incl. s 477A Penal Code falsification of accounts | fact | https://www.police.gov.sg/Media-Hub/News/2024/20240408_security_officer_to_be_charged_for_his_involvement_in_a_case_of_alleged_bomb_threat | 8 Apr 2024 |
| 11 | Same officer pleaded guilty 12 Dec 2024 (false entries, absence), fined S$1,800 on 17 Dec 2024 | reported | https://www.thestar.com.my/aseanplus/aseanplus-news/2024/12/12/ex-security-guard-pleads-guilty-after-failing-to-remove-fake-bomb-from-exercise-sparking-scare and https://www.stomp.sg/singapore-seen/ex-security-guard-who-forgot-to-remove-fake-bomb-also-made-false-entries-in-duty | Dec 2024 |
| 12 | PDPC found security firm Eagle Eye in breach: visitor logbook left open and unattended 8pm-10.30pm; instructions undocumented and ineffective; no contingency when a guard fell ill | fact | https://www.pdpc.gov.sg/-/media/files/pdpc/pdf-files/commissions-decisions/grounds-of-decision---eagle-eye---290617.pdf | 29 Jun 2017 |
| 13 | Earlier PDPC decision on same condo, Spear Security Force: logbook left open on table near the guard post (search summary only) | reported | https://www.pdpc.gov.sg/-/media/files/pdpc/pdf-files/commissions-decisions/grounds-of-decision---spear-security-force-(250716).pdf | 2016 |
| 14 | MINDEF Red Zones: classified information and equipment handled there; camera phones deposited in lockers before entry | fact | https://www.mindef.gov.sg/news-and-events/latest-releases/2013aug23-news-releases-02529/ | 23 Aug 2013 |
| 15 | MINDEF "tiered security management plan", "clearly demarcated zones", applied during sensitive operations and trainings | fact | https://www.mindef.gov.sg/news-and-events/latest-releases/7jul26-pq2/ | 7 Jul 2026 |
| 16 | MINDEF spokesperson: mandatory safekeeping of personal devices at designated storage before sensitive or classified operations (search summary only; page 403) | reported | https://theonlinecitizen.com/2026/04/27/singapore-monitors-fitness-apps-over-risks-of-exposing-military-routines-not-locations | 27 Apr 2026 |
| 17 | Underground Ammunition Facility: "world's first large-scale underground containerised facility", about 90% less land sterilised (cited to a MINDEF article of 7 Mar 2008; no connectivity info on the page) | reported | https://en.wikipedia.org/wiki/Underground_Ammunition_Facility | page edited 23 Apr 2024 |
| 18 | Jurong Rock Caverns: about 150 m below ground, up to 1.47 million m3 of liquid hydrocarbons; no information on communications | fact | https://jtc.gov.sg/find-space/jurong-rock-caverns | accessed 6 Nov 2026 |
| 19 | MINDEF I-net breach: about 850 servicemen and staff, NRIC, phone, date of birth; I-net physically separate from internal systems | fact | https://www.mindef.gov.sg/news-and-events/latest-releases/28feb17_nr/ | 28 Feb 2017 |
| 20 | Dave Lee COI preliminary findings: significant gap before medical centre vs 30-minute cooling protocol; unapproved training deviations; rest cut to 6h15 | reported | https://mothership.sg/2018/08/dave-lee-han-xuan-saf-coi | 6 Aug 2018 |
| 21 | State Coroner: "inordinate delay"; evacuation took 35 min to 1 hour; expert: 10-15 min would cut mortality from 33% to 10%; six servicemen fined S$1,800-S$4,500 | reported | https://malaysia.news.yahoo.com/delayed-evacuation-cfc-dave-lee-death-state-coroner-125942926.html | 27 Jan 2021 |
| 22 | Aloysius Pang COI: death due to lapses by all three servicemen in the gun; safety rules not followed (NZ training area) | reported | https://mothership.sg/2019/05/ng-eng-hen-aloysius-pang-coi-statement/ | 6 May 2019 |
| 23 | AGO FY2013/14: NParks officer backdated 16 letters and falsified 5, "to regularise gaps... for completeness of record"; SCDF cash cards: 62 of 78 transactions on unofficial trips | reported | https://sg.news.yahoo.com/ago-audit-reveals--serious-lapses--in-several-government-agencies-113027444.html | 17 Jul 2014 |
| 24 | AGO FY2023/24 has an MHA section titled "Weaknesses in Inventory and Records Management" (heading only; details not read) | reported | https://www.ago.gov.sg/files/ARs/ar_fy2023_24.pdf (fetch returned 404) | FY2023/24 |
| 25 | Handheld computer data entry: 67.5 errors per 1,000 fields vs paper benchmark 10 per 10,000 (clinical trial data; search summary only) | reported | https://pmc.ncbi.nlm.nih.gov/articles/PMC1804282 | 2007 |
| 26 | Electronic data capture on tablet/netbook not significantly different from paper (5.1%, 5.2% vs 3.6%); PDA worse at 7.9% (clinical data; search summary only) | reported | https://doaj.org/article/3fb54f1c33554004b286f147a1977e84 | Jan 2011 |
| 27 | MHA Security Digital Roadmap lists mobile-enabled patrol and incident management (route tracking and documentation); 97% of agencies use at least one Stage 1 or 2 digital solution per a 2021 MHA speech (search summary only; pages unreachable) | reported | https://www.mha.gov.sg/docs/default-source/default-document-library/security-digital-roadmap.pdf and https://www.mha.gov.sg/media-room/newsroom/security-industry-conference-2021-webinar/ | 2021 |
| 28 | Vendor blog claims 30-50% of paper-logged patrols "may never have taken place" citing unnamed industry data. No primary source. Do not use. | reported (vendor, unverified) | https://www.timemark.com/blog/what-is-proof-of-presence-in-security-patrols | undated |

---

## 3. The 3 strongest evidence-backed problem statements

### A. Offline-by-policy: checklists and logs for secure or sensitive areas where devices cannot be networked
"Safety and security staff working in restricted zones and sensitive operations cannot rely on a network, and by policy often cannot carry connected personal devices. They still must complete inspections, access checks and key or equipment logs, and those records must reach the central system intact, in the right order, once a cleared network is available."
- Evidence: Red Zone rules (item 14) and tiered plan (items 15, 16); US Army edge pattern (item 1); Navy DDIL queue management (item 2).
- Fit with DSO: strongest. It is the DDIL framing in a defence setting.
- Weak spot: no public data on connectivity inside UAF, caverns or Tekong. The link from "phones go in lockers" to "staff need an offline app" is [inference].

### B. Outage-proof frontline checklists: keep working through a network or system outage, then reconcile
"When the network or a cloud service fails, as with Singtel in Oct 2024 and Mar 2026 or CrowdStrike in Jul 2024, frontline staff must keep recording incidents and checks locally, and those records must merge correctly with what colleagues on other devices recorded, with no loss, duplicates or silent overwrites."
- Evidence: items 5 to 9. 995/999 degraded for about 4 hours with half of calls connecting; Changi fell back to manual check-in; IMDA's "could have been very serious".
- Fit with DSO: good. Singapore-specific, recent, regulator-confirmed.
- Weak spot: outages last hours, and the reports I read do not show security guards losing data. The merge-correctness part is [inference] from the DDIL pattern in item 1.

### C. Records you can trust: proving a patrol or inspection happened, even when entered offline
"Paper occurrence books can be filled in to cover an absence. A security officer's guardhouse book showed duty cover while CCTV showed him gone for more than five hours. An offline-first app should capture time, place and sequence on the device when each step happens, make later edits visible, and keep the original entry when synced."
- Evidence: items 10 and 11 (falsified guardhouse book, s 477A charge); item 23 (AGO: records backdated "to regularise gaps"); item 12 (PDPC: unenforced paper-logbook procedures).
- Fit with DSO: moderate to good (audit trail and integrity are core defence concerns).
- Weak spot: the Singapore case is a single, lightly penalised incident. Research shows digital capture is not automatically more accurate (items 25, 26). A guard-tour scan proves a device was read, not that a check was done. Keep the claim to "tamper-evident, timestamped, attributable", not "stops cheating".

---

## 4. Open questions and not found

Not found (searched, nothing usable):
- Any public statement on mobile or data coverage, radio, or phone rules inside the Underground Ammunition Facility, Jurong Rock Caverns, DTSS tunnels, MRT deep tunnels, naval bases, live-firing areas or Pulau Tekong. Public pages only give the physical setting (items 17, 18).
- Any AGO finding on physical access control, key control or guard logbooks. The AGO FY2023/24 MHA heading "Weaknesses in Inventory and Records Management" appeared in search results, but the report PDFs on ago.gov.sg returned 404 to the fetch tool, so its content is unread.
- Any Singapore court case of a security officer jailed specifically for falsifying patrol or attendance records. The one case found ended in a S$1,800 fine.
- Any PDPC decision about a lost (as opposed to unattended) paper logbook at a security firm. Found: unattended logbook decisions (Eagle Eye 2017, Spear 2016).
- Any 2019 MINDEF vendor data breach. Only the 2017 I-net breach was confirmed.
- Any official NATO or NCIA definition of DDIL/DIL. The NATO STO paper fetch returned 403. Terminology varies even between US Army and Navy documents.
- Any peer-reviewed study showing that paper patrol logs are falsified at a given rate. The only figure found (30-50%) is an unsourced vendor claim.
- Any SAF or COI finding that cites record-keeping or reporting-system failure. The two COIs I checked (Dave Lee, Aloysius Pang) cite casualty management and rule compliance, not records.

Open questions worth asking a DSO mentor or the organisers:
1. Is "security process" in the brief closer to physical security (patrols, access, keys), safety (inspections, permits, incident reporting), or cyber and compliance checklists?
2. Are target devices managed phones/tablets, rugged handhelds, or laptops? Can they hold local encrypted storage?
3. When sync happens, is it to a cleared network or via a physical hand-off? Is there a single authoritative server, or can devices sync peer-to-peer?
4. What counts as "correct" sync for the judges: no lost entries, no duplicates, preserved order, or conflict visibility?
5. Do the judges care about tamper evidence and audit trail, or mainly about usability offline?

How to use the claims safely on stage:
- Safe to state: items 1, 2, 5, 10, 12, 14, 15, 19 (primary or authoritative).
- State with attribution as reported: items 6, 7, 8, 11, 20, 21, 22, 23.
- Do not state without checking the page yourself: items 3, 13, 16, 24, 25, 26, 27, 28.
