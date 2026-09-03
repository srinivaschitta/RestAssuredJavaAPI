package com.booker.client;

import com.booker.config.ConfigManager;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.util.Objects;

public abstract class BaseApiClient {

    protected final ConfigManager config = ConfigManager.getInstance();

    protected RequestSpecification requestSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(config.baseUri())
                .setContentType(ContentType.JSON)
                // Restful Booker returns 418 if Accept includes Rest Assured's extra javascript types.
                .addHeader("Accept", "application/json")
                .addFilter(new RequestLoggingFilter(LogDetail.ALL))
                .addFilter(new ResponseLoggingFilter(LogDetail.ALL))
                .build();
    }

    protected RequestSpecification authenticatedRequestSpec(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token is required");
        }
        return requestSpec().cookie("token", token);
    }

    protected static <T> T requireNonNullBody(T requestBody, String argumentName) {
        return Objects.requireNonNull(requestBody, argumentName + " is required");
    }

    protected static int requirePositiveId(Integer bookingId) {
        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException("id is required and must be positive");
        }
        return bookingId;
    }
}
