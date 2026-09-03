---
name: update-api-test
description: Extends an existing Restful Booker test (new field, extra assert, client method) without adding new client classes. Use when the user asks to update or extend an API test.
---

# Update API test

1. Prefer changing the existing `@Test` or adding a sibling method in the same class.
2. New payload fields go on the request/response POJO + `BookingDataFactory` + `BookingAssert.assertBookingMatches`.
3. Do not introduce `AuthClient` / `Utils` / extra `*Client` types. Extend `BookerClient`.
4. Keep tests thin: no JSONPath, headers, or status codes in the test body unless the line is the negative business outcome via `ResponseAssert`.
5. Keep independence: do not chain tests or reuse ids from another method.
