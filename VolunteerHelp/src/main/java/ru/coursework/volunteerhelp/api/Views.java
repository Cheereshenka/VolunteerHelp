package ru.coursework.volunteerhelp.api;

import ru.coursework.volunteerhelp.domain.*;
import java.math.BigDecimal;
import java.time.*;

public final class Views {
    private Views() {}

    public record UserView(Long id, String username, String fullName, AppUser.Role role) {}
    public record VolunteerView(Long id, Long userId, String username, String fullName, String skills,
                                String district, String about, BigDecimal rating,
                                LocalTime availableFrom, LocalTime availableTo, boolean active) {}
    public record RequestView(Long id, Long customerId, String customerName, String category,
                              String description, String address, LocalDate requestedDate,
                              LocalTime startTime, LocalTime endTime, ServiceRequest.Status status) {}
    public record BookingView(Long id, Long requestId, Long volunteerId, String volunteerName,
                              String category, String address, LocalDate reservedDate,
                              LocalTime startTime, LocalTime endTime, Booking.Status status,
                              OffsetDateTime createdAt, OffsetDateTime completedAt, String report) {}
    public record ReviewView(Long id, Long bookingId, int rating, String comment, OffsetDateTime createdAt) {}
    public record AuditView(Long id, OffsetDateTime occurredAt, String actor, String action,
                            String entityType, Long entityId, String details) {}
    public record PageResult<T>(java.util.List<T> content, int page, int size, long totalElements, int totalPages) {}
    public record DashboardView(long openRequests, long availableVolunteers, long activeBookings, long completedBookings) {}
}
