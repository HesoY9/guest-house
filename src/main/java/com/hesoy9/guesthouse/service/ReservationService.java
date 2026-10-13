package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.*;
import com.hesoy9.guesthouse.repository.GuestRepository;
import com.hesoy9.guesthouse.repository.ReservationRepository;
import com.hesoy9.guesthouse.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

// Every booking rule lives here, so the staff pages, the REST API and the public
// site all get exactly the same checks - none of them re-implement any of it.
@Service
public class ReservationService {

    // Longest single stay accepted online or at the desk - catches typos like 2062 instead of 2026.
    static final int MAX_NIGHTS = 60;

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;

    public ReservationService(ReservationRepository reservationRepository,
                               RoomRepository roomRepository,
                               GuestRepository guestRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.guestRepository = guestRepository;
    }

    // @Transactional: the room lock, the overlap check and the save happen as one unit
    @Transactional
    public Reservation createReservation(Long guestId, Long roomId, LocalDate checkIn,
                                          LocalDate checkOut, Integer numberOfGuests,
                                          BookingChannel channel) {
        return createReservation(guestId, roomId, checkIn, checkOut, numberOfGuests, channel, null);
    }

    @Transactional
    public Reservation createReservation(Long guestId, Long roomId, LocalDate checkIn,
                                          LocalDate checkOut, Integer numberOfGuests,
                                          BookingChannel channel, String notes) {

        validateDates(checkIn, checkOut, true);   // DR2 + no past check-in dates
        validateGuestCount(numberOfGuests);

        // Lock the room row first so two people booking the same room at the same moment
        // can't both pass the overlap check below.
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        validateCapacity(room, numberOfGuests);

        List<Reservation> overlaps = reservationRepository
                .findOverlappingReservations(roomId, checkIn, checkOut);
        if (!overlaps.isEmpty()) {
            throw new IllegalStateException("Room is already booked for these dates"); // DR1, FR8
        }

        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new IllegalArgumentException("Guest not found: " + guestId));

        Reservation reservation = new Reservation();
        reservation.setGuest(guest);
        reservation.setRoom(room);
        reservation.setCheckInDate(checkIn);
        reservation.setCheckOutDate(checkOut);
        reservation.setNumberOfGuests(numberOfGuests);
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setBookingChannel(channel != null ? channel : BookingChannel.IN_PERSON);
        reservation.setNotes(blankToNull(notes));

        // Room status is a "right now" snapshot for the front desk, so only a same-day
        // arrival marks the room as reserved. Future bookings are tracked by date instead
        // (the overlap query above and the room calendar).
        if (!checkIn.isAfter(LocalDate.now()) && room.getStatus() == RoomStatus.AVAILABLE) {
            room.setStatus(RoomStatus.RESERVED);
            roomRepository.save(room);
        }

        return reservationRepository.save(reservation); // FR5, FR6 (id is auto-generated)
    }

    // Price summary + "would this booking work?" without saving anything.
    // Used by the live summary on the public room/booking pages and the staff booking form.
    @Transactional(readOnly = true)
    public BookingQuote quote(Long roomId, LocalDate checkIn, LocalDate checkOut, Integer guests) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        long nights = (checkIn != null && checkOut != null && checkOut.isAfter(checkIn))
                ? ChronoUnit.DAYS.between(checkIn, checkOut) : 0;
        double rate = room.getPrice() != null ? room.getPrice() : 0;

        String problem = null;
        try {
            if (checkIn == null || checkOut == null) {
                throw new IllegalArgumentException("Choose your check-in and check-out dates");
            }
            validateDates(checkIn, checkOut, true);
            if (guests != null) {
                validateGuestCount(guests);
                validateCapacity(room, guests);
            }
            if (!reservationRepository.findOverlappingReservations(roomId, checkIn, checkOut).isEmpty()) {
                throw new IllegalStateException("Sorry - this room is already booked for those dates");
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            problem = ex.getMessage();
        }
        return new BookingQuote(room, checkIn, checkOut, guests, nights, rate, nights * rate, problem);
    }

    // FR9 - staff cancellation
    @Transactional
    public Reservation cancelReservation(Long reservationId) {
        Reservation reservation = getReservation(reservationId);

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Only active reservations can be cancelled");
        }
        if (reservation.isCheckedIn()) {
            throw new IllegalStateException("The guest is already checked in - check them out instead");
        }
        return doCancel(reservation);
    }

    // Guest self-service cancellation from "My Bookings": their own booking, before check-in only.
    @Transactional
    public Reservation cancelByGuest(Long reservationId, Long guestId) {
        Reservation reservation = getReservation(reservationId);

        // Same message as "not found", so guests can't probe other people's booking numbers.
        if (!reservation.getGuest().getId().equals(guestId)) {
            throw new IllegalArgumentException("Reservation not found: " + reservationId);
        }
        if (!reservation.guestCancellableOn(LocalDate.now())) {
            throw new IllegalStateException(
                    "This booking can no longer be cancelled online - please contact the front desk");
        }
        return doCancel(reservation);
    }

    private Reservation doCancel(Reservation reservation) {
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());

        Room room = reservation.getRoom();
        if (room.getStatus() == RoomStatus.RESERVED) {
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepository.save(room);
        }

        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation modifyReservation(Long reservationId, LocalDate newCheckIn, LocalDate newCheckOut) { // FR10
        Reservation reservation = getReservation(reservationId);

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Only active reservations can be changed");
        }
        if (newCheckIn == null || newCheckOut == null) {
            throw new IllegalArgumentException("Check-in and check-out dates are required");
        }
        // Once the guest is in the room, only the departure date can move.
        if (reservation.isCheckedIn() && !newCheckIn.equals(reservation.getCheckInDate())) {
            throw new IllegalStateException("The guest is already checked in - only the check-out date can change");
        }
        validateDates(newCheckIn, newCheckOut, !reservation.isCheckedIn());

        roomRepository.findByIdForUpdate(reservation.getRoom().getId());
        List<Reservation> overlaps = reservationRepository.findOverlappingReservations(
                reservation.getRoom().getId(), newCheckIn, newCheckOut);
        overlaps.removeIf(r -> r.getId().equals(reservationId)); // ignore the reservation being modified

        if (!overlaps.isEmpty()) {
            throw new IllegalStateException("Room is already booked for the new dates");
        }

        reservation.setCheckInDate(newCheckIn);
        reservation.setCheckOutDate(newCheckOut);
        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));
    }

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    // Staff reservations list: status null = all statuses; q matches guest name, booking # or room #.
    public List<Reservation> search(ReservationStatus status, String q) {
        String term = blankToNull(q);
        if (term != null) {
            term = term.trim().replaceFirst("(?i)^BH-0*", ""); // "BH-000042" -> "42"
        }
        return reservationRepository.search(status, term);
    }

    public List<Reservation> getGuestReservations(Long guestId) {
        return reservationRepository.findByGuestIdOrderByCheckInDateDesc(guestId);
    }

    public List<Reservation> getArrivals(LocalDate today) {
        return reservationRepository.findArrivals(today);
    }

    public List<Reservation> getDepartures(LocalDate today) {
        return reservationRepository.findDepartures(today);
    }

    public List<Reservation> getInHouse() {
        return reservationRepository.findInHouse();
    }

    // ---- shared rules ----

    // DR2, plus: no check-in in the past (skipped when changing a stay that already started).
    private void validateDates(LocalDate checkIn, LocalDate checkOut, boolean checkInMustNotBePast) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Check-in and check-out dates are required");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date"); // DR2
        }
        if (checkInMustNotBePast && checkIn.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Check-in date can't be in the past");
        }
        if (ChronoUnit.DAYS.between(checkIn, checkOut) > MAX_NIGHTS) {
            throw new IllegalArgumentException("Stays longer than " + MAX_NIGHTS + " nights must be arranged with the front desk");
        }
    }

    private void validateGuestCount(Integer numberOfGuests) {
        if (numberOfGuests == null || numberOfGuests < 1) {
            throw new IllegalArgumentException("Number of guests must be at least 1");
        }
    }

    private void validateCapacity(Room room, Integer numberOfGuests) {
        if (room.getCapacity() != null && numberOfGuests > room.getCapacity()) {
            throw new IllegalArgumentException("Room " + room.getRoomNumber() + " sleeps at most "
                    + room.getCapacity() + (room.getCapacity() == 1 ? " guest" : " guests"));
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
