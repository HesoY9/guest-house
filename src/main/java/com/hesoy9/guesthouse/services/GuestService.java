package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.Guest;
import com.hesoy9.guesthouse.entity.Reservation;
import com.hesoy9.guesthouse.entity.ReservationStatus;
import com.hesoy9.guesthouse.repository.GuestRepository;
import com.hesoy9.guesthouse.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GuestService {

    private final GuestRepository guestRepository;
    private final ReservationRepository reservationRepository;

    public GuestService(GuestRepository guestRepository, ReservationRepository reservationRepository) {
        this.guestRepository = guestRepository;
        this.reservationRepository = reservationRepository;
    }

    public Guest registerGuest(Guest guest) { // FR11
        normalize(guest);
        ensureUnique(guest, null);
        return guestRepository.save(guest);
    }

    // Staff edit of a guest profile. Email is editable so a walk-in can later sign in with
    // Google using the same address and see their bookings under "My Bookings".
    @Transactional
    public Guest updateGuest(Long guestId, Guest details) {
        normalize(details);
        ensureUnique(details, guestId);
        Guest guest = getGuest(guestId);
        guest.setName(details.getName());
        guest.setIdOrPassport(details.getIdOrPassport());
        guest.setContactNumber(details.getContactNumber());
        guest.setEmail(details.getEmail());
        guest.setNotes(details.getNotes());
        return guestRepository.save(guest);
    }

    public Guest getGuest(Long guestId) {
        return guestRepository.findById(guestId)
                .orElseThrow(() -> new IllegalArgumentException("Guest not found: " + guestId));
    }

    public List<Guest> searchByName(String name) { // FR13
        return guestRepository.findByNameContainingIgnoreCase(name);
    }

    // Staff search box - name, ID/passport, phone or email.
    public List<Guest> search(String q) {
        return (q == null || q.isBlank()) ? getAllGuests() : guestRepository.search(q.trim());
    }

    public List<Reservation> getGuestHistory(Long guestId) { // FR12
        return reservationRepository.findByGuestIdOrderByCheckInDateDesc(guestId);
    }

    public List<Guest> getAllGuests() {
        return guestRepository.findAllByOrderByNameAsc();
    }

    // guest id -> number of (non-cancelled) stays, for the guest list
    public Map<Long, Long> getStayCounts() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : reservationRepository.countStaysByGuest()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    // Lifetime numbers for the guest profile header.
    public GuestStats getStats(Long guestId) {
        List<Reservation> history = getGuestHistory(guestId);
        long stays = history.stream().filter(r -> r.getStatus() == ReservationStatus.COMPLETED).count();
        long nights = history.stream().filter(r -> r.getStatus() == ReservationStatus.COMPLETED)
                .mapToLong(Reservation::getNights).sum();
        long upcoming = history.stream().filter(r -> r.getStatus() == ReservationStatus.ACTIVE).count();
        long cancelled = history.stream().filter(r -> r.getStatus() == ReservationStatus.CANCELLED).count();
        return new GuestStats(stays, nights, upcoming, cancelled);
    }

    public record GuestStats(long completedStays, long nights, long activeBookings, long cancellations) {
    }

    // Blank form fields arrive as "" - store them as null so the UNIQUE columns
    // (ID/passport, email) don't collide on the second guest who left them empty.
    private void normalize(Guest guest) {
        guest.setName(trimToNull(guest.getName()));
        guest.setIdOrPassport(trimToNull(guest.getIdOrPassport()));
        guest.setContactNumber(trimToNull(guest.getContactNumber()));
        String email = trimToNull(guest.getEmail());
        guest.setEmail(email != null ? email.toLowerCase() : null);
        guest.setNotes(trimToNull(guest.getNotes()));
        if (guest.getName() == null) {
            throw new IllegalArgumentException("Name is required");
        }
    }

    private void ensureUnique(Guest guest, Long ignoreGuestId) {
        if (guest.getIdOrPassport() != null) {
            guestRepository.findByIdOrPassport(guest.getIdOrPassport())
                    .filter(other -> !other.getId().equals(ignoreGuestId))
                    .ifPresent(other -> {
                        throw new IllegalStateException("ID/passport " + guest.getIdOrPassport() + " already belongs to " + other.getName());
                    });
        }
        if (guest.getEmail() != null) {
            guestRepository.findByEmail(guest.getEmail())
                    .filter(other -> !other.getId().equals(ignoreGuestId))
                    .ifPresent(other -> {
                        throw new IllegalStateException("Email " + guest.getEmail() + " already belongs to " + other.getName());
                    });
        }
    }

    private static String trimToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
