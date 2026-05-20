package ru.neo.study.dossier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.neo.study.dossier.enums.Theme;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailMessage {
    @NotNull(message = "Email клиента не должен быть пустым")
    @NotBlank(message = "Email клиента не должен быть пустым")
    private String address;

    @NotNull(message = "Тема письма не должна быть null")
    private Theme theme;

    @NotNull(message = "statementId не должен быть null")
    private UUID statementId;

    private String text;
}
