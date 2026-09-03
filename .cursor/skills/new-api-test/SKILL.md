---
name: new-api-test
description: Adds a new Restful Booker API test following this project's thin-test conventions. Use when the user asks to write, add, or scaffold a new API test.
---

# New API test

1. Put the test in `AuthTests` (token behavior) or `BookingTests` (booking lifecycle, query, booking negatives). Do not create a new test class unless the user asks.
2. Arrange with `BookingDataFactory` when a booking is needed. Act through `BookerClient` only. Assert with `ResponseAssert` and/or `BookingAssert`.
3. Name the method as a `validate…` behavior (`validateCreatedBookingCanBeRetrievedWithSameDetails`), not HTTP verbs alone.
4. Tests stay independent: create own data; no seed IDs; no shared mutable ids across tests.
5. If a new endpoint is required, add it to `BookerClient` (typed happy path + `submit…`/`fetch…` for negatives). Do not put Rest Assured in the test.
6. Reuse existing `*Request` / `*Response` models, or add a new pair only if the payload shape is new. Assert on the deserialized model, not a JSON schema.
