package com.booker.client;

import com.booker.assertion.BookingAssert;
import com.booker.assertion.ResponseAssert;
import com.booker.model.AuthRequest;
import com.booker.model.AuthResponse;
import com.booker.model.BookingIdResponse;
import com.booker.model.BookingRequest;
import com.booker.model.BookingResponse;
import com.booker.model.CreateBookingResponse;
import io.restassured.response.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class BookerClient extends BaseApiClient {

    public String createToken() {
        return createToken(new AuthRequest(config.username(), config.password()));
    }

    public String createToken(AuthRequest authRequest) {
        Response httpResponse = postAuth(requireNonNullBody(authRequest, "auth request"));
        ResponseAssert.ok(httpResponse);
        ResponseAssert.json(httpResponse);
        AuthResponse authResponse = httpResponse.as(AuthResponse.class);
        BookingAssert.assertAuthResponseHasToken(authResponse);
        return authResponse.getToken();
    }

    /** POSTs /auth and returns the HTTP response as-is. Use for negative cases. */
    public Response submitAuthRequest(AuthRequest authRequest) {
        return postAuth(requireNonNullBody(authRequest, "auth request"));
    }

    public CreateBookingResponse createBooking(BookingRequest bookingRequest) {
        Response httpResponse = postBooking(requireNonNullBody(bookingRequest, "booking request"));
        // Restful Booker returns 200 (not 201) on create.
        ResponseAssert.ok(httpResponse);
        ResponseAssert.json(httpResponse);
        CreateBookingResponse createBookingResponse = httpResponse.as(CreateBookingResponse.class);
        BookingAssert.assertCreateBookingResponseIsComplete(createBookingResponse);
        return createBookingResponse;
    }

    /** POSTs /booking and returns the HTTP response as-is. Use for negative cases. */
    public Response submitCreateBooking(BookingRequest bookingRequest) {
        return postBooking(bookingRequest);
    }

    public BookingResponse getBooking(int bookingId) {
        Response httpResponse = fetchBooking(requirePositiveId(bookingId));
        ResponseAssert.ok(httpResponse);
        ResponseAssert.json(httpResponse);
        BookingResponse bookingResponse = httpResponse.as(BookingResponse.class);
        BookingAssert.assertBookingResponseIsComplete(bookingResponse);
        return bookingResponse;
    }

    /** GETs /booking/{id} and returns the HTTP response as-is. Use for negative cases. */
    public Response fetchBooking(int bookingId) {
        return given().spec(requestSpec())
                .when()
                .get("/booking/{id}", requirePositiveId(bookingId));
    }

    public BookingResponse updateBooking(int bookingId, BookingRequest bookingRequest, String token) {
        Response httpResponse = given().spec(authenticatedRequestSpec(token))
                .body(requireNonNullBody(bookingRequest, "booking request"))
                .when()
                .put("/booking/{id}", requirePositiveId(bookingId));
        ResponseAssert.ok(httpResponse);
        ResponseAssert.json(httpResponse);
        BookingResponse bookingResponse = httpResponse.as(BookingResponse.class);
        BookingAssert.assertBookingResponseIsComplete(bookingResponse);
        return bookingResponse;
    }

    public Response submitUpdateWithoutToken(int bookingId, BookingRequest bookingRequest) {
        return given().spec(requestSpec())
                .body(requireNonNullBody(bookingRequest, "booking request"))
                .when()
                .put("/booking/{id}", requirePositiveId(bookingId));
    }

    public Response submitUpdateWithToken(int bookingId, BookingRequest bookingRequest, String token) {
        return given().spec(authenticatedRequestSpec(token))
                .body(requireNonNullBody(bookingRequest, "booking request"))
                .when()
                .put("/booking/{id}", requirePositiveId(bookingId));
    }

    public BookingResponse patchBooking(int bookingId, Map<String, Object> fieldsToPatch, String token) {
        Response httpResponse = given().spec(authenticatedRequestSpec(token))
                .body(requireNonNullBody(fieldsToPatch, "patch fields"))
                .when()
                .patch("/booking/{id}", requirePositiveId(bookingId));
        ResponseAssert.ok(httpResponse);
        ResponseAssert.json(httpResponse);
        BookingResponse bookingResponse = httpResponse.as(BookingResponse.class);
        BookingAssert.assertBookingResponseIsComplete(bookingResponse);
        return bookingResponse;
    }

    public Response deleteBooking(int bookingId, String token) {
        Response deleteResponse = given().spec(authenticatedRequestSpec(token))
                .when()
                .delete("/booking/{id}", requirePositiveId(bookingId));
        // Restful Booker returns 201 Created on a successful delete.
        return ResponseAssert.created(deleteResponse);
    }

    public Response submitDeleteWithoutToken(int bookingId) {
        return given().spec(requestSpec())
                .when()
                .delete("/booking/{id}", requirePositiveId(bookingId));
    }

    public List<BookingIdResponse> getBookingIds(Map<String, String> queryFilters) {
        Response httpResponse = given().spec(requestSpec())
                .queryParams(queryFilters == null ? Map.of() : queryFilters)
                .when()
                .get("/booking");
        ResponseAssert.ok(httpResponse);
        ResponseAssert.json(httpResponse);
        return Arrays.asList(httpResponse.as(BookingIdResponse[].class));
    }

    public Response ping() {
        return given().spec(requestSpec()).when().get("/ping");
    }

    private Response postAuth(AuthRequest authRequest) {
        return given().spec(requestSpec())
                .body(authRequest)
                .when()
                .post("/auth");
    }

    private Response postBooking(BookingRequest bookingRequest) {
        return given().spec(requestSpec())
                .body(bookingRequest)
                .when()
                .post("/booking");
    }
}
