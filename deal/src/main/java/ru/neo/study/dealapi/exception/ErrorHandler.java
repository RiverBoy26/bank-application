package ru.neo.study.dealapi.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.neo.study.dealapi.dto.ErrorResponseDto;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponseDto> handleOptimisticLockingFailureException(
            OptimisticLockingFailureException ex
    ) {
        log.warn("Optimistic locking conflict", ex);

        HttpStatus status = HttpStatus.CONFLICT;

        return ResponseEntity
                .status(status)
                .body(new ErrorResponseDto(
                        LocalDateTime.now(),
                        status.value(),
                        "STATEMENT_UPDATE_CONFLICT",
                        "Заявка была изменена другим запросом. Обновите данные и повторите попытку."
                ));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleEntityNotFoundException(
            EntityNotFoundException ex
    ) {
        log.warn("Entity not found", ex);

        HttpStatus status = HttpStatus.NOT_FOUND;

        return ResponseEntity
                .status(status)
                .body(new ErrorResponseDto(
                        LocalDateTime.now(),
                        status.value(),
                        "ENTITY_NOT_FOUND",
                        "Запись не найдена"
                ));
    }
}