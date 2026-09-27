package com.billpay.exception;

import org.springframework.http.HttpStatus;

public class DailyLimitExceededException extends BusinessException {
    public DailyLimitExceededException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "Límite diario excedido");
    }
}
