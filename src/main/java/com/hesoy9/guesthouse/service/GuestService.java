package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.Guest;
import com.hesoy9.guesthouse.entity.Reservation;
import com.hesoy9.guesthouse.repository.GuestRepository;
import com.hesoy9.guesthouse.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GuestService {

    private final GuestRepository guestRepository;
    private final ReservationRepository reservationRepository;

    public GuestService(GuestRepository guestRepository, ReservationRepository reservationRepository) {
        this.guestRepository = guestRepository;
        this.reservationRepository = reservationRepository;
    }

    public Guest registerGuest(Guest guest) { // FR11
        return guestRepository.save(guest);
    }

    public List<Guest> searchByName(String name) { // FR13
        return guestRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Reservation> getGuestHistory(Long guestId) { // FR12
        return reservationRepository.findByGuestId(guestId);
    }
}
