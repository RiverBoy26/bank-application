package ru.neo.study.dossier.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.neo.study.dossier.dto.EmailMessage;
import ru.neo.study.dossier.validator.MessageValidator;

@Service
@Slf4j
@RequiredArgsConstructor
public class DossierServiceImpl implements DossierService {

    private final MailService mailService;

    @Override
    public void process(EmailMessage message) {
        MessageValidator.validateMessage(message);
        log.debug(
                "Начата обработка EmailMessage: statementId={}, theme={}, address={}",
                message.getStatementId(), message.getTheme(), message.getAddress());

        String subject = message.getTheme().getTopicName();
        String body = "Заявка: " + message.getStatementId() + "\n\n" + message.getText();

        log.debug(
                "Сформировано письмо для клиента: statementId={}, theme={}, subject={}",
                message.getStatementId(), message.getTheme(), subject);

        mailService.send(message.getAddress(), subject, body);

        log.debug(
                "Обработка EmailMessage завершена: statementId={}, theme={}, address={}",
                message.getStatementId(), message.getTheme(), message.getAddress());
    }
}