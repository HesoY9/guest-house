package com.hesoy9.guesthouse.web;

import com.hesoy9.guesthouse.dto.ReservationRequest;
import com.hesoy9.guesthouse.entity.Guest;
import com.hesoy9.guesthouse.entity.Invoice;
import com.hesoy9.guesthouse.entity.Reservation;
import com.hesoy9.guesthouse.service.CheckInOutService;
import com.hesoy9.guesthouse.service.GuestService;
import com.hesoy9.guesthouse.service.ReservationService;
import com.hesoy9.guesthouse.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/web/reservations")
public class ReservationWebController {

    private static final String SECTION_FRAGMENT = "fragments/reservations-section :: section";

    private final ReservationService reservationService;
    private final CheckInOutService checkInOutService;
    private final GuestService guestService;
    private final RoomService roomService;

    public ReservationWebController(ReservationService reservationService,
                                     CheckInOutService checkInOutService,
                                     GuestService guestService,
                                     RoomService roomService) {
        this.reservationService = reservationService;
        this.checkInOutService = checkInOutService;
        this.guestService = guestService;
        this.roomService = roomService;
    }

    @GetMapping
    public String listReservations(Model model) {
        addDropdownAndTableData(model);
        return "reservations";
    }

    @PostMapping
    public String createReservation(@Valid @ModelAttribute ReservationRequest request,
                                     BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", ValidationUtil.firstErrorMessage(bindingResult));
            addDropdownAndTableData(model);
            return SECTION_FRAGMENT;
        }
        try {
            Long guestId = resolveGuestId(request); // existing guest, or registers a new one inline
            Reservation reservation = reservationService.createReservation(
                    guestId, request.getRoomId(),
                    request.getCheckInDate(), request.getCheckOutDate(),
                    request.getNumberOfGuests(), request.getBookingChannel());
            model.addAttribute("successMessage",
                    "Reservation #" + reservation.getId() + " created for " + reservation.getGuest().getName() + ".");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage()); // e.g. DR1/DR2, or missing guest info
        }
        addDropdownAndTableData(model);
        return SECTION_FRAGMENT;
    }

    @PutMapping("/{id}/cancel")
    public String cancelReservation(@PathVariable Long id, Model model) {
        reservationService.cancelReservation(id);
        model.addAttribute("successMessage", "Reservation cancelled.");
        addDropdownAndTableData(model);
        return SECTION_FRAGMENT;
    }

    @PutMapping("/{id}/checkin")
    public String checkIn(@PathVariable Long id, Model model) {
        try {
            checkInOutService.checkIn(id);
            model.addAttribute("successMessage", "Guest checked in.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage()); // e.g. DR3 - missing ID
        }
        addDropdownAndTableData(model);
        return SECTION_FRAGMENT;
    }

    @PutMapping("/{id}/checkout")
    public String checkOut(@PathVariable Long id, Model model) {
        try {
            Invoice invoice = checkInOutService.checkOut(id);
            model.addAttribute("successMessage", "Checked out - total charge: " + invoice.getTotalAmount());
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }
        addDropdownAndTableData(model);
        return SECTION_FRAGMENT;
    }

    // Existing guest selected -> use that id. Dropdown left on "+ New guest" (blank) -> register
    // one inline from the typed fields, same as a normal walk-in registration, just in one step.
    private Long resolveGuestId(ReservationRequest request) {
        if (request.getGuestId() != null) {
            return request.getGuestId();
        }
        if (request.getNewGuestName() == null || request.getNewGuestName().isBlank()) {
            throw new IllegalArgumentException("Select an existing guest, or enter a name to register a new one");
        }
        Guest guest = new Guest();
        guest.setName(request.getNewGuestName());
        // blankToNull matters here: idOrPassport is UNIQUE, and an empty string ("", which is what
        // an empty HTML text input submits - not null) would collide on the second guest left blank.
        guest.setIdOrPassport(blankToNull(request.getNewGuestIdOrPassport()));
        guest.setContactNumber(blankToNull(request.getNewGuestContactNumber()));
        return guestService.registerGuest(guest).getId();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    // Shared by every action: the combined fragment always needs fresh dropdown options
    // (a newly-registered guest must appear immediately) plus the current reservation list.
    private void addDropdownAndTableData(Model model) {
        model.addAttribute("guests", guestService.getAllGuests());
        model.addAttribute("rooms", roomService.getAvailableRooms());
        model.addAttribute("reservations", reservationService.getAllReservations());
    }
}
