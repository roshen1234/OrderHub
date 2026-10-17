package com.orderhub.common.exception;

public class GatewayUnavailableException extends RuntimeException {
    public GatewayUnavailableException() { super("Payment gateway is unavailable, please retry"); }
}
