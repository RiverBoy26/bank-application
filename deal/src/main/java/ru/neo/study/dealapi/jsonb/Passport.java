package ru.neo.study.dealapi.jsonb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Passport {
    private UUID id;
    private String series;
    private String number;
    private String issueBranch;
    private LocalDate issueDate;
}


