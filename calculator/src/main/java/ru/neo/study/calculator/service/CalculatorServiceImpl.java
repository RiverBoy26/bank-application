package ru.neo.study.calculator.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.neo.study.calculator.dto.*;
import ru.neo.study.calculator.exceptions.CancelCreditException;
import ru.neo.study.calculator.exceptions.ValidationException;
import ru.neo.study.calculator.mapper.LoanStatementRequestMapper;
import ru.neo.study.calculator.mapper.ScoringDataMapper;
import ru.neo.study.calculator.model.*;
import ru.neo.study.calculator.repository.CalculatorStorage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

@Service
@AllArgsConstructor
public class CalculatorServiceImpl implements CalculatorService {
    private CalculatorStorage calculatorStorage;
    private static final int MAX_COUNT_SALARY = 24;
    private static final List<Integer> RANGE_AGE_FEMALE = List.of(32, 60);
    private static final List<Integer> RANGE_AGE_MALE = List.of(30, 55);
    private static final int MIN_TOTAL_WORK_MONTH = 18;
    private static final int MIN_CURRENT_WORK_MONTH = 3;
    private static final Pattern FULL_NAME_PATTERN = Pattern.compile("^[A-Za-z]{2,30}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-z0-9A-Z_!#$%&'*+/=?`{|}~^.-]+@[a-z0-9A-Z.-]+$");
    private static final BigDecimal MIN_AMOUNT = BigDecimal.valueOf(20000);
    private static final Integer MIN_TERM = 6;
    private static final Pattern PASS_SERIES_PATTERN = Pattern.compile("^\\d{4}$");
    private static final Pattern PASS_NUMBER_PATTERN = Pattern.compile("^\\d{6}$");


    public List<LoanOfferDto> offers(LoanStatementRequestDto loanStatementRequest) {
        return calculatorStorage.offers(LoanStatementRequestMapper.toLoanStatementRequest(loanStatementRequest));
    }

    public CreditDto calc(ScoringDataDto scoringData) {
        BigDecimal rateAfterScoring = scoring(scoringData);
        return calculatorStorage.calc(ScoringDataMapper.toScoringData(scoringData), rateAfterScoring);
    }

    private BigDecimal scoring(ScoringDataDto scoringDataDto) {
        BigDecimal resultRate = BigDecimal.valueOf(0);
        EmploymentDto employmentDto = scoringDataDto.getEmployment();
        int age = LocalDate.now().getYear() - scoringDataDto.getBirthdate().getYear();

        if (scoringDataDto.getEmployment().getEmploymentStatus() == EmploymentStatus.UNEMPLOYED) {
            throw new CancelCreditException("Отказано в кредите в связи со статусом работы: Безработный");
        } else if (employmentDto.getEmploymentStatus() == EmploymentStatus.SELF_EMPLOYED) {
            resultRate = resultRate.add(BigDecimal.TWO);
        } else if (employmentDto.getEmploymentStatus() == EmploymentStatus.BUSINESS_OWNER) {
            resultRate = resultRate.add(BigDecimal.ONE);
        }

        if (employmentDto.getPosition() == Position.MIDDLE_MANAGEMENT) {
            resultRate = resultRate.subtract(BigDecimal.TWO);
        } else if (employmentDto.getPosition() == Position.TOP_MANAGEMENT) {
            resultRate = resultRate.subtract(BigDecimal.valueOf(3));
        }

        if (scoringDataDto.getAmount().compareTo(employmentDto.getSalary().multiply(BigDecimal.valueOf(MAX_COUNT_SALARY))) > 0) {
            throw new CancelCreditException("Отказано в кредите: Сумма займа больше, чем 24 зарплаты");
        }

        if (scoringDataDto.getMaritalStatus() == MaritalStatus.MARRIED) {
            resultRate = resultRate.subtract(BigDecimal.valueOf(3));
        } else if (scoringDataDto.getMaritalStatus() == MaritalStatus.DIVORCED) {
            resultRate = resultRate.add(BigDecimal.ONE);
        }

        if (age < 20 || age > 65) {
            throw new CancelCreditException("Отказано в кредите: Возраст должен быть более 20 и менее 65 лет");
        }

        if (scoringDataDto.getGender() == Gender.FEMALE && age >= RANGE_AGE_FEMALE.getFirst() && age <= RANGE_AGE_FEMALE.getLast() ||
                scoringDataDto.getGender() == Gender.MALE && age >= RANGE_AGE_MALE.getFirst() && age <= RANGE_AGE_MALE.getLast()) {
            resultRate = resultRate.subtract(BigDecimal.valueOf(3));
        } else if (scoringDataDto.getGender() == Gender.NON_BINARY) {
            resultRate = resultRate.add(BigDecimal.valueOf(7));
        }

        if (employmentDto.getWorkExperienceTotal() < MIN_TOTAL_WORK_MONTH ||
                employmentDto.getWorkExperienceCurrent() < MIN_CURRENT_WORK_MONTH) {
            throw new CancelCreditException("Отказано в кредите: Несоответствующий стаж работы");
        }

        return resultRate;
    }

    private void prescoring(LoanStatementRequestDto loanStatementRequestDto) {
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

        if (lastName == null || !FULL_NAME_PATTERN.matcher(lastName).matches()) {
            throw new ValidationException("Фамилия должна содержать от 2 до 30 латинских букв!");
        }

        if (!FULL_NAME_PATTERN.matcher(middleName).matches()) {
            throw new ValidationException("Отчество (при наличии) должно содержать от 2 до 30 латинских букв!");
        }

        if (amount == null || amount.compareTo(MIN_AMOUNT) < 0) {
            throw new ValidationException("Сумма кредита должно быть больше или равно 20000");
        }

        if (term == null || term < MIN_TERM) {
            throw new ValidationException("Срок кредита должен быть больше или равно 6");
        }

        if (birthdate == null || birthdate.isAfter(LocalDate.now().minusYears(18))) {
            throw new ValidationException("Клиент должен быть совершеннолетним!");
        }

        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Формат почты некорректен!");
        }

        if (passportSeries == null || !PASS_SERIES_PATTERN.matcher(passportSeries).matches()) {
            throw new ValidationException("Серия паспорта должна состоять из 4 цифр!");
        }

        if (passportNumber == null || !PASS_NUMBER_PATTERN.matcher(passportNumber).matches()) {
            throw new ValidationException("Номер паспорта должна состоять из 6 цифр!");
        }
    }

}
