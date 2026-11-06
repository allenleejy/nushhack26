# Hackathon kit: start here

This folder holds everything our AI tools and our team need for the hackathon (Fri 6 Nov 14:00 to Sat 7 Nov 18:00: about 16–20 hours of building, then judging).

Give this same folder to every machine and tell it which role it plays. That's all the context it needs.

## If you're an AI reading this

1. **Find your role.** The person talking to you will tell you. If they haven't:
   - in claude.ai, you're the design account
   - in Claude Code, you're the build account
   - in Devin, you're Devin
2. **Read your files:**

   | Your role | Read |
   |---|---|
   | Design account (Claude in claude.ai) | `role-design.md`, `playbook.md` |
   | Build account (Claude Code) | `role-build.md`, `playbook.md`, `stack.md` |
   | Devin | `role-devin.md`, `playbook.md`, `stack.md` |

3. **Start.** Reply with one line confirming your role, then do the job you were given, or ask what's next.

## The three machines

| Machine | Tool | Its job |
|---|---|---|
| **Design account** | Claude account A in claude.ai, plus Claude Design | Talks through the problem with the team, picks the idea, writes the spec, writes the Claude Design briefs, prepares the pitch. No code. |
| **Build account** | Claude account B in Claude Code (folder `C:\Users\leeju\Desktop\nushhack26`) | Sets up the app at kickoff, turns the spec into a database and task cards, checks and merges Devin's work, fixes bugs, deploys |
| **Devin** | Devin Max, cloud sessions | Builds the task cards in parallel, one pull request per card, including overnight |

How to start a session:
- **Design account:** open a chat in the claude.ai **Hackathon** project and say: "You are the design account. Read your brief and work with me."
- **Build account:** open Claude Code in `C:\Users\leeju\Desktop\nushhack26` and say: "You are the build account. Read hackathon-kit/README.md and work with me." After the kickoff this happens automatically.
- **Devin:** paste the line the build account gives you. It always starts with "You are Devin on our hackathon team…".

## Where Devin comes in

Devin is the team's builder, and it starts once there's a plan to build.

1. **14:00–17:00: no Devin yet.** The design account works out the idea and the spec with the team. Meanwhile, the build account sets up the app, then turns the spec into a database and 4–6 task cards.
2. **About 17:00: the first wave.** The build account gives you one line to paste into Devin. That Devin session starts one helper Devin per card. Each helper builds its card and opens a pull request, usually within 30–60 minutes. A hard card gets its own line instead: paste it into a separate Devin session set to Fusion mode.
3. **Every 45–60 minutes: a merge round.**
   - The build account checks the pull requests, merges the good ones and updates the live link once.
   - Q tests the live link on real phones.
   - Small bugs go to the build account. Big ones become new cards for Devin's next wave.
4. **Overnight:** Devin keeps building polish cards while you sleep.

In short: the design account decides **what** to build, the build account decides **how** and checks the work, and Devin does most of the typing.

## Files in this folder

| File | Who reads it | What's in it |
|---|---|---|
| `README.md` | Everyone | This page |
| `playbook.md` | Every machine, and you | How we work: the speed plan, subagents, which model for what, timeline, rules, bug reports, emergencies |
| `stack.md` | Build account, Devin | Tech stack, folder layout, coding rules, environment variables, free-plan limits |
| `role-design.md` | Design account | Its jobs, and how to do each one |
| `role-build.md` | Build account | Its jobs, and how to do each one |
| `role-devin.md` | Devin | Its jobs, and how to do each one |
| `setup.md` | You | One-time setup: Thursday, Friday morning, the 14:00 kickoff |
| `templates/` | Build account | Card and pull request templates, and the four Claude Code helper agents it installs at kickoff |
