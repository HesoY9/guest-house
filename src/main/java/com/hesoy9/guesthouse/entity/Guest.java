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
    private String idOrPassport;

    @NotBlank(message = "Name is required")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Contact number is required")
    @Column(name = "contact_number", nullable = false, length = 20)
    private String contactNumber;

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
}
