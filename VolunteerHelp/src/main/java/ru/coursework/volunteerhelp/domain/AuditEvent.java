package ru.coursework.volunteerhelp.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_event")
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false) public OffsetDateTime occurredAt;
    @Column(nullable = false, length = 80) public String actor;
    @Column(nullable = false, length = 80) public String action;
    @Column(nullable = false, length = 80) public String entityType;
    public Long entityId;
    @Column(length = 1000) public String details;
}
