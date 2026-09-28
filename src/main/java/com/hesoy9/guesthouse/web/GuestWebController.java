package com.hesoy9.guesthouse.web;

import com.hesoy9.guesthouse.entity.Guest;
import com.hesoy9.guesthouse.service.GuestService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/web/guests")
public class GuestWebController {

    private final GuestService guestService;

    public GuestWebController(GuestService guestService) {
        this.guestService = guestService;
    }

    @GetMapping
    public String listGuests(Model model) {
        model.addAttribute("guests", guestService.getAllGuests());
        return "guests";
    }

    @PostMapping
    public String addGuest(@Valid @ModelAttribute Guest guest, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", ValidationUtil.firstErrorMessage(bindingResult));
            model.addAttribute("guests", guestService.getAllGuests());
            return "fragments/guests-panel :: panel";
        }
        guestService.registerGuest(guest);
        model.addAttribute("guests", guestService.getAllGuests());
        model.addAttribute("successMessage", "Guest registered.");
        return "fragments/guests-panel :: panel";
    }

    // HTMX live search - same panel, just a filtered list, fired on keyup
    @GetMapping("/search")
    public String searchGuests(@RequestParam(required = false, defaultValue = "") String name, Model model) {
        model.addAttribute("guests", name.isBlank() ? guestService.getAllGuests() : guestService.searchByName(name));
        return "fragments/guests-panel :: panel";
    }
}
