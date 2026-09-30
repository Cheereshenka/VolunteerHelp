package ru.coursework.volunteerhelp.service;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.coursework.volunteerhelp.api.Requests;
import ru.coursework.volunteerhelp.api.Views;
import ru.coursework.volunteerhelp.domain.*;
import ru.coursework.volunteerhelp.repository.*;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class VolunteerService {
    private final VolunteerProfileRepository volunteers;
    private final BookingRepository bookings;
    private final AppUserRepository users;
    private final ReviewRepository reviews;
    private final UserService userService;
    private final AuditService audit;

    public VolunteerService(VolunteerProfileRepository volunteers, AppUserRepository users,
                            ReviewRepository reviews, UserService userService, AuditService audit, BookingRepository bookings) {
        this.volunteers = volunteers; this.users = users; this.reviews = reviews; this.bookings = bookings;
        this.userService = userService; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Views.PageResult<Views.VolunteerView> search(String category, String district,
                                                         LocalDate date, java.time.LocalTime from,
                                                         java.time.LocalTime to, BigDecimal minRating,
                                                         int page, int size) {
        validatePage(page, size);
        Specification<VolunteerProfile> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isTrue(root.get("active")));
            if (district != null && !district.isBlank()) ps.add(cb.like(cb.lower(root.get("district")), "%" + district.toLowerCase() + "%"));
            if (minRating != null) ps.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating));
            if (category != null && !category.isBlank()) ps.add(cb.like(cb.lower(root.get("skills")), "%" + category.toLowerCase() + "%"));
            if (from != null) ps.add(cb.lessThanOrEqualTo(root.get("availableFrom"), from));
            if (to != null) ps.add(cb.greaterThanOrEqualTo(root.get("availableTo"), to));
            return cb.and(ps.toArray(Predicate[]::new));
        };
        if (date == null) {
            Page<VolunteerProfile> result = volunteers.findAll(spec, PageRequest.of(page, size, Sort.by("rating").descending()));
            return new Views.PageResult<>(result.map(this::view).getContent(), page, size, result.getTotalElements(), result.getTotalPages());
        }
        // Для заданной даты дополнительно исключаем интервалы, занятые активными бронированиями.
        List<VolunteerProfile> all = volunteers.findAll(spec, Sort.by("rating").descending());
        List<VolunteerProfile> free = all.stream().filter(v -> {
            var active = bookings.findByVolunteerIdAndReservedDateAndStatusIn(v.id, date, java.util.List.of(ru.coursework.volunteerhelp.domain.Booking.Status.CONFIRMED, ru.coursework.volunteerhelp.domain.Booking.Status.IN_PROGRESS));
            if (from == null || to == null) return active.isEmpty();
            return active.stream().noneMatch(b -> MatchingPolicy.overlaps(b.startTime, b.endTime, from, to));
        }).toList();
        int fromIndex = Math.min(page * size, free.size());
        int toIndex = Math.min(fromIndex + size, free.size());
        List<Views.VolunteerView> content = free.subList(fromIndex, toIndex).stream().map(this::view).toList();
        int totalPages = (int) Math.ceil(free.size() / (double) size);
        return new Views.PageResult<>(content, page, size, free.size(), totalPages);
    }

    @Transactional(readOnly = true)
    public Views.VolunteerView get(Long id) { return view(volunteers.findById(id).orElseThrow(() -> BusinessException.notFound("Профиль волонтёра не найден"))); }

    @Transactional
    public Views.VolunteerView createOrUpdate(Authentication auth, Requests.VolunteerInput in, Long profileId) {
        AppUser actor = userService.current(auth);
        VolunteerProfile p;
        if (profileId == null) {
            if (actor.role != AppUser.Role.VOLUNTEER && actor.role != AppUser.Role.ADMIN)
                throw BusinessException.forbidden("Профиль может создать только волонтёр");
            if (actor.role == AppUser.Role.VOLUNTEER && volunteers.findByUserId(actor.id).isPresent())
                throw BusinessException.conflict("Профиль волонтёра уже существует");
            p = new VolunteerProfile();
            p.user = actor;
        } else {
            p = volunteers.findById(profileId).orElseThrow(() -> BusinessException.notFound("Профиль не найден"));
            if (actor.role != AppUser.Role.ADMIN && !Objects.equals(p.user.id, actor.id))
                throw BusinessException.forbidden("Можно изменять только собственный профиль");
        }
        if (!in.availableFrom().isBefore(in.availableTo())) throw BusinessException.bad("Начало доступности должно быть раньше окончания");
        p.skills = in.skills().trim(); p.district = in.district().trim(); p.about = in.about();
        p.availableFrom = in.availableFrom(); p.availableTo = in.availableTo(); p.active = in.active();
        volunteers.save(p);
        audit.log(actor, profileId == null ? "CREATE" : "UPDATE", "VolunteerProfile", p.id, "Профиль волонтёра");
        return view(p);
    }

    @Transactional
    public void recalculateRating(VolunteerProfile p) {
        var rs = reviews.findAll().stream().filter(r -> r.booking.volunteer.id.equals(p.id)).toList();
        double avg = rs.stream().mapToInt(r -> r.rating).average().orElse(0.0);
        p.rating = BigDecimal.valueOf(avg).setScale(2, java.math.RoundingMode.HALF_UP);
        volunteers.save(p);
    }

    public VolunteerProfile locked(Long id) { return volunteers.findLockedById(id).orElseThrow(() -> BusinessException.notFound("Профиль волонтёра не найден")); }

    private Views.VolunteerView view(VolunteerProfile p) {
        return new Views.VolunteerView(p.id, p.user.id, p.user.username, p.user.fullName, p.skills, p.district, p.about,
                p.rating, p.availableFrom, p.availableTo, p.active);
    }

    public static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new BusinessException(HttpStatus.BAD_REQUEST, "Параметры page/size должны быть в допустимом диапазоне");
    }
}
