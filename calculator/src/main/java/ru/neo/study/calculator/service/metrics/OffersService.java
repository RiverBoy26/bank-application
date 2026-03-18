package ru.neo.study.calculator.service.metrics;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.neo.study.calculator.dto.LoanOfferDto;
import ru.neo.study.calculator.dto.LoanStatementRequestDto;
import ru.neo.study.calculator.service.discounts.parameters.DiscountsOptions;
import ru.neo.study.calculator.service.validations.PrescoringService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OffersService {
    private static final Logger logger = LoggerFactory.getLogger(OffersService.class);
    private final CalcHelperService calcHelperService;
    private final PrescoringService prescoringService;

    public List<LoanOfferDto> offers(LoanStatementRequestDto loanStatementRequest) {
        logger.info("Получен запрос на предложения: {}", loanStatementRequest);

        prescoringService.prescoring(loanStatementRequest);
        UUID statementId = UUID.randomUUID();

        logger.debug("Создан statementId={} для набора предложений", statementId);

        List<LoanOfferDto> result = new ArrayList<>(4);

        result.add(createOffer(statementId, loanStatementRequest, new DiscountsOptions(false, false)));
        result.add(createOffer(statementId, loanStatementRequest, new DiscountsOptions(true, false)));
        result.add(createOffer(statementId, loanStatementRequest, new DiscountsOptions(false, true)));
        result.add(createOffer(statementId, loanStatementRequest, new DiscountsOptions(true, true)));

        List<LoanOfferDto> sortedResult = result.stream()
                .sorted((o1, o2) -> o2.getRate().compareTo(o1.getRate()))
                .toList();
        logger.info("Сформированы 4 кредитных предложения: {}", sortedResult);

        return sortedResult;
    }

    private LoanOfferDto createOffer(UUID statementId,
                                     LoanStatementRequestDto request,
                                     DiscountsOptions options) {
        logger.debug("Расчёт предложения при параметрах: statementId={}, options={}",
                statementId, options);

        BigDecimal requestedAmount = request.getAmount();
        BigDecimal rate = calcHelperService.calculateRate(
                requestedAmount,
                request.getTerm(),
                options
        );
        BigDecimal totalAmount = calcHelperService.calculateTotalAmount(requestedAmount, options);
        BigDecimal monthlyPayment = calcHelperService.calculateMonthlyPayment(totalAmount, rate, request.getTerm());

        LoanOfferDto newLoanOffer = new LoanOfferDto(
                statementId,
                requestedAmount,
                totalAmount,
                request.getTerm(),
                monthlyPayment,
                rate,
                options.getIsInsuranceEnabled(),
                options.getIsSalaryClient()
        );

        logger.debug("Создано предложение: {}", newLoanOffer);

        return newLoanOffer;
    }
}
