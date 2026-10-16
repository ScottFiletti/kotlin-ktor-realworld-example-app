# Agent Worklog

**Walkthrough video:**: https://www.youtube.com/watch?v=5yl2ScDuTbw

Design decisions and known issues are in [DECISIONS.md](DECISIONS.md); this file covers how I worked with agents.

## Harnesses and models

- First session. Upgraded the build (Gradle 8, Kotlin 1.9, dependency fixes) and did the initial investigation. Ended with a written handoff.
- Tools used: Claude Code (VS Code extension), Claude Opus 5.5. Utilized the agent for assistance with investigation, implementation, tests, CI, docs. I made every scope decision, added some extra verification from previous project experiences (Passwords shouldnt be returned, Any security issues like unusual characters or large character limits), and did all commits myself while doing manual testing before each commit to ensure nothing would break.


## Where agents changed my approach

- **The feature request was built on a hollow app.** The agent's first inventory (run, not read) showed that registration always returned 500, all 25 tests were `@Ignore`d behind a green build, and articles had no persistence at all. That changed the job from "add an endpoint" to "make the foundation work, then build the smallest real feature." I picked Article Search because the agent's comparison showed it needed the least new persistence of the three options and was the quickest to build. 
- **Edge-case probing turned one bug fix into a solid auth layer.** Asking for edge cases after each fix surfaced password leaks in responses, a password change that locked users out, duplicate usernames returning 500, and more. Each was reproduced against the running app before it was fixed.
- **Checking JDK 21 locally** showed that unpinned builds produced Java-21-only bytecode, so the build now targets Java 17 on both JDKs.

## How I verified the agent's work

- **Observed failing → observed passing.** Bugs were reproduced with real HTTP requests before fixing, and the same requests were re-run afterwards.
- **Tests that can fail:** the agent deliberately broke the search a few different ways, and each break was caught by the test meant for it. It also checked that each "break" had actually been applied; two initially hadn't.
- **An independent check:** the official RealWorld Postman collection passes for every implemented endpoint (9/9 requests).
- **A security pass:** I asked for a review of the API's guardrails. The agent probed the running app (spoofed fields, SQL injection, oversized input, repeated failed logins), which led to two input limits; the rest is documented in DECISIONS.md.
- **Manual Testing:** I ran the manual test steps myself (including in a separate terminal), and ran the suite on JDK 17 and 21.

## What agents got wrong or missed

- **Chat assistant:** upgraded Exposed without checking H2 compatibility. It compiled, then crashed at startup. Caught by running the app.
- **Claude Code introduced a routing bug** (`/api/articles/feed` was matched as an article slug). Caught because route probes checked each handler's *message*, not just the status code; 501 alone looked right.
- **Claude Code's test tooling twice garbled accented characters**, and both times the result looked like an app bug. Inspecting the bytes actually sent showed the app was correct.
- **Claude Code filtered test output for readability, which hid the "Connection reset" errors I was seeing.** The real cause was my own running app on port 8080. That led to tests running on a random port.

## The gap between asking and trusting

- What made the results trustworthy wasn't the agent's confidence. It was a habit of making every claim checkable: run it, reproduce it, make the test fail on purpose, check that the check actually ran, and compare against an outside source (the spec collection). Most of the agent's mistakes above were caught by that habit, not by reading code.

## What I'd do differently

- When I broke down this project into steps, I didnt commit after each step which would have made the process a bit quicker to deploy. 
- I should have tested out JDK 21 and 17 at the same time, it would have saved some time. I mainly did a lot of the work on 17 first then checked 21
- For testing I should have created a fresh database every time instead of using a shared one
