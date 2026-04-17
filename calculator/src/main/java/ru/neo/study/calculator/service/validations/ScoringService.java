package ru.neo.study.calculator.service.validations;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.neo.study.calculator.dto.EmploymentDto;
import ru.neo.study.calculator.dto.ScoringDataDto;
import ru.neo.study.calculator.enums.EmploymentStatus;
import ru.neo.study.calculator.enums.Gender;
import ru.neo.study.calculator.enums.MaritalStatus;
import ru.neo.study.calculator.enums.Position;
import ru.neo.study.calculator.exceptions.CancelCreditException;
import ru.neo.study.calculator.service.metrics.parameters.MetricsParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

@Service
@RequiredArgsConstructor
@Data
public class ScoringService {
    static final Logger logger = LoggerFactory.getLogger(ScoringService.class);

    private final MetricsParam metricsParam;

    public BigDecimal scoring(ScoringDataDto scoringDataDto) {
        logger.debug("Старт скоринга: {}", scoringDataDto);

        BigDecimal resultRate = BigDecimal.valueOf(0);
        EmploymentDto employmentDto = scoringDataDto.getEmployment();
        int age = Period.between(scoringDataDto.getBirthdate(), LocalDate.now()).getYears();

        if (scoringDataDto.getEmployment().getEmploymentStatus() == EmploymentStatus.UNEMPLOYED) {
            throw new CancelCreditException("Отказано в кредите в связи со статусом работы: Безработный");
        } else if (employmentDto.getEmploymentStatus() == EmploymentStatus.SELF_EMPLOYED) {
            resultRate = resultRate.add(BigDecimal.TWO);
            logger.debug("Самозанятый: +2 -> {}", resultRate);
        } else if (employmentDto.getEmploymentStatus() == EmploymentStatus.BUSINESS_OWNER) {
            resultRate = resultRate.add(BigDecimal.ONE);
            logger.debug("Владелец бизнеса: +1 -> {}", resultRate);
        }

        if (employmentDto.getPosition() == Position.MID_MANAGEMENT) {
            resultRate = resultRate.subtract(BigDecimal.TWO);
            logger.debug("Middle management: -2 -> {}", resultRate);
        } else if (employmentDto.getPosition() == Position.TOP_MANAGEMENT) {
            resultRate = resultRate.subtract(BigDecimal.valueOf(3));
            logger.debug("Top management: -3 -> {}", resultRate);
        }

        if (scoringDataDto.getAmount().compareTo(employmentDto.getSalary().multiply(BigDecimal.valueOf(metricsParam.getMaxCountSalary()))) > 0) {
            throw new CancelCreditException("Отказано в кредите: Сумма займа больше, чем 24 зарплаты");
        }

        if (scoringDataDto.getMaritalStatus() == MaritalStatus.MARRIED) {
            resultRate = resultRate.subtract(BigDecimal.valueOf(3));
            logger.debug("Женат/замужем: -3 -> {}", resultRate);
        } else if (scoringDataDto.getMaritalStatus() == MaritalStatus.DIVORCED) {
            resultRate = resultRate.add(BigDecimal.ONE);
            logger.debug("Разведён(-а): +1 -> {}", resultRate);
        }

        if (age < 20 || age > 65) {
            throw new CancelCreditException("Отказано в кредите: Возраст должен быть более 20 и менее 65 лет");
        }

        if (scoringDataDto.getGender() == Gender.FEMALE && age >= metricsParam.getRangeAgeFemale().getFirst() &&
                age <= metricsParam.getRangeAgeFemale().getLast() ||
                scoringDataDto.getGender() == Gender.MALE && age >= metricsParam.getRangeAgeMale().getFirst() &&
                        age <= metricsParam.getRangeAgeMale().getLast()) {
            resultRate = resultRate.subtract(BigDecimal.valueOf(3));
            logger.debug("Скидка по возрасту и году: -3 -> {}", resultRate);
        } else if (scoringDataDto.getGender() == Gender.NON_BINARY) {
            resultRate = resultRate.add(BigDecimal.valueOf(7));
            logger.debug("NON_BINARY: +7 -> {}", resultRate);
        }

        if (employmentDto.getWorkExperienceTotal() < metricsParam.getMinTotalWorkMonth() ||
                employmentDto.getWorkExperienceCurrent() < metricsParam.getMinCurrentWorkMonth()) {
            throw new CancelCreditException("Отказано в кредите: Несоответствующий стаж работы");
        }

        logger.debug("Итог скоринга={}", resultRate);

        return resultRate;
    }
}
