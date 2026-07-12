package com.medibook.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String specialty;

    @OneToMany(mappedBy = "doctor", fetch = FetchType.LAZY)
    private List<WorkingHours> workingHours = new ArrayList<>();

    protected Doctor() {}

    public Doctor(String name, String specialty) {
        this.name = name;
        this.specialty = specialty;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSpecialty() { return specialty; }
    public List<WorkingHours> getWorkingHours() { return workingHours; }
}
