package com.tenderbot.service;

import com.tenderbot.dto.CreateUserRequest;
import com.tenderbot.dto.UpdateUserRequest;
import com.tenderbot.dto.UserDto;
import com.tenderbot.entity.User;
import com.tenderbot.entity.UserRole;
import com.tenderbot.entity.UserStatus;
import com.tenderbot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserManagementService {

    private static final Logger log = LoggerFactory.getLogger(UserManagementService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toDto(user);
    }

    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(request.role() != null ? request.role() : UserRole.USER);
        user.setStatus(request.status() != null ? request.status() : UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("Admin created user: {}", user.getEmail());
        return toDto(user);
    }

    public UserDto updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        if (request.role() != null) user.setRole(request.role());
        if (request.status() != null) user.setStatus(request.status());
        userRepository.save(user);
        log.info("Admin updated user: {}", user.getEmail());
        return toDto(user);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.isSystem()) {
            throw new IllegalArgumentException("Cannot delete system user");
        }
        userRepository.delete(user);
        log.info("Admin deleted user: {}", user.getEmail());
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getStatus());
    }
}
