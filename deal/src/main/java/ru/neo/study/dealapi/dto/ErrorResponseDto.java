package ru.neo.study.dealapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@AllArgsConstructor
@Data
public class ErrorResponseDto {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String description;
}

