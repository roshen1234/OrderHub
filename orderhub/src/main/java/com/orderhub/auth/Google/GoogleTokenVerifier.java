package com.orderhub.auth.Google;

public interface GoogleTokenVerifier {
    GoogleIdentity verify(String idToken);          // throws InvalidGoogleTokenException
}
