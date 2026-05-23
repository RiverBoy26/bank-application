package ru.neo.study.dossier.service;

import ru.neo.study.dossier.dto.EmailMessage;

public interface DossierService {
    void process(EmailMessage message);
}
