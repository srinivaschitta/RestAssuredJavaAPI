package com.booker.data;

import com.booker.model.BookingDates;
import com.booker.model.BookingRequest;

import java.util.UUID;

public final class BookingDataFactory {

    private BookingDataFactory() {
    }

    public static BookingRequest newValidBooking() {
        return newValidBookingWithDates("2026-03-01", "2026-03-05");
    }

    public static BookingRequest newValidBookingWithDates(String checkinDate, String checkoutDate) {
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setFirstname("Auto" + uniqueSuffix);
        bookingRequest.setLastname("Test" + uniqueSuffix);
        bookingRequest.setTotalprice(120);
        bookingRequest.setDepositpaid(true);
        bookingRequest.setBookingdates(new BookingDates(checkinDate, checkoutDate));
        bookingRequest.setAdditionalneeds("Breakfast");
        return bookingRequest;
    }

    public static BookingRequest copyOf(BookingRequest sourceBooking) {
        BookingRequest bookingCopy = new BookingRequest();
        bookingCopy.setFirstname(sourceBooking.getFirstname());
        bookingCopy.setLastname(sourceBooking.getLastname());
        bookingCopy.setTotalprice(sourceBooking.getTotalprice());
        bookingCopy.setDepositpaid(sourceBooking.getDepositpaid());
        bookingCopy.setAdditionalneeds(sourceBooking.getAdditionalneeds());
        if (sourceBooking.getBookingdates() != null) {
            bookingCopy.setBookingdates(new BookingDates(
                    sourceBooking.getBookingdates().getCheckin(),
                    sourceBooking.getBookingdates().getCheckout()));
        }
        return bookingCopy;
    }
}
