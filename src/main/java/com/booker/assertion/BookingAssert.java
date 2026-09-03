package com.booker.assertion;

import com.booker.model.AuthResponse;
import com.booker.model.BookingDates;
import com.booker.model.BookingIdResponse;
import com.booker.model.BookingRequest;
import com.booker.model.BookingResponse;
import com.booker.model.CreateBookingResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public final class BookingAssert {

    private BookingAssert() {
    }

    public static void assertAuthResponseHasToken(AuthResponse authResponse) {
        assertThat(authResponse).isNotNull();
        assertThat(authResponse.getToken()).isNotBlank();
    }

    public static void assertCreateBookingResponseIsComplete(CreateBookingResponse createBookingResponse) {
        assertThat(createBookingResponse).isNotNull();
        assertThat(createBookingResponse.getBookingid()).isNotNull().isPositive();
        assertBookingResponseIsComplete(createBookingResponse.getBooking());
    }

    public static void assertBookingResponseIsComplete(BookingResponse bookingResponse) {
        assertThat(bookingResponse).isNotNull();
        assertThat(bookingResponse.getFirstname()).isNotBlank();
        assertThat(bookingResponse.getLastname()).isNotBlank();
        assertThat(bookingResponse.getTotalprice()).isNotNull();
        assertThat(bookingResponse.getDepositpaid()).isNotNull();
        assertThat(bookingResponse.getBookingdates()).isNotNull();
        assertThat(bookingResponse.getBookingdates().getCheckin()).isNotBlank();
        assertThat(bookingResponse.getBookingdates().getCheckout()).isNotBlank();
    }

    public static void assertBookingMatches(BookingRequest submittedBooking, BookingResponse returnedBooking) {
        assertBookingResponseIsComplete(returnedBooking);
        assertThat(returnedBooking.getFirstname()).isEqualTo(submittedBooking.getFirstname());
        assertThat(returnedBooking.getLastname()).isEqualTo(submittedBooking.getLastname());
        assertThat(returnedBooking.getTotalprice()).isEqualTo(submittedBooking.getTotalprice());
        assertThat(returnedBooking.getDepositpaid()).isEqualTo(submittedBooking.getDepositpaid());
        assertThat(returnedBooking.getAdditionalneeds()).isEqualTo(submittedBooking.getAdditionalneeds());
        assertDatesMatch(submittedBooking.getBookingdates(), returnedBooking.getBookingdates());
    }

    public static void assertOnlyFirstnameChanged(
            BookingRequest originalBooking, BookingResponse patchedBooking, String expectedFirstname) {
        assertBookingResponseIsComplete(patchedBooking);
        assertThat(patchedBooking.getFirstname()).isEqualTo(expectedFirstname);
        assertThat(patchedBooking.getLastname()).isEqualTo(originalBooking.getLastname());
        assertThat(patchedBooking.getTotalprice()).isEqualTo(originalBooking.getTotalprice());
        assertThat(patchedBooking.getDepositpaid()).isEqualTo(originalBooking.getDepositpaid());
        assertThat(patchedBooking.getAdditionalneeds()).isEqualTo(originalBooking.getAdditionalneeds());
        assertDatesMatch(originalBooking.getBookingdates(), patchedBooking.getBookingdates());
    }

    public static void assertContainsBookingId(List<BookingIdResponse> matchingBookingIds, int expectedBookingId) {
        assertThat(matchingBookingIds)
                .extracting(BookingIdResponse::getBookingid)
                .contains(expectedBookingId);
    }

    public static void assertTokenIsPresent(String token) {
        assertThat(token).isNotBlank();
    }

    public static void assertBadCredentialsReason(String reason) {
        assertThat(reason).containsIgnoringCase("Bad credentials");
    }

    private static void assertDatesMatch(BookingDates expectedDates, BookingDates actualDates) {
        assertThat(actualDates).isNotNull();
        assertThat(actualDates.getCheckin()).isEqualTo(expectedDates.getCheckin());
        assertThat(actualDates.getCheckout()).isEqualTo(expectedDates.getCheckout());
    }
}
