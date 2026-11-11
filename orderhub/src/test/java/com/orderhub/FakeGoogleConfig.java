package com.orderhub;

import com.orderhub.auth.Google.GoogleIdentity;
import com.orderhub.auth.Google.GoogleTokenVerifier;
import com.orderhub.common.exception.InvalidGoogleTokenException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
class FakeGoogleConfig {
    /** Test "tokens" look like:  fake|subject|email|verified  */
    @Bean
    @Primary
    GoogleTokenVerifier fakeGoogleVerifier() {
        return token -> {
            String[] p = token.split("\\|");
            if (p.length != 4 || !p[0].equals("fake")) throw new InvalidGoogleTokenException("Invalid Google token");
            return new GoogleIdentity(p[1], p[2], Boolean.parseBoolean(p[3]));
        };
    }
}

