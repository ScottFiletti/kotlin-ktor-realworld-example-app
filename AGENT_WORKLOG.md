# Agent Worklog

**Walkthrough video:** ✏️ _TODO: add link_

Design decisions and known issues are in [DECISIONS.md](DECISIONS.md); this file covers how I worked with agents.

## Harnesses and models

- : first session. Upgraded the build (Gradle 8, Kotlin 1.9, dependency fixes) and did the initial investigation. Ended with a written handoff.
- **Claude Code** (VS Code extension), **Claude Opus 5.5**: everything after the handoff. Investigation, implementation, tests, CI, docs. One interactive agent. I made every scope decision and did all commits myself.

## Representative instructions

- *"Verify by running things (build, app, tests, curl), not by reading code and assuming. A green build already misled us once."* (from my handoff note)
- *"Ask me before big scope decisions (feature choice, `/api` prefix)."* and *"Be sure to ask me to commit it myself."*
- *"Were there any edge cases you can think of before moving on?"* (asked after most steps)
- *"Would this still work with the requirements in the pdf?"* / *"How does it compare with the requirements given?"*
- *"Whenever we make a design decision, mark it down as part of the worklog."*

## Where agents changed my approach

- **The feature request was built on a hollow app.** The agent's first inventory (run, not read) showed that registration always returned 500, all 25 tests were `@Ignore`d behind a green build, and articles had no persistence at all. That changed the job from "add an endpoint" to "make the foundation work, then build the smallest real feature." I picked Article Search because the agent's comparison showed it needed the least new persistence of the three options.
- **Edge-case probing turned one bug fix into a solid auth layer.** Asking for edge cases after each fix surfaced password leaks in responses, a password change that locked users out, duplicate usernames returning 500, and more. Each was reproduced against the running app before it was fixed.
- **Running the official spec before writing CI changed the CI design.** I had planned "the Auth and Tags folders must pass." The real results showed create/get article would then go unchecked, so CI instead fails only if an *implemented* endpoint fails, and reports the rest.
- **Checking JDK 21 locally** showed that unpinned builds produced Java-21-only bytecode, so the build now targets Java 17 on both JDKs.

## How I verified the agent's work

- **Observed failing → observed passing.** Bugs were reproduced with real HTTP requests before fixing, and the same requests were re-run afterwards.
- **Tests that can fail:** the agent deliberately broke the search code nine ways, and each break was caught by the test meant for it. It also checked that each "break" had actually been applied; two initially hadn't.
- **An independent check:** the official RealWorld Postman collection passes for every implemented endpoint (9/9 requests).
- **A security pass:** I asked for a review of the API's guardrails. The agent probed the running app (spoofed fields, SQL injection, oversized input, repeated failed logins), which led to two input limits; the rest is documented in DECISIONS.md.
- **Manual Testing:** I ran the manual test steps myself (including in a separate terminal), and ran the suite on JDK 17 and 21.

## What agents got wrong or missed

- **Chat assistant:** upgraded Exposed without checking H2 compatibility. It compiled, then crashed at startup. Caught by running the app.
- **Claude Code introduced a routing bug** (`/api/articles/feed` was matched as an article slug). Caught because route probes checked each handler's *message*, not just the status code; 501 alone looked right.
- **Claude Code's test tooling twice garbled accented characters**, and both times the result looked like an app bug. Inspecting the bytes actually sent showed the app was correct.
- **Claude Code filtered test output for readability, which hid the "Connection reset" errors I was seeing.** The real cause was my own running app on port 8080 hijacking the test run. That led to tests running on a random port.
- **Confident predictions were wrong more than once** (e.g. "an old token will get 404"). Reproducing first caught each one.

## The gap between asking and trusting

What made the results trustworthy wasn't the agent's confidence. It was a habit of making every claim checkable: run it, reproduce it, make the test fail on purpose, check that the check actually ran, and compare against an outside source (the spec collection). Most of the agent's mistakes above were caught by that habit, not by reading code.

## What I'd do differently

✏️ _TODO: your own words. Some candidates from this session:_
- _Run the official spec collection first: it shows the app's real state in 20 seconds._
- _Commit after each verified step instead of one large change at the end._
- _Set up CI (with JDK 21) early, so version issues surface from the start._
- _Give each test a fresh database instead of working around a shared one._
