package com.hesoy9.guesthouse.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "guests")
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // No longer @NotBlank / nullable=false: DR3 only requires this at check-in, not at
    // booking time, and an online guest hasn't given it yet when their account is created.
    // Still shown as "required" on the staff walk-in form via the HTML input itself.
    @Column(name = "id_or_passport", unique = true, length = 30)
    private String idOrPassport;

    @NotBlank(message = "Name is required")
    @Column(nullable = false, length = 100)
    private String name;

    // Same reasoning as idOrPassport - collected on the booking form instead, not at signup.
    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    // Only set for guests who signed in with Google; null for staff-registered walk-ins.
    @Column(unique = true, length = 150)
    private String email;

    public Guest() {
        // required by JPA
    }

    public Long getId() {
        return id;
    }

    public String getIdOrPassport() {
        return idOrPassport;
    }

    public void setIdOrPassport(String idOrPassport) {
        this.idOrPassport = idOrPassport;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
