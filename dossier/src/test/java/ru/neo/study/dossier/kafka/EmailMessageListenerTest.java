package ru.neo.study.dossier.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.neo.study.dossier.dto.EmailMessage;
import ru.neo.study.dossier.enums.Theme;
import ru.neo.study.dossier.service.DossierService;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailMessageListenerTest {

    @Mock
    private DossierService dossierService;

    @InjectMocks
    private EmailMessageListener emailMessageListener;

    @Test
    void listenShouldProcessKafkaMessage() {
        EmailMessage message = new EmailMessage(
                "client@mail.ru",
                Theme.CREATE_DOCUMENTS,
                UUID.randomUUID(),
                "Кредит одобрен. Документы по заявке будут сформированы."
        );

        emailMessageListener.listen(message, "create-documents");

        verify(dossierService).process(message);
    }
}