package ru.coursework.volunteerhelp.service;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    public final HttpStatus status;
    public BusinessException(HttpStatus status, String message) { super(message); this.status = status; }
    public static BusinessException bad(String m) { return new BusinessException(HttpStatus.BAD_REQUEST, m); }
    public static BusinessException notFound(String m) { return new BusinessException(HttpStatus.NOT_FOUND, m); }
    public static BusinessException conflict(String m) { return new BusinessException(HttpStatus.CONFLICT, m); }
    public static BusinessException forbidden(String m) { return new BusinessException(HttpStatus.FORBIDDEN, m); }
}
