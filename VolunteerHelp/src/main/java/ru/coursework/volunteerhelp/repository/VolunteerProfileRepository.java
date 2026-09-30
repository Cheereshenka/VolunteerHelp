package ru.coursework.volunteerhelp.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import ru.coursework.volunteerhelp.domain.VolunteerProfile;
import java.util.*;

public interface VolunteerProfileRepository extends JpaRepository<VolunteerProfile, Long>, JpaSpecificationExecutor<VolunteerProfile> {
    Optional<VolunteerProfile> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VolunteerProfile v where v.id = :id")
    Optional<VolunteerProfile> findLockedById(@Param("id") Long id);
}
