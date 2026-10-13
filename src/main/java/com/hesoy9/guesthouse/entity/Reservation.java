package com.hesoy9.guesthouse.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ManyToOne: many reservations can point to the same guest.
    // @JoinColumn creates the actual foreign key column (guest_id) in the reservations table.
    @ManyToOne
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "number_of_guests", nullable = false)
    private Integer numberOfGuests;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_channel", nullable = false, length = 20)
    private BookingChannel bookingChannel;

    // Set when the receptionist checks the guest in. Status stays ACTIVE while the guest is
    // in house, so every "is this room taken?" query keeps working unchanged.
    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // Null for reservations made before this column existed.
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Special requests from the guest / internal notes from staff.
    @Column(length = 500)
    private String notes;

    public Reservation() {
        // required by JPA
    }

    public Reservation(Guest guest, Room room, LocalDate checkInDate, LocalDate checkOutDate, Integer numberOfGuests, ReservationStatus status, BookingChannel bookingChannel) {
        this.guest = guest;
        this.room = room;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.numberOfGuests = numberOfGuests;
        this.status = status;
        this.bookingChannel = bookingChannel;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ---- derived values (not stored) ----

    // Human-friendly booking reference shown to guests and staff, e.g. "BH-000042".
    public String getReference() {
        return id == null ? "" : String.format("BH-%06d", id);
    }

    public long getNights() {
        if (checkInDate == null || checkOutDate == null) return 0;
        return ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    // nights x room price - the amount the invoice will be for (FR17)
    public double getEstimatedTotal() {
        if (room == null || room.getPrice() == null) return 0;
        return getNights() * room.getPrice();
    }

    public boolean isCheckedIn() {
        return checkedInAt != null;
    }

    public ReservationStage getStage() {
        return stageOn(LocalDate.now());
    }

    public ReservationStage stageOn(LocalDate today) {
        if (status == ReservationStatus.CANCELLED) return ReservationStage.CANCELLED;
        if (status == ReservationStatus.COMPLETED) return ReservationStage.COMPLETED;
        if (isCheckedIn()) {
            return checkOutDate.isAfter(today) ? ReservationStage.IN_HOUSE : ReservationStage.DUE_OUT;
        }
        if (checkInDate.isAfter(today)) return ReservationStage.UPCOMING;
        return checkOutDate.isAfter(today) ? ReservationStage.ARRIVING : ReservationStage.NO_SHOW;
    }

    // Guest self-service rule: an active booking the guest hasn't checked in to yet,
    // up to and including the arrival day. ReservationService.cancelByGuest enforces the same rule.
    public boolean isGuestCancellable() {
        return guestCancellableOn(LocalDate.now());
    }

    public boolean guestCancellableOn(LocalDate today) {
        return status == ReservationStatus.ACTIVE
                && !isCheckedIn()
                && !checkInDate.isBefore(today);
    }

    // ---- getters / setters ----

    public Long getId() {
        return id;
    }

    public Guest getGuest() {
        return guest;
    }

    public void setGuest(Guest guest) {
        this.guest = guest;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public Integer getNumberOfGuests() {
        return numberOfGuests;
    }

    public void setNumberOfGuests(Integer numberOfGuests) {
        this.numberOfGuests = numberOfGuests;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public BookingChannel getBookingChannel() {
        return bookingChannel;
    }

    public void setBookingChannel(BookingChannel bookingChannel) {
        this.bookingChannel = bookingChannel;
    }

    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(LocalDateTime checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public LocalDateTime getCheckedOutAt() {
        return checkedOutAt;
    }

    public void setCheckedOutAt(LocalDateTime checkedOutAt) {
        this.checkedOutAt = checkedOutAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
