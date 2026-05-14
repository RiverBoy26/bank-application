package ru.neo.study.statement.exception;

import org.springframework.http.HttpStatus;

public class OffersNotFoundException extends ApiException {

    private static final String CODE = "STATEMENT_OFFERS_NOT_FOUND";
    private static final String MESSAGE = "Кредитные предложения не найдены";

    public OffersNotFoundException() {
        super(HttpStatus.NOT_FOUND, CODE, MESSAGE);
    }

    public OffersNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, CODE, message);
    }
}