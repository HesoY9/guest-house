package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.*;
import com.hesoy9.guesthouse.repository.GuestRepository;
import com.hesoy9.guesthouse.repository.ReservationRepository;
import com.hesoy9.guesthouse.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class CheckInOutService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final BillingService billingService;
    private final GuestRepository guestRepository;

    public CheckInOutService(ReservationRepository reservationRepository,
                              RoomRepository roomRepository,
                              BillingService billingService,
                              GuestRepository guestRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.billingService = billingService;
        this.guestRepository = guestRepository;
    }

    @Transactional
    public Reservation checkIn(Long reservationId) { // FR14
        return checkIn(reservationId, null);
    }

    // idOrPassport: lets the receptionist record the guest's ID at the desk in the same step,
    // for guests who booked online and haven't given one yet (DR3).
    @Transactional
    public Reservation checkIn(Long reservationId, String idOrPassport) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Only active reservations can be checked in");
        }
        if (reservation.isCheckedIn()) {
            throw new IllegalStateException("This guest is already checked in");
        }
        LocalDate today = LocalDate.now();
        if (reservation.getCheckInDate().isAfter(today)) {
            throw new IllegalStateException("This booking starts on " + reservation.getCheckInDate()
                    + " - change the dates first for an early arrival");
        }
        if (!reservation.getCheckOutDate().isAfter(today)) {
            throw new IllegalStateException("This booking's check-out date has already passed");
        }

        Guest guest = reservation.getGuest();
        if (isBlank(guest.getIdOrPassport()) && !isBlank(idOrPassport)) {
            String id = idOrPassport.trim();
            guestRepository.findByIdOrPassport(id)
                    .filter(other -> !other.getId().equals(guest.getId()))
                    .ifPresent(other -> {
                        throw new IllegalStateException("ID/passport " + id + " is already recorded for " + other.getName());
                    });
            guest.setIdOrPassport(id);
            guestRepository.save(guest);
        }
        if (isBlank(guest.getIdOrPassport())) {
            throw new IllegalStateException("Guest must provide ID/passport before check-in"); // DR3
        }

        Room room = reservation.getRoom();
        if (room.getStatus() == RoomStatus.CLEANING_REQUIRED) {
            throw new IllegalStateException("Room " + room.getRoomNumber() + " still needs cleaning"); // DR5
        }
        if (room.getStatus() == RoomStatus.OCCUPIED) {
            throw new IllegalStateException("Room " + room.getRoomNumber() + " is still occupied - check the previous guest out first");
        }
        room.setStatus(RoomStatus.OCCUPIED); // FR15
        roomRepository.save(room);

        reservation.setCheckedInAt(LocalDateTime.now());
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Invoice checkOut(Long reservationId) { // FR16
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Only active reservations can be checked out");
        }
        if (!reservation.isCheckedIn()) {
            throw new IllegalStateException("This guest hasn't been checked in yet");
        }

        reservation.setStatus(ReservationStatus.COMPLETED);
        reservation.setCheckedOutAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        Room room = reservation.getRoom();
        room.setStatus(RoomStatus.CLEANING_REQUIRED); // FR18
        roomRepository.save(room);

        // getOrCreate: an invoice may already exist if the guest paid during their stay
        return billingService.getOrCreateInvoice(reservation); // FR17, FR19
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
