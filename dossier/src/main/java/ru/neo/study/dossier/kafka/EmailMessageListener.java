package ru.neo.study.dossier.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.neo.study.dossier.dto.EmailMessage;
import ru.neo.study.dossier.service.DossierService;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailMessageListener {

    private final DossierService dossierService;

    @KafkaListener(
            topics = {
                    "${app.kafka.topics.finish-registration}",
                    "${app.kafka.topics.create-documents}",
                    "${app.kafka.topics.send-documents}",
                    "${app.kafka.topics.send-ses}",
                    "${app.kafka.topics.credit-issued}",
                    "${app.kafka.topics.statement-denied}"
            },
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void listen(
            @Payload EmailMessage message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        log.debug(
                "Получено сообщение из Kafka: topic={}, statementId={}, theme={}",
                topic,
                message.getStatementId(),
                message.getTheme()
        );

        dossierService.process(message);

        log.debug(
                "Сообщение успешно обработано: topic={}, statementId={}, theme={}",
                topic,
                message.getStatementId(),
                message.getTheme()
        );
    }
}