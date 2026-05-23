package ru.neo.study.dossier.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.neo.study.dossier.enums.Theme;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailMessage {
    private String address;

    private Theme theme;

    private UUID statementId;

    private String text;
}
