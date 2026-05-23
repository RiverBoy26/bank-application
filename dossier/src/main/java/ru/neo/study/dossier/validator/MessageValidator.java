package ru.neo.study.dossier.validator;

import lombok.experimental.UtilityClass;
import ru.neo.study.dossier.dto.EmailMessage;

@UtilityClass
public class MessageValidator {
    public void validateMessage(EmailMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Kafka-сообщение не должно быть null");
        }

        if (message.getStatementId() == null) {
            throw new IllegalArgumentException("statementId не должен быть null");
        }

        if (message.getAddress() == null || message.getAddress().isBlank()) {
            throw new IllegalArgumentException("Email клиента не должен быть пустым");
        }

        if (message.getTheme() == null) {
            throw new IllegalArgumentException("Тема письма не должна быть null");
        }

        if (message.getText() == null || message.getText().isBlank()) {
            throw new IllegalArgumentException("Текст письма не должен быть пустым");
        }
    }
}
