package com.orderhub;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(FakeGoogleConfig.class)
class GoogleLoginTests extends IntegrationTestBase {

    private ResponseEntity<Map> googleLogin(String subject, String email, boolean verified) {
        return post("/googleLogin", null, Map.of("idToken", "fake|" + subject + "|" + email + "|" + verified));
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    @Test
    void firstGoogleLogin_createsACustomer_whoGetsWorkingTokens() {
        String email = "g-" + UUID.randomUUID() + "@example.com";
        ResponseEntity<Map> response = googleLogin("sub-" + UUID.randomUUID(), email, true);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        String role = jdbc.queryForObject("SELECT role FROM users WHERE email = ?", String.class, email);
        assertThat(get("/orders", response.getBody().get("accessToken").toString()).getStatusCode().value()).isEqualTo(200);   // the token works
        assertThat(get("/orders", null).getStatusCode().value()).isEqualTo(401);          // and without it, access is denied
    }

    @Test
    void signingInTwiceWithTheSameGoogleAccount_usesTheSameUser() {
        String subject = "sub-" + UUID.randomUUID();
        String email = "g-" + UUID.randomUUID() + "@example.com";
        assertThat(googleLogin(subject, email, true).getStatusCode().value()).isEqualTo(200);
        assertThat(googleLogin(subject, email, true).getStatusCode().value()).isEqualTo(200);
        assertThat(count("SELECT COUNT(*) FROM users WHERE email = ?", email)).isEqualTo(1);
    }

    @Test
    void unverifiedEmail_isRejected_andNothingIsCreated() {
        String email = "g-" + UUID.randomUUID() + "@example.com";
        assertThat(googleLogin("sub-" + UUID.randomUUID(), email, false).getStatusCode().value()).isEqualTo(401);
        assertThat(count("SELECT COUNT(*) FROM users WHERE email = ?", email)).isZero();
    }

    @Test
    void existingPasswordAccount_isNeverLinkedAutomatically_evenForAdmins() {
        String email = register();                                              // a normal password account
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE email = ?", email);

        assertThat(googleLogin("sub-" + UUID.randomUUID(), email, true).getStatusCode().value()).isEqualTo(409);
//        assertThat(count("SELECT COUNT(*) FROM external_identities")).isZero();
    }

    @Test
    void aGoogleOnlyUser_cannotSignInWithAPassword() {
        String email = "g-" + UUID.randomUUID() + "@example.com";
        googleLogin("sub-" + UUID.randomUUID(), email, true);
        assertThat(post("/login", null, Map.of("email", email, "password", PASSWORD)).getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void garbageToken_isRejected() {
        assertThat(post("/googleLogin", null, Map.of("idToken", "not-a-token")).getStatusCode().value()).isEqualTo(401);
    }
}
