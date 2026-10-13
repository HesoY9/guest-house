package com.hesoy9.guesthouse.site;

import com.hesoy9.guesthouse.entity.Guest;
import com.hesoy9.guesthouse.entity.Reservation;
import com.hesoy9.guesthouse.entity.ReservationStatus;
import com.hesoy9.guesthouse.service.GuestAccountResolver;
import com.hesoy9.guesthouse.service.NotificationService;
import com.hesoy9.guesthouse.service.ReservationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

// Guest self-service: list of bookings, printable confirmation, cancel before check-in.
// Requires Google sign-in via SecurityConfig.
@Controller
public class MyBookingsController {

    private final GuestAccountResolver guestAccountResolver;
    private final ReservationService reservationService;
    private final NotificationService notificationService;

    public MyBookingsController(GuestAccountResolver guestAccountResolver,
                                ReservationService reservationService,
                                NotificationService notificationService) {
        this.guestAccountResolver = guestAccountResolver;
        this.reservationService = reservationService;
        this.notificationService = notificationService;
    }

    @GetMapping("/my-bookings")
    public String myBookings(@AuthenticationPrincipal OAuth2User principal, Model model) {
        Guest guest = guestAccountResolver.resolveGuest(principal);
        List<Reservation> all = reservationService.getGuestReservations(guest.getId()); // FR12
        model.addAttribute("upcoming", all.stream()
                .filter(r -> r.getStatus() == ReservationStatus.ACTIVE)
                .sorted(java.util.Comparator.comparing(Reservation::getCheckInDate))
                .toList());
        model.addAttribute("past", all.stream().filter(r -> r.getStatus() != ReservationStatus.ACTIVE).toList());
        return "site/my-bookings";
    }

    // Printable booking confirmation - only for the signed-in guest's own bookings.
    @GetMapping("/my-bookings/{id}")
    public String bookingDetail(@PathVariable Long id, @AuthenticationPrincipal OAuth2User principal, Model model) {
        model.addAttribute("reservation", ownReservation(id, principal));
        return "site/booking-detail";
    }

    @PostMapping("/my-bookings/{id}/cancel")
    public String cancel(@PathVariable Long id, @AuthenticationPrincipal OAuth2User principal,
                         RedirectAttributes redirect) {
        Guest guest = guestAccountResolver.resolveGuest(principal);
        try {
            Reservation cancelled = reservationService.cancelByGuest(id, guest.getId()); // before check-in only
            notificationService.bookingCancelled(cancelled);
            redirect.addFlashAttribute("successMessage", "Booking " + cancelled.getReference() + " has been cancelled.");
        } catch (IllegalStateException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/my-bookings";
    }

    private Reservation ownReservation(Long id, OAuth2User principal) {
        Guest guest = guestAccountResolver.resolveGuest(principal);
        Reservation reservation = reservationService.getReservation(id);
        if (!reservation.getGuest().getId().equals(guest.getId())) {
            throw new IllegalArgumentException("Reservation not found: " + id); // don't reveal it exists
        }
        return reservation;
    }
}
