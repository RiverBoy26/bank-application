package ru.neo.study.statement.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    @ExceptionHandler(OfferSelectionConflictException.class)
    public ResponseEntity<Map<String, Object>> handleSelectionConflictException(OfferSelectionConflictException ex) {
        log.error("Ошибка при обращении к микросервису deal", ex);

        HttpStatus status = HttpStatus.CONFLICT;

        return ResponseEntity
                .status(status)
                .body(createResponse(
                        status,
                        "Конфликт при обновлении заявки",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(OffersNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOffersNotFoundException(OffersNotFoundException ex) {
        log.warn("Кредитные предложения не найдены", ex);

        HttpStatus status = HttpStatus.NOT_FOUND;

        return ResponseEntity
                .status(status)
                .body(createResponse(
                        status,
                        "Кредитные предложения не найдены",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException ex) {
        log.error("Ошибка валидации", ex);

        HttpStatus status = HttpStatus.BAD_REQUEST;

        return ResponseEntity
                .status(status)
                .body(createResponse(
                        status,
                        "Ошибка валидации",
                        ex.getMessage()
                ));
    }

    private Map<String, Object> createResponse(HttpStatus status, String error, String description) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", status.value());
        response.put("error", error);
        response.put("description", description);
        return response;
    }
}