package com.orderhub.auth.Google;

import com.orderhub.auth.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
public class GoogleAuthController {
    private final GoogleAuthService googleAuthService;

    @PostMapping("/googleLogin")
    public TokenResponse login(@Valid @RequestBody GoogleLoginRequest request) {
        return googleAuthService.login(request.idToken());
    }
}
