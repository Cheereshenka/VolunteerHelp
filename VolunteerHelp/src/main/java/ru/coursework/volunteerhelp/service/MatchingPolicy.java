package ru.coursework.volunteerhelp.service;

import ru.coursework.volunteerhelp.domain.ServiceRequest;
import ru.coursework.volunteerhelp.domain.VolunteerProfile;
import java.math.BigDecimal;
import java.time.LocalTime;

public final class MatchingPolicy {
    private MatchingPolicy() {}

    public static boolean matchesRequest(VolunteerProfile v, ServiceRequest r, BigDecimal minRating) {
        if (!v.active) return false;
        if (!containsCategory(v.skills, r.category)) return false;
        if (!v.district.equalsIgnoreCase(r.address.trim()) && !r.address.toLowerCase().contains(v.district.toLowerCase())) {
            return false;
        }
        if (!isAvailable(v.availableFrom, v.availableTo, r.startTime, r.endTime)) return false;
        return minRating == null || v.rating.compareTo(minRating) >= 0;
    }

    public static boolean containsCategory(String skills, String category) {
        return java.util.Arrays.stream(skills.split(","))
                .map(String::trim).anyMatch(s -> s.equalsIgnoreCase(category.trim()));
    }

    public static boolean isAvailable(LocalTime from, LocalTime to, LocalTime start, LocalTime end) {
        return !start.isBefore(from) && !end.isAfter(to) && start.isBefore(end);
    }

    public static boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }
}
