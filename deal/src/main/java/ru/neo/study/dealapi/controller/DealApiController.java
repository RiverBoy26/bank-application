package ru.neo.study.dealapi.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.neo.study.dealapi.dto.FinishRegistrationRequestDto;
import ru.neo.study.dealapi.dto.LoanOfferDto;
import ru.neo.study.dealapi.dto.LoanStatementRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.neo.study.dealapi.dto.SesCodeDto;
import ru.neo.study.dealapi.service.DealApiService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/deal")
public class DealApiController {
    private final DealApiService dealApiService;

    @PostMapping("/statement")
    public ResponseEntity<List<LoanOfferDto>> calculateLoanTerms(@RequestBody LoanStatementRequestDto loanStatementRequestDto) {
        log.info("Получен запрос на предварительный расчёт кредита: {}", loanStatementRequestDto);
        List<LoanOfferDto> body = dealApiService.calculateLoanTerms(loanStatementRequestDto);
        log.info("Запросы на предварительный расчёт кредита получены: {}", body);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/offer/select")
    public ResponseEntity<Void> selectOffer(@RequestBody LoanOfferDto loanOfferDto) {
        log.info("Получен запрос на выбор предложения: {}", loanOfferDto);
        dealApiService.selectOffer(loanOfferDto);
        log.info("Процесс выбора предложения {} завершён", loanOfferDto);
        return ResponseEntity
                .ok()
                .header("Location", "/statement/" + loanOfferDto.getStatementId())
                .build();
    }

    @PostMapping("/calculate/{statementId}")
    public ResponseEntity<Void> finishRegistrationAndCalculate(@PathVariable UUID statementId,
                                                                   @RequestBody FinishRegistrationRequestDto finishRegistrationRequestDto) {
        log.info("Получен запрос на завершение регистрации и полный расчёт кредита: statementId={}, body={}",
                statementId, finishRegistrationRequestDto);
        dealApiService.finishRegistrationAndCalculate(statementId, finishRegistrationRequestDto);
        log.info("Процесс запроса на завершение регистрации и полный расчёт кредита завершён");
        return ResponseEntity
                .ok()
                .header("StatementStatus", dealApiService.getStatementStatus(statementId))
                .build();
    }

    @PostMapping("/document/{statementId}/send")
    public ResponseEntity<Void> sendDocumentRequest(@PathVariable UUID statementId) {
        log.info("Получен запрос на отправку документов клиенту: statementId={}", statementId);
        dealApiService.sendDocumentRequest(statementId);
        log.info("Запрос на отправку документов успешно обработан: statementId={}", statementId);
        return ResponseEntity.ok()
                .header("StatementStatus", dealApiService.getStatementStatus(statementId))
                .build();
    }

    @PostMapping("/document/{statementId}/sign")
    public ResponseEntity<Void> signDocumentRequest(@PathVariable UUID statementId) {
        log.info("Получен запрос на подписание документов и отправку SES-кода: statementId={}", statementId);
        dealApiService.signDocumentRequest(statementId);
        log.info("Запрос на подписание документов успешно обработан, SES-код отправлен: statementId={}", statementId);
        return ResponseEntity.ok()
                .header("StatementStatus", dealApiService.getStatementStatus(statementId))
                .build();
    }

    @PostMapping("/document/{statementId}/code")
    public ResponseEntity<Void> signDocument(@PathVariable UUID statementId,
                                             @RequestBody @Valid SesCodeDto sesCodeDto) {
        log.info("Получен запрос на проверку SES-кода и выдачу кредита: statementId={}", statementId);
        dealApiService.signDocument(statementId, sesCodeDto.getSesCode());
        log.info("SES-код успешно подтверждён, кредит выдан: statementId={}", statementId);
        return ResponseEntity.ok()
                .header("StatementStatus", dealApiService.getStatementStatus(statementId))
                .build();
    }

}
