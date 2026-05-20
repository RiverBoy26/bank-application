package ru.neo.study.dealapi.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.neo.study.dealapi.dto.EmailMessage;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaEmailProducer {

    private final KafkaTemplate<String, EmailMessage> kafkaTemplate;

    public void send(String topic, EmailMessage message) {
        String key = message.getStatementId() == null
                ? null
                : message.getStatementId().toString();

        kafkaTemplate.send(topic, key, message)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Ошибка отправки сообщения в Kafka: topic={}, key={}", topic, key, exception);
                        return;
                    }

                    log.debug("Сообщение отправлено в Kafka: topic={}, key={}", topic, key);
                });
    }
}