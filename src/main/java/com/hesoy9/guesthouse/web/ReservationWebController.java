package com.hesoy9.guesthouse.web;

import com.hesoy9.guesthouse.dto.ReservationRequest;
import com.hesoy9.guesthouse.entity.Invoice;
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
        model.addAttribute("guests", guestService.getAllGuests());       // for the guest dropdown
        model.addAttribute("rooms", roomService.getAvailableRooms());    // for the room dropdown
        model.addAttribute("reservations", reservationService.getAllReservations());
        return "reservations";
    }

    @PostMapping
    public String createReservation(@Valid @ModelAttribute ReservationRequest request,
                                     BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", ValidationUtil.firstErrorMessage(bindingResult));
            model.addAttribute("reservations", reservationService.getAllReservations());
            return "fragments/reservations-panel :: panel";
        }
        try {
            reservationService.createReservation(
                    request.getGuestId(), request.getRoomId(),
                    request.getCheckInDate(), request.getCheckOutDate(),
                    request.getNumberOfGuests(), request.getBookingChannel());
            model.addAttribute("successMessage", "Reservation created.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage()); // e.g. DR1/DR2 violations
        }
        model.addAttribute("reservations", reservationService.getAllReservations());
        return "fragments/reservations-panel :: panel";
    }

    @PutMapping("/{id}/cancel")
    public String cancelReservation(@PathVariable Long id, Model model) {
        reservationService.cancelReservation(id);
        model.addAttribute("successMessage", "Reservation cancelled.");
        model.addAttribute("reservations", reservationService.getAllReservations());
        return "fragments/reservations-panel :: panel";
    }

    @PutMapping("/{id}/checkin")
    public String checkIn(@PathVariable Long id, Model model) {
        try {
            checkInOutService.checkIn(id);
            model.addAttribute("successMessage", "Guest checked in.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage()); // e.g. DR3 - missing ID
        }
        model.addAttribute("reservations", reservationService.getAllReservations());
        return "fragments/reservations-panel :: panel";
    }

    @PutMapping("/{id}/checkout")
    public String checkOut(@PathVariable Long id, Model model) {
        try {
            Invoice invoice = checkInOutService.checkOut(id);
            model.addAttribute("successMessage", "Checked out - total charge: " + invoice.getTotalAmount());
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }
        model.addAttribute("reservations", reservationService.getAllReservations());
        return "fragments/reservations-panel :: panel";
    }
}
