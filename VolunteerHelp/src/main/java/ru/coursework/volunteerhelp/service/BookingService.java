package ru.coursework.volunteerhelp.service;

import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.coursework.volunteerhelp.api.Requests;
import ru.coursework.volunteerhelp.api.Views;
import ru.coursework.volunteerhelp.domain.*;
import ru.coursework.volunteerhelp.repository.*;

import java.time.*;
import java.util.*;

@Service
public class BookingService {
    private static final List<Booking.Status> ACTIVE = List.of(Booking.Status.CONFIRMED, Booking.Status.IN_PROGRESS);

    private final ServiceRequestRepository requests;
    private final BookingRepository bookings;
    private final VolunteerProfileRepository volunteers;
    private final ReviewRepository reviews;
    private final UserService userService;
    private final AuditService audit;
    private final VolunteerService volunteerService;
    private final Clock clock;

    public BookingService(ServiceRequestRepository requests, BookingRepository bookings,
                          VolunteerProfileRepository volunteers, ReviewRepository reviews,
                          UserService userService, AuditService audit, VolunteerService volunteerService,
                          Clock clock) {
        this.requests = requests; this.bookings = bookings; this.volunteers = volunteers; this.reviews = reviews;
        this.userService = userService; this.audit = audit; this.volunteerService = volunteerService; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Views.PageResult<Views.RequestView> requests(Authentication auth, int page, int size) {
        VolunteerService.validatePage(page, size);
        AppUser u = userService.current(auth);
        Page<ServiceRequest> p;
        if (u.role == AppUser.Role.ADMIN) p = requests.findAll(PageRequest.of(page, size, Sort.by("requestedDate").ascending()));
        else if (u.role == AppUser.Role.CUSTOMER) p = requests.findByCustomerId(u.id, PageRequest.of(page, size, Sort.by("requestedDate").ascending()));
        else p = requests.findByStatus(ServiceRequest.Status.OPEN, PageRequest.of(page, size, Sort.by("requestedDate").ascending()));
        return new Views.PageResult<>(p.map(this::requestView).getContent(), page, size, p.getTotalElements(), p.getTotalPages());
    }

    @Transactional
    public Views.RequestView createRequest(Authentication auth, Requests.ServiceRequestInput in) {
        AppUser customer = userService.current(auth);
        if (customer.role != AppUser.Role.CUSTOMER && customer.role != AppUser.Role.ADMIN)
            throw BusinessException.forbidden("Заявку может создавать только заказчик");
        validateInterval(in.requestedDate(), in.startTime(), in.endTime());
        ServiceRequest r = new ServiceRequest();
        r.customer = customer; r.category = in.category().trim(); r.description = in.description().trim();
        r.address = in.address().trim(); r.requestedDate = in.requestedDate(); r.startTime = in.startTime(); r.endTime = in.endTime();
        r.status = ServiceRequest.Status.OPEN;
        requests.save(r);
        audit.log(customer, "CREATE", "ServiceRequest", r.id, "Создана заявка на бытовую помощь");
        return requestView(r);
    }

    @Transactional
    public Views.RequestView updateRequest(Authentication auth, Long id, Requests.ServiceRequestInput in) {
        AppUser actor = userService.current(auth);
        ServiceRequest r = getRequest(id);
        ensureCustomerOrAdmin(actor, r);
        if (r.status != ServiceRequest.Status.OPEN) throw BusinessException.conflict("Изменять можно только открытую заявку");
        validateInterval(in.requestedDate(), in.startTime(), in.endTime());
        r.category = in.category().trim(); r.description = in.description().trim(); r.address = in.address().trim();
        r.requestedDate = in.requestedDate(); r.startTime = in.startTime(); r.endTime = in.endTime();
        audit.log(actor, "UPDATE", "ServiceRequest", r.id, "Изменена заявка");
        return requestView(r);
    }

    @Transactional
    public Views.RequestView cancelRequest(Authentication auth, Long id, Requests.CancelInput in) {
        AppUser actor = userService.current(auth);
        ServiceRequest r = getRequest(id);
        ensureCustomerOrAdmin(actor, r);
        if (r.status == ServiceRequest.Status.COMPLETED || r.status == ServiceRequest.Status.CANCELLED)
            throw BusinessException.conflict("Заявка уже завершена или отменена");
        if (LocalDate.now(clock).isAfter(r.requestedDate) ||
                (LocalDate.now(clock).isEqual(r.requestedDate) && !LocalTime.now(clock).isBefore(r.startTime)))
            throw BusinessException.conflict("После начала временного интервала заявку отменить нельзя");
        r.status = ServiceRequest.Status.CANCELLED;
        bookings.findByRequestId(r.id).stream()
                .filter(b -> b.status == Booking.Status.CONFIRMED || b.status == Booking.Status.IN_PROGRESS)
                .forEach(b -> b.status = Booking.Status.CANCELLED);
        audit.log(actor, "CANCEL", "ServiceRequest", r.id, in.reason());
        return requestView(r);
    }

    @Transactional
    public Views.BookingView createBooking(Authentication auth, Long requestId, Requests.BookingInput in) {
        AppUser customer = userService.current(auth);
        if (customer.role != AppUser.Role.CUSTOMER && customer.role != AppUser.Role.ADMIN)
            throw BusinessException.forbidden("Бронировать волонтёра может только заказчик");
        ServiceRequest r = getRequest(requestId);
        ensureCustomerOrAdmin(customer, r);
        if (r.status != ServiceRequest.Status.OPEN) throw BusinessException.conflict("Заявка не доступна для бронирования");

        // Lock the volunteer first. This serializes competing bookings for the same volunteer.
        VolunteerProfile v = volunteerService.locked(in.volunteerId());
        if (!MatchingPolicy.matchesRequest(v, r, null))
            throw BusinessException.conflict("Волонтёр не подходит по виду помощи, району или доступному времени");

        List<Booking> existing = bookings.findByVolunteerIdAndReservedDateAndStatusIn(v.id, r.requestedDate, ACTIVE);
        boolean overlap = existing.stream().anyMatch(b -> MatchingPolicy.overlaps(b.startTime, b.endTime, r.startTime, r.endTime));
        if (overlap) throw BusinessException.conflict("У волонтёра уже есть активное бронирование на пересекающийся интервал");

        Booking b = new Booking();
        b.request = r; b.volunteer = v; b.reservedDate = r.requestedDate; b.startTime = r.startTime; b.endTime = r.endTime;
        b.status = Booking.Status.CONFIRMED; b.createdAt = OffsetDateTime.now(clock);
        bookings.save(b);
        r.status = ServiceRequest.Status.BOOKED;
        audit.log(customer, "BOOK", "Booking", b.id, "Забронирован волонтёр " + v.user.username);
        return bookingView(b);
    }

    @Transactional(readOnly = true)
    public Views.PageResult<Views.BookingView> bookings(Authentication auth, int page, int size) {
        VolunteerService.validatePage(page, size);
        AppUser u = userService.current(auth);
        List<Booking> list;
        if (u.role == AppUser.Role.ADMIN) list = bookings.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        else if (u.role == AppUser.Role.CUSTOMER) {
            list = bookings.findAll().stream().filter(b -> b.request.customer.id.equals(u.id)).sorted(Comparator.comparing(b -> b.createdAt, Comparator.reverseOrder())).toList();
        } else {
            VolunteerProfile v = volunteers.findByUserId(u.id).orElseThrow(() -> BusinessException.notFound("Профиль волонтёра не найден"));
            list = bookings.findByVolunteerId(v.id).stream().sorted(Comparator.comparing(b -> b.createdAt, Comparator.reverseOrder())).toList();
        }
        int from = Math.min(page * size, list.size());
        int to = Math.min(from + size, list.size());
        List<Views.BookingView> content = list.subList(from, to).stream().map(this::bookingView).toList();
        int totalPages = (int) Math.ceil(list.size() / (double) size);
        return new Views.PageResult<>(content, page, size, list.size(), totalPages);
    }

    @Transactional
    public Views.BookingView start(Authentication auth, Long id) {
        AppUser actor = userService.current(auth);
        Booking b = getBooking(id);
        ensureParticipant(actor, b);
        if (b.status != Booking.Status.CONFIRMED) throw BusinessException.conflict("Начать можно только подтверждённое бронирование");
        b.status = Booking.Status.IN_PROGRESS;
        audit.log(actor, "START", "Booking", id, "Поручение начато");
        return bookingView(b);
    }

    @Transactional
    public Views.BookingView complete(Authentication auth, Long id, Requests.CompleteInput in) {
        AppUser actor = userService.current(auth);
        Booking b = getBooking(id);
        if (actor.role != AppUser.Role.ADMIN && !(actor.role == AppUser.Role.VOLUNTEER && b.volunteer.user.id.equals(actor.id)))
            throw BusinessException.forbidden("Завершить поручение может назначенный волонтёр");
        if (b.status != Booking.Status.IN_PROGRESS) throw BusinessException.conflict("Завершить можно только начатое поручение");
        b.status = Booking.Status.COMPLETED;
        b.completedAt = OffsetDateTime.now(clock);
        b.report = in.report();
        b.request.status = ServiceRequest.Status.COMPLETED;
        audit.log(actor, "COMPLETE", "Booking", id, "Поручение выполнено");
        return bookingView(b);
    }

    @Transactional
    public Views.BookingView cancelBooking(Authentication auth, Long id, Requests.CancelInput in) {
        AppUser actor = userService.current(auth);
        Booking b = getBooking(id);
        if (actor.role != AppUser.Role.ADMIN && !(actor.role == AppUser.Role.CUSTOMER && b.request.customer.id.equals(actor.id)))
            throw BusinessException.forbidden("Отменить бронирование может заказчик заявки");
        if (b.status != Booking.Status.CONFIRMED) throw BusinessException.conflict("Отменить можно только подтверждённое бронирование");
        if (LocalDate.now(clock).isAfter(b.reservedDate) ||
                (LocalDate.now(clock).isEqual(b.reservedDate) && !LocalTime.now(clock).isBefore(b.startTime)))
            throw BusinessException.conflict("После начала интервала бронирование отменить нельзя");
        b.status = Booking.Status.CANCELLED;
        b.request.status = ServiceRequest.Status.OPEN;
        audit.log(actor, "CANCEL", "Booking", id, in.reason());
        return bookingView(b);
    }

    @Transactional
    public Views.ReviewView review(Authentication auth, Long id, Requests.ReviewInput in) {
        AppUser actor = userService.current(auth);
        Booking b = getBooking(id);
        if (actor.role != AppUser.Role.CUSTOMER || !b.request.customer.id.equals(actor.id))
            throw BusinessException.forbidden("Отзыв может оставить только заказчик своей заявки");
        if (b.status != Booking.Status.COMPLETED) throw BusinessException.conflict("Отзыв доступен только после выполнения");
        if (reviews.findByBookingId(id).isPresent()) throw BusinessException.conflict("Для бронирования уже оставлен отзыв");
        Review r = new Review(); r.booking = b; r.rating = in.rating(); r.comment = in.comment(); r.createdAt = OffsetDateTime.now(clock);
        reviews.save(r);
        volunteerService.recalculateRating(b.volunteer);
        audit.log(actor, "REVIEW", "Review", r.id, "Оценка волонтёра: " + in.rating());
        return new Views.ReviewView(r.id, id, r.rating, r.comment, r.createdAt);
    }

    @Transactional(readOnly = true)
    public ServiceRequest getRequest(Long id) { return requests.findById(id).orElseThrow(() -> BusinessException.notFound("Заявка не найдена")); }
    @Transactional(readOnly = true)
    public Booking getBooking(Long id) { return bookings.findById(id).orElseThrow(() -> BusinessException.notFound("Бронирование не найдено")); }

    private void ensureCustomerOrAdmin(AppUser actor, ServiceRequest r) {
        if (actor.role != AppUser.Role.ADMIN && !r.customer.id.equals(actor.id)) throw BusinessException.forbidden("Заявка принадлежит другому заказчику");
    }
    private void ensureParticipant(AppUser actor, Booking b) {
        boolean ok = actor.role == AppUser.Role.ADMIN ||
                (actor.role == AppUser.Role.VOLUNTEER && b.volunteer.user.id.equals(actor.id)) ||
                (actor.role == AppUser.Role.CUSTOMER && b.request.customer.id.equals(actor.id));
        if (!ok) throw BusinessException.forbidden("Бронирование недоступно текущему пользователю");
    }
    private void validateInterval(LocalDate date, LocalTime start, LocalTime end) {
        if (date.isBefore(LocalDate.now(clock))) throw BusinessException.bad("Дата поручения не может быть в прошлом");
        if (!start.isBefore(end)) throw BusinessException.bad("Начало интервала должно быть раньше окончания");
    }
    private Views.RequestView requestView(ServiceRequest r) {
        return new Views.RequestView(r.id, r.customer.id, r.customer.fullName, r.category, r.description, r.address,
                r.requestedDate, r.startTime, r.endTime, r.status);
    }
    private Views.BookingView bookingView(Booking b) {
        return new Views.BookingView(b.id, b.request.id, b.volunteer.id, b.volunteer.user.fullName,
                b.request.category, b.request.address, b.reservedDate, b.startTime, b.endTime, b.status,
                b.createdAt, b.completedAt, b.report);
    }
}
