package ru.coursework.volunteerhelp.domain;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_booking_volunteer_date", columnList = "volunteer_id,reserved_date")
})
public class Booking {
    public enum Status { CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    public ServiceRequest request;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id", nullable = false)
    public VolunteerProfile volunteer;

    @Column(nullable = false)
    public LocalDate reservedDate;

    @Column(nullable = false)
    public LocalTime startTime;

    @Column(nullable = false)
    public LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Status status = Status.CONFIRMED;

    @Column(nullable = false)
    public OffsetDateTime createdAt;

    public OffsetDateTime completedAt;

    @Column(length = 3000)
    public String report;
}
