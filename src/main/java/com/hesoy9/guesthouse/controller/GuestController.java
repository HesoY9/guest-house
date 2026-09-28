package com.hesoy9.guesthouse.controller;

import com.hesoy9.guesthouse.entity.Guest;
import com.hesoy9.guesthouse.entity.Reservation;
import com.hesoy9.guesthouse.service.GuestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guests")
public class GuestController {

    private final GuestService guestService;

    public GuestController(GuestService guestService) {
        this.guestService = guestService;
    }

    @PostMapping
    public ResponseEntity<Guest> registerGuest(@RequestBody Guest guest) {
        return ResponseEntity.ok(guestService.registerGuest(guest));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Guest>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(guestService.searchByName(name));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<Reservation>> getGuestHistory(@PathVariable Long id) {
        return ResponseEntity.ok(guestService.getGuestHistory(id));
    }
}
