package ru.neo.study.dossier.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.mail.SimpleMailMessage;

@Mapper(componentModel = "spring")
public interface SimpleMailMessageMapper {

    @Mapping(target = "from", source = "from")
    @Mapping(target = "to", expression = "java(new String[]{to})")
    @Mapping(target = "subject", source = "subject")
    @Mapping(target = "text", source = "body")
    @Mapping(target = "replyTo", ignore = true)
    @Mapping(target = "cc", ignore = true)
    @Mapping(target = "bcc", ignore = true)
    @Mapping(target = "sentDate", ignore = true)
    SimpleMailMessage toEntity(String from, String to, String subject, String body);
}