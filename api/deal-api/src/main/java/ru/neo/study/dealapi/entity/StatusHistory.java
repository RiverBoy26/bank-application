package ru.neo.study.dealapi.entity;

import enums.ApplicationStatus;
import enums.ChangeType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusHistory {
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private ApplicationStatus status;

    private LocalDateTime time;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private ChangeType changeType;
}
