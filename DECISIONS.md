# Decisions and known issues

What I found, what I decided and why, and what I chose to leave alone. How I worked with agents is in
[AGENT_WORKLOG.md](AGENT_WORKLOG.md).

## Overall Summary

- **Starting state:** The PDF described a working API, but the repo wasn't one. The build only ran on
  tooling that no longer exists, all 25 tests were switched off so the code showed passing, registration always
  failed with a 500 error, and articles, comments and favorites had no storage at all.
- **The feature is Article Search** (`GET /api/articles/search?q=`). I picked it because it needed the
  least new storage of the three options. I built create/get article and tags only because search needs
  some sample articles to exist in the DB
- **Fixes it depended on:** working registration, login and profile updates; proper RealWorld error codes;
  passwords never sent back.
- **Behavior I changed on purpose:** all routes now live under `/api`; unbuilt endpoints say 501 instead of
  404; error messages say what's actually wrong. 
- **How I know it works:** I created a wide variety of test cases each shown to catch the bug it targets; the official
  RealWorld spec passes for every endpoint I built; CI runs on JDK 17 and 21.
- **Biggest remaining risks:** weak password hashing and secrets in the code since production aws secrets are a bit out of scope for me, old dependencies with known vulnerabilities, and no rate limiting. (Security notes, Known issues)

## Starting state

- The build used Gradle 4.10 and jcenter, neither of which works with JDK 17/21 today.
- `./gradlew build` passed, but only because every test class was marked `@Ignore`.
- Registration's validation was backwards, so nobody could sign up, and nothing behind login was reachable.
- Only users, follows and tags were stored. Articles, comments and favorites had controllers but nothing
  behind them.
- Routes had no `/api` prefix, even though the spec, the Postman tests and half the test helpers expected one.
- Every error came back as a 500 with an unreadable message.

## Decisions

### Build and tooling
1. **Upgraded Gradle (4.10 → 8.10.2) and Kotlin (1.3.+ → 1.9.24, pinned).** The old Gradle can't run on
   JDK 17+, and a pinned version means builds don't change underneath us.
2. **Dropped jcenter and moved Exposed from 0.14.1 to 0.17.14.** jcenter is gone, and 0.14.1 only lived
   there. I stayed on the newest 0.17.x rather than jumping to modern Exposed, which would mean rewriting
   the data layer.
3. **Pinned H2 to 1.4.200.** Exposed 0.17 crashes at startup with H2 2.x. Upgrading Exposed instead would
   have been a much bigger change. The catch is that 1.4.200 has known vulnerabilities, which I accepted
   for an in-memory dev/test database.
4. **The build always produces Java 17 bytecode,** whichever JDK runs it. Without this, building on JDK 21
   produced code that wouldn't run on 17. I checked that the JDK-21 build runs on Java 17. I chose 17
   over the original 16 because 16 is out of support and 17 is the oldest JDK CI needs.
5. **Readable test output:** every test prints PASSED or FAILED, and a summary at the end gives one line
   per failure explaining why. The default output just said "BUILD SUCCESSFUL".
6. **Tests start the app on a random free port.** I found this one the hard way: with my own copy of the
   app running on 8080, 13 tests suddenly failed, because the test server quietly failed to start and the
   tests talked to my app instead. `gradlew run` still uses 8080.

### Scope
7. **Article Search.** None of the three features could be built on the existing code, because articles
   didn't exist. Search needed only an articles table plus create; Popular Articles also needs favorites,
   and User Activity needs articles, comments and favorites, or it would just return zeros.
8. **Fix login first.** Everything depends on it, so I treated it as a prerequisite, not extra scope.
9. **Everything under `/api`.** The spec expects it, and most of the existing tests already used it. Old
   URLs now return 404, deliberately (`ApiPrefixTest` checks this). I didn't keep the old URLs working,
   because nothing could have been using them with registration broken. My general rule from here on:
   match the RealWorld spec wherever the code didn't.

### Error handling
10. **Errors use the spec's status codes and format:** bad input → 422, wrong login → 401, missing →
    404, as `{"errors":{"body":["..."]}}`.
11. **Unexpected errors say only "Internal server error."** The details go to the server log, so clients
    never see SQL or stack traces.
12. **A request with no body at all gets a 422,** not a 500. This is handled where the body is read,
    rather than by catching a broad exception type everywhere.

### Users and security
13. **Fixed the backwards validation** that demanded a blank password and username.
14. **Passwords are never sent back,** hashed or not. A hash can be cracked offline, and clients never
    need it. Two existing tests checked that the password *was* returned; they now check that it isn't.
15. **New passwords are hashed on profile update.** Previously a password change stored plain text, and
    the user could no longer log in.
16. **Profile updates return a fresh token.** The token contains the email, so changing your email used
    to leave you with no valid token.
17. **Taken usernames and emails get a 422** instead of a database error (500).
18. **Error messages say what's wrong** ("Username can't be blank.") instead of "User is invalid." A small
    change for any client matching on the old text.
19. **Blank usernames/passwords are rejected on update, and lengths are capped** to match the database
    columns. The limits are shared constants, so validation and the schema can't drift apart.
20. **Profile updates can be partial,** as the spec allows: send only the fields you're changing. I used a
    separate request type rather than making `User.email` optional everywhere. `id` and `token` in the body
    are ignored, since clients often send back what they received. One limitation: you can't clear your
    bio back to nothing, because "not sent" and "empty" look the same.
21. **Emails are case-insensitive:** stored and compared in lowercase, so `Alice@Example.com` and
    `alice@example.com` are one account.

### Articles groundwork
22. **Unbuilt endpoints return 501 Not Implemented** with a message like "The article feed is not
    implemented yet." Before, they returned 404, which looks like the URL doesn't exist.
23. **Public reads are actually public.** The router wrapped every article route in a required login, so
    even "optional login" routes needed a token. Routes are now grouped by path first, then by whether
    they need login.
24. **An article's author is a public profile,** so author emails and tokens don't appear in responses.
25. **Dates are ISO-8601 strings** like `2016-02-18T03:22:56.637Z`, as in the spec, rather than raw numbers.
26. **A missing article body gets a clear message** ("Body can't be blank.") instead of a generic
    parsing error.

### Articles: create and get
27. **I built `POST /api/articles` and `GET /api/articles/:slug`** as the minimum needed for search to be
    real and testable, following the same layering as the user code. An unknown slug returns 404.
28. **Title, description and body are required** (the spec's required fields). Title and description are
    capped at 255 characters and the body at 100,000 (see 37).
29. **Slugs come from the title:** "Café crème" becomes `cafe-creme`, and repeats get `-2`, `-3`. Titles
    with no usable letters (like `!!!` or `日本語`) fall back to `article`. Random suffixes would avoid
    a lookup but make uglier URLs. Two simultaneous posts with the same title could collide (one would get
    a 500); that's rare and listed below.
30. **Tags are stored and shared between articles,** which also made `GET /api/tags` return real data for
    the first time. They're trimmed, de-duplicated, and returned alphabetically, so the author's original
    order isn't kept.
31. **`favorited`, `favoritesCount` and `following` are always false/0** because favorites and follows
    aren't built. The fields are still there, so the response shape matches the spec.

### Search
32. **Search matches the title or the body,** case-insensitively, as the brief asks. Description and tags
    aren't searched (easy to add, but not requested). Multi-word terms match as a phrase. No login needed.
33. **Newest first,** like the spec's article list, with a tie-breaker so pages stay stable. I skipped
    relevance ranking: it needs full-text indexing, and newest-first is predictable.
34. **Pagination uses the spec's `limit` (default 20) and `offset` (default 0).** `articlesCount` is the
    total number of matches, not the page size, so clients can page through results. I capped `limit` at
    100 so one request can't fetch everything.
35. **An empty search returns 422** rather than every article, since it's almost certainly a client bug.
36. **`%` and `_` are matched literally,** so searching `100%` doesn't also find "100 dragons". The
    trade-off: this kind of search reads every article, which is fine at this size but would need
    full-text search at scale.
37. **Size limits on input:** article body ≤ 100,000 characters and search term ≤ 200. A security check
    showed a 20 MB article being accepted, and very long search terms were only stopped by accident (by
    the web server's URL limit).

### Tests
38. **64 tests: 46 pass and 18 are skipped,** each skip saying why (they cover endpoints that aren't
    built yet). We started with 25 tests, all skipped. `ArticleSearchTest` reads as a description of how
    search behaves.
39. **Tests keep out of each other's way** in the shared database by using unique names, and each search
    test searches for its own made-up word. A fresh database per test would be cleaner, but it's a bigger
    change (see known issues).
40. **The tests are proven to catch bugs.** I broke the search code on purpose nine different ways, and
    each break was caught by the test meant for it. A check that each break actually took effect caught
    two that hadn't, which would otherwise have looked like gaps in the tests.

### CI
41. **One GitHub Actions workflow with two jobs,** replacing the dead Travis and JDK 16 configs.
    - **Build & test** on JDK 17 and 21, with Gradle caching. Failures show in the log, on the run's
      summary page, and in a downloadable HTML report.
    - **RealWorld spec (bonus):** runs the official Postman collection against the app. Most of the
      collection covers unbuilt endpoints, so `check-results.js` fails the job only if an endpoint I
      *built* fails, and it errors if one of those requests goes missing, so a rename can't quietly drop
      coverage. Locally, all 9 built requests pass. I considered requiring only some folders (misses
      create/get article), report-only (easy to ignore), and requiring everything (always red).
    - The spec doesn't cover search; `ArticleSearchTest` does.

### Docs
42. **The README describes what actually works:** a status table, how to use search (with an example
    checked against the running app), how to run the tests, and what CI does.

## Security notes

I probed the running app for common problems. **These held up:** fields like `author`, `slug` and
`favoritesCount` in a request are ignored, so you can't post as someone else; search is safe from SQL
injection; login gives the same answer for an unknown email and a wrong password; passwords and tokens
don't appear in logs; and errors don't leak internals.

**Not addressed, on purpose:**
- **No rate limiting.** 30 wrong passwords in a row were all accepted for checking. This is usually
  handled in front of the app (a gateway or proxy).
- **Signup reveals whether an email is registered.** A common usability trade-off.
- **Future update/delete endpoints must check ownership,** so people can't edit others' articles. Nothing
  is exposed today, since those endpoints return 501.
- **CI actions are pinned to major versions** (`@v4`), not exact commits. The CI token is read-only, and
  newman is pinned to an exact version.
- **Deployment:** no HTTPS (expected to sit behind a proxy that adds it), and tokens last 10 hours with no
  way to revoke them.

## Known issues

- Passwords are hashed with HMAC-SHA256, a secret written in the code, and no per-user salt. Real systems
  should use bcrypt, scrypt or Argon2. Switching would change how every stored password works, so I left it.
- The token-signing secret is also written in the code.
- H2 1.4.200 has known vulnerabilities (decision 3).
- The email check rejects single-letter domains like `a@b.com`.
- Registration returns `"id": null` (login returns the real id), because it echoes the submitted user rather
  than the saved one. Harmless, since the spec doesn't include `id`, but inconsistent.
- Two signups with the same username at the same instant can both pass the check; one gets a 500.
- Request bodies that aren't valid UTF-8 are saved with `�` in place of the bad characters. Windows
  PowerShell 5.1 does this by default ("Café" became "Caf�"). Rejecting such requests would be safer.
- Requests with unknown JSON fields are rejected with a 422: strict but safe.
- `GET` on a POST-only route returns 404 rather than 405 (Ktor's default).
- All tests share one in-memory database, so they rely on unique names. (The JDBC URL actually names the
  database `DATABASE_TO_UPPER=false`, which is why the data is shared.)
- Starting the app while port 8080 is taken leaves a "zombie" that claims to be running but serves
  nothing. A startup check could catch this.
- Tests wait a fixed half-second for the app to start, which could be flaky on a slow CI machine.
- The app starts an unused H2 database server on port 5435. H2 refuses outside connections by default;
  I left it in case someone uses it for debugging.
- Each test creates a database connection pool that's never closed. Harmless at this size.
- The random test port could, very rarely, be taken between being picked and being used.
- The CI workflow is verified locally; its first real run on GitHub is the final check.
- Pushes to a branch with an open pull request run CI twice. Accepted for simplicity.
- The server prints noisy "Connection reset" errors when a client hangs up abruptly. They're harmless;
  switching the web engine to Netty would likely quiet them, but that's a riskier change.
- `UserRepository.kt:104` has a warning that will become an error in Kotlin 2.0. Not a problem on 1.9.
