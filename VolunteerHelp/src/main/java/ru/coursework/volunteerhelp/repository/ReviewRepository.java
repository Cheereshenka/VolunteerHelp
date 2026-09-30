package ru.coursework.volunteerhelp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.coursework.volunteerhelp.domain.Review;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByBookingId(Long bookingId);
}
