# Setup checklist (for you, not the AIs)

Every step says where to click or what to type. Where a menu label may look slightly different, the step says what to look for.

**Names used here**
- **Account A** is the design account (claude.ai and Claude Design). Email: ________
- **Account B** is the build account (Claude Code). Use the account the Claude desktop app is signed into. Email: ________
- **P** is the teammate on product and pitch; **Q** is the teammate on testing and data.

**Where the project lives:** `C:\Users\leeju\Desktop\nushhack26`, which has its own git repository (`allenleejy/nushhack26` on GitHub). Your user folder `C:\Users\leeju` is also a git repository (it holds your DigiGallery history), but git uses the closest one, so work inside `nushhack26` stays in `nushhack26`.

**When to do what**
- **Part 0, now:** the switch to an Android app. Do this before the kickoff.
- **Part 1, Thursday (about 30 minutes):** the Claude accounts, the claude.ai project and the timers.
- **Part 2, Friday morning (about 45 minutes):** Supabase, Devin, phones and the rest of the technical setup.
- **Part 3, the kickoff.**
- **Part 4, before you sleep (about 5 minutes):** re-arm the timers.

---

## Part 0: Switching to an Android app (Fri 6 Nov, about 15 minutes)

On Friday afternoon we switched from a web app to a native Android app. Parts 1 to 3 below are updated for it. If you did Parts 1 and 2 before the switch, do these:

1. **Update the design account's files.** In the `Claude A` window, open the **Hackathon** project. In its knowledge panel, delete the old kit files, then upload all seven `.md` files from `C:\Users\leeju\Desktop\nushhack26\hackathon-kit` (not the `templates` folder). If the design account already has a chat going, tell it: "We're building a native Android app now. Re-read role-design.md and playbook.md from the project files."
2. **Supabase keys.** Do Part 2, step 1, items 6 and 7 if you haven't: you need the project URL and the publishable key. The kickoff reads them from `local.properties` (Part 3, step 1).
3. **Devin.** Do Part 2, step 2, item 3: turn on Desktop mode.
4. **Phones.** Do Part 2, step 3: USB debugging on both Android phones.
5. **Skip Vercel.** We no longer need it. If you already imported the repo there, delete that Vercel project (**Settings → Delete Project**).

Then go to Part 3.

---

## Part 1: Thursday (about 30 minutes)

### 1. Two browser profiles, so both Claude accounts stay signed in (5 min)

Skip this if you've already done it.

1. In Chrome, click your profile picture (top right), then **Add** (or **Add Chrome profile**), then **Continue without an account**. Name the profile `Claude A`.
2. In the `Claude A` window, go to https://claude.ai and sign in with **Account A**.
3. Your normal Chrome profile stays signed in to **Account B**.

### 2. Check both accounts' weekly limits (5 min)

In each profile:

1. Open https://claude.ai/settings/usage.
2. Note when the **weekly** limit resets and how much of the week is already used:
   - Account A: resets ________, used ____%
   - Account B: resets ________, used ____%
3. Use both accounts as little as possible until Friday's timers.
4. **Optional:** in **Usage credits**, turn them on with a monthly cap (for example $10). They're only spent after the plan's own limit runs out.

### 3. The claude.ai project for the design account (5 min)

In the `Claude A` window:

1. In the left sidebar, open **Projects**, click **Create project**, and name it `Hackathon`.
2. Open the project. Find the instructions box (**Set project instructions** or **Instructions**), paste the text below, and save:
   ```
   You are the design account for our hackathon team. Your brief is in this project's files: read README.md, role-design.md and playbook.md, and follow them in every chat.
   ```
3. Upload the kit:
   1. In the project's knowledge panel, click **+** (or **Add content**), then **Upload from device**.
   2. Open `C:\Users\leeju\Desktop\nushhack26\hackathon-kit`.
   3. Select all seven `.md` files at the top level (not the `templates` folder) and upload them.

**Uploaded the kit before Friday afternoon?** It changed for the Android switch. See Part 0.

### 4. Timers that open each 5-hour window on Friday (10 min)

A 5-hour window starts at an account's first message. A scheduled task can send that first message at an exact time, even while your laptop is closed. Make one in each account: first in the `Claude A` window (Account A), then in your normal profile (Account B).

1. Go to https://claude.ai/code/routines and click **New task**.
   - If the button says **New routine**, click it. A full page opens with the same fields.
   - If a menu offers **Create with Claude** and **Set up manually**, choose **Set up manually**.
2. **Name:** `Prime window`
3. **Prompt** (on the full page, this box is called **Instructions**): `Reply with the single word OK.`
4. **Model:** in the model selector inside the prompt box, choose **Haiku 5.5**, the cheapest. If it also shows **Effort**, choose the lowest level.
5. **Trigger:** choose **Schedule**, then **One-time** (or **Once**). Set the date to **Fri 6 Nov** and the time, in your local time:
   - **11:05** for Account A
   - **13:35** for Account B

   Avoid 11:00 and 13:30: runs set exactly on the hour can start several minutes late.
6. **Repository:** leave it empty. Without one, the task runs as a plain chat, which is all it needs.
   - If the form shows **Repositories** and **Environment** in its main view and won't continue without a repository, pick any small repo you own. The prompt never touches it.
7. **Connectors:** expand **Advanced settings**, open its **Connectors** tab, and remove every connector.
8. Click **Create**. The task's page opens. Check that it shows the run on Fri 6 Nov at the right time.

Don't click **Run now**. Part 2 checks on Friday that each timer ran.

### 5. Send the kit to the team (2 min)

Send `README.md` and `playbook.md` to P and Q, so they can read them tonight.

---

## Part 2: Friday morning (about 45 minutes)

**No Claude messages before the timers.** Send nothing on Account A before 11:05, and nothing on Account B before 13:35. That includes the Claude desktop app, which is signed in to Account B. An early message starts that account's window early and breaks the stagger. Steps 1–4 don't use Claude. If you need Claude's help in the morning, use Account A after 11:05.

### 1. Supabase (15 min)

1. Go to https://supabase.com/dashboard and sign in with GitHub.
2. If you're asked to create an organization, name it `hack` and choose the **Free** plan.
3. Click **New project** and fill in:
   - **Name:** `hack-app`
   - **Database password:** click **Generate** and save it in your password manager
   - **Region:** the one closest to the venue

   Click **Create** and wait about 2 minutes.
4. Go to **Authentication → Sign In / Providers**:
   - turn on **Allow anonymous sign-ins**
   - open the **Email** provider and turn off **Confirm email**
   - save
5. Go to **Authentication → Rate Limits**, raise the anonymous sign-in limit from 30 to **300** per hour, and save. Everyone on the venue Wi-Fi shares one IP address.
6. Click **Connect** at the top of the project page. Copy the **project URL** (`https://xxxx.supabase.co`) and the **publishable key** (it starts with `sb_publishable_`; older projects call it the anon key) into your password manager.
7. Go to **Project Settings → API Keys** and copy the **secret** key (it starts with `sb_secret_`) into the password manager. It never goes into the app, the repo or Devin.
8. Note the **project ref**: the `xxxx` in `https://xxxx.supabase.co`. ________

### 2. Devin (5 min)

1. Go to **Settings → Connections → GitHub** and click **Connect** or **Configure**. On GitHub's page, set **Repository access** to **All repositories** and save.
2. In **Settings → Preferences**, make sure **Auto-approve child sessions** is **on**.
3. In your organisation's settings, turn on **Desktop mode**. Devin needs it to see and tap the Android emulator.
4. Find where Devin shows your usage and note when the daily allowance refreshes: ________

### 3. Phones (10 min)

You need two Android phones for the demo (the laptop's emulator can stand in for the second), plus USB cables, chargers and a phone hotspot in case the venue Wi-Fi fails. On each phone:

1. **Developer options:** open **Settings → About phone** and tap **Build number** 7 times. On some phones it's under **Software information**.
2. **USB debugging:** open **Settings → System → Developer options** (or search Settings for "Developer options") and turn on **USB debugging**.
3. Plug the phone into the laptop and tap **Allow** on the "Allow USB debugging?" prompt. Tick **Always allow from this computer** first.
4. **Installing APKs:** the first time you open a downloaded APK, the phone asks to allow installs from that app (Chrome or Files). Allow it.
5. To download releases on the phone, sign in to GitHub in its browser. (The repo is private.)

### 4. Gemini key (1 min)

Make sure your Gemini API key is in your password manager. AI is optional for this app: the build account asks for the key only if the chosen idea has an AI feature. You don't need any other AI accounts.

### 5. Fill in the blanks (2 min)

Open the build account's brief:
```powershell
notepad C:\Users\leeju\Desktop\nushhack26\hackathon-kit\role-build.md
```
Under **Fill-ins**, enter both teammates' GitHub usernames, the Supabase project ref, and the submission time if you know it. Save.

### 6. Check that the timers ran

1. Just after 11:05, open https://claude.ai/settings/usage in the `Claude A` window. **Current session** should show a reset in about 5 hours (about 16:05).
2. Just after 13:35, do the same in your normal profile for Account B. It should reset at about 18:35.

If an account shows no current session, its timer didn't run. Open https://claude.ai in that profile, start a new chat, and send `OK`. That starts the window by hand.

### 7. Connect Claude Code to Supabase (5 min, after 13:35)

This starts Claude Code with Account B, so wait until its timer has run.

In PowerShell, run this with `PROJECT_REF` replaced by your project ref:

```powershell
claude mcp add --scope user --transport http supabase "https://mcp.supabase.com/mcp?project_ref=PROJECT_REF&features=database,docs,development"
```

Then start Claude Code:

```powershell
claude
```

If it asks you to log in, choose your Claude subscription and sign in as **Account B**. Then:
1. Type `/mcp`, choose **supabase**, then **Authenticate**. Sign in in the browser and pick the `hack` organization.
2. Type `/mcp` again; supabase should show as connected.
3. Type `/exit`.

### 8. Last checks

1. Make sure you'll be able to copy the problem statement and judging criteria as text.
2. Charge everything.

---

## Part 3: The kickoff (about 10 minutes of your time)

### Step 1: Put the Supabase keys in `local.properties`

In PowerShell:

```powershell
notepad C:\Users\leeju\Desktop\nushhack26\local.properties
```

Notepad asks to create the file: click **Yes**. Paste the block below, fill in the values from your password manager, and save. The file is git-ignored, so the keys never reach GitHub.

```
SUPABASE_URL=
SUPABASE_PUBLISHABLE_KEY=
```

### Step 2: Start the build account

1. Open `C:\Users\leeju\Desktop\nushhack26` in Claude Code, signed in as **Account B**.
2. Choose **Opus 5.5** at **Medium** effort:
   - **Desktop app:** click the model picker at the right end of the row under the prompt box (or press Ctrl+Shift+I) and choose Opus 5.5. Then pick **Medium** under **Effort** (or press Ctrl+Shift+E).
   - **Terminal:** type `/model opus`, then `/effort medium`.
3. Say:
   ```
   You are the build account. Read hackathon-kit/README.md, then start the kickoff.
   ```
   This takes about 45–60 minutes. It may ask you to approve installing scrcpy and Maestro.

### Step 3: Meanwhile, start the design account

In the `Claude A` window, start a new chat in the **Hackathon** project. Click the model name next to the send button, choose **Opus 5.5**, and set **Effort** to **High**. Then say:

```
You are the design account. Let's do ideas. Here's the problem statement and the judging criteria:
```

Then paste both. P and Q join in.

Keep High for the ideas and the spec. For the design brief, the pitch and rescopes, switch to **Medium** in the same menu.

### Step 4: Set up Devin

Do this when the build account says "Ready for Devin setup".

1. Start a new Devin session in **Normal** mode with:
   ```
   You are Devin on our hackathon team. Read hackathon-kit/README.md, hackathon-kit/role-devin.md and hackathon-kit/stack.md first. Then set up your environment for this repo: allenleejy/nushhack26.
   ```
2. Approve the setup suggestion cards (the "blueprint"). They install the Android tools and an emulator.
3. In the blueprint editor's **Secrets** tab, add:
   - `SUPABASE_URL`
   - `SUPABASE_PUBLISHABLE_KEY`

   **Don't** add the Supabase secret key.
4. Let it build its snapshot. This takes about 5–15 minutes; you can watch it under **Settings → Environment → Snapshots**.

### Step 5: Install the first release on the phones

When the build account publishes release `r01`, either:
- plug each phone into the laptop and ask the build account to install it, or
- on each phone, open https://github.com/allenleejy/nushhack26/releases, download the APK from the newest release, and open it.

Q opens the app on both phones and checks that Me shows a user id.

### Step 6: Restart Claude Code once

When the kickoff finishes, start a fresh Claude Code session in `C:\Users\leeju\Desktop\nushhack26`. The helper agents and the project instructions load then. From now on, Claude Code knows it's the build account without being told.

---

## Part 4: Before you sleep (about 01:45, 5 minutes)

Re-arm both timers, so each account's next window starts while you sleep and you wake up to fresh windows. Do it in the `Claude A` window for Account A, then in your normal profile for Account B.

1. Open https://claude.ai/settings/usage. Under **Current session**, find when it resets. If it shows a countdown, such as "Resets in 1 hr 20 min", add it to the current time.
   - If there's no current session, nothing has been sent in the last 5 hours. Skip the timer: send `OK` in a new chat now, and that account is done.
2. Go to https://claude.ai/code/routines and click the `Prime window` task. It should say **Completed**.
3. Open the menu next to the task's name and choose **Edit**.
4. In the **Trigger** row, choose **One-time** (or **Once**). Set the date, and the reset time plus 5 minutes, in your local time.
5. Click **Save changes**. The task's page should show the next run at that time.

For example, if Account A resets at 02:05, set 02:10. If Account B resets at 04:35, set 04:40.

---

## If something breaks

| Problem | What to do |
|---|---|
| A timer didn't run | Send `OK` in a new chat on that account. Its window starts then. |
| A Gradle build fails on the laptop | Copy the error into Claude Code. If it mentions Java 24 or "Unsupported class file", Gradle isn't using Android Studio's Java: ask the build account to check `org.gradle.java.home`. |
| The laptop doesn't see a phone | Check `adb devices` in PowerShell. If it says "unauthorized", unlock the phone and tap **Allow**. Try another cable: some only charge. |
| A phone won't install a new release ("App not installed") | Sync it, uninstall the app, then install the new APK. |
| Supabase tools stop working in Claude Code | In PowerShell, run `claude`, type `/mcp`, choose **supabase**, then **Authenticate** again. Then start a fresh Claude Code session. |
| Claude Code hits its usage limit | Switch it to the other account. See "The two Claude windows" in `playbook.md`. |
| Devin can't see the repo | Go to **Settings → Connections → GitHub → Configure** and check the repo is included. |
| Devin's emulator won't start or is very slow | Fine: cards don't need it. Devin checks its work with builds, tests and screenshots. |
| Phones can't sign in ("rate limit") | In Supabase, go to **Authentication → Rate Limits** and raise the limit. |
