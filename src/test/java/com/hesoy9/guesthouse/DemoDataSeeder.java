package com.hesoy9.guesthouse;

import com.hesoy9.guesthouse.entity.*;
import com.hesoy9.guesthouse.repository.*;
import com.hesoy9.guesthouse.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;

// Demo rooms, guests, bookings and payments for the "local-h2" profile only (see
// application-local-h2.properties). Lives in src/test, so it never ships with the real app.
@Component
@Profile("local-h2")
@Order(10)
public class DemoDataSeeder implements CommandLineRunner {

    private final RoomRepository rooms;
    private final GuestRepository guests;
    private final ReservationRepository reservations;
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final UserService userService;

    public DemoDataSeeder(RoomRepository rooms, GuestRepository guests, ReservationRepository reservations,
                          InvoiceRepository invoices, PaymentRepository payments, UserService userService) {
        this.rooms = rooms;
        this.guests = guests;
        this.reservations = reservations;
        this.invoices = invoices;
        this.payments = payments;
        this.userService = userService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (rooms.count() > 0) return;
        LocalDate today = LocalDate.now();

        // Demo staff accounts, one per role (in-memory database only)
        userService.createUser("reception", "demo-reception-2026", Role.RECEPTIONIST);
        userService.createUser("housekeeper", "demo-housekeeping-2026", Role.HOUSEKEEPING);
        userService.createUser("manager", "demo-manager-2026", Role.MANAGER);

        Room r101 = room("101", "Double", 12500, 2, RoomStatus.OCCUPIED,
                "Bright double room with a queen bed and a private balcony looking over the garden.",
                EnumSet.of(Amenity.WIFI, Amenity.AIR_CONDITIONING, Amenity.HOT_WATER, Amenity.PRIVATE_BATHROOM, Amenity.BALCONY));
        Room r102 = room("102", "Twin", 11000, 2, RoomStatus.AVAILABLE,
                "Two single beds, a work desk and plenty of light. Great for friends travelling together.",
                EnumSet.of(Amenity.WIFI, Amenity.CEILING_FAN, Amenity.HOT_WATER, Amenity.PRIVATE_BATHROOM, Amenity.WORK_DESK));
        Room r201 = room("201", "Deluxe", 18500, 3, RoomStatus.CLEANING_REQUIRED,
                "Our sea-view deluxe room on the upper floor, with a king bed and a day bed for a third guest.",
                EnumSet.of(Amenity.WIFI, Amenity.AIR_CONDITIONING, Amenity.SEA_VIEW, Amenity.BALCONY, Amenity.TV, Amenity.MINI_FRIDGE, Amenity.HOT_WATER, Amenity.BREAKFAST));
        Room r202 = room("202", "Family", 24000, 5, RoomStatus.AVAILABLE,
                "Spacious family room with a double bed and three singles, steps from the beach path.",
                EnumSet.of(Amenity.WIFI, Amenity.AIR_CONDITIONING, Amenity.BEACH_ACCESS, Amenity.TV, Amenity.KITCHENETTE, Amenity.PARKING));
        Room r103 = room("103", "Single", 7500, 1, RoomStatus.AVAILABLE,
                "A cosy single room for solo travellers.",
                EnumSet.of(Amenity.WIFI, Amenity.CEILING_FAN, Amenity.HOT_WATER));
        Room r301 = room("301", "Suite", 32000, 4, RoomStatus.AVAILABLE,
                "Top-floor suite with a separate lounge and a wide terrace facing the ocean.",
                EnumSet.of(Amenity.WIFI, Amenity.AIR_CONDITIONING, Amenity.SEA_VIEW, Amenity.BALCONY, Amenity.TV, Amenity.MINI_FRIDGE, Amenity.BREAKFAST, Amenity.PARKING));

        Guest nimal = guest("Nimal Perera", "901234567V", "+94 77 123 4567", "nimal@example.com");
        Guest sarah = guest("Sarah Collins", "P8812345", "+44 7700 900123", "sarah@example.com");
        Guest kenji = guest("Kenji Watanabe", null, "+81 90 1234 5678", "kenji@example.com");
        Guest amaya = guest("Amaya Fernando", "199012345678", "+94 71 555 0101", null);
        Guest lucas = guest("Lucas Martin", "FR77881122", "+33 6 12 34 56 78", "lucas@example.com");

        // In house, leaving tomorrow
        Reservation inHouse = stay(nimal, r101, today.minusDays(2), today.plusDays(1), 2, BookingChannel.ONLINE_PORTAL);
        inHouse.setCheckedInAt(today.minusDays(2).atTime(14, 20));
        reservations.save(inHouse);

        // Due out today (checked in), room 202 was used - now it's due to leave
        Reservation dueOut = stay(lucas, r202, today.minusDays(3), today, 3, BookingChannel.PHONE);
        dueOut.setCheckedInAt(today.minusDays(3).atTime(13, 5));
        r202.setStatus(RoomStatus.OCCUPIED);
        rooms.save(r202);
        reservations.save(dueOut);

        // Arrivals today: one with ID on file, one who booked online without ID yet
        reservations.save(stay(sarah, r102, today, today.plusDays(4), 2, BookingChannel.ONLINE_PORTAL));
        Reservation kenjiStay = stay(kenji, r201, today, today.plusDays(3), 2, BookingChannel.ONLINE_PORTAL);
        kenjiStay.setNotes("Arriving late, around 10pm. Vegetarian breakfast please.");
        reservations.save(kenjiStay);

        // Upcoming
        reservations.save(stay(amaya, r301, today.plusDays(5), today.plusDays(8), 4, BookingChannel.IN_PERSON));
        reservations.save(stay(sarah, r103, today.plusDays(9), today.plusDays(11), 1, BookingChannel.ONLINE_PORTAL));
        reservations.save(stay(nimal, r201, today.plusDays(6), today.plusDays(9), 2, BookingChannel.PHONE));

        // Completed + paid, completed + part-paid, cancelled
        Reservation done1 = stay(amaya, r201, today.minusDays(8), today.minusDays(5), 2, BookingChannel.IN_PERSON);
        done1.setStatus(ReservationStatus.COMPLETED);
        done1.setCheckedInAt(today.minusDays(8).atTime(14, 0));
        done1.setCheckedOutAt(today.minusDays(5).atTime(10, 30));
        reservations.save(done1);
        Invoice inv1 = invoices.save(new Invoice(done1, done1.getEstimatedTotal(), today.minusDays(5)));
        payment(inv1, done1.getEstimatedTotal(), PaymentMethod.CARD, today.minusDays(5));

        Reservation done2 = stay(kenji, r102, today.minusDays(6), today.minusDays(4), 1, BookingChannel.ONLINE_PORTAL);
        done2.setStatus(ReservationStatus.COMPLETED);
        done2.setCheckedInAt(today.minusDays(6).atTime(15, 0));
        done2.setCheckedOutAt(today.minusDays(4).atTime(11, 0));
        reservations.save(done2);
        Invoice inv2 = invoices.save(new Invoice(done2, done2.getEstimatedTotal(), today.minusDays(4)));
        payment(inv2, 10000.0, PaymentMethod.CASH, today.minusDays(4));

        Reservation cancelled = stay(lucas, r301, today.plusDays(14), today.plusDays(16), 2, BookingChannel.ONLINE_PORTAL);
        cancelled.setStatus(ReservationStatus.CANCELLED);
        cancelled.setCancelledAt(today.minusDays(1).atTime(9, 0));
        reservations.save(cancelled);
    }

    private Room room(String number, String type, double price, int capacity, RoomStatus status,
                      String description, EnumSet<Amenity> amenities) {
        Room room = new Room();
        room.setRoomNumber(number);
        room.setType(type);
        room.setPrice(price);
        room.setCapacity(capacity);
        room.setStatus(status);
        room.setDescription(description);
        room.setAmenities(amenities);
        return rooms.save(room);
    }

    private Guest guest(String name, String id, String phone, String email) {
        Guest guest = new Guest();
        guest.setName(name);
        guest.setIdOrPassport(id);
        guest.setContactNumber(phone);
        guest.setEmail(email);
        return guests.save(guest);
    }

    private Reservation stay(Guest guest, Room room, LocalDate in, LocalDate out, int people, BookingChannel channel) {
        return new Reservation(guest, room, in, out, people, ReservationStatus.ACTIVE, channel);
    }

    private void payment(Invoice invoice, double amount, PaymentMethod method, LocalDate date) {
        Payment payment = new Payment(invoice, amount, method, date);
        payment.setReceivedBy("reception");
        payments.save(payment);
    }
}
