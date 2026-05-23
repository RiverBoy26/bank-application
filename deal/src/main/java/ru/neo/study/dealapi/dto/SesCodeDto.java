package ru.neo.study.dealapi.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SesCodeDto {
    @NotNull
    private Integer sesCode;
}
