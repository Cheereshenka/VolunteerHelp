package ru.coursework.volunteerhelp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.coursework.volunteerhelp.domain.AuditEvent;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    Page<AuditEvent> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
