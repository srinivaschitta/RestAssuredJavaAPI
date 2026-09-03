# Restful Booker API test suite

REST API tests for [Restful Booker](https://restful-booker.herokuapp.com) ([API docs](https://restful-booker.herokuapp.com/apidoc/index.html)).

**Stack:** Java 17, Maven, JUnit 5, Rest Assured, AssertJ, Logback.

The suite covers auth, booking CRUD, query filters, and negative cases. Each test creates its own data. It is a focused Booker suite, not a generic test framework.

| If you want… | Section |
|---|---|
| Fresh-machine setup and `mvn test` | [Setup and run](#setup-and-run-fresh-machine) |
| Why the layout looks like this | [Design decisions and trade-offs](#design-decisions-and-trade-offs) |
| Backlog if there were more time | [What I’d do next with more time](#what-id-do-next-with-more-time) |
| Cursor / skills / MCP | [AI tool usage](#ai-tool-usage) |

---

## Setup and run (fresh machine)

Assume a clean machine: **JDK 17 or newer** and **Maven 3.9 or newer**. Nothing else is required (no Docker, Node, or extra Maven settings). The first `mvn test` downloads dependencies from Maven Central.

**1. Install toolchain**

| Tool | macOS (Homebrew) | Check |
|---|---|---|
| JDK 17+ | `brew install openjdk@17` | `java -version` → 17+ |
| Maven 3.9+ | `brew install maven` | `mvn -version` → 3.9+, Java 17+ |

On Linux/Windows, install a Temurin or OpenJDK 17+ build and Apache Maven, then confirm the same two commands. `JAVA_HOME` must point at that JDK so Maven does not pick an older JRE.

**2. Get the project**

```bash
git clone <this-repo-url>
cd rest_assured_java_api
```

**3. Run the API suite** (hits the public Restful Booker instance by default)

```bash
mvn test
```

Surefire XML and per-class stdout land under `target/surefire-reports`. Rest Assured request/response dumps go to stdout; Surefire redirects them into `*-output.txt`. There is no `target/logs` folder.

HTML from those existing results (does **not** re-run tests):

```bash
mvn surefire-report:report-only
open target/reports/surefire.html   # macOS; elsewhere open the file in a browser
```

Tests plus HTML in **one** Maven invocation:

```bash
mvn test surefire-report:report
open target/reports/surefire.html
```

CI runs `mvn test`, then `mvn surefire-report:report-only`, so HTML is still produced when tests fail. Do not run `mvn surefire-report:report` after a separate `mvn test` locally — that goal runs the test lifecycle again. Use `report-only` if tests already ran.

| Output | Location | After |
|---|---|---|
| Console | short Maven / Surefire summary | `mvn test` |
| Request/response log | `target/surefire-reports/*-output.txt` | `mvn test` |
| Surefire XML / txt | `target/surefire-reports` | `mvn test` |
| HTML report | `target/reports/surefire.html` | `surefire-report:report` or `report-only` |

Point at another environment without changing code:

```bash
# properties file: src/test/resources/config/qa.properties
mvn test -Denv=qa

# or override a single value
BOOKER_BASE_URI=https://restful-booker.herokuapp.com mvn test
```

If `/ping` is slow on first contact, wait and re-run; CI warms the demo API with retries. Credentials for the public API are in `src/test/resources/config/dev.properties` (`admin` / `password123`). Override with `BOOKER_USERNAME` / `BOOKER_PASSWORD` for any other host.

---

## Configuration

`ConfigManager` loads `src/test/resources/config/{env}.properties` (`dev` by default). An environment variable wins over the file value when both are set.

| Source | What it selects |
|---|---|
| `BOOKER_ENV` or `-Denv` | Which file to load (`dev`, `qa`, …). Default: `dev` |
| `BOOKER_BASE_URI` | Overrides `base.uri` |
| `BOOKER_USERNAME` | Overrides `auth.username` |
| `BOOKER_PASSWORD` | Overrides `auth.password` |

Add a new file under `src/test/resources/config/` to introduce an environment.

| Failure | Exception |
|---|---|
| Missing env file, or blank `base.uri` / credentials | `ConfigException` |
| Null body, non-positive id, or blank token passed to the client | `IllegalArgumentException` |

---

## Project layout

```
src/main/java/com/booker/
  client/       BookerClient, BaseApiClient
  config/       ConfigManager, ConfigException
  model/        *Request / *Response POJOs
  data/         BookingDataFactory
  assertion/    ResponseAssert, BookingAssert

src/test/java/com/booker/tests/
  AuthTests.java
  BookingTests.java

src/test/resources/
  config/dev.properties
  config/qa.properties
  logback.xml

.github/workflows/api-tests.yml
.cursor/rules, skills, commands
```

Framework code lives in `src/main/java`. Tests and suite assets (env files, Logback) live in `src/test`.

---

## How the suite is structured

**One client.** `BaseApiClient` builds the Rest Assured spec (base URI, `Content-Type`, `Accept: application/json`, cookie token). `BookerClient` extends it and owns every HTTP call: `/auth`, `/booking`, `/ping`. There are no per-resource clients. Auth is an endpoint of the same API, so `AuthTests` uses `BookerClient`.

**Request vs response models.**

| Model | Direction |
|---|---|
| `AuthRequest` | POST `/auth` body |
| `AuthResponse` | POST `/auth` body (`token` or `reason`) |
| `BookingRequest` | POST/PUT `/booking` body |
| `BookingResponse` | GET/PUT/PATCH `/booking/{id}` body |
| `CreateBookingResponse` | POST `/booking` (`bookingid` + nested booking) |
| `BookingIdResponse` | GET `/booking` list item |
| `BookingDates` | Nested dates on request and response |

**Thin tests.** A test names a behavior (`validate…`), builds data, calls the client, and asserts a domain outcome. Rest Assured DSL does not appear in test methods.

- Happy-path client methods check HTTP status and JSON content-type, deserialize to a `*Response` POJO, assert required fields, and return the POJO.
- Negative client methods (`submit…` / `fetch…`) return `Response`. The test owns one `ResponseAssert` line.

**Assertions.**

| Class | Responsibility |
|---|---|
| `ResponseAssert` | HTTP status and content-type |
| `BookingAssert` | Required fields on models, field-by-field booking match, PATCH remainder, id-list contains, auth token / bad-credentials reason |

**Independence.** Every mutating test creates a booking via `BookingDataFactory` (unique names). Seed IDs 1–10 are never used — the public API resets about every 10 minutes. Query tests assert the created id is **in** the filtered list, not that the list size is 1.

**What is intentionally omitted.** Tests do not run in parallel (shared demo API). There is no Cucumber, Allure, or Extent — Surefire + Logback are the reports. Token auth uses the cookie from `POST /auth`, not Basic auth.

---

## Design decisions and trade-offs

This is a **Booker-specific suite**, not a reusable “API test framework.” The structure exists so a reviewer can find one client, one factory, and two test classes — not a tree of abstractions.

| Choice | Why | Trade-off |
|---|---|---|
| Framework in `src/main`, tests in `src/test` | Client, models, and asserts are reusable from tests without putting Rest Assured in test methods. | Slightly unusual for a tests-only repo (some teams put everything under `src/test`). Clearer than a `src/test` “framework” package that tests also live in. |
| One `BookerClient` (extends `BaseApiClient`) | Auth, booking, and ping are one HTTP API. Per-resource clients would duplicate spec/headers/token. | Class grows with the API. For Booker’s surface that is acceptable; a much larger API would split by domain *after* the spec is shared, not before. |
| Rest Assured only in the client | Tests stay behavior-named (`validate…`) and reviewable. DSL copy-paste is how suites rot. | Negatives need a `submit…`/`fetch…` method that returns `Response` plus one `ResponseAssert` in the test. |
| Happy path returns POJOs; negatives return `Response` | Success paths should fail on status/JSON/required fields inside the client so tests assert domain data. Error paths must not deserialize a success body. | Two method styles to remember; documented in the project rule. |
| `*Request` / `*Response` POJOs, not raw maps | Jackson mapping is the contract. Drift shows up as deserialize or field asserts, not stringly JSON. | Booker’s odd status codes still need the contract table — POJOs do not fix HTTP semantics. |
| `ResponseAssert` vs `BookingAssert` | HTTP vs domain. Keeps “was it 403?” out of booking field matchers. | Another type to open; cheaper than AssertJ soup in every test. |
| `BookingDataFactory` + never seed IDs 1–10 | Public Booker resets ~10 minutes; shared IDs flake and collide. Query tests assert the **created id is in the list**. | Each test pays a create. Safer than depending on demo fixtures. |
| Negatives in `AuthTests` / `BookingTests` | Same resource, same reader. A `NegativeTests` class hides the pairing. | Classes are longer; grep by `validate…` name. |
| JUnit 5 + AssertJ, not Cucumber / TestNG | No Gherkin layer for a REST contract. JUnit is what Surefire already understands. | Non-technical stakeholders do not get feature files; Surefire HTML is the report. |
| Cookie token from `POST /auth`, not Basic | That is how Booker actually authorizes writes. | Header vs cookie is easy to get wrong; `BaseApiClient` owns it. |
| Raw `Accept: application/json` | Rest Assured `ContentType.JSON` adds `application/javascript` and Booker returns **418**. | Slightly less idiomatic Rest Assured; documented in the contract table. |
| Sequential tests, Surefire not Allure | Parallelism on a shared demo API causes cross-talk. Surefire is already the Maven report; Allure is a second toolchain. | No history/trends UI until someone adds a reporter (see “Next with more time”). |
| `ConfigException` vs `IllegalArgumentException` | Missing env/config is ops; null id/body/token is a programmer error. | Callers must know two failure types. |

---

## What is covered

`AuthTests`

- Valid credentials issue a token
- Wrong password and empty credentials return `Bad credentials`

`BookingTests`

- Create then GET returns the same details
- Full PUT replaces the booking
- PATCH changes only the patched field
- DELETE then GET is 404
- Filter by unique name and by stay dates
- Unknown id is 404
- PUT/DELETE without a token, or with a garbage token, are rejected
- Empty create body is not treated as a successful create

---

## Restful Booker contract

The live API does not follow textbook REST. Tests assert the real behavior:

| Call | What Booker actually does |
|---|---|
| `POST /booking` | **200**, not 201 |
| `DELETE /booking/{id}` | **201** |
| Missing or invalid token | **403**, not 401 |
| Bad `POST /auth` | **200** with `{ "reason": "Bad credentials" }` |
| Empty create body | Often **500**; the test only requires it is not 200 |
| `Accept` header | Must be plain `application/json`. Rest Assured's `ContentType.JSON` also lists `application/javascript` and Booker returns **418** |
| Date query params | Exclusive (greater-than / less-than). Filter with a window around the dates you created, not the exact same strings |

---

## CI

GitHub Actions (`.github/workflows/api-tests.yml`) on every push and pull request:

1. JDK 17 (Temurin) with the Maven cache
2. `GET /ping` with retries (the public demo API is often cold)
3. **Run the API suite** — `mvn -B test` (Surefire; this project does not use Failsafe)
4. **Publish reports** — `mvn -B surefire-report:report-only` so HTML is built from the run that just finished (tests are not re-executed). Then upload `target/surefire-reports` (XML, txt, `*-output.txt` request/response dumps) and `target/reports/surefire.html` as a workflow artifact, on **success and failure**
5. **Fail the job when tests fail** — a final step exits non-zero if the Maven test step did not succeed, so the check is red even after reports were uploaded

Download the artifact from the workflow run. Open `surefire.html` for the summary, then the matching `*-output.txt` for the Rest Assured dump.

There is no Allure (or Extent) reporter in this repo. Surefire XML + HTML is the published evidence. Adding Allure later would be a reporter swap, not a second suite.

---

## Evolving this into a real quality gate

Today the workflow is a **signal**: it runs the suite, publishes Surefire output, and goes red when a test fails. A real gate is that signal **plus** branch protection, a policy for flakes, and a plan so the check stays fast as the suite grows.

### What should block a merge

Treat this job as a **required status check** on `main` (GitHub: branch protection / rulesets → require “API tests / API suite”). A red run must block the merge. A skipped or missing check must also block — otherwise a workflow file delete would bypass the gate.

**Block the merge when:**

- Any test in this suite fails (auth, CRUD, query, negatives). Contract drift is a product defect or a test defect; either way it is not mergeable until someone decides.
- The suite does not run (compile error, missing JDK, empty artifact). No results is not a pass.
- New tests or client changes land without going through the same job (no “I’ll run it locally”).

**Do not block the merge on:**

- Public Booker being down after `/ping` retries exhausted — that is infrastructure, not this PR. Fail the job (honest signal) but allow a documented override / re-run rather than inventing a green skip.
- Style-only or docs-only PRs, if you later add path filters. API and `pom.xml` changes must always run the suite.
- One flaky test you have turned off with a ticket id (see below). That is a named skip with an expiry, not “retry until green.”

Optional later split: a **PR smoke** required check (auth + create/GET/delete) and a **full suite** required on `main` or nightly. Until the suite is slow, keep one required job — this one.

### Flaky tests

**How they show up here.** Almost always the **public demo API**: it resets about every 10 minutes, is slow when cold, and returns 418 if `Accept` is wrong. It is not a sleep/timing bug in our client.

**What to do**

1. Confirm it is a flake (fail then pass, same test, no diff).
2. Turn **that one method** off with `@Disabled("flake: ticket-id")`, or tag it `flaky` and skip that tag in the job that blocks merge. Leave the rest of the class running. Optionally run the tagged tests in a second job that does **not** block merge, so they stay visible.
3. Give it an owner and a date. Then fix it (assert, unique data, or a note in the contract table) or delete it. Do not leave it disabled forever.

**What not to do**

- Do not retry create / update / delete in CI. You would create extra bookings and might miss a real server error.
- `/ping` with retries before the suite is fine (API warmup). That is the only retry we want in the pipeline.
- If we retry GETs later, do it inside `BookerClient` after review — not `retry: 3` around `mvn test`.

On an API you **own**, flakes should be rare: each test already creates its own booking and never uses seed IDs 1–10.

### Keeping the suite fast as it grows

While the suite is small, run **all** tests on every PR. That is what we do now.

When PRs start waiting too long:

- **PR job:** a short smoke set only — ping, login, create, GET, delete.
- **Full suite:** on `main` and once a night.
- **Do not** run tests in parallel against public Restful Booker. Everyone shares that data. Parallel is OK only against your own server, one class at a time.
- If one job is still over ~10 minutes on your own server, split `AuthTests` and `BookingTests` into two jobs.
- Keep uploading Surefire reports. Add Allure only if you need history charts. Same rule: upload reports even when tests fail, and still fail the job.
- Keep the Maven cache (already in the workflow). Fail compile before tests so you do not wait on HTTP for a broken build.
- Keep creating fresh data per test so jobs can be split later. Query tests already check that **our** booking id is in the list, not that the list has exactly one row.

The check is doing its job when it fails on real bugs, finishes soon enough that people wait for it, and does not pretend flakes are passes. Reports on every run help you tell those apart.

---

## What I’d do next with more time

In priority order, assuming the current suite stays the source of truth:

1. **Required check on `main`** — turn “API tests / API suite” into a GitHub ruleset required status so a red or missing run cannot merge. Path filters for docs-only PRs.
2. **Stable target** — run CI against a Booker instance you control (container or stub) so public reset/cold-start is not the flake source. Keep public heroku as an optional `dev` profile.
3. **JUnit tags** — `@Tag("smoke")` on auth + create/GET/delete; PR job runs smoke, nightly/`main` runs everything. Only worth it once wall clock hurts.
4. **JSON Schema (or OpenAPI) asserts** on happy-path bodies, in addition to POJO field checks — catches new/removed fields the Java model silently ignores.
5. **Idempotent GET retry in `BookerClient`** (bounded, logged) for transient 5xx; still no pipeline-level retry of PUT/PATCH/DELETE.
6. **Allure (optional)** — same Maven job, generate after Surefire, upload always, fail on test failure. Only if someone needs history; not a second suite.
7. **Parallel + shards** — only on the isolated env, by test class. Never against public Booker.
9. **More Booker edges** — additional PATCH combinations, date-filter exclusivity as a dedicated example, ping as a health check test (today CI curls `/ping` instead).

---

## AI tool usage

This section is about how the repo was (and should be) edited with an assistant. It is not required to run `mvn test`.

**What was used.** Cursor, plus repo-local **skills** and a **rule**. The agent reads those files and follows them when adding or healing tests. Humans still review diffs.

| Mechanism | Where | Role |
|---|---|---|
| Rule| `.cursor/rules/rest-assured.mdc` | One `BookerClient`, thin tests, factory data, no seed IDs, `Accept` as a raw header, assisted heal only |
| Skills | `.cursor/skills/new-api-test`, `update-api-test`, `heal-api-test` | Step-by-step for add / extend / heal |
| Slash commands | `.cursor/commands/*.md` | Invoke those skills (`/new-api-test`, `/update-api-test`, `/heal-api-test`) |

Healing is **assisted only**: propose POJO/assert changes from Surefire output, show the diff, wait for review. Never auto-commit. Never silently rewrite a passing test.

**MCP (Model Context Protocol).** **No MCP server generated, scaffolded, or integrated these API tests.** 
