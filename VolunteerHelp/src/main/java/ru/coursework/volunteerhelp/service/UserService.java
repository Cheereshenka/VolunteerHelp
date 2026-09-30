package ru.coursework.volunteerhelp.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.coursework.volunteerhelp.api.Requests;
import ru.coursework.volunteerhelp.domain.AppUser;
import ru.coursework.volunteerhelp.domain.VolunteerProfile;
import ru.coursework.volunteerhelp.repository.AppUserRepository;
import ru.coursework.volunteerhelp.repository.VolunteerProfileRepository;

import java.time.LocalTime;

@Service
public class UserService {
    private final AppUserRepository users;
    private final VolunteerProfileRepository volunteers;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository users, VolunteerProfileRepository volunteers, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.volunteers = volunteers;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUser current(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) throw BusinessException.forbidden("Требуется авторизация");
        return users.findByUsername(auth.getName()).orElseThrow(() -> BusinessException.notFound("Пользователь не найден"));
    }

    public AppUser byId(Long id) { return users.findById(id).orElseThrow(() -> BusinessException.notFound("Пользователь не найден")); }

    @Transactional
    public AppUser register(Requests.RegistrationInput in) {
        String username = in.username().trim();
        if (users.existsByUsername(username)) throw BusinessException.conflict("Пользователь с таким логином уже существует");
        if (in.role() == AppUser.Role.ADMIN) throw BusinessException.forbidden("Регистрация администратора запрещена");

        AppUser user = users.save(new AppUser(username, passwordEncoder.encode(in.password()), in.fullName().trim(), in.role()));

        if (in.role() == AppUser.Role.VOLUNTEER) {
            if (isBlank(in.skills()) || isBlank(in.district()) || in.availableFrom() == null || in.availableTo() == null) {
                throw BusinessException.bad("Для регистрации волонтёра заполните навыки, район и время доступности");
            }
            if (!in.availableFrom().isBefore(in.availableTo())) {
                throw BusinessException.bad("Начало доступности должно быть раньше окончания");
            }
            VolunteerProfile profile = new VolunteerProfile();
            profile.user = user;
            profile.skills = in.skills().trim();
            profile.district = in.district().trim();
            profile.about = in.about();
            profile.availableFrom = in.availableFrom();
            profile.availableTo = in.availableTo();
            profile.active = true;
            volunteers.save(profile);
        }
        return user;
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
