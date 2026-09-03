package com.booker.tests;

import com.booker.assertion.BookingAssert;
import com.booker.assertion.ResponseAssert;
import com.booker.client.BookerClient;
import com.booker.data.BookingDataFactory;
import com.booker.model.BookingIdResponse;
import com.booker.model.BookingRequest;
import com.booker.model.BookingResponse;
import com.booker.model.CreateBookingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

class BookingTests {

    private BookerClient client;

    @BeforeEach
    void setUp() {
        client = new BookerClient();
    }

    @Test
    void validateCreatedBookingCanBeRetrievedWithSameDetails() {
        BookingRequest bookingToCreate = BookingDataFactory.newValidBooking();
        CreateBookingResponse createdBooking = client.createBooking(bookingToCreate);
        BookingResponse retrievedBooking = client.getBooking(createdBooking.getBookingid());
        BookingAssert.assertBookingMatches(bookingToCreate, retrievedBooking);
    }

    @Test
    void validateFullUpdateReplacesTheBooking() {
        BookingRequest originalBooking = BookingDataFactory.newValidBooking();
        int bookingId = client.createBooking(originalBooking).getBookingid();
        String token = client.createToken();

        BookingRequest replacementBooking = BookingDataFactory.newValidBooking();
        BookingResponse updatedBooking = client.updateBooking(bookingId, replacementBooking, token);

        BookingAssert.assertBookingMatches(replacementBooking, updatedBooking);
        BookingAssert.assertBookingMatches(replacementBooking, client.getBooking(bookingId));
    }

    @Test
    void validatePartialUpdateChangesOnlyThePatchedField() {
        BookingRequest originalBooking = BookingDataFactory.newValidBooking();
        int bookingId = client.createBooking(originalBooking).getBookingid();
        String token = client.createToken();
        String patchedFirstname = "Patch" + UUID.randomUUID().toString().substring(0, 8);

        BookingResponse patchedBooking = client.patchBooking(bookingId, Map.of("firstname", patchedFirstname), token);

        BookingAssert.assertOnlyFirstnameChanged(originalBooking, patchedBooking, patchedFirstname);
        BookingAssert.assertOnlyFirstnameChanged(originalBooking, client.getBooking(bookingId), patchedFirstname);
    }

    @Test
    void validateDeletedBookingCanNoLongerBeRetrieved() {
        int bookingId = client.createBooking(BookingDataFactory.newValidBooking()).getBookingid();
        client.deleteBooking(bookingId, client.createToken());
        ResponseAssert.notFound(client.fetchBooking(bookingId));
    }

    @Test
    void validateBookingsCanBeFoundByUniqueName() {
        BookingRequest bookingToCreate = BookingDataFactory.newValidBooking();
        int bookingId = client.createBooking(bookingToCreate).getBookingid();

        List<BookingIdResponse> matchingBookingIds = client.getBookingIds(Map.of(
                "firstname", bookingToCreate.getFirstname(),
                "lastname", bookingToCreate.getLastname()));

        BookingAssert.assertContainsBookingId(matchingBookingIds, bookingId);
    }

    @Test
    void validateBookingsCanBeFoundByStayDates() {
        String checkinDate = "2024-01-10";
        String checkoutDate = "2024-01-20";
        BookingRequest bookingToCreate = BookingDataFactory.newValidBookingWithDates(checkinDate, checkoutDate);
        int bookingId = client.createBooking(bookingToCreate).getBookingid();

        // Booker's date filter is exclusive on the supplied checkin (greater-than, not equal).
        List<BookingIdResponse> matchingBookingIds = client.getBookingIds(Map.of(
                "checkin", "2024-01-09",
                "checkout", "2024-01-21"));

        BookingAssert.assertContainsBookingId(matchingBookingIds, bookingId);
    }

    @Test
    void validateUnknownBookingIdIsNotFound() {
        ResponseAssert.notFound(client.fetchBooking(9_999_999));
    }

    @Test
    void validateUpdateWithoutTokenIsRejected() {
        int bookingId = client.createBooking(BookingDataFactory.newValidBooking()).getBookingid();
        ResponseAssert.unauthorized(client.submitUpdateWithoutToken(bookingId, BookingDataFactory.newValidBooking()));
    }

    @Test
    void validateUpdateWithGarbageTokenIsRejected() {
        int bookingId = client.createBooking(BookingDataFactory.newValidBooking()).getBookingid();
        ResponseAssert.unauthorized(
                client.submitUpdateWithToken(bookingId, BookingDataFactory.newValidBooking(), "not-a-real-token"));
    }

    @Test
    void validateDeleteWithoutTokenIsRejected() {
        int bookingId = client.createBooking(BookingDataFactory.newValidBooking()).getBookingid();
        ResponseAssert.unauthorized(client.submitDeleteWithoutToken(bookingId));
    }

    @Test
    void validateEmptyBookingBodyIsNotCreated() {
        // Restful Booker often returns 500 rather than 400; either way it must not create a booking (200).
        ResponseAssert.notSuccessfulCreate(client.submitCreateBooking(new BookingRequest()));
    }
}
