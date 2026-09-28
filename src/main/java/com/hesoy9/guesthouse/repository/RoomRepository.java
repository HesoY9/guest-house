package com.hesoy9.guesthouse.repository;

import com.hesoy9.guesthouse.entity.Room;
import com.hesoy9.guesthouse.entity.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    // SELECT * FROM rooms WHERE status = ?
    List<Room> findByStatus(RoomStatus status);

    // SELECT * FROM rooms WHERE room_number = ?
    Optional<Room> findByRoomNumber(String roomNumber);
}
