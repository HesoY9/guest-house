package com.hesoy9.guesthouse.dto;

import com.hesoy9.guesthouse.entity.BookingChannel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public class ReservationRequest {

    @NotNull(message = "Guest is required")
    private Long guestId;

    @NotNull(message = "Room is required")
    private Long roomId;

    @NotNull(message = "Check-in date is required")
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    private LocalDate checkOutDate;

    @NotNull(message = "Number of guests is required")
    @Positive(message = "Number of guests must be at least 1")
    private Integer numberOfGuests;

    @NotNull(message = "Booking channel is required")
    private BookingChannel bookingChannel;

    public Long getGuestId() { return guestId; }
    public void setGuestId(Long guestId) { this.guestId = guestId; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }

    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }

    public Integer getNumberOfGuests() { return numberOfGuests; }
    public void setNumberOfGuests(Integer numberOfGuests) { this.numberOfGuests = numberOfGuests; }

    public BookingChannel getBookingChannel() { return bookingChannel; }
    public void setBookingChannel(BookingChannel bookingChannel) { this.bookingChannel = bookingChannel; }
}
