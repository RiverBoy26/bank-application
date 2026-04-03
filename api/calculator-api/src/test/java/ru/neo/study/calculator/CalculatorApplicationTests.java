package ru.neo.study.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import dto.CreditDto;
import dto.EmploymentDto;
import dto.LoanOfferDto;
import dto.LoanStatementRequestDto;
import dto.PaymentScheduleElementDto;
import dto.ScoringDataDto;
import enums.EmploymentStatus;
import enums.Gender;
import enums.MaritalStatus;
import enums.Position;
import ru.neo.study.calculator.exceptions.CancelCreditException;
import ru.neo.study.calculator.exceptions.ValidationException;
import ru.neo.study.calculator.service.CalculatorServiceImpl;
import ru.neo.study.calculator.service.discounts.RulesProcessor;
import ru.neo.study.calculator.service.discounts.parameters.DiscountsParam;
import ru.neo.study.calculator.service.discounts.rules.DiscountRules;
import ru.neo.study.calculator.service.discounts.rules.InsuranceEnabledRule;
import ru.neo.study.calculator.service.discounts.rules.SalaryClientRule;
import ru.neo.study.calculator.service.metrics.CalcHelperService;
import ru.neo.study.calculator.service.metrics.CalcService;
import ru.neo.study.calculator.service.metrics.OffersService;
import ru.neo.study.calculator.service.metrics.parameters.MetricsParam;
import ru.neo.study.calculator.service.validations.PrescoringService;
import ru.neo.study.calculator.service.validations.ScoringService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculatorApplicationTests {

    private CalculatorServiceImpl service;
    private MetricsParam metricsParam;

    @BeforeEach
    void setUp() {
        metricsParam = new MetricsParam();
        metricsParam.setLoanRate(20);
        metricsParam.setInsuranceRate(new BigDecimal("0.1"));
        metricsParam.setLargeAmountThreshold(new BigDecimal("1000000"));
        metricsParam.setLongTermThreshold(60);
        metricsParam.setScale(10);

        metricsParam.setMaxCountSalary(24);
        metricsParam.setRangeAgeFemale(List.of(32, 60));
        metricsParam.setRangeAgeMale(List.of(30, 55));
        metricsParam.setMinTotalWorkMonth(18);
        metricsParam.setMinCurrentWorkMonth(3);

        metricsParam.setMinAmount(new BigDecimal("20000"));
        metricsParam.setMinTerm(6);

        DiscountsParam discountsParam = new DiscountsParam();
        discountsParam.setSalaryClientDiscount(new BigDecimal("1.0"));
        discountsParam.setInsuranceDiscount(new BigDecimal("0.5"));

        DiscountRules insuranceRule = new InsuranceEnabledRule(discountsParam);
        DiscountRules salaryClientRule = new SalaryClientRule(discountsParam);
        RulesProcessor rulesProcessor = new RulesProcessor(List.of(insuranceRule, salaryClientRule));

        CalcHelperService calcHelperService = new CalcHelperService(metricsParam, rulesProcessor);
        ScoringService scoringService = new ScoringService(metricsParam);
        PrescoringService prescoringService = new PrescoringService(metricsParam);

        CalcService calcService = new CalcService(metricsParam, calcHelperService, scoringService, rulesProcessor);
        OffersService offersService = new OffersService(calcHelperService, prescoringService);
        service = new CalculatorServiceImpl(offersService, calcService);
    }

    @Test
    void offers_shouldReturnFourOffersSortedFromWorstToBest_andWithSameStatementId() {
        LoanStatementRequestDto request = validLoanStatementRequest();

        List<LoanOfferDto> offers = service.offers(request);

        assertEquals(4, offers.size());

        UUID statementId = offers.get(0).getStatementId();
        assertTrue(offers.stream().allMatch(o -> statementId.equals(o.getStatementId())));

        assertTrue(offers.get(0).getRate().compareTo(offers.get(1).getRate()) >= 0);
        assertTrue(offers.get(1).getRate().compareTo(offers.get(2).getRate()) >= 0);
        assertTrue(offers.get(2).getRate().compareTo(offers.get(3).getRate()) >= 0);

        LoanOfferDto ff = findOffer(offers, false, false);
        LoanOfferDto tf = findOffer(offers, true, false);
        LoanOfferDto ft = findOffer(offers, false, true);
        LoanOfferDto tt = findOffer(offers, true, true);

        assertBigDecimalEquals("20.0", ff.getRate());
        assertBigDecimalEquals("19.5", tf.getRate());
        assertBigDecimalEquals("19.0", ft.getRate());
        assertBigDecimalEquals("18.5", tt.getRate());

        assertBigDecimalEquals("300000", ff.getRequestedAmount());
        assertBigDecimalEquals("300000", ff.getTotalAmount());

        assertBigDecimalEquals("330000.00", tf.getTotalAmount());
        assertBigDecimalEquals("300000", ft.getTotalAmount());
        assertBigDecimalEquals("330000.00", tt.getTotalAmount());
    }

    @Test
    void offers_shouldApplyPremiumsForLargeAmountAndLongTerm() {
        LoanStatementRequestDto request = validLoanStatementRequest();
        when(request.getAmount()).thenReturn(new BigDecimal("1500000"));
        when(request.getTerm()).thenReturn(72);

        List<LoanOfferDto> offers = service.offers(request);

        LoanOfferDto ff = findOffer(offers, false, false);
        LoanOfferDto tf = findOffer(offers, true, false);
        LoanOfferDto ft = findOffer(offers, false, true);
        LoanOfferDto tt = findOffer(offers, true, true);

        assertBigDecimalEquals("22", ff.getRate());
        assertBigDecimalEquals("21.5", tf.getRate());
        assertBigDecimalEquals("21", ft.getRate());
        assertBigDecimalEquals("20.5", tt.getRate());
    }

    @Test
    void offers_shouldAllowNullMiddleName() {
        LoanStatementRequestDto request = validLoanStatementRequest();
        when(request.getMiddleName()).thenReturn(null);

        List<LoanOfferDto> offers = service.offers(request);

        assertEquals(4, offers.size());
    }

    @Test
    void offers_shouldThrowValidationException_whenFirstNameIsInvalid() {
        LoanStatementRequestDto request = validLoanStatementRequest();
        when(request.getFirstName()).thenReturn("A");

        assertThrows(ValidationException.class, () -> service.offers(request));
    }

    @Test
    void offers_shouldThrowValidationException_whenAmountIsTooSmall() {
        LoanStatementRequestDto request = validLoanStatementRequest();
        when(request.getAmount()).thenReturn(new BigDecimal("19999"));

        assertThrows(ValidationException.class, () -> service.offers(request));
    }

    @Test
    void offers_shouldThrowValidationException_whenEmailIsInvalid() {
        LoanStatementRequestDto request = validLoanStatementRequest();
        when(request.getEmail()).thenReturn("wrong-email");

        assertThrows(ValidationException.class, () -> service.offers(request));
    }

    @Test
    void offers_shouldThrowValidationException_whenPassportSeriesIsInvalid() {
        LoanStatementRequestDto request = validLoanStatementRequest();
        when(request.getPassportSeries()).thenReturn("12");

        assertThrows(ValidationException.class, () -> service.offers(request));
    }

    @Test
    void calc_shouldReturnCreditDto_withCorrectRateScheduleAndPsk() {
        ScoringMocks mocks = successfulScoringData(
                new BigDecimal("300000"),
                LocalDate.now().minusYears(35),
                null,
                null,
                new BigDecimal("100000"),
                null,
                null,
                true,
                true
        );

        CreditDto credit = service.calc(mocks.scoringData());

        assertNotNull(credit);
        assertBigDecimalEquals("18.5", credit.getRate());
        assertBigDecimalEquals("330000.00", credit.getAmount());
        assertEquals(12, credit.getTerm());
        assertNotNull(credit.getMonthlyPayment());
        assertTrue(credit.getMonthlyPayment().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(credit.getPsk());
        assertTrue(credit.getPsk().compareTo(BigDecimal.ZERO) > 0);

        List<PaymentScheduleElementDto> schedule = credit.getPaymentSchedule();
        assertEquals(12, schedule.size());

        PaymentScheduleElementDto firstPayment = schedule.getFirst();
        PaymentScheduleElementDto lastPayment = schedule.getLast();

        assertEquals(1, firstPayment.getNumber());
        assertEquals(LocalDate.now().plusMonths(1), firstPayment.getDate());

        assertEquals(12, lastPayment.getNumber());
        assertBigDecimalEquals("0.00", lastPayment.getRemainingDebt());
    }

    @Test
    void calc_shouldApplyScoringAdjustmentsToRate() {
        ScoringMocks mocks = successfulScoringData(
                new BigDecimal("300000"),
                LocalDate.now().minusYears(35),
                EmploymentStatus.SELF_EMPLOYED,
                Position.TOP_MANAGEMENT,
                new BigDecimal("100000"),
                MaritalStatus.DIVORCED,
                Gender.NON_BINARY,
                false,
                false
        );

        CreditDto credit = service.calc(mocks.scoringData());

        assertBigDecimalEquals("27", credit.getRate());
    }

    @Test
    void calc_shouldThrowCancelCreditException_whenUnemployed() {
        ScoringMocks mocks = minimalScoringMocks();
        when(mocks.scoringData().getBirthdate()).thenReturn(LocalDate.now().minusYears(35));
        when(mocks.employment().getEmploymentStatus()).thenReturn(EmploymentStatus.UNEMPLOYED);

        assertThrows(CancelCreditException.class, () -> service.calc(mocks.scoringData()));
    }

    @Test
    void calc_shouldThrowCancelCreditException_whenAmountExceeds24Salaries() {
        ScoringMocks mocks = minimalScoringMocks();
        when(mocks.scoringData().getBirthdate()).thenReturn(LocalDate.now().minusYears(35));
        when(mocks.scoringData().getAmount()).thenReturn(new BigDecimal("3000000"));
        when(mocks.employment().getEmploymentStatus()).thenReturn(null);
        when(mocks.employment().getPosition()).thenReturn(null);
        when(mocks.employment().getSalary()).thenReturn(new BigDecimal("100000"));

        assertThrows(CancelCreditException.class, () -> service.calc(mocks.scoringData()));
    }

    @Test
    void calc_shouldThrowCancelCreditException_whenAgeIsOutOfRange() {
        ScoringMocks mocks = minimalScoringMocks();
        when(mocks.scoringData().getBirthdate()).thenReturn(LocalDate.now().minusYears(19));
        when(mocks.employment().getEmploymentStatus()).thenReturn(null);
        when(mocks.employment().getPosition()).thenReturn(null);
        when(mocks.scoringData().getAmount()).thenReturn(new BigDecimal("300000"));
        when(mocks.employment().getSalary()).thenReturn(new BigDecimal("100000"));
        when(mocks.scoringData().getMaritalStatus()).thenReturn(null);

        assertThrows(CancelCreditException.class, () -> service.calc(mocks.scoringData()));
    }

    @Test
    void calc_shouldThrowCancelCreditException_whenWorkExperienceIsInsufficient() {
        ScoringMocks mocks = minimalScoringMocks();
        when(mocks.scoringData().getBirthdate()).thenReturn(LocalDate.now().minusYears(35));
        when(mocks.employment().getEmploymentStatus()).thenReturn(null);
        when(mocks.employment().getPosition()).thenReturn(null);
        when(mocks.scoringData().getAmount()).thenReturn(new BigDecimal("300000"));
        when(mocks.employment().getSalary()).thenReturn(new BigDecimal("100000"));
        when(mocks.scoringData().getMaritalStatus()).thenReturn(null);
        when(mocks.scoringData().getGender()).thenReturn(null);
        when(mocks.employment().getWorkExperienceTotal()).thenReturn(10);

        assertThrows(CancelCreditException.class, () -> service.calc(mocks.scoringData()));
    }

    @Test
    void calc_shouldUseSimpleDivision_whenRateIsZero() {
        metricsParam.setLoanRate(0);

        ScoringMocks mocks = successfulScoringData(
                new BigDecimal("120000"),
                LocalDate.now().minusYears(35),
                null,
                null,
                new BigDecimal("100000"),
                null,
                null,
                false,
                false
        );

        CreditDto credit = service.calc(mocks.scoringData());

        assertBigDecimalEquals("10000.00", credit.getMonthlyPayment());
        assertBigDecimalEquals("0", credit.getRate());
    }

    @Test
    void offers_shouldThrowValidationException_whenLastNameIsInvalid() {
        LoanStatementRequestDto request = validLoanStatementRequest();
        when(request.getLastName()).thenReturn("1");

        assertThrows(ValidationException.class, () -> service.offers(request));
    }

    @Test
    void offers_shouldThrowValidationException_whenMiddleNameIsInvalid() {
        LoanStatementRequestDto request = validLoanStatementRequest();
        when(request.getMiddleName()).thenReturn("!");

        assertThrows(ValidationException.class, () -> service.offers(request));
    }

    @Test
    void offers_shouldThrowValidationException_whenTermIsTooSmall() {
        LoanStatementRequestDto request = validLoanStatementRequest();
        when(request.getTerm()).thenReturn(5);

        assertThrows(ValidationException.class, () -> service.offers(request));
    }

    @Test
    void offers_shouldThrowValidationException_whenBirthdateIsTooRecent() {
        LoanStatementRequestDto request = validLoanStatementRequest();
        when(request.getBirthdate()).thenReturn(LocalDate.now().minusYears(17));

        assertThrows(ValidationException.class, () -> service.offers(request));
    }

    @Test
    void offers_shouldThrowValidationException_whenPassportNumberIsInvalid() {
        LoanStatementRequestDto request = validLoanStatementRequest();
        when(request.getPassportNumber()).thenReturn("123");

        assertThrows(ValidationException.class, () -> service.offers(request));
    }

    @Test
    void calc_shouldApplyBusinessOwnerAdjustment() {
        ScoringMocks mocks = successfulScoringData(
                new BigDecimal("300000"),
                LocalDate.now().minusYears(35),
                EmploymentStatus.BUSINESS_OWNER,
                null,
                new BigDecimal("100000"),
                null,
                null,
                false,
                false
        );

        CreditDto credit = service.calc(mocks.scoringData());

        assertBigDecimalEquals("21", credit.getRate());
    }

    @Test
    void calc_shouldApplyMiddleManagementAdjustment() {
        ScoringMocks mocks = successfulScoringData(
                new BigDecimal("300000"),
                LocalDate.now().minusYears(35),
                null,
                Position.MID_MANAGEMENT,
                new BigDecimal("100000"),
                null,
                null,
                false,
                false
        );

        CreditDto credit = service.calc(mocks.scoringData());

        assertBigDecimalEquals("18", credit.getRate());
    }

    @Test
    void calc_shouldApplyMarriedDiscount() {
        ScoringMocks mocks = successfulScoringData(
                new BigDecimal("300000"),
                LocalDate.now().minusYears(35),
                null,
                null,
                new BigDecimal("100000"),
                MaritalStatus.MARRIED,
                null,
                false,
                false
        );

        CreditDto credit = service.calc(mocks.scoringData());

        assertBigDecimalEquals("17", credit.getRate());
    }

    @Test
    void calc_shouldApplyFemaleAgeDiscount() {
        ScoringMocks mocks = successfulScoringData(
                new BigDecimal("300000"),
                LocalDate.now().minusYears(35),
                null,
                null,
                new BigDecimal("100000"),
                null,
                Gender.FEMALE,
                false,
                false
        );

        CreditDto credit = service.calc(mocks.scoringData());

        assertBigDecimalEquals("17", credit.getRate());
    }

    @Test
    void calc_shouldApplyMaleAgeDiscount() {
        ScoringMocks mocks = successfulScoringData(
                new BigDecimal("300000"),
                LocalDate.now().minusYears(35),
                null,
                null,
                new BigDecimal("100000"),
                null,
                Gender.MALE,
                false,
                false
        );

        CreditDto credit = service.calc(mocks.scoringData());

        assertBigDecimalEquals("17", credit.getRate());
    }

    @Test
    void calc_shouldThrowCancelCreditException_whenCurrentWorkExperienceIsInsufficient() {
        ScoringMocks mocks = successfulScoringData(
                new BigDecimal("300000"),
                LocalDate.now().minusYears(35),
                null,
                null,
                new BigDecimal("100000"),
                null,
                null,
                false,
                false
        );

        when(mocks.employment().getWorkExperienceCurrent()).thenReturn(2);

        assertThrows(CancelCreditException.class, () -> service.calc(mocks.scoringData()));
    }

    @Test
    void calc_shouldKeepAmountUnchanged_whenInsuranceDisabled() {
        ScoringMocks mocks = successfulScoringData(
                new BigDecimal("300000"),
                LocalDate.now().minusYears(35),
                null,
                null,
                new BigDecimal("100000"),
                null,
                null,
                false,
                false
        );

        CreditDto credit = service.calc(mocks.scoringData());

        assertBigDecimalEquals("300000", credit.getAmount());
    }

    @Test
    void calc_shouldIncreaseAmount_whenInsuranceEnabled() {
        ScoringMocks mocks = successfulScoringData(
                new BigDecimal("300000"),
                LocalDate.now().minusYears(35),
                null,
                null,
                new BigDecimal("100000"),
                null,
                null,
                true,
                false
        );

        CreditDto credit = service.calc(mocks.scoringData());

        assertBigDecimalEquals("330000.00", credit.getAmount());
    }

    private LoanStatementRequestDto validLoanStatementRequest() {
        LoanStatementRequestDto request = mock(LoanStatementRequestDto.class);

        when(request.getAmount()).thenReturn(new BigDecimal("300000"));
        when(request.getTerm()).thenReturn(12);
        when(request.getFirstName()).thenReturn("Ivan");
        when(request.getLastName()).thenReturn("Ivanov");
        when(request.getMiddleName()).thenReturn("Ivanovich");
        when(request.getEmail()).thenReturn("ivanov@example.com");
        when(request.getBirthdate()).thenReturn(LocalDate.now().minusYears(25));
        when(request.getPassportSeries()).thenReturn("1234");
        when(request.getPassportNumber()).thenReturn("123456");

        return request;
    }

    private ScoringMocks minimalScoringMocks() {
        ScoringDataDto scoringData = mock(ScoringDataDto.class);
        EmploymentDto employment = mock(EmploymentDto.class);

        when(scoringData.getEmployment()).thenReturn(employment);
        lenient().when(scoringData.getIsInsuranceEnabled()).thenReturn(false);
        lenient().when(scoringData.getIsSalaryClient()).thenReturn(false);

        return new ScoringMocks(scoringData, employment);
    }

    private ScoringMocks successfulScoringData(BigDecimal amount,
                                               LocalDate birthdate,
                                               EmploymentStatus employmentStatus,
                                               Position position,
                                               BigDecimal salary,
                                               MaritalStatus maritalStatus,
                                               Gender gender,
                                               boolean insuranceEnabled,
                                               boolean salaryClient) {
        ScoringMocks mocks = minimalScoringMocks();

        when(mocks.scoringData().getAmount()).thenReturn(amount);
        lenient().when(mocks.scoringData().getTerm()).thenReturn(12);
        when(mocks.scoringData().getBirthdate()).thenReturn(birthdate);
        when(mocks.scoringData().getMaritalStatus()).thenReturn(maritalStatus);
        when(mocks.scoringData().getGender()).thenReturn(gender);
        lenient().when(mocks.scoringData().getIsInsuranceEnabled()).thenReturn(insuranceEnabled);
        lenient().when(mocks.scoringData().getIsSalaryClient()).thenReturn(salaryClient);

        when(mocks.employment().getEmploymentStatus()).thenReturn(employmentStatus);
        when(mocks.employment().getPosition()).thenReturn(position);
        when(mocks.employment().getSalary()).thenReturn(salary);
        when(mocks.employment().getWorkExperienceTotal()).thenReturn(24);
        when(mocks.employment().getWorkExperienceCurrent()).thenReturn(6);

        return mocks;
    }

    private LoanOfferDto findOffer(List<LoanOfferDto> offers, boolean insurance, boolean salary) {
        return offers.stream()
                .filter(o -> Boolean.valueOf(insurance).equals(o.getIsInsuranceEnabled())
                        && Boolean.valueOf(salary).equals(o.getIsSalaryClient()))
                .findFirst()
                .orElseThrow();
    }

    private void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "Ожидалось " + expected + " вместо " + actual);
    }

    private record ScoringMocks(ScoringDataDto scoringData, EmploymentDto employment) {
    }
}