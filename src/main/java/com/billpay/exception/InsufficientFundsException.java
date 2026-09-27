package com.billpay.exception;

import org.springframework.http.HttpStatus;

public class InsufficientFundsException extends BusinessException {
    public InsufficientFundsException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "Fondos insuficientes");
    }
}
