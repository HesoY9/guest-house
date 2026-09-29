package com.hesoy9.guesthouse.site;

import com.hesoy9.guesthouse.service.RoomService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class HomeController {

    private final RoomService roomService;

    public HomeController(RoomService roomService) {
        this.roomService = roomService;
    }

    // Public landing page - browsable without signing in, matching a normal booking site.
    // principal is null when nobody is signed in - Spring just injects null, no exception.
    @GetMapping("/")
    public String home(@AuthenticationPrincipal OAuth2User principal, Model model) {
        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("googleUserName", principal != null ? principal.getAttribute("name") : null);
        return "site/home";
    }

    // HTMX date-range search - swaps in the filtered grid fragment only.
    @GetMapping("/search")
    public String search(@RequestParam LocalDate checkInDate, @RequestParam LocalDate checkOutDate, Model model) {
        model.addAttribute("rooms", roomService.getAvailableRoomsForDates(checkInDate, checkOutDate));
        model.addAttribute("checkInDate", checkInDate);
        model.addAttribute("checkOutDate", checkOutDate);
        return "site/fragments/room-grid :: grid";
    }
}
