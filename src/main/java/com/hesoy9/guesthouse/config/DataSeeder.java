package com.hesoy9.guesthouse.config;

import com.hesoy9.guesthouse.entity.*;
import com.hesoy9.guesthouse.repository.ReservationRepository;
import com.hesoy9.guesthouse.repository.RoomRepository;
import com.hesoy9.guesthouse.repository.UserRepository;
import com.hesoy9.guesthouse.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;

// Order(0): runs before any other startup runner, so the first admin always exists first.
@Component
@Order(0)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    // Used for rooms created before the capacity column existed.
    private static final int DEFAULT_CAPACITY = 2;

    private final UserRepository userRepository;
    private final UserService userService;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final String initialAdminPassword;

    public DataSeeder(UserRepository userRepository, UserService userService,
                      RoomRepository roomRepository, ReservationRepository reservationRepository,
                      @Value("${ADMIN_INITIAL_PASSWORD:}") String initialAdminPassword) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.initialAdminPassword = initialAdminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedFirstAdmin();
        backfillRooms();
        backfillCheckIns();
    }

    // No more hard-coded "admin123": the first admin's password comes from ADMIN_INITIAL_PASSWORD,
    // or a random one is generated and printed once. Only runs on an empty users table.
    private void seedFirstAdmin() {
        if (userRepository.count() > 0) return;
        String password = initialAdminPassword;
        boolean generated = password == null || password.isBlank();
        if (generated) {
            byte[] bytes = new byte[12];
            new SecureRandom().nextBytes(bytes);
            password = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }
        userService.createUser("admin", password, Role.ADMIN);
        if (generated) {
            log.warn("No users found - created 'admin' with a generated password: {}  (change it, or set ADMIN_INITIAL_PASSWORD)", password);
        } else {
            log.warn("No users found - created 'admin' using ADMIN_INITIAL_PASSWORD");
        }
    }

    // One-off upgrades for rooms saved before capacity and multiple photos existed.
    private void backfillRooms() {
        for (Room room : roomRepository.findAll()) {
            boolean changed = false;
            if (room.getCapacity() == null) {
                room.setCapacity(DEFAULT_CAPACITY);
                changed = true;
            }
            if (room.getImageUrl() != null && !room.getImageUrl().isBlank()) {
                if (room.getPhotos().isEmpty()) {
                    room.addPhoto(room.getImageUrl().trim());
                }
                room.setImageUrl(null);
                changed = true;
            }
            if (changed) {
                roomRepository.save(room);
                log.info("Upgraded room {} (capacity / photos)", room.getRoomNumber());
            }
        }
    }

    // Before check-in time was recorded, "checked in" only showed as the room being OCCUPIED.
    // Mark those stays as checked in so they can still be checked out.
    private void backfillCheckIns() {
        LocalDate today = LocalDate.now();
        for (Reservation r : reservationRepository.findByStatus(ReservationStatus.ACTIVE)) {
            if (!r.isCheckedIn()
                    && r.getRoom().getStatus() == RoomStatus.OCCUPIED
                    && !r.getCheckInDate().isAfter(today)
                    && !r.getCheckOutDate().isBefore(today)) {
                r.setCheckedInAt(r.getCheckInDate().atTime(14, 0));
                reservationRepository.save(r);
                log.info("Marked reservation {} as checked in (room {} was occupied)", r.getId(), r.getRoom().getRoomNumber());
            }
        }
    }
}
