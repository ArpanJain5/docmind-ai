package com.docmind.backend.service;

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

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
}