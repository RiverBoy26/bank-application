package ru.neo.study.dealapi.entity;

import ru.neo.study.dealapi.enums.EmploymentStatus;
import ru.neo.study.dealapi.enums.Position;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employment {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private EmploymentStatus status;
    private String employerInn;
    private BigDecimal salary;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private Position position;
    private Integer workExperienceTotal;
    private Integer workExperienceCurrent;
}
