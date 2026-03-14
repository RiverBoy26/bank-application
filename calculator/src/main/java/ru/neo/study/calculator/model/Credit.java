package ru.neo.study.calculator.model;

import lombok.Data;
import ru.neo.study.calculator.dto.PaymentScheduleElementDto;

import java.math.BigDecimal;
import java.util.List;

@Data
public class Credit {
    private BigDecimal amount;
    private Integer term;
    private BigDecimal monthlyPayment;
    private BigDecimal rate;
    private BigDecimal psk;
    private Boolean isInsuranceEnabled;
    private Boolean isSalaryClient;
    private List<PaymentScheduleElementDto> paymentSchedule;
}
