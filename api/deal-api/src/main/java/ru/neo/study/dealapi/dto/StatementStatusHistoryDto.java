package ru.neo.study.dealapi.dto;

import ru.neo.study.dealapi.enums.ApplicationStatus;
import ru.neo.study.dealapi.enums.ChangeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatementStatusHistoryDto {
    private ApplicationStatus status;
    private LocalDateTime time;
    private ChangeType changeType;
}
