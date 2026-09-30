package ru.coursework.volunteerhelp.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "volunteer_profiles")
public class VolunteerProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    public AppUser user;

    @Version
    public long version;

    @Column(nullable = false, length = 1000)
    public String skills;

    @Column(nullable = false, length = 160)
    public String district;

    @Column(length = 2000)
    public String about;

    @Column(nullable = false, precision = 3, scale = 2)
    public BigDecimal rating = BigDecimal.ZERO;

    @Column(nullable = false)
    public LocalTime availableFrom;

    @Column(nullable = false)
    public LocalTime availableTo;

    @Column(nullable = false)
    public boolean active = true;
}
