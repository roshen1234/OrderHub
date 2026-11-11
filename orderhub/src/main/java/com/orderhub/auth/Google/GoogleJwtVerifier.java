package com.orderhub.auth.Google;

import com.orderhub.common.exception.InvalidGoogleTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

@Component
public class GoogleJwtVerifier implements GoogleTokenVerifier {

    private final JwtDecoder decoder;
    private final boolean configured;

    public GoogleJwtVerifier(@Value("${orderhub.google.client-id}") String clientId) {
        this.configured = !clientId.isBlank();

        // Downloads Google's public keys and checks the token's signature with them.
        NimbusJwtDecoder nimbus = NimbusJwtDecoder
                .withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build();

        OAuth2TokenValidator<Jwt> issuer = jwt -> {
            String iss = jwt.getClaimAsString("iss");
            return "https://accounts.google.com".equals(iss) || "accounts.google.com".equals(iss)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Wrong issuer", null));
        };
        OAuth2TokenValidator<Jwt> audience = jwt ->
                jwt.getAudience() != null && jwt.getAudience().contains(clientId)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Wrong audience", null));

        // expiry + issuer + audience, on top of the signature check
        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), issuer, audience));
        this.decoder = nimbus;
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        if (!configured) throw new InvalidGoogleTokenException("Google sign-in is not configured");
        try {
            Jwt jwt = decoder.decode(idToken);
            return new GoogleIdentity(jwt.getSubject(), jwt.getClaimAsString("email"),
                    Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")));
        } catch (JwtException e) {
            throw new InvalidGoogleTokenException("Invalid Google token");
        }
    }
}
