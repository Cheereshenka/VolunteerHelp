package ru.coursework.volunteerhelp.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import ru.coursework.volunteerhelp.domain.AppUser;
import ru.coursework.volunteerhelp.service.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@Tag(name = "VolunteerHelp API")
public class ApiController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserService userService;
    private final ru.coursework.volunteerhelp.repository.AppUserRepository userRepository;
    private final VolunteerService volunteers;
    private final BookingService bookingService;
    private final ru.coursework.volunteerhelp.repository.AuditEventRepository auditRepository;

    public ApiController(AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository,
                         UserService users, VolunteerService volunteers, BookingService bookingService,
                         ru.coursework.volunteerhelp.repository.AuditEventRepository auditRepository,
                         ru.coursework.volunteerhelp.repository.AppUserRepository userRepository) {
        this.authenticationManager = authenticationManager; this.securityContextRepository = securityContextRepository;
        this.userService = users; this.userRepository = userRepository; this.volunteers = volunteers; this.bookingService = bookingService; this.auditRepository = auditRepository;
    }

    @GetMapping("/auth/csrf")
    @Operation(summary = "Получить CSRF-токен")
    public Map<String, String> csrf(CsrfToken token) { return Map.of("token", token.getToken(), "headerName", token.getHeaderName()); }

    @PostMapping("/auth/register")
    @Operation(summary = "Зарегистрировать заказчика или волонтёра")
    public ResponseEntity<Views.UserView> register(@Valid @RequestBody Requests.RegistrationInput input) {
        AppUser u = userService.register(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Views.UserView(u.id, u.username, u.fullName, u.role));
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Войти в систему")
    public Views.UserView login(@Valid @RequestBody Requests.LoginInput input,
                                HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(input.username(), input.password()));
        request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        AppUser u = userService.current(auth);
        return new Views.UserView(u.id, u.username, u.fullName, u.role);
    }

    @PostMapping("/auth/logout")
    @Operation(summary = "Завершить сеанс")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        if (request.getSession(false) != null) request.getSession(false).invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/auth/me")
    @Operation(summary = "Текущий пользователь")
    public Views.UserView me(Authentication auth) {
        AppUser u = userService.current(auth);
        return new Views.UserView(u.id, u.username, u.fullName, u.role);
    }

    @GetMapping("/volunteers")
    @Operation(summary = "Поиск волонтёров")
    public Views.PageResult<Views.VolunteerView> volunteers(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) LocalTime from,
            @RequestParam(required = false) LocalTime to,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return volunteers.search(category, district, date, from, to, minRating, page, size);
    }

    @GetMapping("/volunteers/{id}")
    public Views.VolunteerView volunteer(@PathVariable Long id) { return volunteers.get(id); }

    @PostMapping("/volunteers")
    @ResponseStatus(HttpStatus.CREATED)
    public Views.VolunteerView createVolunteer(@Valid @RequestBody Requests.VolunteerInput input, Authentication auth) {
        return volunteers.createOrUpdate(auth, input, null);
    }

    @PutMapping("/volunteers/{id}")
    public Views.VolunteerView updateVolunteer(@PathVariable Long id, @Valid @RequestBody Requests.VolunteerInput input, Authentication auth) {
        return volunteers.createOrUpdate(auth, input, id);
    }

    @GetMapping("/requests")
    public Views.PageResult<Views.RequestView> requests(Authentication auth,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return bookingService.requests(auth, page, size);
    }

    @PostMapping("/requests")
    public ResponseEntity<Views.RequestView> createRequest(@Valid @RequestBody Requests.ServiceRequestInput input, Authentication auth) {
        Views.RequestView r = bookingService.createRequest(auth, input);
        return ResponseEntity.created(URI.create("/api/requests/" + r.id())).body(r);
    }

    @PutMapping("/requests/{id}")
    public Views.RequestView updateRequest(@PathVariable Long id, @Valid @RequestBody Requests.ServiceRequestInput input, Authentication auth) {
        return bookingService.updateRequest(auth, id, input);
    }

    @PatchMapping("/requests/{id}/cancel")
    public Views.RequestView cancelRequest(@PathVariable Long id, @Valid @RequestBody Requests.CancelInput input, Authentication auth) {
        return bookingService.cancelRequest(auth, id, input);
    }

    @PostMapping("/requests/{id}/bookings")
    public ResponseEntity<Views.BookingView> createBooking(@PathVariable Long id, @Valid @RequestBody Requests.BookingInput input, Authentication auth) {
        Views.BookingView b = bookingService.createBooking(auth, id, input);
        return ResponseEntity.created(URI.create("/api/bookings/" + b.id())).body(b);
    }

    @GetMapping("/bookings")
    public Views.PageResult<Views.BookingView> bookings(Authentication auth,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return bookingService.bookings(auth, page, size);
    }

    @GetMapping("/bookings/{id}")
    public Views.BookingView booking(@PathVariable Long id, Authentication auth) {
        var b = bookingService.getBooking(id);
        if (auth == null) throw BusinessException.forbidden("Требуется авторизация");
        AppUser u = userService.current(auth);
        boolean ok = u.role == AppUser.Role.ADMIN || b.request.customer.id.equals(u.id) || b.volunteer.user.id.equals(u.id);
        if (!ok) throw BusinessException.forbidden("Бронирование недоступно текущему пользователю");
        return new Views.BookingView(b.id, b.request.id, b.volunteer.id, b.volunteer.user.fullName, b.request.category,
                b.request.address, b.reservedDate, b.startTime, b.endTime, b.status, b.createdAt, b.completedAt, b.report);
    }

    @PostMapping("/bookings/{id}/start")
    public Views.BookingView start(@PathVariable Long id, Authentication auth) { return bookingService.start(auth, id); }

    @PostMapping("/bookings/{id}/complete")
    public Views.BookingView complete(@PathVariable Long id, @Valid @RequestBody Requests.CompleteInput input, Authentication auth) {
        return bookingService.complete(auth, id, input);
    }

    @PatchMapping("/bookings/{id}/cancel")
    public Views.BookingView cancelBooking(@PathVariable Long id, @Valid @RequestBody Requests.CancelInput input, Authentication auth) {
        return bookingService.cancelBooking(auth, id, input);
    }

    @PostMapping("/bookings/{id}/review")
    public Views.ReviewView review(@PathVariable Long id, @Valid @RequestBody Requests.ReviewInput input, Authentication auth) {
        return bookingService.review(auth, id, input);
    }

    @GetMapping("/dashboard")
    public Views.DashboardView dashboard(Authentication auth) {
        AppUser u = userService.current(auth);
        long open = bookingService.requests(auth, 0, 100).content().stream().filter(r -> r.status() == ru.coursework.volunteerhelp.domain.ServiceRequest.Status.OPEN).count();
        long available = volunteers.search(null, null, null, null, null, null, 0, 100).totalElements();
        long active = bookingService.bookings(auth, 0, 100).content().stream().filter(b -> b.status() == ru.coursework.volunteerhelp.domain.Booking.Status.CONFIRMED || b.status() == ru.coursework.volunteerhelp.domain.Booking.Status.IN_PROGRESS).count();
        long completed = bookingService.bookings(auth, 0, 100).content().stream().filter(b -> b.status() == ru.coursework.volunteerhelp.domain.Booking.Status.COMPLETED).count();
        return new Views.DashboardView(open, available, active, completed);
    }

    @GetMapping("/users")
    public List<Views.UserView> users(Authentication auth) {
        AppUser u = userService.current(auth);
        if (u.role != AppUser.Role.ADMIN) throw BusinessException.forbidden("Список пользователей доступен администратору");
        return userRepository.findAll().stream().map(x -> new Views.UserView(x.id, x.username, x.fullName, x.role)).toList();
    }

    @GetMapping("/audit")
    public Views.PageResult<Views.AuditView> audit(Authentication auth,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        AppUser u = userService.current(auth);
        if (u.role != AppUser.Role.ADMIN) throw BusinessException.forbidden("Журнал аудита доступен администратору");
        VolunteerService.validatePage(page, size);
        var p = auditRepository.findAllByOrderByOccurredAtDesc(org.springframework.data.domain.PageRequest.of(page, size));
        var content = p.getContent().stream().map(e -> new Views.AuditView(e.id, e.occurredAt, e.actor, e.action, e.entityType, e.entityId, e.details)).toList();
        return new Views.PageResult<>(content, page, size, p.getTotalElements(), p.getTotalPages());
    }
}
