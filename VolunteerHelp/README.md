# VolunteerHelp

Курсовой проект: **«Разработка REST API для сервиса поиска и бронирования волонтёров»**.

## Назначение

VolunteerHelp предоставляет REST API и небольшой веб-клиент для бытовой помощи: походы в магазин, уборка, доставка небольших покупок и другие поручения.

Основные возможности:
- роли CUSTOMER, VOLUNTEER и ADMIN;
- самостоятельная регистрация заказчиков и волонтёров;
- автоматическое создание профиля при регистрации волонтёра;
- профили волонтёров и доступные интервалы;
- поиск по виду помощи, району, времени и рейтингу;
- создание и отмена заявок;
- бронирование волонтёра;
- запрет пересекающихся активных бронирований;
- начало и завершение поручения;
- отзывы и пересчёт рейтинга;
- аудит изменений;
- JSON API, OpenAPI/Swagger UI;
- H2 в файловом режиме;
- серверная сессия JSESSIONID и CSRF-защита.

## Технологии

Java 17, Spring Boot 3.5.13, Spring Web, Spring Data JPA/Hibernate, Spring Security, Jakarta Bean Validation, H2, springdoc-openapi, JUnit 5 и MockMvc.

## Запуск

Требуется JDK 17 и Maven 3.9+.

```bash
mvn clean test
mvn spring-boot:run
```

После запуска:
- веб-клиент: http://localhost:8080/
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- H2 Console: http://localhost:8080/h2-console

Для H2 Console:
- JDBC URL: `jdbc:h2:file:./data/volunteerhelp`
- user: `sa`
- password: пусто

## Демо-учётные записи

| Логин | Пароль | Роль |
|---|---|---|
| admin | admin | ADMIN |
| customer | customer | CUSTOMER |
| volunteer | volunteer | VOLUNTEER |

Демо-профиль волонтёра имеет имя **Роман Волонтёр**. При повторном запуске существующий демонстрационный пользователь `volunteer` также приводится к этому имени.

## REST API

Основные ресурсы:
- `/api/auth/*` — CSRF, регистрация, вход, выход, текущий пользователь;
- `/api/volunteers` — поиск и профили волонтёров;
- `/api/requests` — заявки на помощь;
- `/api/requests/{id}/bookings` — бронирование;
- `/api/bookings` — жизненный цикл бронирования;
- `/api/bookings/{id}/review` — отзыв;
- `/api/audit` — аудит администратора.

Для POST/PUT/PATCH сначала получить CSRF-токен через `GET /api/auth/csrf`, затем передавать его в заголовке `X-XSRF-TOKEN` вместе с cookie `XSRF-TOKEN`.

## Структура

`api` — REST-контроллеры и DTO; `service` — бизнес-правила; `domain` — JPA-сущности; `repository` — Spring Data JPA; `config` — безопасность и демо-данные; `static` — учебный веб-клиент; `docs` — документация проекта.
