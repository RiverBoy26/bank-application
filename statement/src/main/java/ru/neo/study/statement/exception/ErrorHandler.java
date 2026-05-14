package ru.neo.study.statement.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.neo.study.statement.dto.ErrorResponseDto;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponseDto> handleApiException(ApiException ex) {
        log.warn("API error [{}]: {}", ex.getCode(), ex.getMessage(), ex);

        return ResponseEntity
                .status(ex.getStatus())
                .body(new ErrorResponseDto(
                        LocalDateTime.now(),
                        ex.getStatus().value(),
                        ex.getCode(),
                        ex.getMessage()
                ));
    }
}