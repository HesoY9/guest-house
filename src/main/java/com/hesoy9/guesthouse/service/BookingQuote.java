package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.Room;

import java.time.LocalDate;

// The "nights x price = total" summary shown before a booking is confirmed.
// 'problem' is null when the booking would succeed, otherwise the reason it wouldn't
// (same wording ReservationService would throw), so the page can warn before submitting.
public record BookingQuote(Room room,
                           LocalDate checkIn,
                           LocalDate checkOut,
                           Integer guests,
                           long nights,
                           double nightlyRate,
                           double total,
                           String problem) {

    public boolean ok() {
        return problem == null;
    }

    public boolean hasDates() {
        return checkIn != null && checkOut != null && nights > 0;
    }
}
