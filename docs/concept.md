# Concept: offline custody tokens

6 Nov 2026. **Status:** concept, written before we choose the DSO or Singapore setting, so the items and roles here are deliberately generic. Updated with what authenticator-style codes add (5.8), the ways a handover can travel (5.9), the signing step (5.10), and more threats (6).
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
- a one-time number and an expiry time for the offer, so a photographed or replayed offer can't be used later
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

1. **Offer.** A picks E-104 and B from the roster and confirms with a fingerprint or PIN. A's phone signs an offer and shows it as a QR code. The QR carries the latest checkpoint plus every record since it, so B can see the history. The offer names B, carries a one-time number, and expires after about 60 seconds.
2. **B checks it, offline.** B's phone scans the offer and confirms:
   - the checkpoint is signed by the server
   - every later record is signed correctly and links to the one before
   - A really is the current holder according to those signatures
3. **B ties it to the item.** B scans E-104's tag or seal. The phone checks that the number matches the item.
4. **Acceptance.** B confirms with a fingerprint or PIN, and B's phone signs the acceptance (offer plus seal number plus B's time) and shows it as a QR code. **Liability moves to B the moment B signs.**
5. **A confirms.** A scans B's acceptance QR. Both phones now hold the complete record. A keeps it as a **receipt**, and B holds it as the **token**. Both screens then show the same 6-digit match code, worked out from the record, and A and B check aloud that the codes match (5.10).
6. Both phones put the record in their outbox, ready to sync.

With QR codes this needs no network and no radio, only two scans. Section 5.9 covers Bluetooth, Wi-Fi and NFC as alternatives. **A physical handover is the one moment both phones are sure to be side by side**, so the token can always travel exactly the way the item does.

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

### 5.8 Authenticator codes: what they add, and where they don't fit

**How authenticator apps work** (general knowledge). The open standards are HOTP (RFC 4226) for codes from a counter, and TOTP (RFC 6238) for codes from the time:
1. **Setup, online.** The service shows a QR code containing a random **shared secret**. The app scans it, and from then on both the app and the service hold the same secret.
2. **Afterwards, offline.** The app turns the secret plus the current 30-second time window (TOTP) or a counter (HOTP) into a 6-digit code, using a keyed hash (HMAC).
3. **Checking.** The verifier holds the same secret, computes the same code and compares the two. A match shows the person has the secret.

Step 1 is the same pattern our handover uses: scan a QR code once, then work offline.

**Comparison with our signed handover:**

| | Authenticator code | Signed handover (our design) |
|---|---|---|
| Key | One shared secret, held by both sides | A private key on one phone, plus a public key everyone has |
| Who can produce a valid code or signature | Anyone holding the secret, **including the verifier** | Only the phone holding the private key |
| Can a third party later tell who produced it? | **No.** Either side could have. | **Yes.** The signer can't deny it. |
| What it's tied to | Only the time or the counter | The exact handover: item, giver, receiver, seal, previous record |
| Size | 6 to 8 digits, which can be read aloud, typed or written down | About 64 bytes per signature, so it needs a QR code |
| Needs phone clocks to agree | TOTP yes; HOTP and challenge-based codes no | No |

**For the handover itself, keep signatures.** The whole point of a handover is to prove to someone *else*, later, who accepted liability. A shared-secret code can't do that: B could always say "A could have made that code too".

**Where the idea does add something, in three places:**

1. **A custody check over the radio (stretch goal).** A supervisor somewhere else wants to confirm, offline, that B still holds E-104. A QR code can't cross that distance, but six digits can be read over a radio. This is the challenge-response form of these codes (OCRA, RFC 6287), the same idea as a bank token that ties its code to a transfer's details (general knowledge):
   - The supervisor's phone shows a random challenge, for example `4821`, and the supervisor reads it out.
   - B's phone computes a short code from four things: a secret it shares with the supervisor's phone (handed out at enrolment), the challenge, E-104's ID, and the latest record in B's chain. B reads back, for example, `305 917`.
   - The supervisor's phone computes the same and compares. A match shows **B's phone holds E-104's latest chain right now**. A mismatch means B's phone has an older or forked chain, or isn't B's phone at all.
   - A fresh challenge replaces the clock, so a wrong phone clock doesn't matter, and an old code can't be replayed.
   - **Limits:** it proves B's *phone* holds the chain, not that B holds the *item*. And because the secret is shared, it's a live check, not lasting proof. The signed records remain the proof.
2. **Proof the physical item is there (for production).** A printed QR sticker or seal number can be photographed and "scanned" later from the photo. The authenticator idea fixes this if the **item's own tag makes the code**. Some NFC tags have cryptography built in: NXP's NTAG 424 DNA is one, used against counterfeiting (check the details). They produce a new one-time code on every tap, from a secret inside the chip and a counter. The receiver's phone would put that code in the signed handover, and the server would check it at sync. A photo can't produce a fresh code. This is out of scope for the hackathon because it needs special tags, so printed QR codes and seal numbers stand in.
3. **A voice or paper fallback.** A short code can be spoken or written down, so the radio check still works when someone's only link is a voice call or a written message.

**Practical notes, if we build the radio check:**
- Keep the shared secrets in the Android Keystore, which supports HMAC keys (general knowledge; check current docs).
- Hand out each shared secret at enrolment, online, by scanning a QR code, as authenticator apps do.
- A 6-digit code has a one-in-a-million chance of being guessed, and the supervisor's phone should limit the number of attempts.

### 5.9 How the handover travels: light, radio or a tap

The QR code is only the way the record gets from one phone to the other. **The security comes from the signatures inside it**, so the swap can use whichever way a site allows, and the same signed record travels each way. Think of a letter: the QR code is the envelope, and the signature is the seal on it. Anyone can photograph the envelope, but nobody can forge the seal without the key, which never leaves the giver's phone. Singapore already trusts QR codes for serious work in the same way: Singpass logins and the SGQR payment standard (general knowledge).

The options on Android (general knowledge; check current documentation before building):

| Method | How it works | Does it prove the two people are together? | Where it fits | Hackathon effort |
|---|---|---|---|---|
| **QR code** | One phone shows a code, the other's camera scans it | Mostly. The phones have to be within arm's length. | Sites that don't want radio signals. A QR code sends none, which matters where radio emissions are controlled. It does need a camera, and SAF high-security zones restrict camera phones (fact, 2013 fact sheet). | Low. **The hackathon build.** |
| **Bluetooth or Wi-Fi (Google Nearby Connections)** | The phones find each other and connect directly over Bluetooth, Bluetooth Low Energy or a direct Wi-Fi link. No internet is needed, and the link is encrypted. | No. It works across a room, so the seal scan and the match code (5.10) show the people are together. | Most sites, as a "tap to hand over" convenience | Medium. Needs two real phones (the emulator can't do Bluetooth properly) and Bluetooth and nearby-device permissions. **Stretch goal.** |
| **NFC tap, phone to phone** | One phone acts as a contactless card (host card emulation), and the other reads it | **Yes.** It only works within about 4 cm. | Sites that ban cameras. It feels like contactless payment. | Medium to high. Android removed its built-in phone-to-phone NFC sharing (Android Beam) in Android 10, so we'd write the card emulation ourselves. Testable only on real phones. **Next steps.** |

**Pitch line:** *"The handover can travel by light, radio or a tap, whichever your site allows. It's the same signed record every way."*

### 5.10 Making the handover visibly secure: the signing step

A bare QR code looks casual. The handover screen should look and work like a careful signing step, and every part of it below has a real security purpose:

1. **Who and what, up front.** A card shows the giver's and receiver's names and roles, the item, and the source and seal numbers. The QR code sits inside that card, never on its own.
2. **A fingerprint signs.** The signing key is created so that it only works after a fingerprint or PIN (an Android Keystore key that requires the user to authenticate; general knowledge, check docs). The screen says *"Place your finger to sign this handover."* The person, not just the phone, agrees to each handover.
3. **A 6-digit match code.** After the swap, both screens show the same code, for example `48 21 07`, worked out from the hash of the full signed record. The two people check aloud that the codes match. It's the same idea as matching the codes when pairing a Bluetooth device, or checking safety numbers in Signal. It proves both phones hold the identical record.
4. **A countdown.** The offer shows "Valid for 0:58", so a photographed or relayed offer goes stale within a minute.
5. **A verified state.** After signing, both screens show *"Signed by both · checked offline · 2 of 2 signatures valid"*, plus a short key fingerprint for anyone who wants to inspect it.

**How to talk about it.** Never call it "foolproof": security people distrust that word on sight. Say this instead:

> *"We don't claim nothing can go wrong. We claim nothing can go wrong silently, and when it does, the record shows exactly who did it."*

**Attack it live.** The most convincing demo for a defence judge is the team trying to break its own system:
- **A replayed QR:** scanning a photo of an earlier offer is refused, because it names a different receiver, has expired, and its one-time number is used up.
- **A double handover:** at sync, the fork alert names who did it.
- **Signing on someone else's phone:** the wrong fingerprint is refused, so nothing gets signed.

---

## 6. Failure cases and threats

| Case | What happens |
|---|---|
| The holder's phone is lost | The giver's countersigned receipt still shows who held the item last. A supervisor checks, then reissues custody as a signed event. |
| A phone dies during a handover | The rules in 4.3 decide: the receiver's signature is what moves liability |
| A double handover by a tampered phone | Detected at sync, and the giver's own signatures prove who did it (5.3) |
| Giver and receiver collude | Signatures can't stop two people agreeing to lie. Every step is still recorded, attributed and impossible to deny. |
| A rooted or tampered phone | Keys in the Keystore can't be exported, but a rooted phone can still misbehave. **Tamper-evident, not tamper-proof.** |
| The phone clock is changed | Order comes from the chain, so a changed clock only shows up as a flagged gap (5.5) |
| The wrong item is handed over | The receiver's seal scan doesn't match the item, so the handover can't be signed |
| A photo of the item's QR sticker is scanned instead of the item | Printed tags can be copied this way. Checking the seal number and having a witness make it harder. Tags that make a new code on every tap (5.8) would close the gap in production. |
| A seal is broken or replaced | The custody check flags it for investigation |
| A user's access was revoked while they were offline | The phone doesn't know yet. At sync, the server refuses records signed after the revocation and sets them aside for review (kit "Security extras"). |
| The receiver refuses the item | No acceptance is signed, so the giver still holds it |
| A photo of a handover QR on screen is replayed later | The offer names one receiver, one item and one previous record, carries a one-time number, and expires in about 60 seconds. Only the named receiver's key can accept it. A replayed acceptance is the same signed record, so the server just sees a harmless duplicate. |
| Someone else signs on your phone, or a thief uses it | Signing needs a fingerprint or PIN every time (5.10). A lost phone is revoked, and its records are refused from its next sync. |
| A fake phone pretends to be a team member | Every signature is checked against the roster of public keys saved at the last sync. An unknown key is rejected. |
| Keys are pulled off a rooted phone | Keys are kept in secure hardware where the phone has it (Android StrongBox). In production, the server checks at enrolment that the key really is hardware-backed, using Android key attestation (general knowledge; check docs). |
| A relay: someone streams the offer QR to a receiver who isn't there | The receiver must scan the item's seal, and the offer expires within seconds. An NFC tap (5.9) needs the phones within about 4 cm. |
| Someone listens in on a Bluetooth or Wi-Fi handover | The records aren't secret; what matters is that nobody can change them, and the signatures guarantee that. Nearby Connections encrypts the link anyway. Sensitive details could also be encrypted to the receiver's public key. |

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
- the signing step (5.10): who-and-what card, fingerprint, match code, countdown
- the live attacks (5.10): a replayed QR refused, a double handover named
- stretch goal: the custody check over the radio (5.8)
- stretch goal: handover over Bluetooth or Wi-Fi with Nearby Connections (5.9)

**Out of scope:**
- radio-based swapping (Bluetooth, local hotspot)
- real hardware seals (printed QR codes stand in)
- NFC tags that make a one-time code on every tap (5.8)
- NFC phone-to-phone handover, and hardware key attestation (5.9, 6)
- legal weight of digital signatures
- multiple sites

**Demo, about 3 minutes, both phones in airplane mode:**
1. Store officer A hands E-104 to investigator B. B scans the seal, then they swap two QR codes. B's phone shows *"You hold E-104"*, and A's shows a signed receipt.
2. B hands E-104 to C in the field the same way. The store never saw it.
3. Someone asks A where E-104 is. A shows B's signed receipt, and it's checked offline.
4. A tampered phone tries to hand E-104 a second time from an old copy of the token. Then someone scans a photo of an earlier offer QR, and it's refused: it has expired and names a different receiver.
5. Phones reconnect. The server rebuilds A → B → C on the audit screen and raises a **red fork alert naming who did it**, backed by their own signature.
6. The supervisor resolves it, and the decision joins the chain.

---

## 11. Open questions and assumptions

- **The setting** is the next step: porting this to a DSO or Singapore defence context. Which items (keys, police exhibits, lab samples, classified media, arms and controlled equipment) and which roles?
- **Are phones allowed** where the handovers happen? SAF high-security zones restrict camera phones (fact, 2013 fact sheet). We assume issued phones without cameras, using QR codes shown on screen or an NFC tap.
- **Are tamper-evident seals already used** on these items? We assume yes for evidence (general practice). To confirm.
- **Do the items carry tags, and could those be NFC tags with built-in cryptography** (5.8)? That would decide whether "the item is really there" can be proven, or only claimed.
- **Evidence:** we have no Singapore case yet of a custody dispute caused by being offline. The porting step should look for one.
- **Legal weight:** whether a digital signature from both people would count as an acceptable custody record. We haven't researched this, and won't claim it.
