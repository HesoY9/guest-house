package com.hesoy9.guesthouse.controller;

import com.hesoy9.guesthouse.dto.DateRangeRequest;
import com.hesoy9.guesthouse.dto.ReservationRequest;
import com.hesoy9.guesthouse.entity.Reservation;
import com.hesoy9.guesthouse.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<Reservation> createReservation(@RequestBody ReservationRequest request) {
        Reservation reservation = reservationService.createReservation(
                request.getGuestId(),
                request.getRoomId(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                request.getNumberOfGuests(),
                request.getBookingChannel());
        return ResponseEntity.ok(reservation);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Reservation> cancelReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.cancelReservation(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reservation> modifyReservation(@PathVariable Long id,
                                                           @RequestBody DateRangeRequest request) {
        return ResponseEntity.ok(reservationService.modifyReservation(
                id, request.getCheckInDate(), request.getCheckOutDate()));
    }
}
