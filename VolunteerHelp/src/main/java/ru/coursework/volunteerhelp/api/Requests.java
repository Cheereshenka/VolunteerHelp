package ru.coursework.volunteerhelp.api;

import jakarta.validation.constraints.*;
import ru.coursework.volunteerhelp.domain.AppUser;
import java.time.LocalDate;
import java.time.LocalTime;

public final class Requests {
    private Requests() {}

    public record LoginInput(@NotBlank String username, @NotBlank String password) {}

    public record RegistrationInput(
            @NotBlank @Size(max = 80) String username,
            @NotBlank @Size(min = 6, max = 100) String password,
            @NotBlank @Size(max = 120) String fullName,
            @NotNull AppUser.Role role,
            @Size(max = 1000) String skills,
            @Size(max = 160) String district,
            @Size(max = 2000) String about,
            LocalTime availableFrom,
            LocalTime availableTo) {}

    public record VolunteerInput(
            @NotBlank @Size(max = 1000) String skills,
            @NotBlank @Size(max = 160) String district,
            @Size(max = 2000) String about,
            @NotNull LocalTime availableFrom,
            @NotNull LocalTime availableTo,
            boolean active) {}

    public record ServiceRequestInput(
            @NotBlank @Size(max = 120) String category,
            @NotBlank @Size(max = 3000) String description,
            @NotBlank @Size(max = 300) String address,
            @NotNull LocalDate requestedDate,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime) {}

    public record BookingInput(@NotNull Long volunteerId) {}
    public record CompleteInput(@Size(max = 3000) String report) {}
    public record ReviewInput(@Min(1) @Max(5) int rating, @Size(max = 2000) String comment) {}
    public record CancelInput(@NotBlank @Size(max = 500) String reason) {}
}
