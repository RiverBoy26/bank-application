package ru.neo.study.calculator.service.validations;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import ru.neo.study.calculator.dto.LoanStatementRequestDto;
import ru.neo.study.calculator.exceptions.ValidationException;
import ru.neo.study.calculator.service.CalculatorService;
import ru.neo.study.calculator.service.CalculatorServiceImpl;
import ru.neo.study.calculator.service.metrics.parameters.MetricsParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Data
public class PrescoringService {
    static final Logger logger = LoggerFactory.getLogger(PrescoringService.class);

    private static final Pattern FULL_NAME_PATTERN = Pattern.compile("^[A-Za-z]{2,30}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-z0-9A-Z_!#$%&'*+/=?`{|}~^.-]+@[a-z0-9A-Z.-]+$");
    private static final Pattern PASS_SERIES_PATTERN = Pattern.compile("^\\d{4}$");
    private static final Pattern PASS_NUMBER_PATTERN = Pattern.compile("^\\d{6}$");

    private final MetricsParam metricsParam;

    public void prescoring(LoanStatementRequestDto loanStatementRequestDto) {
        logger.debug("Старт прескоринга: {}", loanStatementRequestDto);

        String firstName = loanStatementRequestDto.getFirstName();
        String lastName = loanStatementRequestDto.getLastName();
        String middleName = loanStatementRequestDto.getMiddleName();
        BigDecimal amount = loanStatementRequestDto.getAmount();
        Integer term = loanStatementRequestDto.getTerm();
        LocalDate birthdate = loanStatementRequestDto.getBirthdate();
        String email = loanStatementRequestDto.getEmail();
        String passportSeries = loanStatementRequestDto.getPassportSeries();
        String passportNumber = loanStatementRequestDto.getPassportNumber();

        if (firstName == null || !FULL_NAME_PATTERN.matcher(firstName).matches()) {
            throw new ValidationException("Имя должно содержать от 2 до 30 латинских букв!");
        }

        logger.debug("Имя прошло прескоринг!");

        if (lastName == null || !FULL_NAME_PATTERN.matcher(lastName).matches()) {
            throw new ValidationException("Фамилия должна содержать от 2 до 30 латинских букв!");
        }

        logger.debug("Фамилия прошла прескоринг!");

        if (middleName != null && !middleName.isBlank() && !FULL_NAME_PATTERN.matcher(middleName).matches()) {
            throw new ValidationException("Отчество (при наличии) должно содержать от 2 до 30 латинских букв!");
        }

        logger.debug("Отчество прошло прескоринг!");

        if (amount == null || amount.compareTo(metricsParam.getMinAmount()) < 0) {
            throw new ValidationException("Сумма кредита должно быть больше или равно 20000");
        }

        logger.debug("Сумма прошла прескоринг!");

        if (term == null || term < metricsParam.getMinTerm()) {
            throw new ValidationException("Срок кредита должен быть больше или равно 6");
        }

        logger.debug("Срок прошёл прескоринг!");

        if (birthdate == null || birthdate.isAfter(LocalDate.now().minusYears(18))) {
            throw new ValidationException("Клиент должен быть совершеннолетним!");
        }

        logger.debug("Дата рождения прошла прескоринг!");

        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Формат почты некорректен!");
        }

        logger.debug("Email прошёл прескоринг!");

        if (passportSeries == null || !PASS_SERIES_PATTERN.matcher(passportSeries).matches()) {
            throw new ValidationException("Серия паспорта должна состоять из 4 цифр!");
        }

        logger.debug("Серия паспорта прошла прескоринг!");

        if (passportNumber == null || !PASS_NUMBER_PATTERN.matcher(passportNumber).matches()) {
            throw new ValidationException("Номер паспорта должна состоять из 6 цифр!");
        }

        logger.debug("Номер паспорта прошёл прескоринг!");

        logger.debug("Прескоринг успешно завершён!");
    }
}
