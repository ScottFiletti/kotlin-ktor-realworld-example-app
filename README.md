[![CI](https://github.com/ScottFiletti/kotlin-ktor-realworld-example-app/actions/workflows/ci.yml/badge.svg)](https://github.com/ScottFiletti/kotlin-ktor-realworld-example-app/actions/workflows/ci.yml)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/5b6503dfa3024a0dbbf173e333f80bcf)](https://app.codacy.com/app/Rudge/kotlin-ktor-realworld-example-app?utm_source=github.com&utm_medium=referral&utm_content=Rudge/kotlin-ktor-realworld-example-app&utm_campaign=Badge_Grade_Dashboard)
[![BCH compliance](https://bettercodehub.com/edge/badge/Rudge/kotlin-ktor-realworld-example-app?branch=master)](https://bettercodehub.com/)

# ![RealWorld Example App](logo.png)

> ### Kotlin + Ktor codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld) spec and API

### [RealWorld](https://github.com/gothinkster/realworld)

This codebase was created to demonstrate a fully fledged fullstack application built with **Kotlin + Ktor + Kodein + Exposed** including CRUD operations, authentication, routing, pagination, and more.

We've gone to great lengths to adhere to the **Kotlin + Ktor** community styleguides & best practices.

For more information on how to this works with other frontends/backends, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

# Status

Only part of the RealWorld API is implemented. Endpoints that are not built yet answer
**501 Not Implemented** with a message saying so.

| Area | Endpoints | Status |
|---|---|---|
| Users & auth | `POST /api/users`, `POST /api/users/login`, `GET/PUT /api/user` | ✅ Implemented |
| Articles | `POST /api/articles`, `GET /api/articles/:slug` | ✅ Implemented |
| **Article search** | `GET /api/articles/search?q=` | ✅ Implemented (not part of the RealWorld spec, see below) |
| Tags | `GET /api/tags` | ✅ Implemented |
| Articles (other) | list/filter, feed, update, delete, favorite/unfavorite | 501 |
| Comments | add, list, delete | 501 |
| Profiles | get, follow/unfollow | 501 |

Design decisions, behavior changes, and known issues are recorded in [DECISIONS.md](DECISIONS.md).

## Article search

`GET /api/articles/search?q=<term>` returns articles whose **title or body** contains the term,
case-insensitive, newest first, in the standard article-list format. Login is optional.

| Parameter | Default | Rules |
|---|---|---|
| `q` | (required) | Not blank, at most 200 characters; matched as a phrase; `%` and `_` match literally |
| `limit` | 20 | Whole number from 1 to 100 |
| `offset` | 0 | Whole number, 0 or more |

```
GET /api/articles/search?q=dragon&limit=2

{"articles":[
  {"slug":"the-last-dragon","title":"The last dragon","tagList":["dragons"],"createdAt":"2026-10-16T18:38:55.802Z", ...},
  {"slug":"here-be-dragons","title":"Here be dragons", ...}
 ],
 "articlesCount":4}
```

`articlesCount` is the **total** number of matches (for pagination), not the size of the page.
Invalid input returns **422** with a message, e.g. `{"errors":{"body":["Search term q can't be blank."]}}`.

# How it works

The application was built with:

  - [Kotlin](https://github.com/JetBrains/kotlin) as programming language
  - [Ktor](https://github.com/ktorio/ktor) as web framework
  - [Kodein](https://github.com/Kodein-Framework/Kodein-DI) as dependency injection framework
  - [Jackson](https://github.com/FasterXML/jackson-module-kotlin) as data bind serialization/deserialization
  - [Java-jwt](https://github.com/auth0/java-jwt) for JWT spec implementation
  - [HikariCP](https://github.com/brettwooldridge/HikariCP) as datasource to abstract driver implementation
  - [H2](https://github.com/h2database/h2database) as database
  - [Exposed](https://github.com/JetBrains/Exposed) as Sql framework to persistence layer

Tests:

  - [junit](https://github.com/junit-team/junit4)
  - [Unirest](https://github.com/Kong/unirest-java) to call endpoints in tests

#### Structure
      + config/
          All app setups. Ktor, Kodein and Database
      + domain/
        + repository/
            Persistence layer and tables definition
        + service/
            Logic layer and transformation data
      + ext/
          Extensions: email validation, request-body parsing
      + utils/
          Jwt and Encrypt classes
      + web/
        + controllers
            Classes and methods to mapping actions of routes
        Router definition to features and exceptions
      - App.kt <- The main class

# Getting started

You need JDK 17 or 21 installed.

The server is configured to start on [8080](http://localhost:8080). All endpoints are under `/api` (e.g. `http://localhost:8080/api/tags`).

Build (compiles and runs all tests):
> ./gradlew clean build

Run only the tests:
> ./gradlew test

Each test prints PASSED/FAILED, followed by a summary that gives a one-line reason for every failure.
The full report is in `build/reports/tests/test/index.html`. Tests for endpoints that are not implemented
yet are skipped, and each states why.

Start the server:
> ./gradlew run

### RealWorld API spec tests

The [spec-api](spec-api) folder has the official RealWorld Postman collection. With the server running
and Node.js installed:

> APIURL=http://localhost:8080/api ./spec-api/run-api-tests.sh --reporters cli,json --reporter-json-export newman-report.json
>
> node spec-api/check-results.js newman-report.json

The collection covers the whole RealWorld API, so requests for unimplemented endpoints fail (501).
`check-results.js` fails only if a request for an **implemented** endpoint fails, and prints a
per-request table. The collection does not cover article search, which is tested in `ArticleSearchTest`.

### CI

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on every push:

- **Build & test** on JDK 17 and JDK 21 (output targets Java 17, so it runs on both). Failures show
  on the run's summary page, and the HTML test report is attached to the run.
- **RealWorld API spec**: starts the app and runs the collection plus `check-results.js` as above.

# Help

Please fork and PR to improve the code.
