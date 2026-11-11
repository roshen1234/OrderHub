package com.orderhub.auth.Google;

import com.orderhub.auth.JwtService;
import com.orderhub.auth.RefreshTokenService;
import com.orderhub.auth.dto.TokenResponse;
import com.orderhub.common.exception.ConflictException;
import com.orderhub.common.exception.InvalidGoogleTokenException;
import com.orderhub.user.Role;
import com.orderhub.user.User;
import com.orderhub.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private static final String PROVIDER = "GOOGLE";

    private final GoogleTokenVerifier verifier;
    private final ExternalIdentityRepository identityRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public TokenResponse login(String idToken) {
        GoogleIdentity google = verifier.verify(idToken);
        if (!google.emailVerified() || google.email() == null || google.subject() == null) {
            throw new InvalidGoogleTokenException("Google did not confirm this email address");
        }

        User user = identityRepository.findByProviderAndSubject(PROVIDER, google.subject())
                .map(ExternalIdentity::getUser)          // a returning Google user
                .orElseGet(() -> createUser(google));    // a first-time Google user

        return new TokenResponse(
                jwtService.generateAccessToken(user.getEmail(), List.of("ROLE_" + user.getRole().name())),
                refreshTokenService.create(user),
                "Bearer",
                jwtService.getAccessTokenSeconds());
    }

    private User createUser(GoogleIdentity google) {
        String email = google.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            // Deliberately NOT linked automatically. See the note below.
            throw new ConflictException("An account with this email already exists. Please sign in with your password.");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(randomSecret()));   // nobody knows it, so password login is impossible
        user.setRole(Role.CUSTOMER);                                    // always CUSTOMER; Google never decides roles
        userRepository.save(user);

        ExternalIdentity identity = new ExternalIdentity();
        identity.setUser(user);
        identity.setProvider(PROVIDER);
        identity.setSubject(google.subject());
        identityRepository.save(identity);
        return user;
    }

    private String randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
