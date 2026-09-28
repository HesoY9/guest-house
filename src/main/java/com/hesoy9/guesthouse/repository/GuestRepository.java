package com.hesoy9.guesthouse.repository;

import com.hesoy9.guesthouse.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuestRepository extends JpaRepository<Guest, Long> {

    // exact match - useful for checking if a guest already exists (FR13)
    Optional<Guest> findByIdOPassport(String idOPassport);

    // partial, case-insensitive match - for a search box (FR13)
    List<Guest> findByNameContainingIgnoreCase(String name);
}
