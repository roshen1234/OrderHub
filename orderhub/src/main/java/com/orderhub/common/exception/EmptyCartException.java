package com.orderhub.common.exception;

public class EmptyCartException extends RuntimeException {
    public EmptyCartException(String message) {

        super(message);
    }
}
