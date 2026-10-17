package com.orderhub.payment.paymentGateway;

public record WebhookEvent(String eventId, WebhookEventType type, String providerRef){}
