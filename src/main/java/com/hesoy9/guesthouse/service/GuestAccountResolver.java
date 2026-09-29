package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.Guest;
import com.hesoy9.guesthouse.repository.GuestRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class GuestAccountResolver {

    private final GuestRepository guestRepository;

    public GuestAccountResolver(GuestRepository guestRepository) {
        this.guestRepository = guestRepository;
    }

    // idOrPassport and contactNumber are intentionally left blank here - DR3 only requires
    // an ID at check-in, and contact number gets collected on the booking form itself.
    public Guest resolveGuest(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        return guestRepository.findByEmail(email)
                .orElseGet(() -> {
                    Guest guest = new Guest();
                    guest.setEmail(email);
                    guest.setName(name != null && !name.isBlank() ? name : email);
                    return guestRepository.save(guest);
                });
    }
}
