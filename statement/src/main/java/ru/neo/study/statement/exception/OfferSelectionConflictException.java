package ru.neo.study.statement.exception;

import org.springframework.http.HttpStatus;

public class OfferSelectionConflictException extends ApiException {

    private static final String CODE = "STATEMENT_OFFER_SELECTION_CONFLICT";

    public OfferSelectionConflictException(String message) {
        super(HttpStatus.CONFLICT, CODE, message);
    }
}