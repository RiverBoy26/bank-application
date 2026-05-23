package ru.neo.study.dossier.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.neo.study.dossier.dto.EmailMessage;
import ru.neo.study.dossier.enums.Theme;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DossierServiceImplTest {

    @Mock
    private MailService mailService;

    @InjectMocks
    private DossierServiceImpl dossierService;

    @Test
    void processShouldBuildMessageAndSendEmail() {
        UUID statementId = UUID.randomUUID();

        EmailMessage message = new EmailMessage(
                "client@mail.ru",
                Theme.CREATE_DOCUMENTS,
                statementId,
                "Кредит одобрен. Документы по заявке будут сформированы."
        );

        dossierService.process(message);

        verify(mailService).send(
                "client@mail.ru",
                "create-documents",
                "Заявка: " + statementId + "\n\nКредит одобрен. Документы по заявке будут сформированы."
        );
    }

    @Test
    void processShouldPropagateExceptionWhenMailServiceFails() {
        UUID statementId = UUID.randomUUID();

        EmailMessage message = new EmailMessage(
                "client@mail.ru",
                Theme.SEND_SES,
                statementId,
                "Код подписания документов: 1234"
        );

        doThrow(new IllegalStateException("Mail error"))
                .when(mailService)
                .send(
                        "client@mail.ru",
                        "send-ses",
                        "Заявка: " + statementId + "\n\nКод подписания документов: 1234"
                );

        assertThatThrownBy(() -> dossierService.process(message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Mail error");
    }
}