package com.docmind.backend.service;

import com.docmind.backend.dto.AuthResponse;
import com.docmind.backend.dto.LoginRequest;
import com.docmind.backend.dto.RegisterRequest;
import com.docmind.backend.dto.UserResponse;
import com.docmind.backend.model.User;
import com.docmind.backend.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public UserResponse register(RegisterRequest request) {

        String email = request.email().strip().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw emailTaken();
        }

        String hash = passwordEncoder.encode(request.password());
        User user = new User(email, hash, request.displayName().strip());

        try {
            return UserResponse.from(userRepository.save(user));
        } catch (DataIntegrityViolationException e) {
            // Two people registered the same email at the same instant.
            // The database's unique rule caught what our check above missed.
            throw emailTaken();
        }
    }

    private ResponseStatusException emailTaken() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email).orElse(null);

        // Same message for "no such email" and "wrong password", so attackers
        // cannot use the login form to discover which emails have accounts.
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return new AuthResponse(
                tokenService.createToken(user),
                tokenService.expirySeconds(),
                UserResponse.from(user));
    }

    public UserResponse getUser(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User no longer exists"));
    }
}