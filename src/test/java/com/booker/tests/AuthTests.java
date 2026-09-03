package com.booker.tests;

import com.booker.assertion.BookingAssert;
import com.booker.assertion.ResponseAssert;
import com.booker.client.BookerClient;
import com.booker.model.AuthRequest;
import com.booker.model.AuthResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthTests {

    private BookerClient client;
    private Response response;

    @BeforeEach
    void setUp() {
        client = new BookerClient();
    }

    @Test
    void validateAuthTokenWithValidCredentials() {
        String token = client.createToken();
        BookingAssert.assertTokenIsPresent(token);
    }

    @Test
    void validateAuthTokenRejectedWithWrongPassword() {
        response = client.submitAuthRequest(new AuthRequest("admin", "wrong-password"));
        ResponseAssert.ok(response);
        BookingAssert.assertBadCredentialsReason(response.as(AuthResponse.class).getReason());
    }

    @Test
    void validateAuthTokenRejectedWithEmptyCredentials() {
        response = client.submitAuthRequest(new AuthRequest("", ""));
        ResponseAssert.ok(response);
        BookingAssert.assertBadCredentialsReason(response.as(AuthResponse.class).getReason());
    }
}
