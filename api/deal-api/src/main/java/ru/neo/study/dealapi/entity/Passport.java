package ru.neo.study.dealapi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Passport {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;
    private String series;
    private String number;
    private String issueBranch;
    private LocalDate issueDate;
}


