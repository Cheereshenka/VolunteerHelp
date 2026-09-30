package ru.coursework.volunteerhelp.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "reviews")
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    public Booking booking;

    @Column(nullable = false)
    public int rating;

    @Column(length = 2000)
    public String comment;

    @Column(nullable = false)
    public OffsetDateTime createdAt;
}
