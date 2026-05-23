package ru.neo.study.dealapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.neo.study.dealapi.dto.EmailMessage;
import ru.neo.study.dealapi.entity.Client;
import ru.neo.study.dealapi.entity.Statement;
import ru.neo.study.dealapi.enums.Theme;
import ru.neo.study.dealapi.kafka.KafkaEmailProducer;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailMessageService {
    private final KafkaEmailProducer kafkaEmailProducer;

    public void sendEmailMessage(Statement statement, Theme theme, String text) {
        Client client = statement.getClient();

        EmailMessage message = EmailMessage.builder()
                .statementId(statement.getId())
                .address(client.getEmail())
                .theme(theme)
                .text(text)
                .build();

        String topic = theme.getTopicName();

        try {
            kafkaEmailProducer.send(topic, message);

            log.debug(
                    "Email-событие отправлено в Kafka: statementId={}, theme={}, topic={}",
                    statement.getId(), theme, topic
            );
        } catch (Exception exception) {
            log.error(
                    "Ошибка отправки email-события в Kafka: statementId={}, theme={}, topic={}",
                    statement.getId(),
                    theme,
                    topic,
                    exception
            );
        }
    }
}