package ru.coursework.volunteerhelp.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "service_requests")
public class ServiceRequest {
    public enum Status { OPEN, BOOKED, COMPLETED, CANCELLED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    public AppUser customer;

    @Column(nullable = false, length = 120)
    public String category;

    @Column(nullable = false, length = 3000)
    public String description;

    @Column(nullable = false, length = 300)
    public String address;

    @Column(nullable = false)
    public LocalDate requestedDate;

    @Column(nullable = false)
    public LocalTime startTime;

    @Column(nullable = false)
    public LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Status status = Status.OPEN;
}
