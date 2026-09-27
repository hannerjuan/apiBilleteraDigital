package com.billpay.exception;

import org.springframework.http.HttpStatus;

public class InactiveAccountException extends BusinessException {
    public InactiveAccountException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "Cuenta no activa");
    }
}
