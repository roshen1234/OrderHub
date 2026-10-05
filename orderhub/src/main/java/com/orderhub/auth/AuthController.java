package com.orderhub.auth;

import com.orderhub.auth.dto.*;
import com.orderhub.user.User;
import com.orderhub.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    public final AuthService authService;
    public final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));

        User user = userRepository.findByEmail(email).orElseThrow();
        String access = jwtService.generateAccessToken(authentication);
        String refresh = refreshTokenService.create(user);

        return ResponseEntity.ok(new TokenResponse(access, refresh, "Bearer", jwtService.getAccessTokenSeconds()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        var pair = refreshTokenService.rotate(request.refreshToken());
        return ResponseEntity.ok(new TokenResponse(pair.accessToken(), pair.refreshToken(),
                "Bearer", jwtService.getAccessTokenSeconds()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
        return ResponseEntity.noContent().build(); //204 (because no content) succeded
    }


    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.createUser(request);
        RegisterResponse body = new RegisterResponse(user.getId(), user.getEmail(), user.getRole().name());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }


    @GetMapping("/")
    public String testLogin() {
        return "BasicAuthSucessfull";
    }


}
