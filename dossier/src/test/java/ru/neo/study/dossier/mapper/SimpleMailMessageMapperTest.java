package ru.neo.study.dossier.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.mail.SimpleMailMessage;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleMailMessageMapperTest {

    private final SimpleMailMessageMapper mapper = Mappers.getMapper(SimpleMailMessageMapper.class);

    @Test
    void toEntityShouldMapEmailDataToSimpleMailMessage() {
        SimpleMailMessage message = mapper.toEntity(
                "no-reply@bank.local",
                "client@mail.ru",
                "Кредит выдан",
                "Документы подписаны. Кредит успешно выдан."
        );

        assertThat(message).isNotNull();
        assertThat(message.getFrom()).isEqualTo("no-reply@bank.local");
        assertThat(message.getTo()).containsExactly("client@mail.ru");
        assertThat(message.getSubject()).isEqualTo("Кредит выдан");
        assertThat(message.getText()).isEqualTo("Документы подписаны. Кредит успешно выдан.");
    }
}