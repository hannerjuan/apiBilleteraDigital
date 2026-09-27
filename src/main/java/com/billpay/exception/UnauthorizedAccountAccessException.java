package com.billpay.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedAccountAccessException extends BusinessException {
    public UnauthorizedAccountAccessException(String message) {
        super(message, HttpStatus.FORBIDDEN, "Acceso no autorizado");
    }
}
