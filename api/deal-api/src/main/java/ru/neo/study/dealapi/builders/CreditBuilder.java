package ru.neo.study.dealapi.builders;

import ru.neo.study.dealapi.dto.CreditDto;
import ru.neo.study.dealapi.enums.CreditStatus;
import org.springframework.stereotype.Component;
import ru.neo.study.dealapi.entity.Credit;

@Component
public class CreditBuilder {
    public Credit buildCredit(CreditDto creditDto) {
        return Credit.builder()
                .amount(creditDto.getAmount())
                .term(creditDto.getTerm())
                .monthlyPayment(creditDto.getMonthlyPayment())
                .rate(creditDto.getRate())
                .psk(creditDto.getPsk())
                .paymentSchedule(creditDto.getPaymentSchedule())
                .insuranceEnabled(creditDto.getIsInsuranceEnabled())
                .salaryClient(creditDto.getIsSalaryClient())
                .creditStatus(CreditStatus.CALCULATED)
                .build();
    }
}
