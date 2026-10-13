package com.hesoy9.guesthouse.web;

import com.hesoy9.guesthouse.entity.Guest;
import com.hesoy9.guesthouse.service.BillingService;
import com.hesoy9.guesthouse.service.GuestService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/web/guests")
public class GuestWebController {

    private static final String FORM_DRAWER = "staff/drawers/guest-form :: drawer";

    private final GuestService guestService;
    private final BillingService billingService;

    public GuestWebController(GuestService guestService, BillingService billingService) {
        this.guestService = guestService;
        this.billingService = billingService;
    }

    // q: live search on name, ID/passport, phone or email (FR13)
    @GetMapping
    public String listGuests(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("guests", guestService.search(q));
        model.addAttribute("stays", guestService.getStayCounts());
        model.addAttribute("q", q);
        return "staff/guests";
    }

    @GetMapping("/new")
    public String newGuestForm(Model model) {
        model.addAttribute("guest", new Guest());
        return FORM_DRAWER;
    }

    @PostMapping
    public Object addGuest(@ModelAttribute Guest guest, Model model, HttpServletResponse response) {
        try {
            Guest saved = guestService.registerGuest(guest); // FR11
            return Hx.done(saved.getName() + " registered.", "/web/guests/" + saved.getId(), "Open profile");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return showFormError(model, guest, null, ex.getMessage(), response);
        }
    }

    // Guest profile: details, lifetime stats, stay history (FR12) and invoices.
    @GetMapping("/{id}")
    public String profile(@PathVariable Long id, Model model) {
        model.addAttribute("guest", guestService.getGuest(id));
        model.addAttribute("stats", guestService.getStats(id));
        model.addAttribute("history", guestService.getGuestHistory(id));
        model.addAttribute("invoices", billingService.getGuestSummaries(id));
        return "staff/guest-profile";
    }

    @GetMapping("/{id}/edit")
    public String editGuestForm(@PathVariable Long id, Model model) {
        model.addAttribute("guest", guestService.getGuest(id));
        model.addAttribute("guestId", id);
        return FORM_DRAWER;
    }

    @PostMapping("/{id}")
    public Object updateGuest(@PathVariable Long id, @ModelAttribute Guest details,
                              Model model, HttpServletResponse response) {
        try {
            Guest saved = guestService.updateGuest(id, details);
            return Hx.done(saved.getName() + "'s details saved.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return showFormError(model, details, id, ex.getMessage(), response);
        }
    }

    private String showFormError(Model model, Guest guest, Long guestId, String message, HttpServletResponse response) {
        model.addAttribute("guest", guest);
        model.addAttribute("guestId", guestId);
        model.addAttribute("errorMessage", message);
        Hx.toast(response, "error", message);
        return FORM_DRAWER;
    }
}
