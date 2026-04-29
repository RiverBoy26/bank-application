package ru.neo.study.statement.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.neo.study.statement.dto.LoanOfferDto;
import ru.neo.study.statement.dto.LoanStatementRequestDto;
import ru.neo.study.statement.service.StatementService;

import java.util.List;

@RestController
@RequestMapping("/statement")
@RequiredArgsConstructor
@Slf4j
public class StatementController {
    private final StatementService statementService;

    @PostMapping
    public ResponseEntity<List<LoanOfferDto>> calculateLoanOffers(@RequestBody LoanStatementRequestDto loanStatementRequestDto) {
        log.info("Получен запрос на расчёт кредитных предложений: {}", loanStatementRequestDto);
        List<LoanOfferDto> offers = statementService.calculateLoanOffers(loanStatementRequestDto);

        log.info("Кредитные предложения успешно рассчитаны. Количество предложений: {}", offers == null ? 0 : offers.size());
        log.info("Кредитные предложения: {}", offers);

        return ResponseEntity.status(HttpStatus.CREATED).body(offers);
    }

    @PostMapping("/offer")
    public ResponseEntity<Void> selectOffer(@RequestBody LoanOfferDto loanOfferDto) {
        log.info("Получен запрос на выбор кредитного предложения: {}", loanOfferDto);
        statementService.selectOffer(loanOfferDto);

        log.info("Кредитное предложение успешно выбрано. ID заявки: {}", loanOfferDto.getStatementId());
        return ResponseEntity.ok().build();
    }
}
