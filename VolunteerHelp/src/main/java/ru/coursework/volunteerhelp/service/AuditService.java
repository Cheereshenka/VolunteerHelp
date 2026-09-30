package ru.coursework.volunteerhelp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.coursework.volunteerhelp.domain.AuditEvent;
import ru.coursework.volunteerhelp.domain.AppUser;
import ru.coursework.volunteerhelp.repository.AuditEventRepository;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class AuditService {
    private final AuditEventRepository repository;
    private final Clock clock;
    public AuditService(AuditEventRepository repository, Clock clock) { this.repository = repository; this.clock = clock; }

    @Transactional
    public void log(AppUser actor, String action, String entityType, Long entityId, String details) {
        AuditEvent e = new AuditEvent();
        e.occurredAt = OffsetDateTime.now(clock);
        e.actor = actor == null ? "system" : actor.username;
        e.action = action;
        e.entityType = entityType;
        e.entityId = entityId;
        e.details = details;
        repository.save(e);
    }
}
