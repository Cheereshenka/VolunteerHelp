package ru.coursework.volunteerhelp.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.coursework.volunteerhelp.domain.*;
import ru.coursework.volunteerhelp.repository.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Configuration
public class DemoData {
    @Bean
    CommandLineRunner seed(AppUserRepository users, VolunteerProfileRepository volunteers,
                           ServiceRequestRepository requests, PasswordEncoder encoder) {
        return args -> {
            AppUser admin = users.findByUsername("admin").orElseGet(() -> users.save(new AppUser("admin", encoder.encode("admin"), "Администратор", AppUser.Role.ADMIN)));
            AppUser customer = users.findByUsername("customer").orElseGet(() -> users.save(new AppUser("customer", encoder.encode("customer"), "Иван Заказчик", AppUser.Role.CUSTOMER)));
            AppUser volunteer = users.findByUsername("volunteer").orElseGet(() -> users.save(new AppUser("volunteer", encoder.encode("volunteer"), "Роман Волонтёр", AppUser.Role.VOLUNTEER)));

            // Демо-профиль намеренно называется Роман. Это также обновляет старую запись «Анна» при повторном запуске.
            volunteer.fullName = "Роман Волонтёр";
            users.save(volunteer);

            VolunteerProfile p = volunteers.findByUserId(volunteer.id).orElseGet(() -> {
                VolunteerProfile profile = new VolunteerProfile();
                profile.user = volunteer;
                return profile;
            });
            p.skills = "покупки,уборка,доставка";
            p.district = "Центральный район";
            p.about = "Помогаю с покупками и бытовыми поручениями.";
            p.availableFrom = LocalTime.of(9,0);
            p.availableTo = LocalTime.of(19,0);
            p.active = true;
            volunteers.save(p);

            if (requests.count() == 0) {
                ServiceRequest r = new ServiceRequest();
                r.customer = customer; r.category = "покупки"; r.description = "Купить продукты по списку в ближайшем магазине.";
                r.address = "Центральный район, ул. Примерная, 10"; r.requestedDate = LocalDate.now().plusDays(7);
                r.startTime = LocalTime.of(15,0); r.endTime = LocalTime.of(17,0); r.status = ServiceRequest.Status.OPEN;
                requests.save(r);
            }
        };
    }
}
