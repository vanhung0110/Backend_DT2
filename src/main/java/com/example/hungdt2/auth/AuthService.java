package com.example.hungdt2.auth;

import com.example.hungdt2.auth.dto.LoginRequest;
import com.example.hungdt2.auth.dto.LoginResponse;
import com.example.hungdt2.auth.dto.RegisterRequest;
import com.example.hungdt2.auth.dto.RegisterResponse;
import com.example.hungdt2.exceptions.ConflictException;
import com.example.hungdt2.exceptions.ForbiddenException;
import com.example.hungdt2.exceptions.UnauthorizedException;
import com.example.hungdt2.user.entity.UserEntity;
import com.example.hungdt2.user.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username()))
            throw new ConflictException("USER_CONFLICT", "Username already exists");
        if (userRepository.existsByEmail(req.email()))
            throw new ConflictException("USER_CONFLICT", "Email already exists");
        if (req.phone() != null && userRepository.existsByPhone(req.phone()))
            throw new ConflictException("USER_CONFLICT", "Phone already exists");

        UserEntity user = new UserEntity();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setPhone(req.phone());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setDisplayName(req.displayName());
        user.setIsActive(true);

        UserEntity saved = userRepository.save(user);
        return new RegisterResponse(saved.getId(), saved.getUsername(), saved.getEmail(), saved.getPhone(), saved.getDisplayName(), saved.getIsActive());
    }

    public LoginResponse login(LoginRequest req) {
        Optional<UserEntity> opt = userRepository.findFirstByUsernameOrEmailOrPhone(req.identifier(), req.identifier(), req.identifier());
        if (opt.isEmpty()) throw new UnauthorizedException("INVALID_CREDENTIALS", "Invalid credentials");
        UserEntity user = opt.get();
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) throw new UnauthorizedException("INVALID_CREDENTIALS", "Invalid credentials");
        if (user.getIsActive() != null && !user.getIsActive()) throw new ForbiddenException("USER_INACTIVE", "User account is inactive");
        String token = jwtService.generateToken(user.getId());
        long expiresInSeconds = jwtService.parseToken(token).getBody().getExpiration().getTime() / 1000L - System.currentTimeMillis() / 1000L;
        return new LoginResponse(token, "Bearer", expiresInSeconds);
    }
}
