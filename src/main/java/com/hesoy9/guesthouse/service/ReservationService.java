package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.*;
import com.hesoy9.guesthouse.repository.GuestRepository;
import com.hesoy9.guesthouse.repository.ReservationRepository;
import com.hesoy9.guesthouse.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationService {

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

    // @Transactional: the overlap check and the save happen as one unit
    @Transactional
    public Reservation createReservation(Long guestId, Long roomId, LocalDate checkIn,
                                          LocalDate checkOut, Integer numberOfGuests,
                                          BookingChannel channel) {

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date"); // DR2
        }

        List<Reservation> overlaps = reservationRepository
                .findOverlappingReservations(roomId, checkIn, checkOut);
        if (!overlaps.isEmpty()) {
            throw new IllegalStateException("Room is already booked for these dates"); // DR1, FR8
        }

        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new IllegalArgumentException("Guest not found: " + guestId));
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

        Reservation reservation = new Reservation();
        reservation.setGuest(guest);
        reservation.setRoom(room);
        reservation.setCheckInDate(checkIn);
        reservation.setCheckOutDate(checkOut);
        reservation.setNumberOfGuests(numberOfGuests);
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setBookingChannel(channel);

        room.setStatus(RoomStatus.RESERVED);
        roomRepository.save(room);

        return reservationRepository.save(reservation); // FR5, FR6 (id is auto-generated)
    }

    public Reservation cancelReservation(Long reservationId) { // FR9
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        reservation.setStatus(ReservationStatus.CANCELLED);

        Room room = reservation.getRoom();
        if (room.getStatus() == RoomStatus.RESERVED) {
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepository.save(room);
        }

        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation modifyReservation(Long reservationId, LocalDate newCheckIn, LocalDate newCheckOut) { // FR10
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        if (!newCheckOut.isAfter(newCheckIn)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }

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
}
