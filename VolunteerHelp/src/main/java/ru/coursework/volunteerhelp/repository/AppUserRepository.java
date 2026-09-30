package ru.coursework.volunteerhelp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.coursework.volunteerhelp.domain.AppUser;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);
}
