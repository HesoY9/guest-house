package com.hesoy9.guesthouse.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "guests")
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "ID/Passport is required")
    @Column(name = "id_or_passport", nullable = false, unique = true, length = 30)
    private String idOPassport;

    @NotBlank(message = "Name is required")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Contact number is required")
    @Column(name = "contact_number", nullable = false, length = 20)
    private String contactNumber;

    public Guest() {
        // required by JPA
    }

    public Guest(String idOPassport, String name, String contactNumber) {
        this.idOPassport = idOPassport;
        this.name = name;
        this.contactNumber = contactNumber;
    }

    public Long getId() {
        return id;
    }

    public String getIdOPassport() {
        return idOPassport;
    }

    public void setIdOPassport(String idOPassport) {
        this.idOPassport = idOPassport;
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
}
