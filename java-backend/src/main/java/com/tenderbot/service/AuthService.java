package com.tenderbot.service;

import com.tenderbot.dto.*;
import com.tenderbot.entity.User;
import com.tenderbot.entity.UserRole;
import com.tenderbot.entity.UserStatus;
import com.tenderbot.repository.UserRepository;
import com.tenderbot.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // In-memory refresh token blacklist
    private final Set<String> revokedRefreshTokens = ConcurrentHashMap.newKeySet();

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
            User user = ((com.tenderbot.security.UserDetailsImpl) authentication.getPrincipal()).getUser();
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new BadCredentialsException("Account is blocked");
            }
            String accessToken = jwtUtil.generateAccessToken(user.getEmail());
            String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
            log.info("User logged in: {}", user.getEmail());
            return new AuthResponse(accessToken, refreshToken, toUserDto(user));
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for email: {}", request.email());
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());
        String accessToken = jwtUtil.generateAccessToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        return new AuthResponse(accessToken, refreshToken, toUserDto(user));
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtUtil.isTokenValid(refreshToken) || revokedRefreshTokens.contains(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }
        String email = jwtUtil.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException("Account is blocked");
        }
        String newAccessToken = jwtUtil.generateAccessToken(email);
        String newRefreshToken = jwtUtil.generateRefreshToken(email);
        revokedRefreshTokens.add(refreshToken);
        log.info("Token refreshed for user: {}", email);
        return new AuthResponse(newAccessToken, newRefreshToken, toUserDto(user));
    }

    public void logout(String refreshToken) {
        if (refreshToken != null && jwtUtil.isTokenValid(refreshToken)) {
            revokedRefreshTokens.add(refreshToken);
            log.info("User logged out, refresh token invalidated");
        }
    }

    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        return toUserDto(user);
    }

    private UserDto toUserDto(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getStatus());
    }
}
