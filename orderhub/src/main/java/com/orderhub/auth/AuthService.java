package com.orderhub.auth;

import com.orderhub.auth.dto.RegisterRequest;
import com.orderhub.common.exception.EmailAlreadyExistsException;
import com.orderhub.user.Role;
import com.orderhub.user.User;
import com.orderhub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    public final UserRepository userRepository;
    public final PasswordEncoder passwordEncoder;

    public User createUser(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.CUSTOMER);
        return userRepository.save(user);
    }



}
