package com.orderhub.auth;

import com.orderhub.auth.dto.LoginRequest;
import com.orderhub.auth.dto.RegisterRequest;
import com.orderhub.auth.dto.RegisterResponse;
import com.orderhub.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    public final AuthService authService;

//    @PostMapping("/login")
//    public void login(@RequestBody LoginRequest request)
//    {
//       String email=request.getEmail();
//       String password=request.getPassword();
//
//
//    }

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
