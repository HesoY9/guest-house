package com.hesoy9.guesthouse.site;

import com.hesoy9.guesthouse.dto.RoomSearch;
import com.hesoy9.guesthouse.entity.Amenity;
import com.hesoy9.guesthouse.entity.Room;
import com.hesoy9.guesthouse.service.ReservationService;
import com.hesoy9.guesthouse.service.RoomService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Controller
public class HomeController {

    private final RoomService roomService;
    private final ReservationService reservationService;

    public HomeController(RoomService roomService, ReservationService reservationService) {
        this.roomService = roomService;
        this.reservationService = reservationService;
    }

    // Public landing page - browsable without signing in, matching a normal booking site.
    @GetMapping("/")
    public String home(Model model) {
        List<Room> rooms = roomService.getAllRooms().stream()
                .sorted(Comparator.comparing((Room r) -> r.getPhotos().isEmpty()).thenComparing(Room::getPrice))
                .limit(6)
                .toList();
        model.addAttribute("rooms", rooms);
        model.addAttribute("search", new RoomSearch());
        return "site/home";
    }

    // Search results with filters and sorting. Works as a normal page (shareable URL, back button),
    // and the filter form uses HTMX to swap just the results without reloading.
    @GetMapping("/rooms")
    public String rooms(@ModelAttribute("search") RoomSearch search, Model model) {
        String dateProblem = null;
        if (search.getCheckIn() != null && search.getCheckOut() != null) {
            if (!search.getCheckOut().isAfter(search.getCheckIn())) {
                dateProblem = "Check-out must be after check-in.";
            } else if (search.getCheckIn().isBefore(LocalDate.now())) {
                dateProblem = "Check-in can't be in the past.";
            }
        }
        if (dateProblem != null) {
            model.addAttribute("dateProblem", dateProblem);
            search.setCheckIn(null);
            search.setCheckOut(null);
        }
        model.addAttribute("rooms", roomService.search(search));
        model.addAttribute("nights", search.hasDates() ? ChronoUnit.DAYS.between(search.getCheckIn(), search.getCheckOut()) : 0);
        model.addAttribute("roomTypes", roomService.getRoomTypes());
        model.addAttribute("amenities", Amenity.values());
        return "site/rooms";
    }

    // Room page: photo gallery, amenities, capacity, and a booking box with a live price summary.
    @GetMapping("/rooms/{id}")
    public String room(@PathVariable Long id,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                       @RequestParam(required = false) Integer guests,
                       Model model) {
        model.addAttribute("room", roomService.getRoomById(id));
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("guests", guests != null ? guests : 1);
        if (checkIn != null && checkOut != null) {
            model.addAttribute("quote", reservationService.quote(id, checkIn, checkOut, guests != null ? guests : 1));
        }
        return "site/room";
    }

    // HTMX fragment: "3 nights x LKR 12,000 = LKR 36,000" + availability, updated as dates change.
    // Accepts the room page's field names (checkIn...) and the booking form's (checkInDate...).
    @GetMapping("/rooms/{id}/quote")
    public String quote(@PathVariable Long id,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                        @RequestParam(required = false) Integer guests,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
                        @RequestParam(required = false) Integer numberOfGuests,
                        Model model) {
        LocalDate in = checkIn != null ? checkIn : checkInDate;
        LocalDate out = checkOut != null ? checkOut : checkOutDate;
        Integer people = guests != null ? guests : (numberOfGuests != null ? numberOfGuests : 1);
        model.addAttribute("quote", reservationService.quote(id, in, out, people));
        return "site/fragments/quote :: quote";
    }
}
