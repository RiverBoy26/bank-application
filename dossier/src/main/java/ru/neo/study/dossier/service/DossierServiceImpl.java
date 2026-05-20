package ru.neo.study.dossier.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.neo.study.dossier.dto.EmailMessage;
import ru.neo.study.dossier.enums.Theme;

import java.nio.file.Path;

@Service
@Slf4j
@RequiredArgsConstructor
public class DossierServiceImpl implements DossierService {

    private final MailService mailService;

    @Override
    public void process(EmailMessage message) {

        log.debug(
                "Начата обработка EmailMessage: statementId={}, theme={}, address={}",
                message.getStatementId(),
                message.getTheme(),
                message.getAddress()
        );

        String subject = buildSubject(message.getTheme());
        String body = "Заявка: " + message.getStatementId()
                + "\n\n"
                + message.getText();

        log.debug(
                "Сформировано письмо для клиента: statementId={}, theme={}, subject={}",
                message.getStatementId(),
                message.getTheme(),
                subject
        );

        mailService.send(
                message.getAddress(),
                subject,
                body
        );

        log.debug(
                "Обработка EmailMessage завершена: statementId={}, theme={}, address={}",
                message.getStatementId(),
                message.getTheme(),
                message.getAddress()
        );
    }

    private String buildSubject(Theme theme) {
        return switch (theme) {
            case FINISH_REGISTRATION -> "Завершение регистрации кредитной заявки";
            case CREATE_DOCUMENTS -> "Формирование кредитных документов";
            case SEND_DOCUMENTS -> "Документы по кредитной заявке";
            case SEND_SES -> "Код подтверждения подписания документов";
            case CREDIT_ISSUED -> "Кредит выдан";
            case STATEMENT_DENIED -> "Отказ по кредитной заявке";
        };
    }
}