package com.billpay.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BusinessException {
    public DuplicateResourceException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "Recurso duplicado");
    }
}
