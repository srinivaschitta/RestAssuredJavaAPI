package com.booker.assertion;

import io.restassured.response.Response;

import static org.assertj.core.api.Assertions.assertThat;

public final class ResponseAssert {

    private ResponseAssert() {
    }

    public static Response ok(Response response) {
        return status(response, 200);
    }

    public static Response created(Response response) {
        return status(response, 201);
    }

    public static Response notFound(Response response) {
        return status(response, 404);
    }

    /**
     * Restful Booker returns 403 Forbidden (not 401) when a cookie token is missing or invalid.
     */
    public static Response unauthorized(Response response) {
        assertThat(response.statusCode())
                .as("unauthorized should be 401 or 403 (Booker uses 403)")
                .isIn(401, 403);
        return response;
    }

    public static Response json(Response response) {
        assertThat(response.getContentType())
                .as("Content-Type should be JSON")
                .containsIgnoringCase("application/json");
        return response;
    }

    public static Response status(Response response, int expectedStatus) {
        assertThat(response.statusCode())
                .as("HTTP status")
                .isEqualTo(expectedStatus);
        return response;
    }

    /** Use when the API should not treat the call as a successful JSON create (Booker returns 200 on create). */
    public static Response notSuccessfulCreate(Response response) {
        assertThat(response.statusCode())
                .as("empty/invalid create should not return 200")
                .isNotEqualTo(200);
        return response;
    }
}
