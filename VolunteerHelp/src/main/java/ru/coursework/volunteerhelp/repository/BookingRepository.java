package ru.coursework.volunteerhelp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.coursework.volunteerhelp.domain.Booking;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByVolunteerIdAndReservedDateAndStatusIn(Long volunteerId, LocalDate date, Collection<Booking.Status> statuses);
    List<Booking> findByRequestId(Long requestId);
    List<Booking> findByVolunteerId(Long volunteerId);
    boolean existsByRequestIdAndStatusIn(Long requestId, Collection<Booking.Status> statuses);
}
