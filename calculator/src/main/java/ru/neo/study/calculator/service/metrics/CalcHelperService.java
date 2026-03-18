package ru.neo.study.calculator.service.metrics;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.neo.study.calculator.dto.PaymentScheduleElementDto;
import ru.neo.study.calculator.service.discounts.RulesProcessor;
import ru.neo.study.calculator.service.discounts.parameters.DiscountsOptions;
import ru.neo.study.calculator.service.metrics.parameters.MetricsParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Data
public class CalcHelperService {
    private final MetricsParam metricsParam;
    private final RulesProcessor rulesProcessor;

    static final Logger logger = LoggerFactory.getLogger(CalcHelperService.class);

    public BigDecimal calculateRate(BigDecimal amount,
                                     Integer term,
                                     DiscountsOptions discountsOptions) {
        BigDecimal rate = BigDecimal.valueOf(metricsParam.getLoanRate());

        logger.debug("Старт расчёта ставки: baseRate={}", rate);

        if (amount.compareTo(metricsParam.getLargeAmountThreshold()) > 0) {
            rate = rate.add(BigDecimal.ONE);
            logger.debug("Сумма кредита {} > {}, ставка увеличена до {}", amount, metricsParam.getLargeAmountThreshold(), rate);
        }

        if (term != null && term > metricsParam.getLongTermThreshold()) {
            rate = rate.add(BigDecimal.ONE);
            logger.debug("Срок кредита {} > {}, ставка увеличена до {}", amount, metricsParam.getLongTermThreshold(), rate);
        }

        BigDecimal result = rulesProcessor.applyAll(rate, discountsOptions);

        logger.debug("Итоговая ставка предложения: {}", result);
        return result;
    }

    public BigDecimal calculateMonthlyPayment(BigDecimal totalAmount, BigDecimal annualRate, Integer term) {
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
                .divide(BigDecimal.valueOf(100), metricsParam.getScale(), RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), metricsParam.getScale(), RoundingMode.HALF_UP);

        double r = monthlyRate.doubleValue();
        double n = term.doubleValue();
        double payment = totalAmount.doubleValue() * r / (1 - Math.pow(1 + r, -n));

        BigDecimal result = BigDecimal.valueOf(payment).setScale(2, RoundingMode.HALF_UP);

        logger.debug("Рассчитан monthlyPayment={}, monthlyRate={}", result, monthlyRate);

        return result;
    }

    public BigDecimal calculateTotalAmount(BigDecimal requestedAmount, DiscountsOptions options) {
        BigDecimal result = requestedAmount;

        if (options.getIsInsuranceEnabled()) {
            BigDecimal insuranceAmount = requestedAmount
                    .multiply(metricsParam.getInsuranceRate())
                    .setScale(2, RoundingMode.HALF_UP);

            result = result.add(insuranceAmount);
        }

        return result;
    }

    public BigDecimal calculatePsk(BigDecimal issuedAmount,
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
}
