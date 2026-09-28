package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.*;
import com.hesoy9.guesthouse.repository.ReservationRepository;
import com.hesoy9.guesthouse.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckInOutService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final BillingService billingService;

    public CheckInOutService(ReservationRepository reservationRepository,
                              RoomRepository roomRepository,
                              BillingService billingService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.billingService = billingService;
    }

    @Transactional
    public Reservation checkIn(Long reservationId) { // FR14
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        Guest guest = reservation.getGuest();
        if (guest.getIdOrPassport() == null || guest.getIdOrPassport().isBlank()) {
            throw new IllegalStateException("Guest must provide ID/passport before check-in"); // DR3
        }

        Room room = reservation.getRoom();
        room.setStatus(RoomStatus.OCCUPIED); // FR15
        roomRepository.save(room);

        return reservation;
    }

    @Transactional
    public Invoice checkOut(Long reservationId) { // FR16
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        reservation.setStatus(ReservationStatus.COMPLETED);
        reservationRepository.save(reservation);

        Room room = reservation.getRoom();
        room.setStatus(RoomStatus.CLEANING_REQUIRED); // FR18
        roomRepository.save(room);

        return billingService.generateInvoice(reservation); // FR17, FR19
    }
}
