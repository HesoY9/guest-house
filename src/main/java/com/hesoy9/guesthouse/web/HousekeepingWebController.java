package com.hesoy9.guesthouse.web;

import com.hesoy9.guesthouse.entity.Reservation;
import com.hesoy9.guesthouse.entity.Room;
import com.hesoy9.guesthouse.entity.RoomStatus;
import com.hesoy9.guesthouse.repository.ReservationRepository;
import com.hesoy9.guesthouse.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

// Housekeeping board (FR24, FR25): rooms grouped by state, one click to mark a room clean.
@Controller
@RequestMapping("/web/housekeeping")
public class HousekeepingWebController {

    private final RoomService roomService;
    private final ReservationRepository reservationRepository;

    public HousekeepingWebController(RoomService roomService, ReservationRepository reservationRepository) {
        this.roomService = roomService;
        this.reservationRepository = reservationRepository;
    }

    @GetMapping
    public String board(Model model) {
        LocalDate today = LocalDate.now();
        List<Room> rooms = roomService.getAllRooms();

        Map<RoomStatus, List<Room>> columns = new EnumMap<>(RoomStatus.class);
        for (RoomStatus status : List.of(RoomStatus.CLEANING_REQUIRED, RoomStatus.OCCUPIED, RoomStatus.RESERVED, RoomStatus.AVAILABLE)) {
            columns.put(status, new ArrayList<>());
        }
        rooms.forEach(room -> columns.get(room.getStatus()).add(room));

        // Next arrival per room - "guest arriving today" tells housekeeping what to clean first.
        Map<Long, Reservation> nextArrival = new HashMap<>();
        for (Reservation r : reservationRepository.findUpcomingNotArrived(today)) {
            nextArrival.putIfAbsent(r.getRoom().getId(), r);
        }

        model.addAttribute("columns", columns);
        model.addAttribute("nextArrival", nextArrival);
        model.addAttribute("roomCount", rooms.size());
        model.addAttribute("cleanCount", columns.get(RoomStatus.CLEANING_REQUIRED).size());
        return "staff/housekeeping";
    }

    @PostMapping("/{roomId}/clean")
    public ResponseEntity<Void> markClean(@PathVariable Long roomId) {
        Room room = roomService.markClean(roomId); // FR25, DR5
        return Hx.done("Room " + room.getRoomNumber() + " marked clean and ready.");
    }

    @PostMapping("/{roomId}/dirty")
    public ResponseEntity<Void> markNeedsCleaning(@PathVariable Long roomId) {
        Room room = roomService.markNeedsCleaning(roomId); // FR24
        return Hx.done("Room " + room.getRoomNumber() + " flagged for cleaning.");
    }
}
