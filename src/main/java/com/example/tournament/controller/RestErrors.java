package com.example.tournament.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class RestErrors {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> business(ResponseStatusException e) {
        return error(e.getStatusCode().value(), e.getReason() == null ? "Запрос отклонён" : e.getReason());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> denied() {
        return error(403, "Недостаточно прав");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict() {
        return error(409, "Запись уже существует или связана с другими данными");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> invalid() {
        return error(400, "Неверный формат запроса");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected() {
        return error(500, "Ошибка сервера");
    }

    private ResponseEntity<?> error(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("status", status, "message", message));
    }
}
