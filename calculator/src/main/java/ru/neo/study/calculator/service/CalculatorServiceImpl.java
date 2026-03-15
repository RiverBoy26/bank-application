package ru.neo.study.calculator.service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import ru.neo.study.calculator.dto.*;
import ru.neo.study.calculator.enums.EmploymentStatus;
import ru.neo.study.calculator.enums.Gender;
import ru.neo.study.calculator.enums.MaritalStatus;
import ru.neo.study.calculator.enums.Position;
import ru.neo.study.calculator.exceptions.CancelCreditException;
import ru.neo.study.calculator.exceptions.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.time.Period;

@Service
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "credit-properties")
@Setter
public class CalculatorServiceImpl implements CalculatorService {
    static final Logger logger = LoggerFactory.getLogger(CalculatorServiceImpl.class);

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

    private static final BigDecimal SALARY_CLIENT_DISCOUNT = new BigDecimal("1.0");
    private static final BigDecimal INSURANCE_DISCOUNT = new BigDecimal("0.5");
    private static final BigDecimal INSURANCE_RATE = new BigDecimal("0.1");
    private static final BigDecimal LARGE_AMOUNT_THRESHOLD = new BigDecimal("1000000");
    private static final Integer LONG_TERM_THRESHOLD = 60;
    private static final int SCALE = 10;

    private Integer loanRate;

    public List<LoanOfferDto> offers(LoanStatementRequestDto loanStatementRequest) {
        logger.info("Получен запрос на предложения: {}", loanStatementRequest);

        prescoring(loanStatementRequest);
        UUID statementId = UUID.randomUUID();

        logger.debug("Создан statementId={} для набора предложений", statementId);

        List<LoanOfferDto> result = new ArrayList<>(4);

        result.add(createOffer(statementId, loanStatementRequest, false, false));
        result.add(createOffer(statementId, loanStatementRequest, true, false));
        result.add(createOffer(statementId, loanStatementRequest, false, true));
        result.add(createOffer(statementId, loanStatementRequest, true, true));

        List<LoanOfferDto> sortedResult = result.stream()
                .sorted((o1, o2) -> o2.getRate().compareTo(o1.getRate()))
                .toList();
        logger.info("Сформированы 4 кредитных предложения: {}", sortedResult);

        return sortedResult;
    }

    private LoanOfferDto createOffer(UUID statementId,
                                     LoanStatementRequestDto request,
                                     boolean isInsuranceEnabled,
                                     boolean isSalaryClient) {
        logger.debug("Расчёт предложения при параметрах: statementId={}, insurance={}, salary={}",
                statementId, isInsuranceEnabled, isSalaryClient);

        BigDecimal requestedAmount = request.getAmount();
        BigDecimal rate = calculateRate(
                requestedAmount,
                request.getTerm(),
                isInsuranceEnabled,
                isSalaryClient
        );
        BigDecimal totalAmount = calculateTotalAmount(requestedAmount, isInsuranceEnabled);
        BigDecimal monthlyPayment = calculateMonthlyPayment(totalAmount, rate, request.getTerm());

        LoanOfferDto newLoanOffer = new LoanOfferDto(
                statementId,
                requestedAmount,
                totalAmount,
                request.getTerm(),
                monthlyPayment,
                rate,
                isInsuranceEnabled,
                isSalaryClient
        );

        logger.debug("Создано предложение: {}", newLoanOffer);

        return newLoanOffer;
    }

    public CreditDto calc(ScoringDataDto scoringData) {
        logger.info("Получен запрос на расчёт: {}", scoringData);

        BigDecimal rateBeforeScoring = scoring(scoringData);

        logger.debug("Результат скоринга: {}", rateBeforeScoring);

        BigDecimal rate = BigDecimal.valueOf(loanRate)
                .add(rateBeforeScoring);

        logger.debug("Ставка после скоринга: {}", rate);

        if (Boolean.TRUE.equals(scoringData.getIsInsuranceEnabled())) {
            rate = rate.subtract(INSURANCE_DISCOUNT);
            logger.debug("Применена скидка за страховку {} -> ставка {}", INSURANCE_DISCOUNT, rate);
        }

        if (Boolean.TRUE.equals(scoringData.getIsSalaryClient())) {
            rate = rate.subtract(SALARY_CLIENT_DISCOUNT);
            logger.debug("Применена скидка зарплатного клиента {} -> ставка {}", SALARY_CLIENT_DISCOUNT, rate);
        }

        BigDecimal amount = scoringData.getAmount();
        Integer term = scoringData.getTerm();
        BigDecimal totalAmount = calculateTotalAmount(amount, scoringData.getIsInsuranceEnabled());
        BigDecimal monthlyPayment = calculateMonthlyPayment(totalAmount, rate, term);
        List<PaymentScheduleElementDto> paymentSchedule = buildPaymentSchedule(totalAmount, rate, term);
        BigDecimal psk = calculatePsk(amount, paymentSchedule);

        CreditDto credit = new CreditDto(
                totalAmount,
                term,
                monthlyPayment,
                rate,
                psk,
                scoringData.getIsInsuranceEnabled(),
                scoringData.getIsSalaryClient(),
                paymentSchedule
        );

        logger.info("Результат расчёта кредита: {}", credit);

        return credit;
    }

    private BigDecimal calculateRate(BigDecimal amount,
                                     Integer term,
                                     Boolean isInsuranceEnabled,
                                     Boolean isSalaryClient) {
        BigDecimal rate = BigDecimal.valueOf(loanRate);

        logger.debug("Старт расчёта ставки: baseRate={}", rate);

        if (amount.compareTo(LARGE_AMOUNT_THRESHOLD) > 0) {
            rate = rate.add(BigDecimal.ONE);
            logger.debug("Сумма кредита {} > {}, ставка увеличена до {}", amount, LARGE_AMOUNT_THRESHOLD, rate);
        }

        if (term != null && term > LONG_TERM_THRESHOLD) {
            rate = rate.add(BigDecimal.ONE);
            logger.debug("Срок кредита {} > {}, ставка увеличена до {}", amount, LONG_TERM_THRESHOLD, rate);
        }

        if (Boolean.TRUE.equals(isInsuranceEnabled)) {
            rate = rate.subtract(INSURANCE_DISCOUNT);
            logger.debug("Применена скидка за страховку {}, ставка {}", INSURANCE_DISCOUNT, rate);
        }

        if (Boolean.TRUE.equals(isSalaryClient)) {
            rate = rate.subtract(SALARY_CLIENT_DISCOUNT);
            logger.debug("Применена скидка зарплатного клиента {}, ставка {}", SALARY_CLIENT_DISCOUNT, rate);
        }

        logger.debug("Итоговая ставка предложения: {}", rate);
        return rate;
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal totalAmount, BigDecimal annualRate, Integer term) {
        logger.debug("Расчёт ежемесячного платежа: totalAmount={}, annualRate={}, term={}", totalAmount, annualRate, term);

        if (term == null || term <= 0) {
            throw new IllegalArgumentException("Срок кредита должен быть положительным!");
        }

        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal payment = totalAmount.divide(BigDecimal.valueOf(term), 2, RoundingMode.HALF_UP);

            logger.debug("Ставка 0, monthlyPayment={}", payment);

            return payment;
        }

        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), SCALE, RoundingMode.HALF_UP);

        double r = monthlyRate.doubleValue();
        double n = term.doubleValue();
        double payment = totalAmount.doubleValue() * r / (1 - Math.pow(1 + r, -n));

        BigDecimal result = BigDecimal.valueOf(payment).setScale(2, RoundingMode.HALF_UP);

        logger.debug("Рассчитан monthlyPayment={}, monthlyRate={}", result, monthlyRate);

        return result;
    }

    private BigDecimal calculateTotalAmount(BigDecimal requestedAmount, Boolean isInsuranceEnabled) {
        logger.debug("Расчёт totalAmount: requestedAmount={}, insuranceEnabled={}", requestedAmount, isInsuranceEnabled);

        if (!Boolean.TRUE.equals(isInsuranceEnabled)) {
            logger.debug("Страховка выключена, totalAmount={}", requestedAmount);
            return requestedAmount;
        }

        BigDecimal insuranceAmount = requestedAmount
                .multiply(INSURANCE_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal amount = requestedAmount.add(insuranceAmount);

        requestedAmount.add(insuranceAmount);

        return amount;
    }

    private List<PaymentScheduleElementDto> buildPaymentSchedule(BigDecimal totalAmount,
                                                                 BigDecimal annualRate,
                                                                 Integer term) {
        logger.debug("Начало построения графика: totalAmount={}, annualRate={}, term={}", totalAmount, annualRate, term);
        List<PaymentScheduleElementDto> paymentSchedule = new ArrayList<>();
        BigDecimal remainingDebt = totalAmount;

        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), SCALE, RoundingMode.HALF_UP);

        BigDecimal monthlyPayment = calculateMonthlyPayment(totalAmount, annualRate, term);

        for (int i = 1; i <= term; i++) {
            BigDecimal interestPayment = remainingDebt.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal debtPayment = monthlyPayment.subtract(interestPayment).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalPayment = monthlyPayment.setScale(2, RoundingMode.HALF_UP);

            if (i == term) {
                debtPayment = remainingDebt;
                totalPayment = interestPayment.add(debtPayment);
                remainingDebt = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            } else {
                remainingDebt = remainingDebt.subtract(debtPayment);
            }

            PaymentScheduleElementDto paymentElement = new PaymentScheduleElementDto(
                    i,
                    LocalDate.now().plusMonths(i),
                    totalPayment,
                    interestPayment,
                    debtPayment,
                    remainingDebt
            );

            paymentSchedule.add(paymentElement);
            logger.debug("Платёж #{}: {}", i, paymentElement);
        }

        logger.debug("График платежей построен, размер={}", paymentSchedule.size());

        return paymentSchedule;
    }

    private BigDecimal calculatePsk(BigDecimal issuedAmount,
                                    List<PaymentScheduleElementDto> paymentSchedule) {
        logger.debug("Старт расчёта ПСК: issuedAmount={}, scheduleSize={}", issuedAmount, paymentSchedule.size());

        List<BigDecimal> cashFlows = new ArrayList<>();
        cashFlows.add(issuedAmount.negate());

        for (PaymentScheduleElementDto element : paymentSchedule) {
            cashFlows.add(element.getTotalPayment());
        }

        double left = 0.0;
        double right = 1.0;

        while (npv(cashFlows, right) > 0) {
            right *= 2.0;
            if (right > 1000) {
                throw new IllegalStateException("Не удается вычислить PSK!");
            }
        }

        for (int i = 0; i < 200; i++) {
            double mid = (left + right) / 2.0;
            double npv = npv(cashFlows, mid);

            if (npv > 0) {
                left = mid;
            } else {
                right = mid;
            }
        }

        double monthlyIrr = (left + right) / 2.0;

        BigDecimal psk = BigDecimal.valueOf(monthlyIrr)
                .multiply(BigDecimal.valueOf(12))
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        logger.debug("Рассчитан ПСК={}", psk);

        return psk;
    }

    private double npv(List<BigDecimal> cashFlows, double periodRate) {
        double result = cashFlows.getFirst().doubleValue();

        for (int i = 1; i < cashFlows.size(); i++) {
            result += cashFlows.get(i).doubleValue() / Math.pow(1 + periodRate, i);
        }

        return result;
    }

    private BigDecimal scoring(ScoringDataDto scoringDataDto) {
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

        if (employmentDto.getPosition() == Position.MIDDLE_MANAGEMENT) {
            resultRate = resultRate.subtract(BigDecimal.TWO);
            logger.debug("Middle management: -2 -> {}", resultRate);
        } else if (employmentDto.getPosition() == Position.TOP_MANAGEMENT) {
            resultRate = resultRate.subtract(BigDecimal.valueOf(3));
            logger.debug("Top management: -3 -> {}", resultRate);
        }

        if (scoringDataDto.getAmount().compareTo(employmentDto.getSalary().multiply(BigDecimal.valueOf(MAX_COUNT_SALARY))) > 0) {
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

        if (scoringDataDto.getGender() == Gender.FEMALE && age >= RANGE_AGE_FEMALE.getFirst() && age <= RANGE_AGE_FEMALE.getLast() ||
                scoringDataDto.getGender() == Gender.MALE && age >= RANGE_AGE_MALE.getFirst() && age <= RANGE_AGE_MALE.getLast()) {
            resultRate = resultRate.subtract(BigDecimal.valueOf(3));
            logger.debug("Скидка по возрасту и году: -3 -> {}", resultRate);
        } else if (scoringDataDto.getGender() == Gender.NON_BINARY) {
            resultRate = resultRate.add(BigDecimal.valueOf(7));
            logger.debug("NON_BINARY: +7 -> {}", resultRate);
        }

        if (employmentDto.getWorkExperienceTotal() < MIN_TOTAL_WORK_MONTH ||
                employmentDto.getWorkExperienceCurrent() < MIN_CURRENT_WORK_MONTH) {
            throw new CancelCreditException("Отказано в кредите: Несоответствующий стаж работы");
        }

        logger.debug("Итог скоринга={}", resultRate);

        return resultRate;
    }

    private void prescoring(LoanStatementRequestDto loanStatementRequestDto) {
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

        if (amount == null || amount.compareTo(MIN_AMOUNT) < 0) {
            throw new ValidationException("Сумма кредита должно быть больше или равно 20000");
        }

        logger.debug("Сумма прошла прескоринг!");

        if (term == null || term < MIN_TERM) {
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
