package ru.neo.study.calculator.repository;

import org.springframework.stereotype.Repository;
import ru.neo.study.calculator.dto.CreditDto;
import ru.neo.study.calculator.dto.LoanOfferDto;
import ru.neo.study.calculator.model.LoanStatementRequest;
import ru.neo.study.calculator.model.ScoringData;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CalculatorStorageImpl implements CalculatorStorage {
    private static final BigDecimal BASE_RATE = new BigDecimal("20.0"); // базовая ставка 20%
    private static final BigDecimal SALARY_CLIENT_DISCOUNT = new BigDecimal("1.0");
    private static final BigDecimal INSURANCE_DISCOUNT = new BigDecimal("0.5");
    private static final int SCALE = 10; // точность промежуточных вычислений
    private static final int FINAL_SCALE = 2;

    public List<LoanOfferDto> offers(LoanStatementRequest loanStatementRequest) {
        return new ArrayList<>();
    }

    public CreditDto calc(ScoringData scoringData, BigDecimal rate) {

        return new CreditDto();
    }





}
