package ru.coursework.volunteerhelp.domain;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "app_users")
public class AppUser {
    public enum Role { CUSTOMER, VOLUNTEER, ADMIN }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 80)
    public String username;

    @Column(nullable = false)
    public String passwordHash;

    @Column(nullable = false, length = 120)
    public String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Role role;

    protected AppUser() {}

    public AppUser(String username, String passwordHash, String fullName, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }

    @Override public boolean equals(Object o) { return o instanceof AppUser u && Objects.equals(id, u.id); }
    @Override public int hashCode() { return Objects.hashCode(id); }
}
