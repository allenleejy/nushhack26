# Context D: custody of commercial explosives and detonators (blasting in tunnels, caverns, quarries)

Researched 6 Nov 2026. Budget used: 20 of 20 web calls (0 "extended"); the 20th, a search on underground communications, was refused by the search tool and not retried. Two fetched PDFs (SPF guideline, DSTA handout) were read locally with pdftotext after the fetch tool could not decode them.
Tags: **[fact]** official or primary source. **[reported]** news or secondary. **[inference]** our reasoning. **[search summary only]** seen only in a search result, not on the page.

## Verdict

| Criterion | Score (1-5) | Why |
|---|---|---|
| Role clarity | 4 | The 2025 regulations define "blasting operator" and "shotfirer" (and "shotfirer trainee"); SPF limits rock-blasting use to "qualified shot firers". "Storekeeper" and "magazine keeper" are not defined terms in the text I could read. |
| Process clarity | 3 | The legal hooks exist (inventory and transaction records, a security plan, chain of possession "traced"). I could not retrieve the exact issue / use / return record fields or the loss-report deadline. |
| Evidence of incidents | 3 | Several regional cases, one official bulletin that ties theft to ignored record discrepancies. No Singapore theft or loss of commercial blasting explosives found. Most rows are search-summary only. |
| Current solution and its gap | 3 | Offline-first tablet inventory apps already exist (Maptek BlastLogic, ExploSKI). The gap is not "offline"; it is that they are single-party stock books with no two-party signed handover. [inference] |
| Offline reality | 3 | DSTA names "difficulty in communications (sound and signal barriers)" in tunnels (2005 slides) and a licensed magazine in a cavern on site. Current Singapore site connectivity not verified. |
| Handovers away from the register | 4 | Magazine to blast face is exactly that, with several named hands in between. |
| Fit with the concept | 4 | Tagged cases, sealed magazine, named people. Loses a point because detonators are consumed, so the chain ends at "expended", and Singapore does not (as far as I found) mandate per-detonator codes. |
| Defence or DSO link | 4 | DSTA and MINDEF built the Mandai Underground Ammunition Facility by drill and blast; staff handling explosives need security clearance; diversion is the stated reason for EU traceability. DSO energetics link is [inference] only. |
| Easy to explain | 4 | "Count the detonators out, count them back, and know who held them at every moment." |
| **Total** | **32 / 45** | Two low-ish scores (process, incidents) come partly from what I could not retrieve, not only from the setting. |

## Who the safety personnel are (official titles)

- **Legal framework has changed.** The Arms and Explosives Act 1913 has been replaced for explosives by the **Guns, Explosives and Weapons Control Act 2021 (GEWCA)**. SPF's own 2025 guideline says its explosive licence types are "under GEWCA" [fact] https://www.police.gov.sg/-/media/SPF/E-services/AE-Files/Guideline-on-Application-for-Explosive-Licence.pdf (PDF created 30 Jul 2025). The **Guns, Explosives and Weapons Control (Explosives and Explosive Precursors) Regulations 2025 (S 374/2025)** were made on 21 May 2025 [fact, text read on a mirror of SSO, not sso.agc.gov.sg] https://littdb.sfo2.digitaloceanspaces.com/litt/SG/SSOStatutes/subsidiary_legislation/GEWCA2021-S374-2025.html . Commencement 1 July 2025 [search summary only]. Do not cite the old Act as current in the pitch.
- **Blasting operator**: "a person responsible for organising a blasting operation" (reg 2(1)) [fact, mirror, same URL].
- **Shotfirer**: an individual "employed or otherwise engaged by a blasting operator" to design, load and initiate blasts, handle misfires, or prepare and fire explosives (reg 2(1)). A blasting-user licence application must give shotfirer details, including competence and whether the person is a "shotfirer trainee" (reg 20(1)(g)) [fact, mirror].
- **Licensing authority**: the "Licensing Officer" at SPF's Police Licensing & Regulatory Department (the Arms & Explosives Division) [search summary only] https://police.gov.sg/e-Services/Police-Licences/Arms-and-Explosives-Licence .
- **Not found as defined terms**: "storekeeper", "magazine keeper", "person in charge of the blasting site" (searched the first ~40,000 characters of the regulations; the schedules and reg 46 were not retrieved). Use "shotfirer", "blasting operator" and, informally, "magazine storekeeper".
- **On site, from DSTA**: shotfirer, tunnel foreman, safety supervisor; "work can only be carried out under supervision of authorised blasting specialist"; the shotfirer connects the charges, does the final check, and with the foreman inspects for misfires [fact, DSTA slides dated May 2005 by file metadata] https://srmeg.org.sg/docs/Construction%20safety%20in%20hard%20rock%20tunnelling%20(Handout).pdf
- **Security sensitivity of the role**: employees handling explosives need a security clearance applied for through GoBusiness [fact, SPF guideline 2025, same URL as above].

## The security process, step by step, with its legal basis

What I can source, and what I could not.

1. **Licensing chain.** A rock-blasting user licence can only go to a company that already holds a valid Licence to Supply and Store Explosives [fact, SPF guideline 2025]. Application needs: engagement letter from the main contractor, project details, a Rock Blasting Proposal (scope, methodology, vibration calculation, risk assessment, stakeholders), a **site security plan (access control, storage arrangements, transportation procedures)**, names and qualifications of personnel who will handle explosives, emergency response procedures [fact, same]. Certain blasting operations need 60 days' lead time before applying (reg 10) [fact, mirror].
2. **Blasting conference.** Utilities, LTA (roads or MRT tracks), HDB and others give "No Objection" letters; SPF may order a trial blast [fact, SPF guideline 2025].
3. **Storage.** Blasting and commercial explosives (HD 1.1, 1.2, 1.3) must be in an explosive magazine, with Auxiliary Police Officer escort for transport [fact, SPF guideline 2025]. When not in use, explosives must be kept in a magazine or suitable depositary (reg 24) [search summary only]. At Mandai, DSTA describes a "licensed magazine to store detonators and booster charges in temp cavern on site", guarded 24 hours; bulk emulsion is "non-explosives until being charged" [fact, DSTA slides 2005].
4. **Issue, use, return: what must be recorded.** Not found as a field-by-field rule. What the sources do say: licence renewal requires **inventory records and transaction records** (and incident reports) [fact, SPF guideline 2025]; a Supply licence needs "inventory management procedures" [fact, same]; the safety management plan must provide for "the keeping of records" (reg 5(1)(c)) and the security plan likewise (reg 6(1)(d)) [fact, mirror]; and one fetch summary reports reg 6(1)(b)(iii) requires the **chain of possession to be "traced" between holders** [fact, via a summariser of the mirror; read the wording yourself before quoting]. The First and Second Schedules were not retrieved and may hold the register details.
5. **Cycle at the face.** Drill, charge (only after the whole face is drilled), blast, ventilate, inspect for misfires; re-entry no sooner than 30 min if nothing initiated, 15 min after initiation and ventilation [fact, DSTA slides 2005].
6. **Return of unused explosives.** Not found in a Singapore source.
7. **When a loss must be reported, and to whom.** Not found. The regulations define an "emergency event" to include "a security breach, a theft or an unauthorised access" (reg 2(1)), but no deadline was visible [fact, mirror]. For comparison: US rule is within 24 hours to ATF and local authorities, and the report must give quantity including number of detonators (27 CFR 555.30) [search summary only] https://regulations.atf.gov/555-30 and https://www.atf.gov/explosives/tools-services-explosives-industry/current-licensees/report-explosives-theft-or-loss ; NSW requires immediate notification to the Resources Regulator [search summary only] https://safetowork.com.au/lost-detonator-underground-triggers-alert/
8. **Has Singapore used drill and blast?** Yes.
   - **Mandai Underground Ammunition Facility (SAF/DSTA)**: rock caverns under the former Mandai granite quarry, construction began 1999 with a ceremonial detonation, commissioned 2008 [search summary only] https://www.nas.gov.sg/archivesonline/data/pdfdoc/MINDEF_19990812001.pdf and https://www.asianscientist.com/?p=68846 . DSTA's own slides give "Drill and Blast Method" and "Responsibilities of Shot-firer in Mandai" [fact, DSTA 2005, URL in the roles section above].
   - **Jurong Rock Caverns**: drill-and-blast excavation, about 4.5 m per blast; the 2016 project talk notes "round-the-clock access to explosives" [search summary only] https://mothership.sg/2018/11/spore-dug-40-storeys-below-seabed-to-create-rock-caverns (2018) and https://books.aijr.org/index.php/press/catalog/download/133/58/2144-1?inline=1 (2016).
   - **MRT tunnels**: not found.

## Incidents

Singapore first: **no theft or loss of commercial blasting explosives or detonators found** (one search, no hit). The Singapore items that did appear were not about commercial blasting custody: a man who threw a military explosive into a forest to get back at a supervisor was jailed and fined, with SAF and police searching about two weeks [search summary only; date not seen] https://sg.news.yahoo.com/man-threw-military-explosive-forest-get-back-supervisor-jailed-fined-123412346.html ; and SAF disposal of WWII bombs found at construction sites (see defence link).

| Date | Place | What happened | How it was found | Tag | URL |
|---|---|---|---|---|---|
| Bulletin 19 Dec 2024 (incident date not stated) | Western Australia, a mine site | Theft of explosives; WorkSafe WA says the root cause was the illegal conduct of a trusted employee | Not stated. Site records "showed discrepancies between recorded and actual quantities, but the company did not investigate them" | [fact] | https://worksafe.wa.gov.au/publications/dangerous-goods-safety-bulletin-no-0417-security-explosives |
| Date not found | New South Wales, an underground mine | A missing electronic detonator triggered a regulator alert; licence holders must notify the Resources Regulator immediately of any loss or theft | Not seen; page blocked (HTTP 403) | [search summary only] | https://safetowork.com.au/lost-detonator-underground-triggers-alert/ |
| Undated (file name "sb06-05" suggests 2006 [inference]) | Southern NSW quarry | Two magazines broken into with oxy-cutting gear; explosives, detonators and detonating cord taken | Detonators were kept inside the magazines, which let police work out the exact amount stolen | [search summary only] | https://www.resources.nsw.gov.au/sites/default/files/documents/sb06-05.pdf |
| Nov 2023 | Puchong / Kuala Langat, Selangor, Malaysia | Box of cast booster explosives (400 g) missing on a delivery to a quarry; believed to have fallen off the vehicle, so loss in transit, not confirmed theft. Two reports differ on where (while unloaded vs in transit) | Reported to police by the 28-year-old man delivering; police appealed to the public | [search summary only] | https://www.thestar.com.my/news/nation/2023/11/19/cops-seek-information-regarding-missing-explosives-in-puchong ; https://thesun.my/local/cops-seek-information-on-missing-explosives-OB11766796 |
| Jan 2002 to Feb 2005 | USA, state and local government storage | ATF received 9 reports of thefts or missing explosives; GAO said the real number could be higher | ATF reports | [search summary only] | https://www.gao.gov/assets/a112492.html |
| 2020 (per summary) | Mawanella quarry, Sri Lanka | Explosives stolen; four arrested in Peradeniya | Police investigation | [search summary only] | https://www.ft.lk/News/Four-arrested-in-Peradeniya-following-theft-of-explosivesfrom-Mawanella/56-711090 |
| Date not found | Northern Territory, Australia | Police recovered 104 detonators, believed several years old, of a type used in mining | Not stated | [search summary only] | https://pfes.nt.gov.au/node/30497 |

Best-evidenced lesson: the WA bulletin, where the paper trail already showed a gap and nobody acted. Queensland tightened explosives laws after misuse [search summary only] https://safetowork.com.au/queensland-rolls-out-new-explosives-laws/ . Not covered by any source I read: UK HSE cases, Thailand, ATF annual loss statistics (no current annual figure turned up).

## The current solution and its gaps

- **Law-driven baseline.** EU Directive 2008/43/EC requires unique identification and traceability of civil explosives from production to final user, for law-enforcement tracing of lost or stolen items; detonators get a unique identification on the shell (label, print or stamp) with an associated label on each case, passive electronic tags optional; records kept 10 years after delivery (as amended; the snippet was garbled) [search summary only] https://eur-lex.europa.eu/eli/dir/2008/43 and https://www.legislation.gov.uk/eudr/2008/43 . The directive sets what must be identifiable; it does not say which system, or that it must work offline [inference].
- **Products found** (none of Orica, Dyno Nobel, BME or Maxam turned up; those four are **not checked**):
  - Maptek BlastLogic: tablet works without a network, then syncs and merges; Bluetooth barcode reader [search summary only] https://maptek.com/forge/september_2020/digital-explosives-inventory-management
  - GEOTEST ExploSKI (Swiss): phone camera scans the case barcode to book stock in and out; works fully offline "especially important in the explosives depots, which are often bunkers" [search summary only] https://geotest.ch/en/innovation/exploski
  - GroundHog MagLogs: alerts when inventory numbers don't add up [search summary only] https://groundhogapps.com/explosives-inventory-management/
  - Magman: rugged tablets replacing paper stock sheets that "can take days or even weeks to reconcile" [search summary only] https://cdn.asp.events/CLIENT_Exhibiti_030C4FB2_97AC_2262_74166DECA5FD4BAB/sites/austmine-2023/media/libraries/brochures/11368-Magman-Explosives-Inventory-Management-Presentation.pdf
  - TTE-Europe electronic explosives stock book https://tt-e.eu/en/tracking-tracing/explosives-stock-book.html ; General Data track and trace https://www.general-data.com/node/2812 [offline behaviour not stated].
- **Paper magazine registers**: still the baseline under the licence conditions; I found no source describing Singapore sites' practice.
- **Gap** [inference]: these tools record stock movements as one party's scan. They do not prove who physically held an item between two scans, they do not require the receiver to accept it, and two offline devices that disagree have no signed trail to settle who is liable. Offline sync is already solved by some of them, so "offline" alone is not a winning claim; the signed two-party chain is.

## Where handovers happen, and why they're offline

Handover points [inference, from the process above]: supplier or escort to site magazine; storekeeper to shotfirer at the magazine; shotfirer to charging crew or mobile charging unit; unused items back to the magazine at shift end; used or misfired items recorded at the face.
Why offline: DSTA lists "difficulty in communications (sound and signal barriers)" as unique to tunnelling [fact, DSTA slides, May 2005 by metadata]. The Mandai detonator magazine is in a cavern on site [fact, same]. ExploSKI's vendor says depots are often bunkers [search summary only]. Not verified for current Singapore sites; sites may have radio or leaky-feeder systems, and the one search aimed at this was refused. Ask the DSO judge or a contractor.

## How our concept applies

- **Items**: a case of detonators, a box of boosters, a charging unit, with the case or box ID as the tracked unit. Singapore per-detonator coding: not found. The EU shell code and case label show how a per-item ID would work [search summary only].
- **Seal**: tamper-evident seal on the magazine door and on each case. The receiver scans the seal at pickup; a broken or mismatched seal is logged as a fact in the chain.
- **Chain**: supplier, then storekeeper, then shotfirer, then crew, then back to the storekeeper. Each hop is signed by giver and receiver and swapped by QR, so nobody is liable for what they did not accept.
- **Reconciliation**: issued = used + returned, with misfires counted. A mismatch names the last signer. This is the "discrepancy nobody investigated" from the WA bulletin, made unavoidable.
- **Sync**: the server rebuilds the chain; two handovers of one case from the same giver prove who made a double handover.
- **Position**: a signed evidence layer beside the statutory register, not a replacement for it [inference].

## The defence or DSO link

- **Underground ammunition**: SAF with DSTA and SembCorp built the Mandai Underground Ammunition Facility in rock caverns [search summary only] https://sg101.gov.sg/resources/connexionsg/undergroundamm . MINDEF's 1999 factsheet says the rock mass confines the effect of an accidental explosion [search summary only] https://www.nas.gov.sg/archivesonline/data/pdfdoc/MINDEF_19990812003.pdf . DSTA's tunnelling staff wrote the safety slides cited above. A Mandai shotfirer role is named in them [fact].
- **EOD**: SAF disposes of WWII bombs found at civilian construction sites (Woodlands, Ang Mo Kio, Bukit Timah 2023, near Changi T5 in Apr 2026) [search summary only] https://www.malaymail.com/news/singapore/2026/04/04/singapore-disposes-of-250kg-wwii-bomb-near-future-changi-terminal-5-no-impact-on-flights-video/215036 . That is unexploded ordnance, not commercial custody, so the link is thematic only.
- **Counter-diversion**: EU traceability is justified as preventing misuse and theft and helping trace lost or stolen explosives [search summary only, EUR-Lex URL above]; Singapore requires security clearance for staff handling explosives [fact, SPF 2025]. Terrorism-specific sourcing: not found.
- **DSO energetics**: not sourced. [inference] a DSO judge will read "custody of energetic materials" as familiar territory.

## One demo moment

Two phones in airplane mode, dummy items only (a labelled empty case, no explosive content). Storekeeper scans the case seal, shotfirer scans it too, both sign, QR swap; the chain shows "Storekeeper to Shotfirer, 07:12". Shotfirer logs "46 of 50 used, 4 returned" and hands back; the return handover for 3 is rejected as a count mismatch. Then a second phone presents an earlier signed handover of the same case to a different person; on sync, the app prints both signatures and names the double handover.

## Weak spots and sensitivity

- **Few Singapore blasting sites today** [inference]: the cavern jobs found (Mandai, Jurong) date from 1999 to about 2014; no current project found. Users could look like a handful.
- **No Singapore incident found**, so the "gone wrong before" criterion rests on regional cases, mostly summary-only.
- **Offline apps already exist** (Maptek, ExploSKI); do not claim nobody works offline.
- **The chain ends at use**: detonators are consumed, so only the last hop is "expended, N units".
- **Legal accuracy**: say GEWCA 2021 and the 2025 Explosives Regulations, not "Arms and Explosives Act" as current. Do not claim a specific loss-report deadline or record format.
- **Sensitivity**: explosives at a school hackathon. Keep it on accountability, never on obtaining, making, using or evading checks on explosives. Use dummy items and fictional counts, show no real magazine layouts or security details, and say the app would hold sensitive stock data and needs its own protection. Do not name or profile individuals from the cases above.
- **Real use needs approval**: a licensee's records must satisfy SPF; a student app is an add-on evidence layer, not a registered system [inference].

## Not found

- Singapore theft, loss or court cases involving commercial blasting explosives or detonators.
- Field-by-field record rules for issue / use / return, loss-report deadline, stock-take rules, per-detonator marking rule in Singapore (the First and Second Schedules and reg 46 of the 2025 regulations were not retrieved).
- Whether "storekeeper", "magazine keeper" or "person in charge of the blasting site" are official terms.
- Drill-and-blast confirmed for MRT tunnels.
- Any source on current network coverage at Singapore magazines or blast faces (search refused).
- Orica, Dyno Nobel, BME and Maxam products; any vendor's Singapore customers.
- UK HSE and Thailand cases; ATF annual loss statistics; the Higginsville (WA) 100 kg theft and conviction appeared in a search summary but I could not tie it to a page.
- Primary-source read of Directive 2008/43/EC (EUR-Lex was only seen in search results).
- Page content for the NSW detonator alert (403) and the ATF pages (search-summary only).
