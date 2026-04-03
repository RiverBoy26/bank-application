package ru.neo.study.calculator.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import dto.*;
import ru.neo.study.calculator.service.metrics.CalcService;
import ru.neo.study.calculator.service.metrics.OffersService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Data
public class CalculatorServiceImpl implements CalculatorService {
    static final Logger logger = LoggerFactory.getLogger(CalculatorServiceImpl.class);

    private final OffersService offersService;
    private final CalcService calcService;

    public List<LoanOfferDto> offers(LoanStatementRequestDto loanStatementRequest) {
        return offersService.offers(loanStatementRequest);
    }

    public CreditDto calc(ScoringDataDto scoringData) {
        return calcService.calc(scoringData);
    }
}
