package com.hesoy9.guesthouse.site;

import com.hesoy9.guesthouse.dto.BookingRequest;
import com.hesoy9.guesthouse.entity.BookingChannel;
import com.hesoy9.guesthouse.entity.Guest;
import com.hesoy9.guesthouse.entity.Reservation;
import com.hesoy9.guesthouse.repository.GuestRepository;
import com.hesoy9.guesthouse.service.GuestAccountResolver;
import com.hesoy9.guesthouse.service.NotificationService;
import com.hesoy9.guesthouse.service.ReservationService;
import com.hesoy9.guesthouse.web.ValidationUtil;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/book")
public class BookingController {

    private final ReservationService reservationService;
    private final GuestAccountResolver guestAccountResolver;
    private final GuestRepository guestRepository;
    private final NotificationService notificationService;

    public BookingController(ReservationService reservationService,
                              GuestAccountResolver guestAccountResolver,
                              GuestRepository guestRepository,
                              NotificationService notificationService) {
        this.reservationService = reservationService;
        this.guestAccountResolver = guestAccountResolver;
        this.guestRepository = guestRepository;
        this.notificationService = notificationService;
    }

    // Reaching this at all guarantees the visitor is signed in with Google - SecurityConfig
    // requires authentication for everything under /book/**, redirecting to Google first otherwise
    // (and back here afterwards, with the dates they'd picked still in the URL).
    @GetMapping("/{roomId}")
    public String showBookingForm(@PathVariable Long roomId,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                                  @RequestParam(required = false) Integer guests,
                                  @AuthenticationPrincipal OAuth2User principal,
                                  Model model) {
        BookingRequest form = new BookingRequest();
        form.setCheckInDate(checkIn);
        form.setCheckOutDate(checkOut);
        form.setNumberOfGuests(guests != null ? guests : 1);
        Guest guest = guestAccountResolver.resolveGuest(principal);
        form.setContactNumber(guest.getContactNumber()); // pre-filled if they've booked before
        addPageData(model, roomId, form, guest);
        return "site/booking";
    }

    @PostMapping("/{roomId}")
    public String submitBooking(@PathVariable Long roomId,
                                @Valid @ModelAttribute("form") BookingRequest request,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal OAuth2User principal,
                                Model model) {
        Guest guest = guestAccountResolver.resolveGuest(principal);

        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", ValidationUtil.firstErrorMessage(bindingResult));
            addPageData(model, roomId, request, guest);
            return "site/booking";
        }

        // Keep the guest's contact number current with whatever they entered on this form.
        guest.setContactNumber(request.getContactNumber().trim());
        guestRepository.save(guest);

        try {
            // All booking rules (dates, capacity, double-booking) are enforced inside the service.
            Reservation reservation = reservationService.createReservation(
                    guest.getId(), roomId,
                    request.getCheckInDate(), request.getCheckOutDate(),
                    request.getNumberOfGuests(), BookingChannel.ONLINE_PORTAL, request.getSpecialRequests());
            notificationService.bookingConfirmed(reservation);
            model.addAttribute("reservation", reservation);
            model.addAttribute("emailSent", notificationService.isEmailEnabled() && guest.getEmail() != null);
            return "site/confirmation";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage()); // e.g. DR1/DR2 violations
            addPageData(model, roomId, request, guest);
            return "site/booking";
        }
    }

    private void addPageData(Model model, Long roomId, BookingRequest form, Guest guest) {
        model.addAttribute("form", form);
        model.addAttribute("guest", guest);
        model.addAttribute("quote", reservationService.quote(roomId, form.getCheckInDate(),
                form.getCheckOutDate(), form.getNumberOfGuests()));
    }
}
