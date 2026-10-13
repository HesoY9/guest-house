package com.hesoy9.guesthouse.web;

import com.hesoy9.guesthouse.dto.ReservationRequest;
import com.hesoy9.guesthouse.entity.BookingChannel;
import com.hesoy9.guesthouse.entity.Guest;
import com.hesoy9.guesthouse.entity.Invoice;
import com.hesoy9.guesthouse.entity.Reservation;
import com.hesoy9.guesthouse.entity.ReservationStatus;
import com.hesoy9.guesthouse.repository.InvoiceRepository;
import com.hesoy9.guesthouse.service.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/web/reservations")
public class ReservationWebController {

    private static final String NEW_DRAWER = "staff/drawers/reservation-new :: drawer";
    private static final String DETAIL_DRAWER = "staff/drawers/reservation-detail :: drawer";

    private final ReservationService reservationService;
    private final CheckInOutService checkInOutService;
    private final GuestService guestService;
    private final RoomService roomService;
    private final NotificationService notificationService;
    private final InvoiceRepository invoiceRepository;

    public ReservationWebController(ReservationService reservationService,
                                     CheckInOutService checkInOutService,
                                     GuestService guestService,
                                     RoomService roomService,
                                     NotificationService notificationService,
                                     InvoiceRepository invoiceRepository) {
        this.reservationService = reservationService;
        this.checkInOutService = checkInOutService;
        this.guestService = guestService;
        this.roomService = roomService;
        this.notificationService = notificationService;
        this.invoiceRepository = invoiceRepository;
    }

    // status: active (default) | completed | cancelled | all; q: guest name, BH-number or room number
    @GetMapping
    public String listReservations(@RequestParam(defaultValue = "active") String status,
                                   @RequestParam(required = false) String q,
                                   Model model) {
        ReservationStatus filter = switch (status) {
            case "completed" -> ReservationStatus.COMPLETED;
            case "cancelled" -> ReservationStatus.CANCELLED;
            case "all" -> null;
            default -> ReservationStatus.ACTIVE;
        };
        model.addAttribute("reservations", reservationService.search(filter, q));
        model.addAttribute("status", status);
        model.addAttribute("q", q);
        return "staff/reservations";
    }

    // Slide-over "New reservation" form. roomId / checkIn come pre-filled when opened from the calendar.
    @GetMapping("/new")
    public String newReservationForm(@RequestParam(required = false) Long roomId,
                                     @RequestParam(required = false) Long guestId,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                                     Model model) {
        ReservationRequest form = new ReservationRequest();
        form.setRoomId(roomId);
        form.setGuestId(guestId);
        LocalDate start = checkIn != null && !checkIn.isBefore(LocalDate.now()) ? checkIn : LocalDate.now();
        form.setCheckInDate(start);
        form.setCheckOutDate(start.plusDays(1));
        form.setNumberOfGuests(1);
        form.setBookingChannel(BookingChannel.IN_PERSON);
        addFormData(model, form);
        return NEW_DRAWER;
    }

    @PostMapping
    public Object createReservation(@Valid @ModelAttribute ReservationRequest request,
                                    BindingResult bindingResult, Model model, HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            return showFormError(model, request, ValidationUtil.firstErrorMessage(bindingResult), response);
        }
        try {
            Long guestId = resolveGuestId(request); // existing guest, or registers a new one inline
            Reservation reservation = reservationService.createReservation(
                    guestId, request.getRoomId(),
                    request.getCheckInDate(), request.getCheckOutDate(),
                    request.getNumberOfGuests(), request.getBookingChannel(), request.getNotes());
            notificationService.bookingConfirmed(reservation);
            return Hx.done("Reservation " + reservation.getReference() + " created for "
                    + reservation.getGuest().getName() + ".");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return showFormError(model, request, ex.getMessage(), response); // e.g. DR1/DR2, capacity
        }
    }

    // Live "nights x price = total" + availability check inside the New reservation form.
    @GetMapping("/quote")
    public String quote(@RequestParam(required = false) Long roomId,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
                        @RequestParam(required = false) Integer numberOfGuests,
                        Model model) {
        if (roomId != null) {
            model.addAttribute("quote", reservationService.quote(roomId, checkInDate, checkOutDate, numberOfGuests));
        }
        return "staff/fragments/quote :: quote";
    }

    @GetMapping("/{id}")
    public String reservationDetail(@PathVariable Long id, Model model) {
        addDetailData(model, reservationService.getReservation(id));
        return DETAIL_DRAWER;
    }

    // Change dates (FR10) from the detail panel; the panel re-renders with the new values.
    @PostMapping("/{id}/dates")
    public String changeDates(@PathVariable Long id,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
                              Model model, HttpServletResponse response) {
        Reservation reservation = reservationService.modifyReservation(id, checkInDate, checkOutDate);
        Hx.toastAndRefresh(response, "Dates updated - now " + reservation.getNights() + " night(s).", null, null);
        addDetailData(model, reservation);
        return DETAIL_DRAWER;
    }

    @PutMapping("/{id}/checkin")
    public ResponseEntity<Void> checkIn(@PathVariable Long id, @RequestParam(required = false) String idOrPassport) {
        Reservation reservation = checkInOutService.checkIn(id, idOrPassport); // DR3, DR5 checks inside
        return Hx.done(reservation.getGuest().getName() + " checked in to room "
                + reservation.getRoom().getRoomNumber() + ".");
    }

    @PutMapping("/{id}/checkout")
    public ResponseEntity<Void> checkOut(@PathVariable Long id) {
        Invoice invoice = checkInOutService.checkOut(id);
        return Hx.done("Checked out. Invoice " + invoice.getNumber() + " created.",
                "/web/payments?open=" + invoice.getId(), "Take payment");
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long id) {
        Reservation reservation = reservationService.cancelReservation(id);
        notificationService.bookingCancelled(reservation);
        return Hx.done("Reservation " + reservation.getReference() + " cancelled.");
    }

    private String showFormError(Model model, ReservationRequest request, String message, HttpServletResponse response) {
        model.addAttribute("errorMessage", message);
        Hx.toast(response, "error", message);
        addFormData(model, request);
        return NEW_DRAWER;
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
        guest.setIdOrPassport(request.getNewGuestIdOrPassport());
        guest.setContactNumber(request.getNewGuestContactNumber());
        guest.setEmail(request.getNewGuestEmail());
        Guest saved = guestService.registerGuest(guest); // blank fields become null there
        request.setGuestId(saved.getId()); // if the booking itself fails, the form keeps the new guest selected
        return saved.getId();
    }

    private void addFormData(Model model, ReservationRequest form) {
        model.addAttribute("form", form);
        model.addAttribute("guests", guestService.getAllGuests());
        // All rooms, not only status=AVAILABLE: a room that's occupied tonight can still be booked
        // for next week. The date-aware overlap check in ReservationService decides.
        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("channels", BookingChannel.values());
    }

    private void addDetailData(Model model, Reservation reservation) {
        model.addAttribute("r", reservation);
        model.addAttribute("invoice", invoiceRepository.findByReservationId(reservation.getId()).orElse(null));
    }
}
