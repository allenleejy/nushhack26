# Research A: DSO National Laboratories and what its judge is likely to value

Researched 6 Nov 2026. About 17 searches and 17 fetches. Tags: [fact] = primary or authoritative source, [reported] = news, vendor or secondary source, [inference] = my own reasoning. "Not found" means I looked and could not source it.

Caveat on method: page content was read through a summarising fetch tool, so exact quotes should be re-checked on the live page before they go on a slide. Nothing in any fetched page tried to instruct me.

---

## 1. Key findings (ranked by importance to the challenge)

### 1. DSO's own public AI direction is on-premises, self-reliant and edge, not "cloud first"
DSO's page on its model with Mistral AI says the model was built to be "deployed entirely on premise", with the stated goal of "self-reliant AI" so DSO does not depend on outside vendors. Phase 2 is "small, high-performing reasoning models that can run efficiently at the edge" on field-deployed autonomous systems. [reported: vendor customer page, so it is Mistral's wording of DSO's needs; footer says 2026, a search snippet dated it 24 Apr 2026]
- A DSO engineer will think "no hard dependency on someone else's cloud" is a feature. Our Vercel + Supabase stack is the obvious weak point; see Implications.

### 2. For MINDEF and DSO, "offline" is usually a deliberate security boundary, not bad Wi-Fi
- MINDEF said in Feb 2017 that classified matters run on a separate system "not connected to the Internet", and that an I-net breach was contained "by the physical separation of I-net from our internal systems". [fact]
- MINDEF/SAF's 2021 digital plan splits workloads: commercial cloud for "less sensitive digital services, functions and data", and a dedicated MINDEF-housed cloud "where classified information and workloads will reside". [fact]
- The public service did the same in 2016 to 2017 (Internet Surfing Separation, about 100,000 work computers). The stated reason was to "significantly reduce the attack surface". Staff browse on separate devices or terminals. [reported]
- [inference] So the challenge's "syncs correctly when internet access returns" is, for a DSO judge, probably a controlled crossing between a disconnected zone and a connected one. Sync is a security-relevant gateway: what crosses, who authorised it, what is logged, and what must never cross. The judge will probe this.

### 3. Physical security rules suggest the device may not be a personal phone
- SAF camps use Red Zones (server rooms, operations rooms, offices handling sensitive information) and Green Zones. Camera-equipped phones must be left in lockers before entering a Red Zone; unauthorised photography is banned in all zones. [fact, 23 Aug 2013 fact sheet]
- Secondary reports say the rule was later relaxed so a camera can be disabled instead of banning the phone. I could not confirm this from a MINDEF page. [reported]
- DSO's own premises rules (phones, devices, clearance) are not public. Not found.
- [inference, analogue: SAF camp zoning] In a restricted area the app may run on an issued device, a shared tablet or a kiosk, with no personal phone and possibly no camera. Photo or QR capture should therefore not be the only way to complete a step.

### 4. DSO's own work areas that employ people who do safety or security processes
- Quality division: AS9100 (aerospace quality standard), "reliability, safety, and efficiency". [fact: dso.org.sg/works/quality]
- Human Systems division: "high containment facilities" for toxic chemicals, "highly pathogenic biological agents" and radioactive or nuclear materials. This makes lab and containment safety staff a real DSO persona. [fact: dso.org.sg/works/human-systems]
- National Security Office: protecting "critical physical and digital infrastructure" and countering CBRN threats. Its page links DSO50 chapters on "Responding to National Crises & Major Incidents". [fact]
- Information division: cryptography (the "DSO Crypto Chip"), penetration testing, and critical-infrastructure interdependency modelling. [fact]
- Public sources say nothing on DSO test ranges or remote test sites. Not found.

### 5. The SAF's public safety model gives a vocabulary a DSO judge already uses
The SAF Inspector-General's Office runs independent audits and inspections across the Services and the Digital and Intelligence Service. It checks implementation of safety policy, safety culture, and "safety management systems", and asks whether commanders have "nurtured and are sustaining a safety culture". [fact, MINDEF page, last updated 27 Jul 2025]. Related public practice, all [fact]: 100% inspection of high-risk and field training, Safety Advocates in every unit (2019), near-miss reporting to a commander, a 24-hour hotline or the MINDEF intranet.
- [inference] Strong themes for the demo: inspection checklists, sign-offs, near-miss reports, an audit trail, and "reporting must be easy or people stop doing it".

### 6. What defence hackathon judges visibly reward (thin evidence, no published rubrics)
- Singapore Defense Tech Hackathon (NUSX, 25 to 28 Sep 2026; DSTA and DSO listed as supporting partners; judges included a senior DSTA technology officer and a former DSO CEO). No formal criteria published. One judge stressed "rapid development of cost-effective systems from commercially available technology". A special prize rewarded combining teams' technologies into integrated capabilities. [reported]
- DSTA BrainHack 2026 (AI track, run by AngelHack): scored against objective metrics (for example a transcription accuracy score), plus a surprise round. Organiser's takeaway: the edge came from "problem-solving and mentor guidance, not spend", and the surprise round rewarded "the teams who adapt fastest". [reported]
- [inference] Defence-adjacent judges favour a measurable result over a polished UI. Demo something checkable: for example, "we injected N conflicting edits; the app merged them correctly, here is the log".

### 7. DSO's public values and self-description
- Mission: "technological surprises" to sharpen the edge of Singapore's defence and national security. Core values: People, Passion, Innovation. [fact]
- Student outreach (SAFMC, with Science Centre): the CEO said participants are "not just building machines; they are sharpening their critical thinking"; another speaker said "failure serves as a learning opportunity". The page does not mention safety. [fact: DSO media release; ceremony date given as 11 Apr 2026 in the text but the media-centre listing says 4 Apr 2026, so verify]
- Recent public items: quantum sensing centre (open house 25 Feb 2026), Mistral partnership (from Mar 2025), 2025 Defence Technology Prize R&D award to a DSO team. [reported/fact, see table]
- Operational resilience, safety culture, security culture and edge or offline computing: no DSO leadership speech found on any of them. Not found.

### 8. What DSO is (organisation level)
- Singapore's largest defence R&D organisation; began 1972 as a three-person team; now "more than 1,800 research scientists and engineers". [fact]
- Corporatised in 1997 as DSO National Laboratories (a company, reg. no. 199701777M). [fact]
- Part of the MINDEF "Defence Technology Community" with DSTA (statutory board, formed 2000, implements technology plans, acquires equipment, builds infrastructure) and CSIT (a MINDEF technical agency for cyber defence, counter-terrorism and counter-hostile information operations). DSTA Scholars can be posted to any of the three. [fact]
- Headquartered at the DSO Complex, 12 Science Park Drive (opened 21 Apr 2017, 69,000 m2). [fact]
- Rough division of labour: DSTA implements and acquires, DSO does R&D. The "technologies not commercially available" framing came only from job-board and aggregator pages, so I did not rely on it. [reported]

---

## 2. Evidence table

| Claim | Tag | Source URL | Date |
|---|---|---|---|
| DSO mission: develop technologies that create "technological surprises" to sharpen the edge of Singapore's defence and national security | fact | https://www.dso.org.sg/ and https://www.dso.org.sg/our-heritage | Page undated; viewed 6 Nov 2026 |
| DSO began as a three-person team, now more than 1,800 research scientists and engineers; works across land, sea, air, space, cyberspace | fact | https://www.dso.org.sg/our-heritage | Undated; viewed 6 Nov 2026 |
| Core values: People, Passion, Innovation | fact | https://www.dso.org.sg/our-heritage | Undated |
| DSO is a company (reg. no. 199701777M); renamed and corporatised 1997 | fact / reported | https://www.dso.org.sg/our-heritage ; https://en.wikipedia.org/wiki/DSO_National_Laboratories (search snippet only) | Undated |
| R&D divisions: Electronic Systems, Emerging Systems, Engineering, Guided Systems, Human Systems, Information, Quality, Robotics, Sensors; corporate: Digital, DSO College, National Security Office, others | fact | https://www.dso.org.sg/our-work | Viewed 6 Nov 2026 |
| Quality division: AS9100; "reliability, safety, and efficiency" | fact | https://www.dso.org.sg/works/quality | Undated |
| Human Systems: CBRN defence, human factors, behavioural science; "high containment facilities" for dangerous agents | fact | https://www.dso.org.sg/works/human-systems | Undated |
| Information division: AI/foundation models, pen-testing, DSO Crypto Chip, critical-infrastructure interdependency modelling, AI for DSO's own digital transformation | fact | https://www.dso.org.sg/works/information | Undated |
| National Security Office: protects critical physical and digital infrastructure; counters CBRN threats; coordinates with other agencies | fact | https://www.dso.org.sg/works/national-security-office | Undated |
| DSO Complex opened 21 Apr 2017, 12 Science Park Drive, 69,000 m2, 1,000+ staff, Robotics Laboratory and "Playground" | fact | https://www.dso.org.sg/media-article/the-dso-complex-crucible-for-innovation-in-next-gen-ai-and-robotics | 21 Apr 2017 |
| Defence Technology Community = MINDEF units plus DSTA, DSO, CSIT; DSTA is a statutory board (formed 15 Mar 2000); CSIT supports cyber defence, counter-terrorism, counter hostile information operations | fact (via search summary of MINDEF page) | https://www.mindef.gov.sg/defence-matters/defence-topics/defence-science-technology/ | Undated |
| DSO co-developed a custom multilingual mixture-of-experts LLM with Mistral AI, "deployed entirely on premise", "self-reliant AI"; phase 2 targets small reasoning models "at the edge" for field-deployed autonomous systems | reported (vendor page) | https://mistral.ai/customers/dso/ | Footer 2026; search snippet 24 Apr 2026 |
| DSTA and DSO announced a Mistral AI partnership to train LLMs for the local defence context | reported | https://www.defensemirror.com/news/39093/Singapore___s_Defence_Ministry_Partners_with_France___s_Mistral_AI_to_Develop_Generative_AI_for_Defence_Applications (via search summary) | Mar 2025 |
| DSTA and Mistral reported expanding collaboration on AI agents and drone navigation | reported (thin, few sources) | https://asiapacificdefencereporter.com/posts/singapores-dsta-partners-with-mistral-on-drones-and-ai | Jun 2026 |
| 2025 Defence Technology Prize presented 1 Oct 2025 by the Minister for Defence at DSTA; R&D award to DSO's Guided Systems Technology Team; Engineering award to a DSTA-led team with DSO on a system for adopting fast-moving commercial technology (seen via search snippet only) | fact | https://www.mindef.gov.sg/news-and-events/latest-releases/1oct25-nr/ ; https://www.mindef.gov.sg/news-and-events/latest-releases/1oct25-fs/ | 1 Oct 2025 |
| Minister's DTP 2025 remarks: geopolitics changed, avoid outdated assumptions, long-term systems approach, people are the backbone | fact | https://www.mindef.gov.sg/news-and-events/latest-releases/1oct25-nr/ | 1 Oct 2025 |
| DSO Quantum Sensing Centre open house, 25 Feb 2026; DSO media centre lists a "QSC Centre Launch" post dated 30 May 2025 (title only) | reported | https://www.nus.edu.sg/cfg/events/details/4ac0ca44605ddbe90486cc455c77ad43 ; https://www.dso.org.sg/media-centre | 25 Feb 2026; 30 May 2025 |
| DSO media centre showed no 2024 items on the first page; 2025 to 2026 items include SAFMC (Apr 2026), toy-modification CSR story (May 2026), Defence Technology Prize (Oct 2025) | fact | https://www.dso.org.sg/media-centre | Viewed 6 Nov 2026 |
| SAFMC (co-organised by DSO and Science Centre, open to students and public): record 768 teams, ~2,500 participants; new Smart Drone and Drone Flock categories; CEO: "sharpening their critical thinking"; page does not mention safety | fact | https://www.dso.org.sg/media-article/singapore-amazing-flying-machine-competition-hits-record-turnout-with-debut-of-smart-drones | 4 Apr 2026 listing; ceremony 11 Apr 2026 per text |
| Classified matters run on a separate system not connected to the Internet; I-net breach did not reach internal systems because of "physical separation"; affected server disconnected on detection | fact | https://www.mindef.gov.sg/news-and-events/latest-releases/28feb17_nr/ | 28 Feb 2017 |
| MINDEF/SAF: commercial cloud for less sensitive data; dedicated in-house cloud for classified workloads; RSAF fleet-management app cut admin time by 75% in trial; PACES Mobile gives medical staff remote record access | fact | https://www.mindef.gov.sg/news-and-events/latest-releases/01mar21_fs4/ | 1 Mar 2021 |
| SAF camp Red/Green zoning: camera phones allowed in Green Zones, must be deposited in lockers for Red Zones (sensitive/classified areas); unauthorised photography banned in all zones; pilot began 1 Sep 2012 | fact | https://www.mindef.gov.sg/news-and-events/latest-releases/2013aug23-news-releases-02529/ | 23 Aug 2013 |
| Camera rule reportedly eased later (camera can be disabled); current MINDEF rule not confirmed | reported | https://vulcanpost.com/559741/saf-changed-rules/ (search snippet) ; https://www.mindef.gov.sg/news-and-events/latest-releases/02oct17_pq2/ | 2016 to 2017 |
| Internet Surfing Separation: about 100,000 public service computers lose direct internet access from May to June 2017; CSA chief said it would "significantly reduce the attack surface"; staff use separate devices or terminals; GovTech led the change | reported | https://globalvoices.org/2016/06/14/singapore-plans-to-pull-the-plug-on-internet-access-for-public-servants/ ; https://www.intralinks.com/company/news-press/singapore-unveils-drastic-move-puts-government-pre-internet-era | 8 to 14 Jun 2016 |
| 2018: separation extended to public healthcare systems after the SingHealth breach | reported | https://www.malaymail.com/news/world/2018/07/24/singapore-disconnects-healthcare-computers-from-the-internet-after-cyber-at/1655481 | 24 Jul 2018 |
| SAF Inspector-General's Office: independent audits and inspections across the Army, Navy, Air Force, DIS; checks safety policy, safety culture and safety management systems; page last updated 27 Jul 2025 | fact | https://www.mindef.gov.sg/about-us/organisation/saf-inspector-general-s-office/ | 27 Jul 2025 |
| SAF safety practice: 100% inspection of high-risk and field training, Safety Advocates in all units (2019), near-miss reporting via commander, 24-hour hotline or MINDEF intranet | fact (via search summary) | https://www.mindef.gov.sg/news-and-events/latest-releases/02mar20_fs4/ ; https://www.mindef.gov.sg/news-and-events/latest-releases/15jul21_fs/ | 2019 to 2021 |
| Digital and Intelligence Service (DIS) set up 2022; two Commands inaugurated 18 Mar 2025; drives adoption of cloud, AI, 5G | fact (via search summary) | https://www.mindef.gov.sg/news-and-events/latest-releases/18mar25-speech/ | 18 Mar 2025 |
| Singapore Defense Tech Hackathon (NUSX, with European Defense Tech Hub and TUM Venture Labs), 25 to 28 Sep 2026; DSTA and DSO among partners; DSTA and former-DSO-CEO judges; winners NeuroMech (39 teams, 2-day) and MAELSTROM (22 teams, 3-month); no formal rubric published | reported | https://technode.global/prnasia/singapore-defense-tech-hackathon-names-winners-with-promising-teams-set-for-venture-incubation/ | 28 Sep 2026 |
| BrainHack 2026 TIL-AI: 774 participants, 68 teams, 40 finalists; four tasks (speech, vision, language retrieval, autonomous agents) plus surprise round; objective scoring | reported | https://angelhack.com/?p=18802 | 2026 |
| BrainHack 2024 had six competitions (AI, cybersecurity, app development, space data, AR/VR, fake-news detection) | reported | https://nus.edu.sg/cfg/events/details/3970 and https://www.sp.edu.sg/courses/schools/eee/happenings/detail/eee-happenings/eee-team-clinched-honorable-mention-award-at-dsta-brainhack-2024 (search summary) | 2024 |
| Inaugural BrainHack 2019 was a DSTA youth tech event on AI and cybersecurity | fact | https://mindef.gov.sg/web/portal/pioneer/article/regular-article-detail/technology/2019-Q2/14jun19_news2 | 14 Jun 2019 |

---

## 3. Implications for our app and pitch

Everything here is [inference] unless stated.

**Framing**
1. Pitch the offline state as a security boundary, not a connectivity bug. Suggested line: "Your process keeps running inside the boundary; the sync is a controlled, logged crossing." This matches MINDEF's own public architecture (classified systems off the internet; Red/Green zones).
2. Use the vocabulary of the SAF safety system: inspection, checklist, sign-off, near-miss, audit trail, safety culture. Persona candidates, each tied to a public DSO page: lab or containment safety officer (Human Systems), quality auditor (Quality, AS9100), and a security or infrastructure-protection officer (National Security Office). Pick one persona and one concrete process; do not try to cover all three.
3. Add a measurable claim. MINDEF's own digital pitch quotes a number ("reduced the time taken for administrative processes by 75%"). A number like "N checklists completed with zero lost or duplicated entries across M simulated disconnections" is the equivalent.

**Technical depth that would land with a DSO engineer**
1. Show correctness, not just that it syncs. Explicitly handle: concurrent edits to the same record, edit-after-sign-off, duplicate submissions on retry (idempotency keys), clock skew (do not trust device time; use per-device sequence numbers or logical/hybrid clocks), and partial sync that is interrupted mid-way. A visible "conflict injection" test harness is the strongest demo asset.
2. Treat safety records as append-only with a hash chain or signed entries, so a late-arriving sync cannot silently rewrite history. Conflicts on safety-critical fields should surface for human resolution, not be silently last-write-wins. This is a judgement call, but it reads as security-minded.
3. Local-first client: the full workflow must work from a service worker, IndexedDB and an outbox with zero network. Cache all forms and reference data ahead of time. Local data should be encrypted at rest (WebCrypto) with a note on what happens on a lost device. Be honest that a browser cannot give hardware-grade protection.
4. Address the stack head-on, in one slide: Supabase is open-source and self-hostable, so the sync server can run on an agency's own infrastructure; the client holds no cloud-only logic; for the hackathon we used Vercel + Supabase for speed. This answers the "on-prem, self-reliant" signal from finding 1.
5. State the data-classification assumption: the app is for unclassified safety and process records, with an explicit statement of what it would never carry. Do not claim it is suitable for classified material. Overclaiming would hurt more than it helps.
6. Device realism: design for a shared or issued device with per-user PIN login and quick user switching; make camera capture optional (manual entry, barcode scanner or NFC as alternatives), since camera phones are restricted in sensitive SAF areas.
7. Show degraded modes: a clear online/offline/syncing indicator, a pending-changes count, and a "last synced" time on every record. Safety personnel need to know what the system believes.

**Things to avoid**
- Do not present sync as "magic". A DSO engineer will ask "what if two people edit the same record offline and one has already signed it off?" Have a precise answer ready.
- Do not claim knowledge of DSO's internal processes. Say the design is based on public MINDEF and SAF practice.
- Do not rely on a flashy AI feature. DSO does AI at scale (foundation models, Mistral partnership); a bolt-on LLM will look shallow next to a solid sync engine. If you use AI at all, make it local or optional.

---

## 4. Open questions and not found

- DSO premises rules (phones, devices, clearance, air-gapped networks, working in restricted areas): not found. Only SAF camp rules (2013) and MINDEF's classified/internet separation statement (2017) are public. DSO security-clearance requirements: not found.
- Current (2025 to 2026) SAF camera-phone rule: not confirmed from a MINDEF page (the MINDEF Ask.gov.sg FAQ returned 403). 2013 fact sheet is the only primary source I read in full.
- Primary government source for Internet Surfing Separation (GovTech, CSA or PMO): not found. All sources are 2016 to 2018 news. Current status unchecked.
- DSO CEO or leadership speeches on offline, edge, resilience, safety culture or security culture: not found. The CEO's only public quote I found is the SAFMC "critical thinking" line. I did not profile individuals beyond role titles.
- Previous DSO-set hackathon problem statements: not found. DSO appears as a partner at the Singapore Defense Tech Hackathon (2026) and as co-organiser of SAFMC (a flying-machine competition), but I found no DSO-authored challenge statement.
- An earlier NUS High School hackathon with DSO or DSTA involvement: not found. The DSTA BrainHack 2025 official problem statements and results: not found (only an event listing and an AngelHack write-up of the 2026 edition). CODE_EXP: not found.
- DSO test sites or remote facilities: not found in public sources; the "safety personnel" persona for test-range or explosives staff is therefore an inference, not a sourced one.
- DSO 2024 announcements: none on the first page of the media centre; I did not page through older entries. The 30 May 2025 "QSC Centre Launch" has a title only.
- Unverified or inconsistent items to double-check before quoting: the SAFMC ceremony date (4 Apr vs 11 Apr 2026), the June 2026 DSTA and Mistral expansion (few sources), and the Mistral customer page date.
- What the DSO judge on the day actually does (engineer, programme manager, security specialist) is unknown. Ask the organisers if possible.
