# Research C: security and safety workflows, and the rules around them

Prepared 2026-11-06 for the nushhack26 idea-selection session. Track: the processes themselves, Singapore regulation, industry context, and where offline sync matters most.

Tags: [fact] primary or authoritative source; [reported] news, secondary, third-party mirror or vendor page; [inference] my own reasoning or a stated analogue. Where a claim came only from a search-engine summary and not from a page I fetched, I say "search summary only".

Caveats up front:
- sso.agc.gov.sg (Singapore Statutes Online) returned HTTP 403 to my fetcher. Statute text below comes from mirrors (littdb mirror of SSO, Wikisource, legal-wires). Treat section and regulation numbers as likely right but verify on SSO before quoting to a judge.
- Almost nothing public describes how DSO or MINDEF sites actually run these processes. Everything about DSO is [inference] from public analogues.
- Step-by-step process descriptions are mostly [inference] (domain reasoning plus the few sourced analogues), because Singapore agencies do not publish site-level procedures.

---

## 1. Key findings (ranked by importance to the challenge)

1. **Processes that move a single "thing" between people (keys, controlled items, permits, locks) are where offline sync can actually go wrong; log-style processes (patrols, incident reports) merge trivially.** Patrol scans and incident entries are append-only events, so a sync is just a union. Custody, permits and lock-out/tag-out are state machines with invariants (one holder; no close while someone is still inside). A last-write-wins sync silently breaks these. [inference] This is the core design insight for choosing a process.
2. **The confined-space permit regulations contain exactly the conflict scenarios in the brief.** The authorised manager issues the permit and keeps a copy (reg 13); the permit must state gas-test results and validity (reg 9(c)); it expires and needs a fresh application if work overruns (reg 13(4)); the responsible person must tell the authorised manager when the worker has left (reg 15(2)); revoking on completion is "may", not "must" (reg 18(3)(a)). The rules do not make "close only when nobody is inside" an explicit system rule, which is a gap software can enforce. [reported: SSO mirror] https://littdb.sfo2.cdn.digitaloceanspaces.com/litt/SG/SSOStatutes/subsidiary_legislation/WSHA2006-S462-2009.html (undated mirror, fetched 2026-11-06). The gap reading is [inference].
3. **Record retention and reporting deadlines I could source are short and clear:** WSH iReport within 10 days, notify MOM "as soon as reasonably practicable", keep the record 3 years [fact] https://www.mom.gov.sg/workplace-safety-and-health/work-accident-reporting/report-a-work-related-accident (fetched 2026-11-06); confined-space atmosphere-test records at least 2 years (reg 16(3)) [reported: mirror]. For PSIA occurrence books, IPA, and Fire Safety Act records I found no retention period (see section 7).
4. **Attendance and occurrence books are the thing PLRD actually checks, and attendance is the most common breach.** In a PLRD outreach deck, 28 of 36 licence-condition breaches detected 1 Sep to 31 Dec 2016 were attendance-book related, 2 occurrence-book related. "Failure to maintain occurrence or attendance book" scores 3 points as a minor infringement. [reported: PLRD deck on third-party host] https://api.sambuz.com/doc/security-agencies-ppt-presentation-1047585 (deck dated 24 Jan 2017). So digitising the occurrence/attendance book is directly compliance-relevant. [inference] on relevance.
5. **MHA is pushing technology, but agencies are stuck on adoption, not intent.** Over 80% of security agencies want more advanced technology but many struggle to select and deploy it; a Security Technologist training track is launching; all Government Procuring Entities have at least one outcome-based contract since mid-2025; one OBC case notes "less manual incident reporting". [fact] https://www.mha.gov.sg/media-room/newsroom/security-industry-conference-2025/ (speech 12 Nov 2025). A practical, offline-capable tool fits the stated gap.
6. **Manpower is genuinely scarce and wages are rising by statute, so work that removes paperwork per officer has a real buyer.** PWM schedule runs to 2028; in-house Security Officer floor $2,315 (2025) rising $160 a year to $2,795 in 2028 [fact] https://www.mom.gov.sg/newsroom/press-releases/2025/3010-security-tripartite-cluster-recommendations-for-in-house-security-officers (30 Oct 2025). Government says "persistent manpower constraints" [fact] (MHA 12 Nov 2025 link above). Foreign auxiliary police recruitment was opened because of a shrinking local workforce [reported] https://mothership.sg/2024/01/singapore-recruit-auxiliary-police/amp/ (Jan 2024, search summary only).
7. **Existing commercial guard-tour apps already do QR/NFC patrols and digital logbooks, some with offline mode, so a patrol-only app will look like a clone.** Vendors in Singapore: DrPatrol, EasyGuarding; a GetApp listing for "Guard Patrol" claims offline capability. [reported: vendor pages, search summary only] https://www.getapp.sg/software/2096587/guard-patrol . Differentiation has to be correct multi-party sync of contested state, not scanning tags. [inference]
8. **For a DSO judge, controlled-item custody and end-of-day sweeps are the most natural "security process" framing.** The closest public analogue is the US classified-material regime: SF 702 records who opened, closed or checked a container and when; SF 701 is an end-of-day checklist confirming classified material is secured and giving accountability if irregularities are found. [fact about the US regime, inference that DSO has similar] https://www.archives.gov/isoo/security-forms and 32 CFR 2001.80 (2021 ed.). Singapore's own rules for classified material: not found.

**Best-fit pick:** two-party custody handover of keys and controlled items, closed out by an end-of-day sweep sign-off (reasoning in section 5). Runner-up: confined-space permit-to-work with lock-out/tag-out.

---

## 2. Process table

All cells in this table are [inference] unless a source is named. "Offline likelihood" = chance the person doing the task has no usable data link at the moment of the task. "Bad sync" = what happens if two devices diverge and the merge is wrong.

| Process | Who | How often | Data recorded | Likelihood of being offline | Consequence of a bad sync |
|---|---|---|---|---|---|
| Patrols and guard tours | Security officers on rounds; supervisor reviews | Fixed rounds per post instruction, typically hourly to 2-hourly, more at night | Checkpoint ID, time, officer, observations, photos, missed or late checkpoints | High: basements, plant rooms, roofs, tunnels, shielded rooms | Low to medium. Append-only, so merge is a union. Risk is duplicate or back-dated scans and false "missed checkpoint" alarms |
| Incident reporting | Officer on scene, supervisor, site manager, agency; police or MOM when required | Event-driven | Time, place, people, description, actions, photos, notifications made, follow-up | Medium: depends where the incident is | Medium. Duplicate reports of one incident, conflicting timelines, edited-after-the-fact entries. Deadlines (10 days iReport) can be missed |
| Access and visitor control | Gate/lobby officers, pass office, hosts | Continuous | Name, ID, host, purpose, pass number, time in/out, vehicle, items | Low to medium: gates are usually wired, but handheld checks at remote entrances may be offline | Medium to high for muster: a visitor not signed out makes the headcount wrong. Duplicate or blacklisted-visitor checks missed |
| Custody of keys and controlled items (incl. classified) | Custodian or duty officer, key holders, security/registry officer | Every issue and return, plus shift handover count and periodic inventory | Item ID, holder, issuer, authorisation, time out/in, container open/close/check | Medium: cabinets and vaults in shielded or low-signal rooms, remote posts | High. Key handed over twice, item shown held by two people, or "returned" while still out. For classified material this triggers a security investigation |
| End-of-day security sweep | Designated sweeper, last-out staff, duty security officer | Daily at close; also after-hours checks | Room, time, checker, per-item pass/fail (doors, safes, screens, equipment), discrepancies | Medium to high: inside vaults, labs, basements | Medium to high. Two people claim the same room, a failed item gets overwritten by a pass, a room is marked clear while a discrepancy is open |
| Safety inspections (fire, lab/chemical, explosives) | Fire Safety Manager, lab safety officer, licensed explosives personnel, maintenance | Per schedule (annual report for FSM; others not found) | Item, location, status, defect, photo, corrective action, due date, who closed it | Medium to high: plant rooms, lab corridors, magazines | Medium. Defect closed by the wrong person, duplicate defects, overwritten findings. Explosives storage consequences severe but process specifics not found |
| Permit-to-work and lock-out/tag-out | Requester, authorised manager (issuer), gas tester/assessor, attendant, workers | Per job; confined-space permit reviewed daily and has a stated validity | Space and purpose, gas-test results, validity period, isolations, locks and tags, entry/exit times, sign-offs | High: confined spaces, tanks, ducts, plant rooms, tunnels | Very high. Permit closed while someone is inside, lock removed by the wrong person, permit expired but work continuing, stale "all clear" gas result. Loss-of-life class |
| Emergency muster and headcount | Fire wardens, FSM, company emergency response team, security | Drills (see section 4) and real events | Names present, missing, time of count, assembly point, visitor/contractor list | Medium: outdoors usually has signal, but networks congest and building power may fail | High. Person counted twice or not at all; rescuers sent for someone already out, or not sent for someone still inside |

---

## 3. Process detail: steps, tools today, what goes wrong

Source note: unless tagged, these are [inference] from domain knowledge plus the sourced pieces named in each bullet.

### 3.1 Patrols and guard tours
- Steps: receive the route and times from the post instruction; walk it; scan or clock each checkpoint; note anomalies; report exceptions; supervisor reviews the tour print-out or dashboard.
- Tools today: paper tour sheets and occurrence book; QR or NFC tag systems; mobile guard-tour apps. Singapore vendors exist: DrPatrol (patrol monitoring, activity reporting, route planning, scheduling) and EasyGuarding (guard ID card, attendance, patrol history). [reported, vendor pages, search summary only] https://dev.to/software_website_eb3315e7/guard-patrol-monitoring-system-singapore-p9c and https://play.google.com/store/apps/details?id=com.easyguarding . A GetApp listing claims offline mode for areas with limited connectivity. [reported] https://www.getapp.sg/software/2096587/guard-patrol
- Goes wrong: missed or late checkpoints, scans back-filled, tags photographed, no signal on basement or plant-room legs. Attendance-book breaches were the most common PLRD licence-condition finding (28 of 36, Sep-Dec 2016). [reported] https://api.sambuz.com/doc/security-agencies-ppt-presentation-1047585

### 3.2 Incident reporting
- Steps: immediate response; log in occurrence book; notify supervisor and chain; written report with photos; follow-up actions; external notification where law requires (police, MOM).
- Rules found: a licensed security officer who becomes aware that someone other than a law-enforcement officer is conducting surveillance of the premises must lodge a police report "without undue delay" (Conduct Regulations reg 5); an officer who stops an unauthorised person from leaving must hand them to police "without unnecessary delay" (reg 7); officers and agencies must not disclose deployment numbers and details (reg 4). [reported: legal-wires summary of the regulations, undated] https://legal-wires.com/singapore/private-security-industry-conduct-regulations-2009-5
- MOM side: see section 4 (iReport within 10 days; keep records 3 years).
- Goes wrong: several officers file the same incident; conflicting times; edits after the fact; photo attachments lost on a dead link. The reg 4 disclosure restriction means patrol and deployment data in any app is sensitive. [inference]
- Public example of reduced manual reporting: Arts House Group's OBC model involved less manual incident reporting. [fact] https://www.mha.gov.sg/media-room/newsroom/security-industry-conference-2025/

### 3.3 Access and visitor control
- Steps: verify ID or pass or appointment; issue pass; log in and out; escort where required; collect pass.
- Tools: logbooks; MHA mentions automated visitor registration machines as part of the technology mix. [fact] https://www.mha.gov.sg/media-room/newsroom/security-industry-transformation-map-itm-2025/ (page content dated 16 Nov 2022). The page does not describe offline behaviour.
- Competency: Guard and Patrol was expanded to "Guard and Patrol with Access Control Management" (3 days). [fact] https://www.mha.gov.sg/media-room/newsroom/gov-accepts-recommendations-of-the-security-tripartite-cluster/ (2 Feb 2023)
- Goes wrong: visitors not signed out (corrupts muster list), pass not returned, duplicate entries, no check against a watch list when the link is down.

### 3.4 Custody of keys and controlled items (incl. classified)
- Steps: authorise; issue (record who, when, purpose); return; reconcile at shift handover and end of day; periodic inventory. For classified: registry number, container open/close/check log, periodic audit.
- Singapore-specific rules for classified material, and how DSO/MINDEF sites do it: not found.
- Public analogue (US): SF 702 container check sheet records names and times of people who opened, closed or checked a container; SF 701 end-of-day checklist; they are used together, and the forms are kept per agency records schedules. [fact about US] https://www.archives.gov/isoo/security-forms ; https://govinfo.gov/content/pkg/CFR-2021-title32-vol6/pdf/CFR-2021-title32-vol6-sec2001-80.pdf (2021 ed.). Applying this to DSO is [inference].
- Goes wrong: key not returned; two people believe they hold the same key; custody gap at shift change; entry signed on someone else's behalf. These are inherently conflict-prone because the item has one holder at a time. [inference]

### 3.5 End-of-day security sweeps
- Steps: walk assigned rooms with a checklist (doors, windows, safes and containers closed, classified material put away, screens locked, equipment off, in labs: gas cylinders closed, heat sources off), sign per room, escalate discrepancies.
- Tools: paper checklist; in the US analogue, SF 701 (see 3.4).
- Goes wrong: signing without checking; two sweepers overlap; a failed item overwritten by a later pass; discrepancy not escalated. [inference]

### 3.6 Safety inspections
- Fire: the Fire Safety Manager (FSM) is mandatory for public buildings with 9 or more storeys incl. basement, or occupant load 1,000 or more, or floor area 5,000 m2 or more; industrial buildings with occupant load 1,000+ or area 5,000 m2+; all hospitals; foreign-employee dormitories over the same thresholds. FSM must be appointed within 90 days of CSC or TOP; replacement within 14 days. Since 1 April 2025 FSMs submit an Annual Fire Safety Report and Premises Emergency Data through the eFSM portal. Penalty up to $10,000, up to 6 months jail, or both for failing to appoint. [fact] https://www.scdf.gov.sg/home/fire-safety/permits-and-certifications/fire-safety-manager-(fsm) (fetched 2026-11-06). The page does not state a fire safety register, inspection frequency or retention period.
- Specified complex premises: fire risk assessment at least every three years. [reported, search summary only] same SCDF page.
- Lab/chemical and explosives inspection regimes: not found (no Singapore source retrieved). Treat as [inference]: checklist rounds by a lab safety officer or licensed storekeeper.
- Goes wrong: defects logged twice, corrective action closed by the wrong person, no photo evidence, plant rooms with no signal. [inference]

### 3.7 Permit-to-work and lock-out/tag-out
- Confined-space steps from the regulations: appoint an authorised manager (reg 9(a)); application (reg 11); gas test with results recorded in the permit (reg 12(2)(d)); manager issues the permit only if conditions are met (oxygen in range, flammable gas below 10% of LEL, toxics within limits, ventilation) (reg 13(1)); manager retains a copy (reg 13(2)); permit states description, location, purpose, gas results, validity (reg 9(c)); posted copy stays up until expiry, revocation or the worker's exit (reg 14(b)); worker's responsible person tells the manager when the worker has left (reg 15(2)); periodic testing and records kept at least 2 years (reg 16); attendant stays outside and keeps regular contact with those inside and alerts rescue (reg 22(2)); manager reviews the need daily (reg 18(1)) and may revoke on completion (reg 18(3)(a)) and must revoke if a hazardous atmosphere is detected (reg 18(3)(b)); fresh application if work overruns validity (reg 13(4)). [reported: SSO mirror] https://littdb.sfo2.cdn.digitaloceanspaces.com/litt/SG/SSOStatutes/subsidiary_legislation/WSHA2006-S462-2009.html
- General PTW: a sector guideline exists, the Tripartite Guideline on Permit-to-Work for the Oil, Petrochemical, Energy and Chemicals (OPEC) cluster, built after feedback that unclear roles and poor communication on the ground could compromise safety. [reported] https://www.ntuc.org.sg/uportal/news/New-Tripartite-Guideline-on-Permit-to-Work-for-the-OPEC-cluster/ (18 Nov 2015). I did not retrieve the guideline text from MOM.
- LOTO: Singapore Standard SS 571 "Code of practice for energy lockout and tagout"; vendor claims WSH (General Provisions) Regulations reg 16 requires lock-out procedures for maintenance where unexpected activation could injure. [reported, search summary only, weak] https://www.singaporestandardseshop.sg/Product/SSPdtDetail/4f20c20d-8ee4-418d-aa90-b6dd629135db . Per-worker lock, shared hasp, removal only by owner is standard practice in general but not sourced here. [inference]
- Goes wrong: permit closed while a worker is inside; permit expires but work continues; stale gas result; lock removed by someone else; attendant leaves post. All are shared-state conflicts. [inference]

### 3.8 Emergency muster and headcount
- Legal base: Fire Safety (Emergency Response Plan) Regulations require certain premises to prepare and maintain an Emergency Response Plan covering floor layouts, evacuation procedures and assembly areas. SCDF publishes ERP evacuation planning guidelines including assembly area selection. [reported, search summary only] https://scdf.gov.sg/docs/default-source/scdf-library/p-fm/erp-evacuation-planning-guidelines-v2.pdf (the link 404'd for me; verify). A third-party mirror says a minimum of 2 evacuation drills and 2 table-top exercises a year are required (1 evacuation drill for premises with no FSM). [reported, mirror, search summary only] https://legal-wires.com/singapore/fire-safety-emergency-response-plan-regulations-3 . Verify on SSO.
- Company Emergency Response Team (CERT) regulations 2013 cover initial firefighting and rescue before SCDF and coordination with SCDF. [reported] https://legal-wires.com/singapore/fire-safety-company-emergency-response-team-regulations-2013-6
- Practice: workplace fire checklists ask whether department managers can report on an "employee emergency headcount status record form"; Changi Airport's fire drill circular refers to headcount at assembly areas. [reported, search summary only] https://www.changiairport.com/content/dam/cacorp/documents/firepreventioncirculars/2014/2014-03%20Megaplex%201%20fire%20drill.pdf . No electronic roll-call system found in Singapore sources.
- Goes wrong: visitor and contractor lists out of date; double counting by two wardens; list on a phone with no signal or battery; no one owns the combined count. [inference]

---

## 4. Regulatory requirements

### 4.1 Private Security Industry Act (PSIA) and PLRD
- PSIA 2007 is the main statute; licences for security agencies and security officers; licence conditions sit in the Act and are enforced by the Police Licensing and Regulatory Department (PLRD) of SPF. [reported, search summary only] https://sso.agc.gov.sg/act/psia2007 (fetch blocked).
- Occurrence/attendance book: found only as an enforced licence condition. "Failure to maintain occurrence or attendance book" is a minor infringement worth 3 points. Sep-Dec 2016 exercise: 36 breaches detected (attendance book 28, grading decal 3, PWM training 2, occurrence book 2, PWM wage 1). [reported] https://api.sambuz.com/doc/security-agencies-ppt-presentation-1047585 (deck dated 24 Jan 2017). The licence-condition text and any retention period: not found.
- Incident reporting under the Conduct Regulations: surveillance of premises to police "without undue delay"; unauthorised person handed to police "without unnecessary delay"; non-disclosure of deployment details; penalties for regs 5(1) and 7(1)(a),(b) up to $10,000, up to 2 years jail, or both. [reported] https://legal-wires.com/singapore/private-security-industry-conduct-regulations-2009-5 . The occurrence-book and general incident reporting duties were not in that extract.
- Training: new Security Officers must complete the enhanced Basic Licensing Units from 1 January 2024 (Guard and Patrol with Access Control Management, 3 days; Incident Response, 3 days); existing SOs and Senior SOs must obtain the additional Statements of Attainment by 31 December 2025; from 1 January 2024 all SOs must pass an online refresher quiz to renew their licence. [fact] https://www.mha.gov.sg/media-room/newsroom/gov-accepts-recommendations-of-the-security-tripartite-cluster/ (2 Feb 2023). Quiz result valid for one year, PLRD sends a renewal notice 3 months before expiry. [reported, search summary only] https://www.mha.gov.sg/docs/default-source/default-document-library/annex---factsheet-on-new-mandatory-online-refresher-quiz-for-all-licensed-security-officers.pdf
- Agency licence terms: from September 2025, security service providers receive three-year licences instead of one-year; from 1 July 2026, agencies passing all core SACE competencies get three-year licences and those passing electives such as Job Redesign get five-year licences. [fact] https://www.mha.gov.sg/media-room/newsroom/security-industry-conference-2025/ (12 Nov 2025). New-licence paid-up capital raised from $300,000 to $500,000 from 1 July 2023. [fact] https://www.mha.gov.sg/media-room/newsroom/security-industry-transformation-map-itm-2025/

### 4.2 Infrastructure Protection Act 2017 (IPA)
- Applies to "special developments" (new, designated under s 34) and "special infrastructure" (s 40 or designated under s 41). Responsible person is the owner (s 32), or owner/occupier as the designation says. Duties: a security plan with a security risk assessment prepared by a Commissioner-approved competent person and approved by the Commissioner (ss 33, 36, 43); no specified works before approval (ss 35, 42; up to $200,000 or 2 years); implement and maintain pre- and post-completion measures (ss 39-40, 46-47; failure up to $20,000 or 2 years, plus up to $1,000 per day continuing); Commissioner may issue security directives (ss 49-55); Minister may order closure or deployment of security personnel against imminent terrorism risk (s 58). "Protected infrastructure" includes protected areas and protected places. [reported: Wikisource copy of the Act, first 100,000 of 140,781 characters read] https://en.wikisource.org/wiki/Infrastructure_Protection_Act_2017
- Incident reporting, record retention, audit cycles under the IPA: not found in the portion read. The search that returned audit and incident-reporting duties was about the Cybersecurity Act 2018, a different statute. Do not confuse them.
- Relevance: the IPA is about physical security measures at designated buildings; a security plan is the document an auditor would compare against, so the processes in section 3 are what the "security measures" become in practice. [inference]

### 4.3 Workplace Safety and Health
- Incident reporting (WSH (Incident Reporting) Regulations): notify the Commissioner "as soon as is reasonably practicable" for a fatal accident (employer, reg 4(1); occupier for non-employee, reg 4(2)) and for a dangerous occurrence (occupier, reg 5(1)); report within 10 days after the fatal accident (reg 4(3)), dangerous occurrence (reg 5(2)), injury accident from the employer's first notice (reg 6(1)), and occupational disease (reg 7(1)); keep a record of every notification or report for 3 years (reg 8); form and manner per the iReport site (reg 9). [reported: mirror] https://littdb.sfo2.cdn.digitaloceanspaces.com/litt/SG/SSOStatutes/subsidiary_legislation/WSHA2006-RG3.html . MOM confirms iReport deadlines of 10 days and 3-year retention. [fact] https://www.mom.gov.sg/workplace-safety-and-health/work-accident-reporting/report-a-work-related-accident (fetched 2026-11-06; one line in my fetch said "immediately" for an employee who dies after a non-fatal injury).
- Confined spaces (WSH (Confined Spaces) Regulations 2009): see 3.7 for permit rules. Retention: atmosphere test records at least 2 years from the date made (reg 16(3)); equipment examination records at least 2 years (reg 23(3)); authorised manager retains a copy of the permit (reg 13(2), no period stated). [reported: mirror] https://littdb.sfo2.cdn.digitaloceanspaces.com/litt/SG/SSOStatutes/subsidiary_legislation/WSHA2006-S462-2009.html
- Other PTW duties: WSH (Construction) Regulations cover a permit-to-work system and LOTO procedures; Work at Heights regulations mention PTW. [reported, search summary only] https://inetapps.nus.edu.sg/osh/portal/general_safety/legalreq.html . A generic national PTW regulation: not found.

### 4.4 Fire Safety Act
- FSM duties are in the Fire Safety (Fire Safety Managers) Regulations (reg 7); owners appoint through the eFSM portal; annual report and premises emergency data since 1 April 2025; drills and ERP as in 3.8. [fact for FSM page] https://www.scdf.gov.sg/home/fire-safety/permits-and-certifications/fire-safety-manager-(fsm)
- Retention periods for fire safety registers, drill records: not found.

### 4.5 Reporting deadlines and retention at a glance

| Item | Deadline or period | Source | Tag |
|---|---|---|---|
| WSH fatal accident or dangerous occurrence: notify | As soon as reasonably practicable | MOM page, regs 4(1), 5(1) | [fact]/[reported] |
| WSH iReport (accident, dangerous occurrence, disease) | Within 10 days | MOM page, regs 4(3), 5(2), 6(1), 7(1) | [fact] |
| WSH notification/report records | 3 years | MOM page, reg 8(2) | [fact] |
| Confined space atmosphere test records | At least 2 years | Reg 16(3), mirror | [reported] |
| Confined space equipment exam records | At least 2 years | Reg 23(3), mirror | [reported] |
| PSIA surveillance report to police | "Without undue delay" | Conduct Regs reg 5, mirror | [reported] |
| PSIA occurrence book retention | Not found | - | - |
| IPA records and incident reporting | Not found (first 100k chars read) | - | - |
| Fire safety records retention | Not found | - | - |
| FSM appointment | 90 days from CSC/TOP; replacement in 14 days | SCDF | [fact] |
| FSM annual report | Annually via eFSM | SCDF | [fact] |

---

## 5. Singapore security-industry context

- **Workforce size:** not confirmed from a primary source. A search-result summary mentioned "about 47,000 active security officers" without a page I could verify; do not quote it. What I can source: more than 7,600 in-house security officers (about 1,500 full-time) are covered by the in-house PWM ladder. [fact] https://www.mom.gov.sg/newsroom/press-releases/2025/3010-security-tripartite-cluster-recommendations-for-in-house-security-officers (30 Oct 2025)
- **Manpower shortage:** MHA cites "persistent manpower constraints" [fact] https://www.mha.gov.sg/media-room/newsroom/security-industry-conference-2025/ ; ageing population and rising costs [fact] https://www.mha.gov.sg/media-room/newsroom/security-industry-conference-2024/ (19 Nov 2024); a tripartite study is looking at reducing extra hours beyond the 44-hour week (same source). Auxiliary police recruitment from abroad because of a shrinking local workforce; foreign APOs about 3% of the APO workforce at December 2024. [reported, search summary only] https://mothership.sg/2024/01/singapore-recruit-auxiliary-police/amp/ . An opinion piece claims the security workforce will lose over a third of its numbers to retirement in 10 years; unconfirmed, do not quote. [reported] https://vulcanpost.com/470731/singapore-safety-vulnerable-ignore/
- **Security Industry Transformation Map (ITM):** launched February 2018, aims to move the industry away from a manpower-intensive model [fact] https://www.mha.gov.sg/media-room/newsroom/oral-reply-to-parliamentary-question-on-uplifting-the-private-security-industry-by-ms-sun-xueling-senior-parliamentary-secretary-ministry-of-home-affairs-and-ministry-of-national-development/ (14 Jan 2019). Refreshed 16 November 2022. [fact] https://www.mha.gov.sg/media-room/newsroom/security-industry-transformation-map-itm-2025/
- **Progressive Wage Model (PWM):** in-house Security Officer monthly floor $2,315 (2025), $2,475 (2026), $2,635 (2027), $2,795 (2028); Senior SO $2,515 to $2,995; Supervisor $2,745 to $3,225. [fact] https://www.mom.gov.sg/newsroom/press-releases/2025/3010-security-tripartite-cluster-recommendations-for-in-house-security-officers . MHA's 12 Nov 2025 speech says basic monthly wage under PWM rises to $3,090 from 1 January 2026 (level not stated; likely a different rung or the outsourced ladder, so [inference] on that). Progressive Wage Credit Scheme co-funds up to 20% of 2026 increases for workers earning up to $3,000. [fact] MOM link above.
- **Outcome-based contracting (OBC):** buyers specify performance levels, not headcount; government agencies required to adopt OBC for security from 1 May 2020. [fact] https://www.police.gov.sg/knowledge-hub/security-contracting/security-outcome-based-contracting . Since mid-2025 all Government Procuring Entities have at least one OBC; over 40% of surveyed agencies bid for at least one OBC in 2024 (under 30% in 2022); almost half of non-adopting private buyers are interested. Example: Arts House Group needed 26 officers instead of 42. [fact] https://www.mha.gov.sg/media-room/newsroom/security-industry-conference-2025/
- **MHA technology push:** Security Industry Digital Plan (July 2018, IMDA and MHA) with Productivity Solutions Grant funding for pre-approved solutions [fact] (14 Jan 2019 PQ reply above); IMDA CTO-as-a-Service with over 450 subsidised solutions; digital plan being updated (2022 page above); grading exercise reviewed to weight technology adoption and training more (2019 PQ reply); Security Technologist training track (2025); IMDA piloted an Integrated Security Management platform and robotics courses (19 Nov 2024 speech above). None of these pages mention digitalising occurrence books, offline operation, or sync.

---

## 6. Which processes are likely to be offline, and which fail worst

All of this section is [inference] unless stated. No Singapore source quantifies connectivity gaps for security work; the challenge statement itself presumes them.

Why offline happens at a DSO-type site (analogue reasoning only; nothing public on DSO): (a) physical dead zones: basements, plant rooms, tunnels, tanks, lifts; (b) policy: secure or shielded areas may restrict radio links and personal devices; (c) outages and congestion during emergencies. Analogue: controlled-area rules at defence and high-security facilities generally; I did not source a specific policy.

| Rank (offline likelihood) | Process | Why |
|---|---|---|
| 1 | Permit-to-work in confined spaces and plant | Work happens in tanks, ducts, pits; attendant and worker may be out of coverage |
| 2 | Patrols | Rounds go through basements, roofs, plant rooms |
| 3 | Safety inspections | Same locations as patrols |
| 4 | End-of-day sweeps | Vaults, shielded rooms, labs |
| 5 | Custody of keys and controlled items | Vault and cabinet locations; handovers at remote posts |
| 6 | Muster | Outdoor assembly points usually have signal, but power or network can fail in the very event it is used |
| 7 | Incident reporting | Depends on where the incident is |
| 8 | Access and visitor control | Gates usually wired |

| Rank (severity if records conflict or are lost) | Process | Failure mode |
|---|---|---|
| 1 | Permit-to-work / LOTO | Permit closed while someone is inside; lock removed by wrong person; stale gas result. Life safety |
| 2 | Custody of keys and controlled items | Double handover, item "held" by two people, unaccounted classified item. Security breach and investigation |
| 3 | Muster | Wrong headcount directs rescuers wrongly |
| 4 | End-of-day sweep | Room wrongly cleared; discrepancy lost |
| 5 | Safety inspections | Defect closed in error |
| 6 | Incident reporting | Duplicate or lost reports; missed deadline |
| 7 | Patrols | False missed-checkpoint, duplicate scans; low harm per error |
| 8 | Access control | Lost visitor row; indirect harm via muster |

Sync-design implications:
- Log-type data (patrol scans, incident entries, visitor rows): append-only events keyed by device ID plus sequence; merge is a union. [inference]
- Ownership and state-transition data (custody holder, permit status, lock status): model as events that validate against invariants (one holder; handover needs both parties; permit cannot close while any worker is signed in). When two offline devices produce a conflict, do not silently pick a winner; flag it for a human, keep both events. [inference]
- Offline clocks drift: store device time, a monotonic per-device counter, and server receive time. [inference]
- Keep a tamper-evident (hash-chained) audit trail because the records may be inspected under licence or WSH rules. [inference]

---

## 7. Evidence table

| Claim | Tag | Source URL | Date |
|---|---|---|---|
| MOM: iReport due within 10 days; employers/occupiers keep iReports 3 years | [fact] | https://www.mom.gov.sg/workplace-safety-and-health/work-accident-reporting/report-a-work-related-accident | undated; fetched 2026-11-06 |
| WSH incident regs: notify "as soon as reasonably practicable" (regs 4, 5); report within 10 days (regs 4-7); records 3 years (reg 8) | [reported: SSO mirror] | https://littdb.sfo2.cdn.digitaloceanspaces.com/litt/SG/SSOStatutes/subsidiary_legislation/WSHA2006-RG3.html | undated mirror |
| Confined space permit contents, issue, expiry, revoke, attendant, record retention (regs 9, 11-16, 18, 22, 23) | [reported: SSO mirror] | https://littdb.sfo2.cdn.digitaloceanspaces.com/litt/SG/SSOStatutes/subsidiary_legislation/WSHA2006-S462-2009.html | undated mirror |
| Confined space regs do not make revocation on completion mandatory (reg 18(3)(a) "may") | [reported], gap reading [inference] | same as above | undated |
| Tripartite PTW guideline for OPEC cluster launched after concern about unclear roles and poor communication | [reported] | https://www.ntuc.org.sg/uportal/news/New-Tripartite-Guideline-on-Permit-to-Work-for-the-OPEC-cluster/ | 2015-11-18 |
| SS 571 is the code of practice for energy lockout and tagout | [reported, search summary only] | https://www.singaporestandardseshop.sg/Product/SSPdtDetail/4f20c20d-8ee4-418d-aa90-b6dd629135db | SS 571:2011(2026) |
| WSH (General Provisions) reg 16 requires lock-out procedures | [reported, vendor claim, search summary only] | https://safetyware.com/product-category/facility-traffic-safety/lockout-tagout/ | undated |
| FSM thresholds, 90-day and 14-day appointment rules, eFSM annual report from 1 Apr 2025, penalties | [fact] | https://www.scdf.gov.sg/home/fire-safety/permits-and-certifications/fire-safety-manager-(fsm) | undated; fetched 2026-11-06 |
| ERP regs require premises to keep an Emergency Response Plan with assembly areas | [reported, search summary only] | https://scdf.gov.sg/docs/default-source/scdf-library/p-fm/erp-evacuation-planning-guidelines-v2.pdf (404 on my fetch) | undated |
| Minimum 2 evacuation drills and 2 table-top exercises a year | [reported, mirror, search summary only] | https://legal-wires.com/singapore/fire-safety-emergency-response-plan-regulations-3 | undated |
| CERT regulations 2013 set up an internal emergency response team | [reported, search summary only] | https://legal-wires.com/singapore/fire-safety-company-emergency-response-team-regulations-2013-6 | 2013 |
| Headcount appears on employee emergency headcount status record forms; assembly-area headcounts in drills | [reported, search summary only] | https://www.changiairport.com/content/dam/cacorp/documents/firepreventioncirculars/2014/2014-03%20Megaplex%201%20fire%20drill.pdf | 2014-03 |
| PLRD grading deck: Sep-Dec 2016 breaches 36 (attendance book 28, occurrence book 2); occurrence/attendance book failure = minor infringement, 3 points | [reported] | https://api.sambuz.com/doc/security-agencies-ppt-presentation-1047585 | deck 2017-01-24 |
| Conduct Regs: police report on surveillance "without undue delay" (reg 5); hand over detainee "without unnecessary delay" (reg 7); do not disclose deployment (reg 4); penalties up to $10,000 / 2 years | [reported] | https://legal-wires.com/singapore/private-security-industry-conduct-regulations-2009-5 | undated |
| New SOs: enhanced Basic Licensing Units from 1 Jan 2024; Guard and Patrol with Access Control Management 3 days; Incident Response 3 days; refresher quiz for renewal from 1 Jan 2024; existing SOs obtain additional SOAs by 31 Dec 2025 | [fact] | https://www.mha.gov.sg/media-room/newsroom/gov-accepts-recommendations-of-the-security-tripartite-cluster/ | 2023-02-02 |
| Refresher quiz valid for one year; PLRD notice 3 months before expiry | [reported, search summary only] | https://www.mha.gov.sg/docs/default-source/default-document-library/annex---factsheet-on-new-mandatory-online-refresher-quiz-for-all-licensed-security-officers.pdf | ~2023 |
| Agency licences 3 years from Sep 2025; SACE-based 3 and 5 year licences from 1 Jul 2026 | [fact] | https://www.mha.gov.sg/media-room/newsroom/security-industry-conference-2025/ | 2025-11-12 |
| Over 80% of agencies want more advanced tech but struggle to select and deploy; Security Technologist track; all GPEs have an OBC since mid-2025; over 40% of agencies bid for an OBC in 2024; Arts House 26 vs 42 officers | [fact] | https://www.mha.gov.sg/media-room/newsroom/security-industry-conference-2025/ | 2025-11-12 |
| "Persistent manpower constraints"; PWM basic wage rises to $3,090 from 1 Jan 2026 (level not stated) | [fact] | https://www.mha.gov.sg/media-room/newsroom/security-industry-conference-2025/ | 2025-11-12 |
| Ageing population, inflation, tripartite study on hours above 44, CTO-as-a-Service, Integrated Security Management platform pilot | [fact] | https://www.mha.gov.sg/media-room/newsroom/security-industry-conference-2024/ | 2024-11-19 |
| ITM refreshed Nov 2022; 450+ subsidised IMDA solutions; paid-up capital $300,000 to $500,000 from 1 Jul 2023; automated visitor registration machines mentioned | [fact] | https://www.mha.gov.sg/media-room/newsroom/security-industry-transformation-map-itm-2025/ | content 2022-11-16 |
| ITM launched Feb 2018; Security Industry Digital Plan July 2018; PSG funding; government security contracts about $350m a year (about one-fifth of demand) | [fact] | https://www.mha.gov.sg/media-room/newsroom/oral-reply-to-parliamentary-question-on-uplifting-the-private-security-industry-by-ms-sun-xueling-senior-parliamentary-secretary-ministry-of-home-affairs-and-ministry-of-national-development/ | 2019-01-14 |
| Government agencies must adopt OBC for security from 1 May 2020; OBC integrates technology and redesigned work processes | [fact] | https://www.police.gov.sg/knowledge-hub/security-contracting/security-outcome-based-contracting | undated; fetched 2026-11-06 |
| In-house PWM: 7,600+ officers covered, about 1,500 full-time; wage ladder 2025-2028; PWCS co-funds up to 20% of 2026 increases | [fact] | https://www.mom.gov.sg/newsroom/press-releases/2025/3010-security-tripartite-cluster-recommendations-for-in-house-security-officers | 2025-10-30 |
| Foreign auxiliary police recruitment because of shrinking local workforce; about 3% of APO workforce at Dec 2024 | [reported, search summary only] | https://mothership.sg/2024/01/singapore-recruit-auxiliary-police/amp/ | 2024-01 |
| IPA: special development / special infrastructure definitions, responsible person, security plan, penalties, ss 32-58 | [reported: Wikisource copy] | https://en.wikisource.org/wiki/Infrastructure_Protection_Act_2017 | Act passed Oct 2017 |
| Singapore guard-tour vendors (DrPatrol, EasyGuarding); QR/NFC; digital logbooks; GetApp listing mentions offline capability | [reported, vendor pages, search summary only] | https://www.getapp.sg/software/2096587/guard-patrol ; https://dev.to/software_website_eb3315e7/guard-patrol-monitoring-system-singapore-p9c ; https://play.google.com/store/apps/details?id=com.easyguarding | undated |
| US SF 701 end-of-day checklist and SF 702 container check sheet (analogue for DSO custody and sweeps) | [fact about US regime] | https://www.archives.gov/isoo/security-forms ; https://govinfo.gov/content/pkg/CFR-2021-title32-vol6/pdf/CFR-2021-title32-vol6-sec2001-80.pdf | CFR 2021 ed. |
| DSO/MINDEF sites use similar custody and sweep controls | [inference] | analogue: US SF 701/702 above | - |
| Offline likelihood and conflict-severity rankings in section 6 | [inference] | my reasoning | - |

---

## 8. Open questions and not found

Not found (searched or tried, no source):
- Total number of licensed security officers and agencies from a primary source (a 47,000 figure appeared in a search summary only).
- PSIA licence-condition text for occurrence books and attendance books, and any retention period. SSO blocked my fetcher (HTTP 403).
- Any PSIA, IPA or Fire Safety Act retention period for security or fire records.
- IPA incident-reporting or audit duties. I read only the first 100,000 of about 140,781 characters of the Wikisource copy, so later sections (including Part 5 onward) may contain more.
- Singapore rules for custody of classified material (Official Secrets Act, MINDEF or government security instructions): no public source found.
- How DSO or MINDEF sites operate: device restrictions, shielded rooms, connectivity. Nothing public.
- Lab/chemical and explosives inspection regimes in Singapore (including the Arms and Explosives Act duties for storage and inspection). Not researched beyond a failed search.
- MOM's full Permit-to-Work guideline text; any national requirement for PTW outside confined spaces, construction and work at heights.
- Singapore-specific lock-out/tag-out rule text (only a vendor claim about reg 16 of the General Provisions Regulations).
- SCDF fire safety register or logbook requirement and routine inspection frequencies.
- Whether any Singapore operator uses electronic muster / roll-call.
- Quantified connectivity problems in Singapore buildings (basements, MRT tunnels); the offline ranking is reasoning only.
- Average age, vacancy counts and attrition in the security workforce.

Questions the team could ask a DSO mentor or judge:
1. Which process do your officers find most painful when the network drops?
2. Are personal phones allowed, or would a shared ruggedised device be the realistic hardware?
3. When two records disagree after reconnection, who has authority to resolve it, and what must the audit trail show?
4. Is a hash-chained, append-only history acceptable, or must records be editable?

Source quality warnings:
- Several statute citations (WSH regs, Conduct Regs, ERP regs, CERT regs) come from mirrors and summaries, not SSO.
- Items marked "search summary only" were not verified by fetching the page.
- The MHA "ITM 2025" page content is dated 16 November 2022 despite the URL.
