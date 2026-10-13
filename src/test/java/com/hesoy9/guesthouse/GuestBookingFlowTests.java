package com.hesoy9.guesthouse;

import com.hesoy9.guesthouse.entity.Reservation;
import com.hesoy9.guesthouse.entity.ReservationStatus;
import com.hesoy9.guesthouse.repository.ReservationRepository;
import com.hesoy9.guesthouse.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

// The public-site journey end to end, with a simulated Google sign-in:
// book -> confirmation -> My bookings -> printable confirmation -> cancel.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local-h2")
class GuestBookingFlowTests {

    @Autowired MockMvc mvc;
    @Autowired RoomRepository rooms;
    @Autowired ReservationRepository reservations;

    private static final Map<String, Object> ANNA = Map.of("sub", "1", "email", "anna@example.com", "name", "Anna Test");
    private static final Map<String, Object> BOB = Map.of("sub", "2", "email", "bob@example.com", "name", "Bob Other");

    @Test
    void guestCanBookViewPrintAndCancel() throws Exception {
        Long roomId = rooms.findByRoomNumber("102").orElseThrow().getId(); // Twin, LKR 11,000, sleeps 2
        LocalDate in = LocalDate.now().plusDays(30);
        LocalDate out = in.plusDays(3);

        // Booking page shows the nights x price = total summary before confirming
        mvc.perform(get("/book/{id}", roomId).param("checkIn", in.toString()).param("checkOut", out.toString()).param("guests", "2")
                        .with(oauth2Login().attributes(a -> a.putAll(ANNA))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("LKR 11,000 × 3 nights")))
                .andExpect(content().string(containsString("LKR 33,000")));

        // Too many guests is refused by the shared ReservationService rule
        mvc.perform(post("/book/{id}", roomId).with(csrf()).with(oauth2Login().attributes(a -> a.putAll(ANNA)))
                        .param("checkInDate", in.toString()).param("checkOutDate", out.toString())
                        .param("numberOfGuests", "3").param("contactNumber", "+94 77 000 0000"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("sleeps at most 2")));

        // A valid booking goes through to the confirmation page
        mvc.perform(post("/book/{id}", roomId).with(csrf()).with(oauth2Login().attributes(a -> a.putAll(ANNA)))
                        .param("checkInDate", in.toString()).param("checkOutDate", out.toString())
                        .param("numberOfGuests", "2").param("contactNumber", "+94 77 000 0000")
                        .param("specialRequests", "Late arrival"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("You're booked!")))
                .andExpect(content().string(containsString("LKR 33,000")));

        Reservation booked = reservations.findAll().stream()
                .filter(r -> r.getGuest().getEmail() != null && r.getGuest().getEmail().equals("anna@example.com"))
                .max(Comparator.comparing(Reservation::getId)).orElseThrow();
        assertEquals("Late arrival", booked.getNotes());

        mvc.perform(get("/my-bookings").with(oauth2Login().attributes(a -> a.putAll(ANNA))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(booked.getReference())))
                .andExpect(content().string(containsString("Cancel booking")));

        mvc.perform(get("/my-bookings/{id}", booked.getId()).with(oauth2Login().attributes(a -> a.putAll(ANNA))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Booking confirmation")));

        // Someone else can neither see nor cancel it
        mvc.perform(get("/my-bookings/{id}", booked.getId()).with(oauth2Login().attributes(a -> a.putAll(BOB))))
                .andExpect(status().isNotFound());
        mvc.perform(post("/my-bookings/{id}/cancel", booked.getId()).with(csrf()).with(oauth2Login().attributes(a -> a.putAll(BOB))))
                .andExpect(status().isNotFound());
        assertEquals(ReservationStatus.ACTIVE, reservations.findById(booked.getId()).orElseThrow().getStatus());

        // The owner cancels before check-in
        mvc.perform(post("/my-bookings/{id}/cancel", booked.getId()).with(csrf()).with(oauth2Login().attributes(a -> a.putAll(ANNA))))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", containsString("has been cancelled")));
        assertEquals(ReservationStatus.CANCELLED, reservations.findById(booked.getId()).orElseThrow().getStatus());
    }

    @Test
    void cancelWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/my-bookings/1/cancel").with(oauth2Login().attributes(a -> a.putAll(ANNA))))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicPagesShowBrandAndRupees() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Beachside Heaven")))
                .andExpect(content().string(containsString("LKR")))
                .andExpect(content().string(not(containsString("Hesoy9"))))
                .andExpect(content().string(not(containsString("$"))));
    }
}
