package com.hesoy9.guesthouse.controller;

import com.hesoy9.guesthouse.entity.Invoice;
import com.hesoy9.guesthouse.entity.Reservation;
import com.hesoy9.guesthouse.service.CheckInOutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Same base path as ReservationController - that's fine, Spring just merges
// the mappings. It's a separate controller because it's backed by a separate service.
@RestController
@RequestMapping("/api/reservations")
public class CheckInOutController {

    private final CheckInOutService checkInOutService;

    public CheckInOutController(CheckInOutService checkInOutService) {
        this.checkInOutService = checkInOutService;
    }

    @PutMapping("/{id}/checkin")
    public ResponseEntity<Reservation> checkIn(@PathVariable Long id) {
        return ResponseEntity.ok(checkInOutService.checkIn(id));
    }

    @PutMapping("/{id}/checkout")
    public ResponseEntity<Invoice> checkOut(@PathVariable Long id) {
        return ResponseEntity.ok(checkInOutService.checkOut(id));
    }
}
