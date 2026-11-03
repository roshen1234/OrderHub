package com.orderhub.common.exception;

public class ObjectOptimisticLockingFailureException extends RuntimeException {

    public ObjectOptimisticLockingFailureException(String message) {

        super(message);
    }
}
