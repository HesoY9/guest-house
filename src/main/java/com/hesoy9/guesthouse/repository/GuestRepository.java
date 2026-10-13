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

    // Staff search box: matches name, ID/passport, phone or email.
    @Query("SELECT g FROM Guest g WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(g.idOrPassport) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR g.contactNumber LIKE CONCAT('%', :q, '%') " +
           "OR LOWER(g.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "ORDER BY g.name")
    List<Guest> search(@Param("q") String q);

    List<Guest> findAllByOrderByNameAsc();

    // links a Google account to its Guest record
    Optional<Guest> findByEmail(String email);
}
