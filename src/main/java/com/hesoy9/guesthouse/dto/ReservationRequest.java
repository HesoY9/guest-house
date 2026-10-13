package com.hesoy9.guesthouse.dto;

import com.hesoy9.guesthouse.entity.BookingChannel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class ReservationRequest {

    // No longer @NotNull: blank means "register the new guest below instead of
    // picking an existing one". The controller enforces that one or the other is supplied.
    private Long guestId;

    // Only used when guestId is blank - a brand-new guest being registered inline.
    // No validation annotations here since they're conditionally required, checked in the controller.
    private String newGuestIdOrPassport;
    private String newGuestName;
    private String newGuestContactNumber;
    private String newGuestEmail;

    @NotNull(message = "Room is required")
    private Long roomId;

    @NotNull(message = "Check-in date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOutDate;

    @NotNull(message = "Number of guests is required")
    @Positive(message = "Number of guests must be at least 1")
    private Integer numberOfGuests;

    @NotNull(message = "Booking channel is required")
    private BookingChannel bookingChannel;

    @Size(max = 500, message = "Notes can be at most 500 characters")
    private String notes;

    public Long getGuestId() { return guestId; }
    public void setGuestId(Long guestId) { this.guestId = guestId; }

    public String getNewGuestIdOrPassport() { return newGuestIdOrPassport; }
    public void setNewGuestIdOrPassport(String newGuestIdOrPassport) { this.newGuestIdOrPassport = newGuestIdOrPassport; }

    public String getNewGuestName() { return newGuestName; }
    public void setNewGuestName(String newGuestName) { this.newGuestName = newGuestName; }

    public String getNewGuestContactNumber() { return newGuestContactNumber; }
    public void setNewGuestContactNumber(String newGuestContactNumber) { this.newGuestContactNumber = newGuestContactNumber; }

    public String getNewGuestEmail() { return newGuestEmail; }
    public void setNewGuestEmail(String newGuestEmail) { this.newGuestEmail = newGuestEmail; }

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

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
