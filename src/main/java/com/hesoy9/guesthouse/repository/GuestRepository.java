package com.hesoy9.guesthouse.repository;

import com.hesoy9.guesthouse.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuestRepository extends JpaRepository<Guest, Long> {

    // @Query, not a derived name - "idOrPassport" contains "Or" at a word boundary, which
    // Spring's method-name parser reads as an OR condition rather than one property name.
    @Query("SELECT g FROM Guest g WHERE g.idOrPassport = :idOrPassport")
    Optional<Guest> findByIdOrPassport(@Param("idOrPassport") String idOrPassport);

    // partial, case-insensitive match - for a search box (FR13)
    List<Guest> findByNameContainingIgnoreCase(String name);

    // links a Google account to its Guest record
    Optional<Guest> findByEmail(String email);
}
