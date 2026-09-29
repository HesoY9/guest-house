package com.hesoy9.guesthouse.site;

import com.hesoy9.guesthouse.entity.Guest;
import com.hesoy9.guesthouse.service.GuestAccountResolver;
import com.hesoy9.guesthouse.service.GuestService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MyBookingsController {

    private final GuestAccountResolver guestAccountResolver;
    private final GuestService guestService;

    public MyBookingsController(GuestAccountResolver guestAccountResolver, GuestService guestService) {
        this.guestAccountResolver = guestAccountResolver;
        this.guestService = guestService;
    }

    // Requires sign-in via SecurityConfig - reuses the same getGuestHistory the staff side uses.
    @GetMapping("/my-bookings")
    public String myBookings(@AuthenticationPrincipal OAuth2User principal, Model model) {
        Guest guest = guestAccountResolver.resolveGuest(principal);
        model.addAttribute("reservations", guestService.getGuestHistory(guest.getId())); // FR12
        model.addAttribute("googleUserName", principal.getAttribute("name"));
        return "site/my-bookings";
    }
}
