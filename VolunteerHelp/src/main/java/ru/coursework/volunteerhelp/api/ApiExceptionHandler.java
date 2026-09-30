package ru.coursework.volunteerhelp.api;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.coursework.volunteerhelp.service.BusinessException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setTitle(switch (status) {
            case BAD_REQUEST -> "Некорректные данные";
            case NOT_FOUND -> "Не найдено";
            case FORBIDDEN -> "Недостаточно прав";
            case UNAUTHORIZED -> "Требуется авторизация";
            default -> "Конфликт состояния";
        });
        return p;
    }

    @ExceptionHandler(BusinessException.class)
    ProblemDetail business(BusinessException e) { return problem(e.status, e.getMessage()); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException e) {
        ProblemDetail p = problem(HttpStatus.BAD_REQUEST, "Проверьте обязательные поля, длину текста, даты и время");
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        p.setProperty("errors", fields);
        return p;
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    ProblemDetail forbidden() { return problem(HttpStatus.FORBIDDEN, "Операция недоступна вашей роли или объект принадлежит другому пользователю"); }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    ProblemDetail unauthorized() { return problem(HttpStatus.UNAUTHORIZED, "Неверный логин или пароль"); }
}
