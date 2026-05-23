package ru.neo.study.dossier.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ru.neo.study.dossier.mapper.SimpleMailMessageMapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender javaMailSender;
    private final SimpleMailMessageMapper simpleMailMessageMapper;

    @Value("${app.mail.from:no-reply@bank.local}")
    private String from;

    public void send(String to, String subject, String body) {
        SimpleMailMessage message = simpleMailMessageMapper.toEntity(from, to, subject, body);
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        javaMailSender.send(message);

        log.info("Письмо отправлено: from={}, to={}, subject={}",
                from, to, subject);
    }
}