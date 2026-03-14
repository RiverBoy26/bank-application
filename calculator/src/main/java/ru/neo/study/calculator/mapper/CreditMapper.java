package ru.neo.study.calculator.mapper;

import lombok.experimental.UtilityClass;
import ru.neo.study.calculator.dto.CreditDto;
import ru.neo.study.calculator.model.Credit;

@UtilityClass
public class CreditMapper {
    public CreditDto toCreditDto(Credit credit) {
        return new CreditDto(
                credit.getAmount(),
                credit.getTerm(),
                credit.getMonthlyPayment(),
                credit.getRate(),
                credit.getPsk(),
                credit.getIsInsuranceEnabled(),
                credit.getIsSalaryClient(),
                credit.getPaymentSchedule()
        );
    }

    public Credit toCredit(CreditDto creditDto) {
        Credit credit = new Credit();

        credit.setAmount(creditDto.getAmount());
        credit.setTerm(creditDto.getTerm());
        credit.setMonthlyPayment(creditDto.getMonthlyPayment());
        credit.setRate(creditDto.getRate());
        credit.setPsk(creditDto.getPsk());
        credit.setIsInsuranceEnabled(creditDto.getIsInsuranceEnabled());
        credit.setIsSalaryClient(creditDto.getIsSalaryClient());
        credit.setPaymentSchedule(creditDto.getPaymentSchedule());

        return credit;
    }
}
