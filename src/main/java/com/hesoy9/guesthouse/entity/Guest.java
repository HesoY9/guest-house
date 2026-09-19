package com.hesoy9.guesthouse.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "guests")
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_or_passport", nullable = false, unique = true, length = 30)
    private String idOrPassport;

    @Column(nullable = false, length = 100)
    private String name;

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
