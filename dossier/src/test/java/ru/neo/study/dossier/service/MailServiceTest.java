package ru.neo.study.dossier.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import ru.neo.study.dossier.mapper.SimpleMailMessageMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private SimpleMailMessageMapper simpleMailMessageMapper;

    @InjectMocks
    private MailService mailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailService, "from", "no-reply@bank.local");
    }

    @Test
    void sendShouldCreateAndSendEmail() {
        SimpleMailMessage mailMessage = new SimpleMailMessage();

        when(simpleMailMessageMapper.toEntity(
                "no-reply@bank.local",
                "client@mail.ru",
                "credit-issued",
                "Кредит успешно выдан"
        )).thenReturn(mailMessage);

        mailService.send(
                "client@mail.ru",
                "credit-issued",
                "Кредит успешно выдан"
        );

        assertThat(mailMessage.getFrom()).isEqualTo("no-reply@bank.local");
        assertThat(mailMessage.getTo()).containsExactly("client@mail.ru");
        assertThat(mailMessage.getSubject()).isEqualTo("credit-issued");
        assertThat(mailMessage.getText()).isEqualTo("Кредит успешно выдан");

        verify(javaMailSender).send(mailMessage);
    }

    @Test
    void sendShouldThrowExceptionWhenJavaMailSenderFails() {
        SimpleMailMessage mailMessage = new SimpleMailMessage();

        when(simpleMailMessageMapper.toEntity(
                "no-reply@bank.local",
                "client@mail.ru",
                "send-ses",
                "Код подписания документов: 1234"
        )).thenReturn(mailMessage);

        doThrow(new MailSendException("SMTP error"))
                .when(javaMailSender)
                .send(mailMessage);

        assertThatThrownBy(() -> mailService.send(
                "client@mail.ru",
                "send-ses",
                "Код подписания документов: 1234"
        ))
                .isInstanceOf(MailSendException.class)
                .hasMessageContaining("SMTP error");
    }
}