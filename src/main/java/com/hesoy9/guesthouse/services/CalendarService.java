package com.hesoy9.guesthouse.service;

import com.hesoy9.guesthouse.entity.Reservation;
import com.hesoy9.guesthouse.entity.Room;
import com.hesoy9.guesthouse.repository.ReservationRepository;
import com.hesoy9.guesthouse.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Builds the "tape chart": one row per room, one column per night, and a bar for each stay.
@Service
public class CalendarService {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public CalendarService(RoomRepository roomRepository, ReservationRepository reservationRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
    }

    public CalendarView build(LocalDate start, int days) {
        LocalDate end = start.plusDays(days); // exclusive
        List<LocalDate> dates = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            dates.add(start.plusDays(i));
        }

        Map<Long, List<Reservation>> byRoom = reservationRepository.findStaysOverlapping(start, end).stream()
                .collect(Collectors.groupingBy(r -> r.getRoom().getId()));

        List<RoomRow> rows = new ArrayList<>();
        for (Room room : roomRepository.findAllByOrderByRoomNumberAsc()) {
            List<Bar> bars = new ArrayList<>();
            for (Reservation r : byRoom.getOrDefault(room.getId(), List.of())) {
                LocalDate barStart = r.getCheckInDate().isBefore(start) ? start : r.getCheckInDate();
                LocalDate barEnd = r.getCheckOutDate().isAfter(end) ? end : r.getCheckOutDate();
                int column = (int) ChronoUnit.DAYS.between(start, barStart) + 2; // column 1 = room label
                int span = (int) ChronoUnit.DAYS.between(barStart, barEnd);
                if (span > 0) {
                    bars.add(new Bar(r, column, span,
                            r.getCheckInDate().isBefore(start), r.getCheckOutDate().isAfter(end)));
                }
            }
            rows.add(new RoomRow(room, bars));
        }
        return new CalendarView(start, days, dates, rows);
    }

    public record CalendarView(LocalDate start, int days, List<LocalDate> dates, List<RoomRow> rows) {
        public LocalDate previous() { return start.minusDays(days); }
        public LocalDate next() { return start.plusDays(days); }
        public LocalDate end() { return start.plusDays(days - 1); }
    }

    public record RoomRow(Room room, List<Bar> bars) {
    }

    // column/span are CSS grid positions; continuesBefore/After draw a cut edge at the range limits.
    public record Bar(Reservation reservation, int column, int span,
                      boolean continuesBefore, boolean continuesAfter) {
    }
}
