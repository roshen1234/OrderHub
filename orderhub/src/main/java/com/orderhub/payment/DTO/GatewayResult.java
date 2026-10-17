package com.orderhub.payment.DTO;


public record GatewayResult(boolean success, String providerRef) {}
