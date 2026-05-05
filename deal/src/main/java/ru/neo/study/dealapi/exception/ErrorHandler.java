package ru.neo.study.dealapi.exception;

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

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLockingFailureException(
            OptimisticLockingFailureException ex
    ) {
        HttpStatus status = HttpStatus.CONFLICT;
        log.warn("Конфликт версий при обновлении заявки", ex);

        return ResponseEntity
                .status(status)
                .body(createResponse(
                        status,
                        "Конфликт при обновлении заявки",
                        "Заявка была изменена другим запросом. Обновите данные и повторите попытку."
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