# Setting: who holds the radioactive source right now?

6 Nov 2026. This applies the custody-token concept (`docs/concept.md`) to one real setting: **the custody of sealed radioactive sources used in industrial radiography.** The research behind it is in `docs/research/contexts/A-radioactive.md`, with a comparison of the other settings in the same folder.

Tags: **fact** (primary or official source, checked), **reported** (news, secondary source, or a third-party copy of a law), **inference** (our reasoning), **general knowledge** (widely known, not sourced here). Regulation numbers come from a third-party copy of Singapore's 2023 regulations. **Verify them on sso.agc.gov.sg before the pitch.**

---

## 0. Summary

Companies in Singapore use small sealed radioactive sources to "X-ray" welds in ships, pipes and tanks. The law makes them record when a source **leaves and returns to the store**, and where it is **once each working day**. It doesn't record **who holds the source while it's out**, or when it passes from one worker to another, and it asks for no signature from the person receiving it. Overseas and in Malaysia, sources have gone missing in exactly that gap, and the losses were often noticed late.

**What we build:** every time a source changes hands, both workers sign the handover on their phones and swap it by QR code, with no network needed. The chain of signatures always shows who is liable. If a source isn't back on time, an alarm goes off. When phones reconnect, the server rebuilds one true record and proves who made any false or double handover.

---

## 1. Background in plain words

### 1.1 What a sealed radioactive source is
A sealed source is a small capsule of radioactive material sealed inside metal. Industry uses it because its radiation passes through steel, a bit like an X-ray (general knowledge). If it's lost, it can seriously injure whoever finds and handles it. A stolen source is also a security worry, because it could be misused in a "dirty bomb" (general knowledge). That's why the international rulebook is the IAEA's *"Code of Conduct on the **Safety and Security** of Radioactive Sources"* (reported, IAEA).

### 1.2 What industrial radiography is
To check a weld in a ship hull, a pipeline or a pressure tank, a radiographer takes a **radiography camera** to the site. The camera is a shielded container with the source inside. The radiographer cordons off the area, places film behind the weld, winds the source out to expose the film, then winds it back into the camera (general knowledge). The camera, with its source, goes back to a secure store at the end of the job.

**This happens in Singapore.** NEA's licence forms list *shipyard* and *refinery* as kinds of business, and *industrial radiography* as a purpose (reported, NEA form via Scribd). Workers doing this register with NEA as radiation workers, with an industrial radiography safety course (reported, GoBusiness listing). Handling and transporting radioactive material needs its own licence, the L6A (reported, GoBusiness listing).

---

## 2. How it maps to the problem statement

> "Build an app that lets **safety personnel** keep completing **security processes** **offline** and **syncs their changes correctly** when internet access returns."

| Phrase | In this setting |
|---|---|
| **Safety personnel** | The **radiation safety officer**, appointed when NEA requires one and approved by NEA (reg 19). The **registered radiation workers** (radiographers) who carry and use the sources. The **store officer** who issues and receives them. |
| **Security processes** | The **security of radioactive sources**: keeping each source under control so it can't be lost, stolen or misused. That means signing it out of the store, carrying it, passing it between workers, returning it, and accounting for where it is every day (regs 26 and 27; NEA requires security measures in use, storage and transport). |
| **Offline** | The work happens inside steel hulls and tanks, out at sea, and in hazardous plant areas. Signal is poor inside steel, and ordinary phones are often restricted in areas with explosive atmospheres (both inference and general knowledge; no Singapore source found). Network outages also happen. |
| **Syncs their changes correctly** | Many people's records of one source, made on different phones at different times, must come together into **one true chain**: nothing lost, nothing doubled, no false handover accepted silently, and every gap or fork flagged. |

---

## 3. How the process works today

| Step | Who | What happens | What the law says gets recorded |
|---|---|---|---|
| 1. Licensing | Company (licensee) | NEA licences it to keep and use sources, and to handle and transport them | Licence inventory of sources and storage locations (reported) |
| 2. Storage | Store officer | Sources are kept in a secure store | Security measures to stop unauthorised access (fact, NEA) |
| 3. Sign-out | Store officer → radiographer | The camera leaves the store for a job | **"All movements into and out of the storage location"**, reg 26(1)(f)(ii) (reported) |
| 4. Carrying it | Radiographer | Moved in a suitable container by **"a radiation worker authorised by the licensee"**, and the person receiving it must be **made aware** it's a sealed source, reg 27 (reported) | Nothing about the handover itself |
| 5. On site | Radiographer crew | Used on the job, and possibly passed between crew members, for example at a shift change | Nothing person-to-person |
| 6. Daily location | Licensee | The location record must be **"kept up to date on each working day"**, reg 26(1)(e) (reported) | One location a day |
| 7. Return | Radiographer → store officer | The camera goes back into the store | The movement into storage, reg 26(1)(f)(ii) |
| 8. Checks | Licensee | Yearly inventory review and leak test for each sealed source (fact, NEA) | Check results |
| 9. Loss | Worker → licensee → NEA | The worker must **"immediately notify the licensee"**, and the licensee must **"immediately notify"** NEA and start searching, reg 26(3)–(4) (reported) | The report |

## 4. The gap

1. **Nobody records who holds the source while it's out of the store.** The law records it leaving and coming back, plus one location a day (reg 26). Handovers between the store and the site, or between workers, leave no record.
2. **Nobody has to sign for it.** Reg 27(c) only requires that the person receiving the source is *made aware* it's a sealed source. Nothing proves they accepted it (we checked the regulation text: no signature or acknowledgement is required).
3. **Records are kept by one side.** The licensee writes them. When a handover is recorded by only one person, accounts can conflict later. Singapore's Court of Appeal threw out capital drug convictions for exactly this reason (section 5).
4. **Nothing raises the alarm when a source doesn't come back.** Losses are found when someone happens to notice. In Western Australia, a missing capsule was only noticed when its container arrived, up to two weeks later (section 5).
5. **The places where handovers happen are often offline**, so an online system would fail at exactly the moment it's needed (inference).

---

## 5. What has gone wrong

### 5.1 Lost and stolen sources

| When | Where | What happened | What it shows |
|---|---|---|---|
| Aug 2018 | Malaysia | A 23 kg radiography device containing iridium-192 went missing in transit, believed to have fallen off a truck. **Singapore tightened its border checks** (reported, [Mothership](https://mothership.sg/2018/08/radioactive-iridium-malaysia-missing/), [FMT](https://www.freemalaysiatoday.com/category/nation/2018/08/24/singapore-alert-over-radioactive-device-missing-in-malaysia)) | Custody during transport was never confirmed. It affected Singapore directly. |
| Feb 2017 | Malaysia | Two radiography projectors containing iridium-192 were stolen from an oil and gas company. They were later found at a flat and a scrapyard, and eight people were arrested (reported, [NST](https://nst.com.my/news/2017/02/212097/eight-nabbed-connection-radioactive-material-theft-case-video)) | A custody lapse at the company |
| Jan 2023 | Western Australia | A caesium-137 capsule was lost from a truck on a trip of about 1,400 km. It was **only noticed missing on arrival**, and a detector vehicle found it by the road on 1 Feb (reported, [WA Health](https://www.health.wa.gov.au/Media-releases/2023/February/Radioactive-capsule-found-in-Pilbara)) | No handover was expected or confirmed on the way, so the loss went unnoticed for about two weeks |
| 2006–2012 | United States | Four thefts of radiography cameras or the trucks carrying them. In 2012, **a camera was left in a truck instead of being returned to storage** (fact, [US GAO-14-293](https://gao.gov/assets/670/664075.txt)) | A return to the store that never happened, and nobody noticed |
| Mar 2023 | Prachinburi, Thailand | A caesium-137 cylinder went missing from a power plant and was traced to a foundry where it had already been melted (reported, [Khaosod](https://www.khaosodenglish.com/news/crimecourtscalamity/2023/03/20/radioactive-cylinder-found-at-prachinburi-plant-poses-no-public-risk-officials-say/)) | Missing for about two weeks before it was reported |
| 1993–2025 | Worldwide | The IAEA says about 55% of 730 recorded thefts happened **during transport** (search summary only; [IAEA](https://www.iaea.org/newscenter/pressreleases/iaea-database-about-55-of-nuclear-and-other-radioactive-material-thefts-since-1993-occurred-during-transport); check before quoting) | The points where a source moves are the weak points |

**The pattern:** sources go missing **while they're away from the store and moving**. The loss is **found late**, and afterwards nobody can show **who last had it**.

### 5.2 One-sided handover records fail, even in Singapore

- ***Mohamed Affandi bin Rosli v Public Prosecutor* [2018] SGCA 87** (fact, [eLitigation](https://www.elitigation.sg/gd/s/2018_SGCA_87)). Two officers gave conflicting accounts of who held drug exhibits between about 6.50 pm and 10.56 pm. One said he handed them over at about 10.47 pm; another said he held them until 10.56 pm. The officer who received them didn't record it in her pocketbook. The Court of Appeal found "two plausible but irreconcilable versions of the chain of custody" and set the capital convictions aside.
- **Singapore Police Force release, 11 Aug 2023** (fact, [SPF](https://www.police.gov.sg/media-hub/news/2023/20230811_two_men_charged_for_criminal_breach_of_trust_related_offences)). An investigation officer allegedly **forged acknowledgement receipts** so that his supervisor would believe restitution processes had been followed. It was found years later, through case reviews.

These aren't radioactive cases. They prove the **mechanism**: a handover recorded by one person, or a receipt only one person controls, can't be trusted later. **A handover that both people must sign closes that gap.**

---

## 6. Today's solutions, and why they don't close the gap

| Solution | What it does | Why it isn't enough |
|---|---|---|
| Store register and records under reg 26 (paper or spreadsheet; what Singapore firms actually use isn't public) | Records sign-out, return and a daily location | Doesn't see handovers away from the store, and needs no signature from the receiver |
| GPS trackers on trucks or cameras | Shows where a vehicle is | Tracks the truck, not **who holds the source**. Needs a network to report. |
| National tracking systems (for example the US National Source Tracking System) | Tracks transfers between licensed companies, reported by the next business day (reported, research notes) | Works company to company, not person to person, and only by later reporting |
| Electronic key and asset cabinets (for example Traka) | Logs who took an item from the cabinet | **The log ends at the cabinet door.** A hand-on to someone else is invisible (inference, research notes E). |
| Any online custody app | Records handovers in a central system | Fails where there's no network, which is where the handovers happen |

---

## 7. Our solution

### 7.1 In one sentence
**Every time a radiography camera changes hands, the giver and the receiver both sign the handover on their phones and swap it by QR code, with no network. The latest signed handover shows who is liable now. A return that's overdue raises an alarm, and when phones reconnect the server rebuilds one checked chain.**

### 7.2 The parts
- **The item:** a radiography camera, with the serial number of the source inside it. It carries a QR tag and a tamper-evident seal.
- **Each person's phone:** holds a private signing key that can't be exported, plus the public keys of everyone on the team, so any phone can check any signature offline.
- **The chain:** the linked list of signed handovers for each camera. This **is** the custody token (`concept.md` §3).
- **The expected return:** set at sign-out, for example "back by 19:00". Every phone in the chain knows it.
- **The server:** rebuilds the chain from whatever phones sync, checks every signature and link, raises fork and overdue alerts, and produces the records the law requires.

### 7.3 A day with the app

People: **Siti** is the store officer, **Ravi** and **Wei Ming** are radiographers, and **Dr Tan** is the radiation safety officer. The item is **camera C-12**, containing source **IR-0457**. The job is weld inspection inside a tank at a shipyard.

1. **07:30, sign-out at the store (online).** Siti picks C-12 and Ravi. Ravi scans C-12's seal, and they swap two QR codes. Both phones show the signed handover. **Return due: 19:00.** The server records the movement out of storage (reg 26).
2. **09:00, work inside the tank (offline).** Ravi's phone shows *"You hold C-12. Return by 19:00."* There's no signal inside the steel tank, and it doesn't matter.
3. **13:00, shift change in the yard (offline).** Ravi hands C-12 to Wei Ming. Wei Ming scans the seal, which matches, and his acceptance screen says *"This is sealed source IR-0457"*, satisfying reg 27(c) on the record. They swap QR codes, and **liability moves to Wei Ming the moment he signs.** Ravi keeps the signed receipt.
4. **15:30, "Who has C-12?"** Dr Tan radios Ravi. Ravi shows Wei Ming's signed acceptance, and it's checked offline. Dr Tan can also run the six-digit radio check (`concept.md` §5.8) to confirm that Wei Ming's phone holds C-12's latest chain.
5. **19:00, overdue.** Wei Ming's phone alarms on its own, offline: *"C-12 return overdue."* The server, which hasn't heard from Wei Ming's phone, flags Dr Tan's dashboard: *"C-12 overdue. Last holder the server knows of: Ravi (as of 07:30). Newer handovers may not have synced."*
6. **19:40, return and sync.** Wei Ming hands C-12 back to Siti. She scans the seal, which matches, and they swap QR codes. Phones sync. The server rebuilds the chain Siti → Ravi → Wei Ming → Siti, clears the alarm, and produces today's location record and the movement back into storage (reg 26).

If C-12 had not come back, Dr Tan would have known **within minutes of 19:00 who last signed for it**, instead of finding out days later.

### 7.4 What each person sees
- **Store officer:** every camera, who holds it according to the latest signature, and when it's due back. Issue and receive screens.
- **Radiographer:** what I hold, when it's due back, and the handover screen (scan the seal, sign, swap QR codes).
- **Radiation safety officer:** the chain for each camera, plus overdue alarms, fork alerts and seal mismatches, and the reg 26 records exported.
- **On every screen:** online or offline, the number of changes waiting to sync, and when this phone last synced.

### 7.5 How it fits the existing rules
- **Reg 26(1)(e) and (f):** the daily location record and the movements in and out of storage are **produced from the chain**, not typed by hand.
- **Reg 27(b) and (c):** only authorised radiation workers on the roster can accept a handover, and the acceptance screen states that the item is a sealed source.
- **Reg 26(3) and (4):** a "Report missing" button sends the last signed holder and the full chain to the licensee straight away, and the report waits for the next connection if needed.

### 7.6 Hard cases (detail in `concept.md` §4.3, §5 and §6)
| Case | What happens |
|---|---|
| A phone keeps a copy and "hands over" C-12 twice | At sync the server sees two handovers signed by the same person, both following the same previous record. That fork proves who did it, from their own signature. |
| A handover is cut short | Liability moves the moment the receiver signs. If the giver never scans the acceptance, it still reaches the server from the receiver's phone. |
| The wrong camera is handed over | The seal doesn't match, so the handover can't be signed |
| A photo of the QR tag is used | A seal number check helps. In production, NFC tags that make a new code on every tap would close it (`concept.md` §5.8). |
| A phone is lost | The giver's countersigned receipt still names the last holder. The radiation safety officer reissues custody as a signed event. |
| A worker's access was revoked while offline | At sync, handovers they signed after the revocation are set aside for review |
| A phone clock is changed | Order comes from the chain, not the clock, and a large time gap is flagged |

---

## 8. Why this counts as "syncing correctly"

| Property | How it's met here |
|---|---|
| 1. Nothing lost | Each handover is stored on both phones, so either one can sync it |
| 2. Nothing doubled | A handover synced twice is the same signed record, so it's kept once |
| 3. No silent overwrite | Conflicting claims (a fork) are both kept and shown to the radiation safety officer |
| 4. The rules always hold | One holder at a time, and only authorised workers can accept. A fork or seal mismatch is never accepted silently. |
| 5. An honest history | Every step is signed by both people and linked to the one before, with the phone's time and the server's arrival time both kept |

Plus one property that is specific to this setting: **a missing handover is noticed.** The overdue alarm turns "noticed weeks later" into "noticed at 19:00".

---

## 9. Why this setting

| Judging criterion | How the setting helps |
|---|---|
| **Functionality (10)** | A small data model (cameras, sources, people, handovers, due times) leaves build time for the sync and signature engine. Every step can be demoed live in airplane mode. |
| **Originality (5)** | Handovers that work offline, signed by both people, with proof of who made a double handover. The tools we found track stores, cabinets or trucks, not people. |
| **Design (5)** | Three clear roles, one handover screen, and an alarm everyone understands |
| **Pitching (5)** | It uses the challenge's own two words ("safety and security"), comes with a Singapore court case, and asks a question anyone gets: *"Who has the radioactive source right now?"* |
| **Sudden Dev (5)** | The same engine handles any controlled item, and new rules (for example two-person sign-out) slot in |

**For DSO:** DSO's National Security Office works on countering CBRN threats, which include radiological ones (fact, dso.org.sg). Keeping radioactive sources accounted for is part of preventing their misuse. The design is also self-hostable and works offline, matching how defence keeps sensitive systems separate.

---

## 10. What we are not claiming
- We don't know how Singapore companies keep these records today. We only know what the law requires.
- We found **no lost source in Singapore**. Our incidents are from Malaysia, Australia, Thailand and the US. We say so.
- We don't claim every site is offline. We say handovers often happen where signal is poor or phones are restricted, and that the signed handover is worth having even online (section 5.2).
- **Tamper-evident, not tamper-proof.** A rooted phone can misbehave. What it can't do is fake someone else's signature.
- The demo uses **dummy items only**. We describe the security of sources only at the level of public rules.

## 11. Verify before the pitch
- Reg numbers 19, 26 and 27 on sso.agc.gov.sg (we read a third-party copy)
- The IAEA "55% of thefts during transport" figure, on the IAEA page itself
- Whether radiography sites in Singapore (shipyards, Jurong Island) restrict phones. Ask a mentor.
- If possible, ask the DSO mentor: *"Is radioactive-source custody a process DSO's National Security Office cares about, and who would use this?"*

## 12. Demo script, about 3 minutes
1. **The hook (20 s).** "Singapore's Court of Appeal threw out convictions because two officers' records of one handover didn't match. Now picture that item is a radioactive source."
2. **Sign-out (30 s).** Siti's phone issues C-12 to Ravi: seal scan, two QR codes. Then **airplane mode on, on every phone.**
3. **Handover in the yard (40 s).** Ravi hands C-12 to Wei Ming. The seal is scanned, and the acceptance screen states it's a sealed source. Ravi shows the signed receipt.
4. **The cheat (30 s).** A tampered phone tries to hand C-12 a second time from an old copy of the token.
5. **Overdue (20 s).** The clock passes 19:00, and Wei Ming's phone alarms with no network.
6. **Reconnect (40 s).** Airplane mode off. The supervisor's screen rebuilds Siti → Ravi → Wei Ming and shows a **red fork alert naming the cheat**, backed by their own signature. The reg 26 record is produced automatically.

## 13. Sources
- Singapore regulations (third-party copy of S 85/2023): https://littdb.sfo2.cdn.digitaloceanspaces.com/litt/SG/SSOStatutes/subsidiary_legislation/RPA2007-S85-2023.html
- NEA summary of the 2023 regulations: https://www.nea.gov.sg/our-services/radiation-safety/regulatory-information/summary-of-ionising-radiation-regulations-2023
- NEA licence listings: https://www.gobusiness.gov.sg/browse-all-licences/National-Environment-Agency-(NEA)/IR2---Licence-to-keep--possess-for-use-(other-than-sale)-or-use-irradiating-apparatus-or-radioactive-materials ; https://govtech-gobusiness-vica.netlify.app/browse-all-licences/National-Environment-Agency-(NEA)/L6A---Licence-to-handle-and-transport-radioactive-materials ; https://govtech-gobusiness-vica.netlify.app/browse-all-licences/National-Environment-Agency-(NEA)/R1--Registration-as-a-radiation-worker ; https://es.scribd.com/document/335902717/Singapore-NEA-L3-License-for-Individual-Equipment-Owner
- Court of Appeal, [2018] SGCA 87: https://www.elitigation.sg/gd/s/2018_SGCA_87
- SPF, 11 Aug 2023: https://www.police.gov.sg/media-hub/news/2023/20230811_two_men_charged_for_criminal_breach_of_trust_related_offences
- Incidents: see the table in section 5.1
- DSO National Security Office: https://www.dso.org.sg/works/national-security-office
- Full research notes: `docs/research/contexts/A-radioactive.md`
