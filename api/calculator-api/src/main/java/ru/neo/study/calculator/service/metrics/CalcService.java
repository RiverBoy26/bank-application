package ru.neo.study.calculator.service.metrics;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import dto.CreditDto;
import dto.PaymentScheduleElementDto;
import dto.ScoringDataDto;
import ru.neo.study.calculator.service.discounts.RulesProcessor;
import ru.neo.study.calculator.service.discounts.parameters.DiscountsOptions;
import ru.neo.study.calculator.service.metrics.parameters.MetricsParam;
import ru.neo.study.calculator.service.validations.ScoringService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalcService {
    static final Logger logger = LoggerFactory.getLogger(CalcService.class);

    private final MetricsParam metricsParam;
    private final CalcHelperService calcHelperService;
    private final ScoringService scoringService;
    private final RulesProcessor rulesProcessor;

    public CreditDto calc(ScoringDataDto scoringData) {
        logger.info("Получен запрос на расчёт: {}", scoringData);

        BigDecimal rateBeforeScoring = scoringService.scoring(scoringData);

        logger.debug("Результат скоринга: {}", rateBeforeScoring);

        DiscountsOptions options = new DiscountsOptions(
                scoringData.getIsInsuranceEnabled(),
                scoringData.getIsSalaryClient()
        );

        BigDecimal baseRate = BigDecimal.valueOf(metricsParam.getLoanRate())
                .add(rateBeforeScoring);

        logger.debug("Ставка после скоринга: {}", baseRate);

        BigDecimal rate = rulesProcessor.applyAll(baseRate, options);

        BigDecimal amount = scoringData.getAmount();
        Integer term = scoringData.getTerm();
        BigDecimal totalAmount = calcHelperService.calculateTotalAmount(amount, options);
        BigDecimal monthlyPayment = calcHelperService.calculateMonthlyPayment(totalAmount, rate, term);
        List<PaymentScheduleElementDto> paymentSchedule = buildPaymentSchedule(totalAmount, rate, term);
        BigDecimal psk = calcHelperService.calculatePsk(amount, paymentSchedule);

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

    private List<PaymentScheduleElementDto> buildPaymentSchedule(BigDecimal totalAmount,
                                                                 BigDecimal annualRate,
                                                                 Integer term) {
        logger.debug("Начало построения графика: totalAmount={}, annualRate={}, term={}", totalAmount, annualRate, term);
        List<PaymentScheduleElementDto> paymentSchedule = new ArrayList<>();
        BigDecimal remainingDebt = totalAmount;

        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(100), metricsParam.getScale(), RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), metricsParam.getScale(), RoundingMode.HALF_UP);

        BigDecimal monthlyPayment = calcHelperService.calculateMonthlyPayment(totalAmount, annualRate, term);

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
}
