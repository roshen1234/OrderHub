package com.orderhub.auth.Google;

public record GoogleIdentity(String subject, String email, boolean emailVerified) {}