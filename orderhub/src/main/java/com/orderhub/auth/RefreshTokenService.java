package com.orderhub.auth;


import com.orderhub.common.exception.InvalidRefreshTokenException;
import com.orderhub.user.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repo;
    private final JwtService jwtService;
    private final SecureRandom random = new SecureRandom();

    @Value("${orderhub.jwt.refresh-token-days}")
    private long refreshDays;

    public record TokenPair(String accessToken, String refreshToken) {}

    /** Creates a token, saves only its hash, returns the raw value for the client. */
    public String create(User user) {
        byte[] bytes = new byte[32];                       // 256 bits of randomness
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        repo.save(new RefreshToken(user, hash(raw), LocalDateTime.now().plusDays(refreshDays)));
        return raw;
    }

    /** Each refresh token works exactly once: it is revoked and a new one is issued. */
    @org.springframework.transaction.annotation.Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public TokenPair rotate(String raw) {
        RefreshToken stored = repo.findByTokenHash(hash(raw))
                .orElseThrow(InvalidRefreshTokenException::new);
        User user = stored.getUser();

        if (stored.isRevoked()) {                          // an old token came back: possible theft
            repo.revokeAllForUser(user.getId());
            throw new InvalidRefreshTokenException();
        }
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException();
        }
        if (repo.revokeIfActive(stored.getId()) == 0) {    // lost a race with a parallel request
            throw new InvalidRefreshTokenException();
        }

        String newRaw = create(user);
        String access = jwtService.generateAccessToken(
                user.getEmail(), List.of("ROLE_" + user.getRole().name()));
        return new TokenPair(access, newRaw);
    }

    @Transactional
    public void revoke(String raw) {
        repo.findByTokenHash(hash(raw)).ifPresent(t -> repo.revokeIfActive(t.getId()));
    }

    private String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}