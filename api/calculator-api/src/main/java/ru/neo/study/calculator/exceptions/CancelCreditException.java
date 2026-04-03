package ru.neo.study.calculator.exceptions;

public class CancelCreditException extends RuntimeException {
    public CancelCreditException(String message) {
        super(message);
    }
}
