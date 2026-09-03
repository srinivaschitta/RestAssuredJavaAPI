---
name: heal-api-test
description: Assisted healing for failing Rest Assured tests. Use when Surefire fails, a POJO is out of date, or Restful Booker contract drift is suspected. Never auto-commit.
---

# Heal API test (assisted)

This is not UI locator healing and not a watcher that rewrites tests on failure.

1. Read the failing test, Surefire XML/txt under `target/surefire-reports`, and request/response dumps in `target/surefire-reports/*-output.txt`.
2. Diff the actual response against the request/response POJO.
3. If the **live API contract** changed, propose updates to the POJO and/or `ResponseAssert` / `BookingAssert`.
4. If this is a known Restful Booker quirk (200 on create, 201 on delete, 403 instead of 401, 200 + `reason` on bad auth), keep asserting the real contract and add a short comment — do not "fix" the test to textbook REST.
5. Show the proposed diff and wait for review. Never auto-commit. Never silently edit passing tests.
