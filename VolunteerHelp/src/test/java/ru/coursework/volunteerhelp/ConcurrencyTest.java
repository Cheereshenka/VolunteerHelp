package ru.coursework.volunteerhelp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import ru.coursework.volunteerhelp.api.Requests;
import ru.coursework.volunteerhelp.domain.*;
import ru.coursework.volunteerhelp.repository.*;
import ru.coursework.volunteerhelp.service.BookingService;
import ru.coursework.volunteerhelp.service.BusinessException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConcurrencyTest {
    @Autowired BookingService service;
    @Autowired AppUserRepository users;
    @Autowired VolunteerProfileRepository volunteers;
    @Autowired ServiceRequestRepository requests;
    @Autowired BookingRepository bookings;

    @Test void concurrentBookingLeavesAtMostOneActiveBooking() throws Exception {
        AppUser customer = users.findByUsername("customer").orElseThrow();
        VolunteerProfile volunteer = volunteers.findByUserId(users.findByUsername("volunteer").orElseThrow().id).orElseThrow();
        ServiceRequest r = new ServiceRequest();
        r.customer = customer; r.category = "покупки"; r.description = "Конкурентная проверка";
        r.address = "Центральный район, тест"; r.requestedDate = LocalDate.now().plusDays(20); r.startTime=LocalTime.of(13,0); r.endTime=LocalTime.of(14,0); r.status=ServiceRequest.Status.OPEN;
        ServiceRequest saved = requests.saveAndFlush(r);
        long requestId = saved.id;
        var auth = new TestingAuthenticationToken("customer", "", "ROLE_CUSTOMER"); auth.setAuthenticated(true);
        ExecutorService pool = Executors.newFixedThreadPool(2); CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> task = () -> { start.await(); try { service.createBooking(auth, requestId, new Requests.BookingInput(volunteer.id)); return true; } catch (BusinessException e) { return false; } };
        Future<Boolean> a=pool.submit(task), b=pool.submit(task); start.countDown(); a.get(); b.get(); pool.shutdown();
        long active = bookings.findByRequestId(requestId).stream()
                .filter(x -> x.status == Booking.Status.CONFIRMED || x.status == Booking.Status.IN_PROGRESS)
                .count();
        assertEquals(1, active);
    }
}
