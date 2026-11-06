# Concept: offline custody tokens

6 Nov 2026. **Status:** concept, written before we choose the DSO or Singapore setting, so the items and roles here are deliberately generic.
Background research, with sources: `docs/research/brief.md` and the five track notes beside it.
Tags where a claim matters: **fact** (primary source), **reported** (secondary source), **inference** (our reasoning), **general practice** (widely known, not sourced in our research).

---

## 1. In one sentence

When an item such as a key, a piece of evidence or a controlled piece of equipment changes hands, the giver and the receiver both sign a digital handover on their phones and swap it directly, with no network. The chain of signed handovers travels with the item and proves who is liable for it at every moment. When phones reconnect, the server rebuilds the chain, checks it, and proves who caused any break.

The challenge we're answering: *"Build an app that lets safety personnel keep completing security processes offline and syncs their changes correctly when internet access returns."*

---

## 2. The problem

### 2.1 How custody works with a network

A custodian (for example a store officer) issues an item to a receiver (for example an investigator). The system records it: *A gave E-104 to B, and B accepted.* From then on everyone can see that B holds E-104 and is liable for it. Every later handover, whether B to C or C back to the store, goes through the system the same way.

### 2.2 What breaks without a network

The handover still has to happen, because the investigator needs the item now. But the system can't be updated at that moment, so:

- **The giver can't prove they gave it away.** If A is later asked where E-104 is, all A has is their word. If the item is valuable, A could be suspected of losing it or keeping it.
- **The receiver can deny receiving it.** Nothing binds B to the handover.
- **A third person asking "who has E-104?"** can't check anyone's claim.
- **Handovers between holders are invisible.** If B passes E-104 to C in the field, the store's records never see it.

So the security process (passing liability from one person to the next) either stops, carries on based on trust, or falls back to paper.

### 2.3 Why today's workarounds fall short

| Approach | Works offline? | Proves who's liable? | Can people elsewhere see it? | Weakness |
|---|---|---|---|---|
| A logbook at the store | Yes | Only for handovers at the store | No | Can't see B handing to C in the field |
| A paper chain-of-custody form that travels with the item (general practice) | Yes | Yes, by signatures | No | Signatures can be forged, and the form can be lost, altered or recreated. Nobody sees it until it comes back. |
| An online custody system | **No** | Yes | Yes | Every handover needs a network |
| An offline app with an upload queue (how today's vendors work, per track D) | Yes | **No.** It records claims, not agreement. | Later | No proof from the receiver, and clashes are handled silently or not at all |

### 2.4 The one-book test

Could this process run correctly with one book in one place? **No.** The item moves away from the store, and handovers happen wherever the people are. That's why the shared record and its sync are needed here at all (see `brief.md` and the reasoning behind the one-book test).

---

## 3. The idea

### 3.1 Terms

| Term | Meaning |
|---|---|
| **Item** | The physical object under custody, such as Item E-104 |
| **Tag or seal** | A QR or NFC tag on the item, or the number on a tamper-evident seal. It identifies *this* physical object. |
| **Holder** | The person currently liable for the item |
| **Handover record** | One signed record: *this item passes from giver to receiver* |
| **Custody chain (the token)** | The linked series of handover records for one item. **The chain is the token.** |
| **Device key** | A private signing key made on each phone at enrolment, kept in the Android Keystore so it can't be exported (general knowledge; check current docs) |
| **Checkpoint** | A statement signed by the server: *as of this moment, E-104 is held by A, and the chain so far is valid* |

### 3.2 What a handover record contains

- the item's ID, and the tag or seal number the receiver scanned
- the giver and the receiver
- the giver's phone time, and the receiver's phone time (both are only claims)
- a **link to the previous record** (its hash), so the records form a chain
- a sequence number for each phone
- **the giver's signature and the receiver's signature**

That's a few hundred bytes, small enough for a single QR code (inference from QR capacity, which is roughly 2.9 KB at the largest size).

### 3.3 Online and offline behaviour

| | With a network | Without a network |
|---|---|---|
| Handover | Signed by both, **checked by the server straight away** | Signed by both, swapped phone to phone, **checked by the server at sync** |
| A double handover | **Prevented**: the server refuses the second one | **Proven afterwards**: the server finds the fork and the giver's own signatures show who did it |
| "Who has E-104?" | The server answers | Any phone answers by checking the latest signed record |

---

## 4. How it works, step by step

### 4.1 Setup, while online

1. Each officer enrols their phone. The phone makes its device key, and the server records the matching public key.
2. Every phone downloads the **roster**, meaning everyone's public keys, plus the **server's public key**. That lets any phone check any signature offline.
3. Each item gets a tag or seal, registered on the server. The server signs a first checkpoint: *E-104 is held by the store.*

### 4.2 A handover (works offline)

Example: store officer A gives E-104 to investigator B.

1. **Offer.** A picks E-104 and B from the roster. A's phone signs an offer and shows it as a QR code. The QR carries the latest checkpoint plus every record since it, so B can see the history.
2. **B checks it, offline.** B's phone scans the offer and confirms:
   - the checkpoint is signed by the server
   - every later record is signed correctly and links to the one before
   - A really is the current holder according to those signatures
3. **B ties it to the item.** B scans E-104's tag or seal. The phone checks that the number matches the item.
4. **Acceptance.** B's phone signs the acceptance (offer plus seal number plus B's time) and shows it as a QR code. **Liability moves to B the moment B signs.**
5. **A confirms.** A scans B's acceptance QR. Both phones now hold the complete record. A keeps it as a **receipt**, and B holds it as the **token**.
6. Both phones put the record in their outbox, ready to sync.

This needs no network and no radio, only two QR scans. **A physical handover is the one moment both phones are sure to be side by side**, so the token can always travel exactly the way the item does.

### 4.3 If the handover is cut short

- **B never signs:** A still holds E-104.
- **B signs, but A doesn't scan the acceptance:** B is liable, because B's signature exists. A's phone shows *"Handover not confirmed. Scan B's acceptance."* If A never scans it, the record still reaches the server from B's phone at sync, and the handover completes there.

### 4.4 "Where is E-104?", offline

Anyone can ask any phone in the chain. A shows the receipt B signed. The person asking checks B's signature against the roster on their own phone. No network is needed, and nobody has to take anyone's word for it.

### 4.5 Return to the store

The item comes back with the same kind of handover (holder to store officer), and the chain closes.

### 4.6 Sync and reconciliation

Whenever any phone gets a connection, it sends its outbox, using the kit's `sync_push`. For each handover record, the server:

1. checks both signatures against the roster
2. checks the link: the record must follow the latest record the server knows for that item
3. checks the seal number matches the item
4. **applies it** if all three checks pass, or **flags it** if not
5. **detects forks**: two records from the same giver that both follow the same previous record
6. signs a **new checkpoint** for the item

Records can arrive in any order, from any phone. Even if B, C and A sync hours apart, the server rebuilds the same single chain.

### 4.7 The custody check after reconnecting

When the server learns of a new holder, it asks that holder for a **custody check** within a set time: re-scan the seal, ideally with a witness. A photo is optional supporting evidence. An overdue check, a different seal number or a broken seal is flagged for a supervisor.

---

## 5. Design decisions, and why

### 5.1 The proof is the receiver's signature, not the missing token

An empty phone proves nothing: A could have hidden a copy, or given the item to someone else. What proves A's case is the **positive** record: B's signature accepting E-104 at 14:05. It works like signing for a parcel. Once B has signed, the liability is B's and B can't deny it. A also can't invent a handover, because A can't forge B's signature.

### 5.2 Why not an NFT or a blockchain

An NFT is the right intuition: one unique token whose owner everyone can check. But it only works because a blockchain records every transfer, and a blockchain needs a network to do that. Offline, an "NFT" is just a file, and files can be copied. A chain of records that both people sign gives the same properties without any network. In the pitch, call it a **signed custody token**, "like an NFT without the blockchain". A DSO engineer may hear "NFT" as a buzzword.

### 5.3 Copies can't be prevented offline, but double handovers can be proven

**Example:** A's phone is tampered with and keeps a copy of the token. While offline, A gives E-104 to B, then "gives" it to C as well. B and C each see a valid chain, so neither can tell offline. Software alone can't stop this. Offline digital cash has the same "spend it twice" problem and usually needs secure hardware to prevent it (general knowledge).

**What the design can do:** once either B's or C's record syncs, the server holds **two records signed by A that both follow the same previous record**. That fork is proof of a double handover, and **A's own signatures show A did it.** Any phone that ends up holding both branches can detect it too, even offline.

**Pitch line:** *"We can't stop a double handover offline, but we can prove who did it, with their own signature."*

### 5.4 Tying the token to the physical object

A token without the object is worthless, and a photo can be old, staged or of a look-alike. So:
- **at every handover**, the receiver scans the item's tag or **tamper-evident seal**, and that number goes into the signed record
- **at custody checks**, the seal is re-scanned
- **a new seal** (for example after an exhibit is opened for examination) is recorded as its own signed event by the holder and a witness

### 5.5 Ordering without trusting clocks

Phone times are kept, but only as **claims**. The real order comes from the chain links and each phone's sequence numbers. The server also records when each record arrived (kit rule 8). A big gap between a record's phone time and its arrival time is flagged for review, but the record is still accepted.

### 5.6 Server checkpoints keep the chain short

Without checkpoints, every offer would have to carry the item's whole history. A checkpoint signed by the server says "everything up to here is valid", so the offer QR only carries the checkpoint plus the few records since it. Every phone can check the checkpoint using the server's public key.

### 5.7 What the server decides, and what a person decides

- **The server decides** whether signatures and links are valid, applies good records, refuses bad ones, finds forks and assigns liability from the signatures.
- **A supervisor decides** where the item actually is after a fork or a seal problem, and records that decision as a signed event in the chain. The server never guesses.

---

## 6. Failure cases

| Case | What happens |
|---|---|
| The holder's phone is lost | The giver's countersigned receipt still shows who held the item last. A supervisor checks, then reissues custody as a signed event. |
| A phone dies during a handover | The rules in 4.3 decide: the receiver's signature is what moves liability |
| A double handover by a tampered phone | Detected at sync, and the giver's own signatures prove who did it (5.3) |
| Giver and receiver collude | Signatures can't stop two people agreeing to lie. Every step is still recorded, attributed and impossible to deny. |
| A rooted or tampered phone | Keys in the Keystore can't be exported, but a rooted phone can still misbehave. **Tamper-evident, not tamper-proof.** |
| The phone clock is changed | Order comes from the chain, so a changed clock only shows up as a flagged gap (5.5) |
| The wrong item is handed over | The receiver's seal scan doesn't match the item, so the handover can't be signed |
| A seal is broken or replaced | The custody check flags it for investigation |
| A user's access was revoked while they were offline | The phone doesn't know yet. At sync, the server refuses records signed after the revocation and sets them aside for review (kit "Security extras"). |
| The receiver refuses the item | No acceptance is signed, so the giver still holds it |

---

## 7. How it matches the problem statement

| Phrase | How the idea answers it |
|---|---|
| **"safety personnel"** | Custodians: store officers, evidence officers, key controllers, and the investigators and officers who receive items |
| **"keep completing security processes"** | Chain of custody, meaning who is liable for an item at every moment. Normally it **needs the system** at each handover, and this idea makes it work without one. An unbroken chain is what makes evidence usable (general practice). |
| **"offline"** | Handovers happen wherever people are: in the field, on site, or in secure zones with no network. The two QR scans need no network or radio. |
| **"syncs their changes correctly when internet access returns"** | The server rebuilds one chain from records synced by many phones, in any order. It applies valid records, refuses broken links, and **proves** who caused any fork. |

The five properties of "correct" sync (from `brief.md`):

| # | Property | How this idea delivers it |
|---|---|---|
| 1 | Nothing lost | Every handover is held on both phones, so either can sync it |
| 2 | Nothing doubled | A record synced twice is the same signed record, so the server keeps one |
| 3 | No silent overwrite | Two claims about the same item (a fork) are both kept and shown |
| 4 | The rules always hold | One holder at a time, and every link signed by both people. A fork is never accepted silently. |
| 5 | An honest history | Every step is signed and linked, with phone time and arrival time both recorded |

**Why it needs a shared database:** each phone holds only its own part of the chain. Only the shared database sees every part, which it needs to rebuild the full chain and find forks.

---

## 8. Against the judging criteria

| Criterion (points) | How the idea earns it | Risk |
|---|---|---|
| **Functionality (10)** | Small data model (items, seals, handover records, checkpoints, flags). The demo is two QR scans plus a sync, all repeatable live in airplane mode. | Signing and checking chains are new code. Unit-test them first, against the fake server the kit already plans. |
| **Originality (5)** | Handovers that work offline because both people sign, plus **proof of who caused a double handover**. Today's tools only queue uploads (track D). | Pitch it as a signed custody token, not an NFT |
| **Design (5)** | One clear handover screen (scan, sign, scan), a custody timeline for each item, a red fork alert naming who did it | Keep it to about five screens |
| **Pitching (5)** | "Signing for a parcel, but it works with no network and can't be forged." The hook is liability, which everyone understands. | We have no sourced Singapore case yet. That's for the porting step. |
| **Sudden Dev (5)** | Any item type, and any new rule (for example a witness required for certain items) fits the same chain | A twist that needs phones to stay in step while far apart (we only swap at handovers) |

---

## 9. Fit with our Android kit

- **Room** holds items, handover records and checkpoints. Every new record goes into the **outbox** (kit rules 1 to 3).
- **`sync_push`** gets one more clash rule, using the rule we added to `stack.md` rule 4: *an item's custody chain must not fork, and every signature must verify.* A fork becomes a conflict for a supervisor.
- **Signing:** ECDSA P-256 keys in the Android Keystore. Public keys come down with the roster at sync.
- **The swap:** QR codes, shown on one phone and scanned by the other. A QR or barcode library is a new dependency, so the build account adds it to the shared files.
- **No new database** and no blockchain. Supabase stays the server and can run on an organisation's own machines.

---

## 10. Hackathon scope and demo

**In scope:**
- one item type
- enrolment and roster
- the handover (offer, seal scan, acceptance)
- the custody timeline
- sync with server checks and checkpoints
- the fork alert
- the custody check
- a supervisor's decision

**Out of scope:**
- radio-based swapping (Bluetooth, local hotspot)
- real hardware seals (printed QR codes stand in)
- legal weight of digital signatures
- multiple sites

**Demo, about 3 minutes, both phones in airplane mode:**
1. Store officer A hands E-104 to investigator B. B scans the seal, then they swap two QR codes. B's phone shows *"You hold E-104"*, and A's shows a signed receipt.
2. B hands E-104 to C in the field the same way. The store never saw it.
3. Someone asks A where E-104 is. A shows B's signed receipt, and it's checked offline.
4. A tampered phone tries to hand E-104 a second time from an old copy of the token.
5. Phones reconnect. The server rebuilds A → B → C on the audit screen and raises a **red fork alert naming who did it**, backed by their own signature.
6. The supervisor resolves it, and the decision joins the chain.

---

## 11. Open questions and assumptions

- **The setting** is the next step: porting this to a DSO or Singapore defence context. Which items (keys, police exhibits, lab samples, classified media, arms and controlled equipment) and which roles?
- **Are phones allowed** where the handovers happen? SAF high-security zones restrict camera phones (fact, 2013 fact sheet). We assume issued phones without cameras, using QR codes shown on screen or an NFC tap.
- **Are tamper-evident seals already used** on these items? We assume yes for evidence (general practice). To confirm.
- **Evidence:** we have no Singapore case yet of a custody dispute caused by being offline. The porting step should look for one.
- **Legal weight:** whether a digital signature from both people would count as an acceptable custody record. We haven't researched this, and won't claim it.
