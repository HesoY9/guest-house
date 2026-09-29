package com.hesoy9.guesthouse.site;

import com.hesoy9.guesthouse.dto.BookingRequest;
import com.hesoy9.guesthouse.entity.BookingChannel;
import com.hesoy9.guesthouse.entity.Guest;
import com.hesoy9.guesthouse.entity.Reservation;
import com.hesoy9.guesthouse.repository.GuestRepository;
import com.hesoy9.guesthouse.service.GuestAccountResolver;
import com.hesoy9.guesthouse.service.ReservationService;
import com.hesoy9.guesthouse.service.RoomService;
import com.hesoy9.guesthouse.web.ValidationUtil;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/book")
public class BookingController {

    private final RoomService roomService;
    private final ReservationService reservationService;
    private final GuestAccountResolver guestAccountResolver;
    private final GuestRepository guestRepository;

    public BookingController(RoomService roomService,
                              ReservationService reservationService,
                              GuestAccountResolver guestAccountResolver,
                              GuestRepository guestRepository) {
        this.roomService = roomService;
        this.reservationService = reservationService;
        this.guestAccountResolver = guestAccountResolver;
        this.guestRepository = guestRepository;
    }

    // Reaching this at all guarantees the visitor is signed in with Google - SecurityConfig
    // requires authentication for everything under /book/**, redirecting to Google first otherwise.
    @GetMapping("/{roomId}")
    public String showBookingForm(@PathVariable Long roomId,
                                   @AuthenticationPrincipal OAuth2User principal,
                                   Model model) {
        Guest guest = guestAccountResolver.resolveGuest(principal);
        model.addAttribute("room", roomService.getRoomById(roomId));
        model.addAttribute("guest", guest); // pre-fills contact number if they've booked before
        model.addAttribute("googleUserName", principal.getAttribute("name"));
        return "site/booking";
    }

    @PostMapping("/{roomId}")
    public String submitBooking(@PathVariable Long roomId,
                                 @Valid @ModelAttribute BookingRequest request,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal OAuth2User principal,
                                 Model model) {
        Guest guest = guestAccountResolver.resolveGuest(principal);
        model.addAttribute("googleUserName", principal.getAttribute("name"));

        if (bindingResult.hasErrors()) {
            model.addAttribute("room", roomService.getRoomById(roomId));
            model.addAttribute("guest", guest);
            model.addAttribute("errorMessage", ValidationUtil.firstErrorMessage(bindingResult));
            return "site/booking";
        }

        // Keep the guest's contact number current with whatever they entered on this form.
        guest.setContactNumber(request.getContactNumber());
        guestRepository.save(guest);

        try {
            Reservation reservation = reservationService.createReservation(
                    guest.getId(), roomId,
                    request.getCheckInDate(), request.getCheckOutDate(),
                    request.getNumberOfGuests(), BookingChannel.ONLINE_PORTAL);
            model.addAttribute("reservation", reservation);
            return "site/confirmation";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("room", roomService.getRoomById(roomId));
            model.addAttribute("guest", guest);
            model.addAttribute("errorMessage", ex.getMessage()); // e.g. DR1/DR2 violations
            return "site/booking";
        }
    }
}
